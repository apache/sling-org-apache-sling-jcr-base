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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = ConfigurationListener.class)
public class ConfigurationUpdaterEventListener implements ConfigurationListener {

    static final String LOGIN_ADMIN_WHITELIST_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist";
    static final String LOGIN_ADMIN_ALLOWLIST_PID = "org.apache.sling.jcr.base.internal.LoginAdminAllowList";
    private static final Map<String, String> LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE = new HashMap<>();
    static {
        LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE.put("whitelist.bypass", "allowlist.bypass");
        LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE.put("whitelist.bundles.regexp", "allowlist.bundles.regexp");
    }
    private static final String ALLOWLIST_FRAGMENT_PID = "org.apache.sling.jcr.base.internal.LoginAdminAllowList.fragment";
    private static final String WHITELIST_FRAGMENT_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment";
    private static final Map<String, String> FRAGMENT_PROPS_TO_REPLACE = new HashMap<>();

    static {
        FRAGMENT_PROPS_TO_REPLACE.put("whitelist.name", "allowlist.name");
        FRAGMENT_PROPS_TO_REPLACE.put("whitelist.bundles", "allowlist.bundles");
    }

    private final List<ConfigurationUpdater> configurationUpdaterList;

    private final ConfigurationAdmin configurationAdmin;

    @Activate
    public ConfigurationUpdaterEventListener(@Reference ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
        configurationUpdaterList = new ArrayList<>();
        configurationUpdaterList.add(ConfigurationUpdater.forPid(LOGIN_ADMIN_WHITELIST_PID, LOGIN_ADMIN_ALLOWLIST_PID, LOGIN_ADMIN_WHITELIST_PROPS_TO_REPLACE));
        configurationUpdaterList.add(ConfigurationUpdater.forFactoryPid(WHITELIST_FRAGMENT_PID, ALLOWLIST_FRAGMENT_PID, FRAGMENT_PROPS_TO_REPLACE));

        configurationUpdaterList.forEach(configurationUpdater -> configurationUpdater.updateProps(this.configurationAdmin));
    }

    @Override
    public void configurationEvent(final ConfigurationEvent event) {
        if ( event.getType() == ConfigurationEvent.CM_UPDATED ) {
            configurationUpdaterList.forEach(configurationUpdater -> {
                configurationUpdater.updateProps(configurationAdmin, event);
            });
        }
    }
}
