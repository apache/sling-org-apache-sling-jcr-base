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
import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = ConfigurationUpdater.class, immediate = true)
public class ConfigurationUpdater {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationUpdater.class);

    private final ConfigurationAdmin configurationAdmin;

    @Activate
    public ConfigurationUpdater(@Reference ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public void updateProps(Map<String, String> propsToReplace, String targetConfigPid, String sourceConfigPid) {
        try {
            Configuration sourceConfiguration = configurationAdmin.getConfiguration(sourceConfigPid, null);
            updateProps(propsToReplace, targetConfigPid, sourceConfiguration);
        } catch (IOException e) {
            LOG.warn("Failed to retrieve configuration for PID: {}. PID's {} configuration is not updated.",
                sourceConfigPid, targetConfigPid, e);
        }
    }

    public void updatePropsForFactoryPid(Map<String, String> propsToReplace, String targetConfigFactoryPid, String sourceConfigFactoryPid) {
        final String pidFactoryFilter = MessageFormat.format("(service.factoryPid={0})", sourceConfigFactoryPid);
        try {
            Configuration[] sourceConfigs = configurationAdmin.listConfigurations(pidFactoryFilter);
            for (Configuration sourceConfig : sourceConfigs) {
                String targetConfigPid = sourceConfig.getPid().replace(sourceConfigFactoryPid, targetConfigFactoryPid);
                updateProps(propsToReplace, targetConfigPid, sourceConfig);
            }
        } catch (IOException | InvalidSyntaxException e) {
            LOG.warn("Failed to list configurations for filter: {}.", pidFactoryFilter, e);
        }
    }

    private void updateProps(Map<String, String> propsToReplace, String targetConfigPid, Configuration sourceConfiguration) {
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
                LOG.warn("Deleting configuration for PID: {}", sourceConfiguration.getPid());
                sourceConfiguration.delete();
            } catch (IOException e) {
                LOG.warn("Failed to update configuration for PID: {} from source PID: {}", targetConfigPid,
                    sourceConfiguration.getPid(), e);
            }
        }
    }
}
