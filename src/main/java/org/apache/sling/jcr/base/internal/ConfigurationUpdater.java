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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.function.Function;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ConfigurationUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUpdater.class);

    protected final String oldPid;
    protected final String newPid;
    protected final Function<ConfigurationEvent, String> pidMapper;
    protected final Map<String, String> propsToReplace;

    public ConfigurationUpdater(String oldPid, String newPid, Function<ConfigurationEvent, String> pidMapper, Map<String, String> propsToReplace) {
        this.oldPid = oldPid;
        this.newPid = newPid;
        this.pidMapper = pidMapper;
        this.propsToReplace = propsToReplace;
    }

    public static ConfigurationUpdater forPid(String oldPid, String newPid, Map<String, String> propsToReplace) {
        return new PidConfigurationUpdater(oldPid, newPid, ConfigurationEvent::getPid, propsToReplace);
    }

    public static ConfigurationUpdater forFactoryPid(String oldPid, String newPid, Map<String, String> propsToReplace) {
        return new FactoryPidConfigurationUpdater(oldPid, newPid, ConfigurationEvent::getFactoryPid, propsToReplace);
    }

    public boolean canHandle(ConfigurationEvent event) {
       return oldPid.equals(pidMapper.apply(event));
    }

    public abstract void updateProps(ConfigurationAdmin configurationAdmin);

    private static class PidConfigurationUpdater extends ConfigurationUpdater {
        public PidConfigurationUpdater(String oldPid, String newPid, Function<ConfigurationEvent, String> pidMapper, Map<String, String> propsToReplace) {
            super(oldPid, newPid, pidMapper, propsToReplace);
        }

        public void updateProps(ConfigurationAdmin configurationAdmin) {
            try {
                Configuration sourceConfiguration = configurationAdmin.getConfiguration(oldPid, null);
                updateProps(propsToReplace, newPid, sourceConfiguration, configurationAdmin);
            } catch (IOException e) {
                LOG.warn("Failed to retrieve configuration for PID: {}. PID's {} configuration is not updated.",
                    oldPid, newPid, e);
            }
        }
    }

    private static class FactoryPidConfigurationUpdater extends ConfigurationUpdater {

        public FactoryPidConfigurationUpdater(String oldPid, String newPid, Function<ConfigurationEvent, String> pidMapper, Map<String, String> propsToReplace) {
            super(oldPid, newPid, pidMapper, propsToReplace);
        }

        @Override
        public void updateProps(ConfigurationAdmin configurationAdmin) {
            final String pidFactoryFilter = String.format("(service.factoryPid=%s)", oldPid);
            try {
                Configuration[] sourceConfigs = configurationAdmin.listConfigurations(pidFactoryFilter);
                for (Configuration sourceConfig : sourceConfigs) {
                    String targetConfigPid = sourceConfig.getPid().replace(oldPid, newPid);
                    updateProps(propsToReplace, targetConfigPid, sourceConfig, configurationAdmin);
                }
            } catch (IOException | InvalidSyntaxException e) {
                LOG.warn("Failed to list configurations for filter: {}.", pidFactoryFilter, e);
            }
        }
    }

    protected void updateProps(Map<String, String> propsToReplace, String targetConfigPid, Configuration sourceConfiguration, ConfigurationAdmin configurationAdmin) {
        final Dictionary<String, Object> targetProperties = new Hashtable<>();
        propsToReplace.forEach((oldKey, newKey) -> {
            final Dictionary<String, Object> sourceProperties;
            sourceProperties = sourceConfiguration.getProperties();
            final Object propValue = sourceProperties != null ? sourceProperties.get(oldKey) : null;
            if (propValue != null) {
                LOG.debug("Received configuration value: {} for old key: {}. Setting the new property {} to {}",
                    propValue, oldKey, newKey, propValue);
                targetProperties.put(newKey, propValue);
            }
        });
        if (!targetProperties.isEmpty()) {
            try {
                LOG.warn("Updating configuration for PID: {} with configuration from source PID: {}", targetConfigPid,
                    sourceConfiguration.getPid());
                configurationAdmin.getConfiguration(targetConfigPid, null).update(targetProperties);
                LOG.warn("Deleting configuration for PID: {} after it was migrated", sourceConfiguration.getPid());
                sourceConfiguration.delete();
            } catch (IOException e) {
                LOG.warn("Failed to update configuration for PID: {} from source PID: {}", targetConfigPid,
                    sourceConfiguration.getPid(), e);
            }
        }
    }
}
