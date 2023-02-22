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
        LOG.debug("Injecting {} into ConfigurationUpdater", configurationAdmin);
        this.configurationAdmin = configurationAdmin;
    }

    public void updateProps(Map<String, String> propsToReplace, String targetConfigPid, String sourceConfigPid) {
        final Dictionary<String, Object> targetProperties = new Hashtable<>();

        propsToReplace.forEach((oldKey, newKey) -> {
            final Dictionary<String, Object> sourceProperties;
            try {
                sourceProperties = configurationAdmin.getConfiguration(sourceConfigPid, null).getProperties();
                final Object propValue = sourceProperties != null ? sourceProperties.get(oldKey) : null;
                if (propValue != null) {
                    LOG.debug("Received configuration value: {} for old key: {}. Setting the new property {} to {}",
                        propValue, oldKey, newKey, propValue);
                    targetProperties.put(newKey, propValue);
                }
            } catch (IOException e) {
                LOG.warn("Failed to retrieve configuration for PID: {}. PID's {} configuration is not updated.",
                    sourceConfigPid, targetConfigPid, e);
            }
        });
        if (!targetProperties.isEmpty()) {
            try {
                configurationAdmin.getConfiguration(targetConfigPid, null).update(targetProperties);
            } catch (IOException e) {
                LOG.warn("Failed to update configuration for PID: {}", targetConfigPid, e);
            }
        }
    }
}
