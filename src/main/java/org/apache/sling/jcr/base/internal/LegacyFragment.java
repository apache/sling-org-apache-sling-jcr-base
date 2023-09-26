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

/**
 * Legacy fragment configuration. Use {@link AllowListFragment} instead.
 */
@Component(
        configurationPid = LegacyFragment.LEGACY_FACTORY_PID,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AllowListFragment.class
)
public class LegacyFragment extends AllowListFragment {

    public static final String LEGACY_FACTORY_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment";

    public @interface Configuration {
        String whitelist_name();

        String[] whitelist_bundles() default {};
    }


    @Activate
    public LegacyFragment(Configuration configuration) {
        super(configuration.whitelist_name(), configuration.whitelist_bundles());
        LoginAdminAllowList.LOG.warn("Using deprecated factory configuration '{}' with whitelist.name='{}'. " +
            "Update your configuration to use configuration '{}' instead.", 
            LEGACY_FACTORY_PID, configuration.whitelist_name(), AllowListFragment.FACTORY_PID);
    }
}