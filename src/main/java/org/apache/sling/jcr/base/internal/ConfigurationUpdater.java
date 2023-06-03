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
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigurationUpdater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final String oldPid;
    protected final String newPid;
    protected final Map<String, String> propsToReplace;

    public ConfigurationUpdater(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
        this.oldPid = oldPid;
        this.newPid = newPid;
        this.propsToReplace = propsToReplace;
    }

    public static ConfigurationUpdater forPid(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
        return new PidConfigurationUpdater(oldPid, newPid, propsToReplace);
    }

    public static ConfigurationUpdater forFactoryPid(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
        return new FactoryPidConfigurationUpdater(oldPid, newPid, propsToReplace);
    }

    public abstract void updateProps(ConfigurationAdmin configurationAdmin, ConfigurationEvent event);

    protected abstract void updateProps(ConfigurationAdmin configurationAdmin);

    protected abstract Configuration createConfiguration(ConfigurationAdmin configurationAdmin, String oldPid) throws IOException;

    protected void updateProps(final Configuration sourceConfig, ConfigurationAdmin configurationAdmin) {
        final Dictionary<String, Object> sourceProps = sourceConfig.getProperties();
        final Dictionary<String, Object> targetProps = new Hashtable<>();
        for(final String name : Collections.list(sourceProps.keys())) {
            final Object value = sourceProps.get(name);
            String newName = this.propsToReplace.get(name);
            if (newName == null) {
                newName = name;
            } else {
                logger.debug("Received configuration value: {} for old key: {}. Setting the new property {} to {}",
                    value, name, newName, value);
            }
            targetProps.put(newName, value);
        }
        if (!targetProps.isEmpty()) {
            try {
                final Configuration cfg = this.createConfiguration(configurationAdmin, sourceConfig.getPid());
                if (cfg==null) return;
                logger.info("Creating new configuration with PID {} for source PID: {}", cfg.getPid(), sourceConfig.getPid());
                cfg.update(targetProps);
                logger.info("Deleting source configuration wuth PID {} after it was migrated", sourceConfig.getPid());
                sourceConfig.delete();
            } catch (final IOException e) {
                logger.warn("Failed to update configuration with PID {}", sourceConfig.getPid(), e);
            }
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
    
    private static class PidConfigurationUpdater extends ConfigurationUpdater {

        public PidConfigurationUpdater(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
            super(oldPid, newPid, propsToReplace);
        }

        @Override
        public void updateProps(final ConfigurationAdmin configurationAdmin, final ConfigurationEvent event) {
            if (this.oldPid.equals(event.getPid())) {
                this.updateProps(configurationAdmin);
            }
        }

        protected Configuration createConfiguration(final ConfigurationAdmin configurationAdmin, final String oldPid) throws IOException {
            return configurationAdmin.getConfiguration(newPid, null);
        }

        @Override
        protected void updateProps(final ConfigurationAdmin configurationAdmin) {
            final String filter = String.format("(%s=%s)", Constants.SERVICE_PID, encode(this.oldPid));
            try {
                final Configuration[] configs = configurationAdmin.listConfigurations(filter);
                if (configs != null && configs.length > 0) {
                    this.updateProps(configs[0], configurationAdmin);
                }
            } catch (final IOException | InvalidSyntaxException e) {
                this.logger.error("Failed to retrieve configuration for PID: {}. Configuration is not updated to PID {}.",
                    oldPid, newPid, e);
            }
        }
    }

    private static class FactoryPidConfigurationUpdater extends ConfigurationUpdater {

        public FactoryPidConfigurationUpdater(final String oldPid, final String newPid, final Map<String, String> propsToReplace) {
            super(oldPid, newPid, propsToReplace);
        }

        @Override
        public void updateProps(final ConfigurationAdmin configurationAdmin, final ConfigurationEvent event) {
            if (this.oldPid.equals(event.getFactoryPid())) {
                this.updateProps(configurationAdmin);
            }
        }

        @Override
        protected void updateProps(final ConfigurationAdmin configurationAdmin) {
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

        protected Configuration createConfiguration(final ConfigurationAdmin configurationAdmin, final String oldFullPid) throws IOException {
            final String prefix = this.oldPid.concat("~");
            if (oldFullPid.startsWith(prefix)) {
                return configurationAdmin.getFactoryConfiguration(newPid, oldFullPid.substring(prefix.length()), null);
            }
            return configurationAdmin.createFactoryConfiguration(newPid, null);
        }
    }
}
