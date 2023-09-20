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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.util.converter.Converters;

import java.util.Map;

/**
 * Legacy fragment configuration. Use {@link AllowListFragment} instead.
 */
@Component(
        configurationPid = LegacyFragment.LEGACY_FACTORY_PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = LegacyFragment.class
)
public class LegacyFragment {

    public static final String LEGACY_FACTORY_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment";

    private static final String LEGACY_NAME = "whitelist.name";
    private static final String LEGACY_BUNDLES = "whitelist.bundles";

    private final AllowListFragment fragment;

    private final LoginAdminAllowList allowList;

    @Activate
    public LegacyFragment(final @Reference LoginAdminAllowList allowList, final Map<String, Object> config) {
        LoginAdminAllowList.LOG.warn("Using deprecated factory configuration '{}'. " +
            "Update your configuration to use configuration '{}' instead.", 
            LEGACY_FACTORY_PID, AllowListFragment.FACTORY_PID);
        this.allowList = allowList;
        final String name = Converters.standardConverter().convert(config.get(LEGACY_NAME)).to(String.class);
        final String[] bundles = Converters.standardConverter().convert(config.get(LEGACY_BUNDLES)).to(String[].class);
        this.fragment = new AllowListFragment(name, bundles);
        this.allowList.bindAllowListFragment(fragment);
    }

    @Deactivate
    public void deactivate() {
        this.allowList.unbindAllowListFragment(fragment);
    }
}
