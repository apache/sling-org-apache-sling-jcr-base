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

import java.util.HashMap;
import java.util.Map;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component
public class LoginAdminAllowlistFragmentConfigurationEventListener implements ConfigurationListener {
    private final ConfigurationUpdater configurationUpdater;
    private static final String ALLOWLIST_FRAGMENT_PID = "org.apache.sling.jcr.base.internal.LoginAdminAllowlist.fragment";
    private static final String WHITELIST_FRAGMENT_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment";
    private static final Map<String, String> FRAGMENT_PROPS_TO_REPLACE = new HashMap<>();

    static {
        FRAGMENT_PROPS_TO_REPLACE.put("whitelist.name", "allowlist.name");
        FRAGMENT_PROPS_TO_REPLACE.put("whitelist.bundles", "allowlist.bundles");
    }
    @Activate
    public LoginAdminAllowlistFragmentConfigurationEventListener(@Reference ConfigurationUpdater configurationUpdater) {
        this.configurationUpdater = configurationUpdater;
        this.configurationUpdater.updatePropsForFactoryPid(FRAGMENT_PROPS_TO_REPLACE, ALLOWLIST_FRAGMENT_PID,
            WHITELIST_FRAGMENT_PID);
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        if (WHITELIST_FRAGMENT_PID.equals(event.getFactoryPid())) {
            configurationUpdater.updateProps(FRAGMENT_PROPS_TO_REPLACE, event.getPid().replace(WHITELIST_FRAGMENT_PID,
                ALLOWLIST_FRAGMENT_PID),  event.getPid());
        }
    }
}
