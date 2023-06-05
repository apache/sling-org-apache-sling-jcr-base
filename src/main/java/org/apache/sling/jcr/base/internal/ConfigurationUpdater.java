/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.base.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is updating configuration and configuration property names to use
 * more inclusive language.
 * See https://issues.apache.org/jira/browse/SLING-11741
 */
@Component(service = {ConfigurationListener.class, ConfigurationUpdater.class})
public class ConfigurationUpdater implements ConfigurationListener {

    static final String LOGIN_ADMIN_WHITELIST_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist";
    static final String LOGIN_ADMIN_ALLOWLIST_PID = LoginAdminAllowList.PID;
    private static final Map<String, String> LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE = new HashMap<>();
    static {
        LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE.put("whitelist.bypass", "allowlist.bypass");
        LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE.put("whitelist.bundles.regexp", "allowlist.bundles.regexp");
    }

    private static final String WHITELIST_FRAGMENT_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment";
    private static final String ALLOWLIST_FRAGMENT_PID = AllowListFragment.FACTORY_PID;
    private static final Map<String, String> FRAGMENT_PROPS_TO_REPLACE = new HashMap<>();

    static {
        FRAGMENT_PROPS_TO_REPLACE.put("whitelist.name", "allowlist.name");
        FRAGMENT_PROPS_TO_REPLACE.put("whitelist.bundles", "allowlist.bundles");
    }

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final List<Updater> configurationUpdaterList = new ArrayList<>();

    private final ConfigurationAdmin configurationAdmin;

    @Activate
    public ConfigurationUpdater(@Reference ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        configurationUpdaterList.add(new PidConfigurationUpdater(LOGIN_ADMIN_WHITELIST_PID, LOGIN_ADMIN_ALLOWLIST_PID, LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE));
        configurationUpdaterList.add(new FactoryPidConfigurationUpdater(WHITELIST_FRAGMENT_PID, ALLOWLIST_FRAGMENT_PID, FRAGMENT_PROPS_TO_REPLACE));

        configurationUpdaterList.forEach(configurationUpdater -> configurationUpdater.updateProps());
    }

    @Override
    public void configurationEvent(final ConfigurationEvent event) {
        if ( event.getType() == ConfigurationEvent.CM_UPDATED ) {
            configurationUpdaterList.forEach(configurationUpdater -> {
                configurationUpdater.updateProps(event);
            });
        }
    }

    /**
     * Encode the value for the ldap filter: \, *, (, and ) should be escaped.
     */
    private static String encode(final String value) {
        return value.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
    
    protected abstract class Updater {

        protected final String oldPid;
        protected final String newPid;
        protected final Map<String, String> propsToReplace;

        public Updater(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
            this.oldPid = oldPid;
            this.newPid = newPid;
            this.propsToReplace = propsToReplace;
        }

        protected abstract void updateProps(ConfigurationEvent event);

        protected abstract void updateProps();

        protected abstract Configuration createConfiguration(String oldPid) throws IOException;

        /**
         * Update a configuration
         */
        protected void updateProps(final Configuration sourceConfig, ConfigurationAdmin configurationAdmin) {
            final Dictionary<String, Object> sourceProps = sourceConfig.getProperties();
            final Dictionary<String, Object> targetProps = new Hashtable<>();
            for(final String name : Collections.list(sourceProps.keys())) {
                targetProps.put(this.propsToReplace.getOrDefault(name, name), sourceProps.get(name));
            }
            try {
                final Configuration cfg = this.createConfiguration(sourceConfig.getPid());
                if (cfg==null) return;
                cfg.update(targetProps);
                sourceConfig.delete();
                logger.info("Updated configuration with PID {} to new configuration with PID {}. "+
                "Please see https://sling.apache.org/documentation/the-sling-engine/service-authentication.html for more information.", 
                sourceConfig.getPid(), cfg.getPid());
            } catch (final IOException e) {
                logger.warn("Failed to update configuration with PID {}", sourceConfig.getPid(), e);
            }
        }
    }

    private class PidConfigurationUpdater extends Updater {

        public PidConfigurationUpdater(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
            super(oldPid, newPid, propsToReplace);
        }

        @Override
        protected void updateProps(final ConfigurationEvent event) {
            if (this.oldPid.equals(event.getPid())) {
                this.updateProps();
            }
        }

        @Override
        protected Configuration createConfiguration(final String oldPid) throws IOException {
            return configurationAdmin.getConfiguration(newPid, null);
        }

        @Override
        protected void updateProps() {
            final String filter = String.format("(%s=%s)", Constants.SERVICE_PID, encode(this.oldPid));
            try {
                final Configuration[] configs = configurationAdmin.listConfigurations(filter);
                if (configs != null && configs.length > 0) {
                    this.updateProps(configs[0], configurationAdmin);
                }
            } catch (final IOException | InvalidSyntaxException e) {
                logger.error("Failed to retrieve configuration for PID: {}. Configuration is not updated to PID {}.",
                    oldPid, newPid, e);
            }
        }
    }

    private class FactoryPidConfigurationUpdater extends Updater {

        public FactoryPidConfigurationUpdater(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
            super(oldPid, newPid, propsToReplace);
        }

        @Override
        protected void updateProps(final ConfigurationEvent event) {
            if (this.oldPid.equals(event.getFactoryPid())) {
                this.updateProps();
            }
        }

        @Override
        protected void updateProps() {
            final String filter = String.format("(%s=%s)", ConfigurationAdmin.SERVICE_FACTORYPID, encode(this.oldPid));
            try {
                final Configuration[] configs = configurationAdmin.listConfigurations(filter);
                if ( configs != null) {
                    for (final Configuration sourceConfig : configs) {
                        this.updateProps(sourceConfig, configurationAdmin);
                    }    
                }
            } catch (final IOException | InvalidSyntaxException e) {
                logger.error("Failed to list configurations for filter: {}.", filter, e);
            }
        }

        @Override
        protected Configuration createConfiguration(final String oldFullPid) throws IOException {
            final String prefix = this.oldPid.concat("~");
            if (oldFullPid.startsWith(prefix)) {
                return configurationAdmin.getFactoryConfiguration(newPid, oldFullPid.substring(prefix.length()), null);
            }
            return configurationAdmin.createFactoryConfiguration(newPid, null);
        }
    }
}
