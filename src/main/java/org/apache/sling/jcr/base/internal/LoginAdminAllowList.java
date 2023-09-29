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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.util.converter.Converters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allow list that defines which bundles can use the
 * {@link SlingRepository#loginAdministrative} method.
 *
 * The default configuration lets a few trusted Sling bundles
 * use the loginAdministrative method.
 */
@Component(service = LoginAdminAllowList.class, 
    configurationPid = {LoginAdminAllowList.PID, LoginAdminAllowList.LEGACY_PID}
)
public class LoginAdminAllowList {

    public static final String PID = "org.apache.sling.jcr.base.LoginAdminAllowList";

    public static final String LEGACY_PID = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist";

    static final Logger LOG = LoggerFactory.getLogger(LoginAdminAllowList.class);

    // for backwards compatibility only (read properties directly to prevent them from appearing in the metatype)
    private static final String LEGACY_BYPASS_PROPERTY = "whitelist.bypass";

    private static final String LEGACY_BUNDLES_PROPERTY = "whitelist.bundles.regexp";

    private static final String PROP_LEGACY_BUNDLES_DEFAULT = "whitelist.bundles.default";

    private static final String PROP_LEGACY_BUNDLES_ADDITIONAL = "whitelist.bundles.additional";

    @SuppressWarnings("java:S3077")
    // java:S3077 - the field is updated and read atomically, and the object is
    // immutable, hence the use of "volatile" is adequate
    private volatile ConfigurationState config;

    private final List<AllowListFragment> allowListFragments = new CopyOnWriteArrayList<>();

    private final Map<String, AllowListFragment> backwardsCompatibleFragments = new ConcurrentHashMap<>();

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    )
    void bindAllowListFragment(AllowListFragment fragment) {
        allowListFragments.add(fragment);
        LOG.info("AllowListFragment added '{}'", fragment);
    }

    void unbindAllowListFragment(AllowListFragment fragment) {
        allowListFragments.remove(fragment);
        LOG.info("AllowListFragment removed '{}'", fragment);
    }

    @Activate @Modified
    void configure(final LoginAdminAllowListConfiguration configuration, final Map<String, Object> properties) {
        this.config = new ConfigurationState(configuration, properties);
        ensureBackwardsCompatibility(properties, PROP_LEGACY_BUNDLES_DEFAULT);
        ensureBackwardsCompatibility(properties, PROP_LEGACY_BUNDLES_ADDITIONAL);
    }

    public boolean allowLoginAdministrative(Bundle b) {
        // create local copy of ConfigurationState to avoid reading mixed configurations during an configure
        final ConfigurationState localConfig = this.config;
        if (localConfig == null) {
            throw new IllegalStateException("LoginAdminAllowList has no configuration.");
        }

        if(localConfig.bypassAllowList) {
            LOG.debug("Allow list is bypassed, all bundles allowed to use loginAdministrative");
            return true;
        }

        final String bsn = b.getSymbolicName();

        if(localConfig.allowListRegexp != null && localConfig.allowListRegexp.matcher(bsn).matches()) {
            LOG.debug("{} is allow listed to use loginAdministrative, by regexp", bsn);
            return true;
        }

        for (final AllowListFragment fragment : allowListFragments) {
            if (fragment.allows(bsn)) {
                LOG.debug("{} is allow listed to use loginAdministrative, by allow list fragment '{}'",
                        bsn, fragment);
                return true;
            }
        }

        LOG.debug("{} is not allow listed to use loginAdministrative", bsn);
        return false;
    }

    // encapsulate configuration state for atomic configuration updates
    static class ConfigurationState {

        public final boolean bypassAllowList;

        public final Pattern allowListRegexp;

        ConfigurationState(final LoginAdminAllowListConfiguration config, final Map<String, Object> properties) {
            // first check for legacy properties
            boolean bypass = config.allowlist_bypass();
            final Object legacyBypassObject = properties.get(LEGACY_BYPASS_PROPERTY);
            if (legacyBypassObject != null) {
                LOG.warn("Using deprecated configuration property '{}' from configuration '{}'. " +
                    "Update your configuration to use configuration '{}' and property '{}' instead.", 
                    LEGACY_BYPASS_PROPERTY, LEGACY_PID, PID, "allowlist.bypass");
                bypass = Converters.standardConverter().convert(legacyBypassObject).defaultValue(false).to(Boolean.class);
            }
            String legacyRegexp = null;
            final Object legacyBundlesObject = properties.get(LEGACY_BUNDLES_PROPERTY);
            if (legacyBypassObject != null) {
                LOG.warn("Using deprecated configuration property '{}' from configuration '{}'. " +
                    "Update your configuration to use configuration '{}' and property '{}' instead.", 
                    LEGACY_BUNDLES_PROPERTY, LEGACY_PID, PID, "allowlist.bundles.regexp");
                legacyRegexp = Converters.standardConverter().convert(legacyBundlesObject).to(String.class);
            }

            final String regexp = config.allowlist_bundles_regexp();
            if (regexp.trim().length() > 0) {
                if (legacyRegexp != null) {
                    LOG.warn("Both deprecated configuration property '{}' and non-deprecated configuration property '{}' are set. " +
                        "The deprecated property '{}' is ignored.", 
                        LEGACY_BUNDLES_PROPERTY, "allowlist.bundles.regexp", LEGACY_BUNDLES_PROPERTY);
                }
                this.allowListRegexp = Pattern.compile(regexp);
            } else {
                this.allowListRegexp = legacyRegexp != null ? Pattern.compile(legacyRegexp) : null;
            }
            if (this.allowListRegexp != null) {
                LOG.warn("A 'allowlist.bundles.regexp' is configured, this is NOT RECOMMENDED for production: {}", allowListRegexp);
            }
            this.bypassAllowList = bypass;
            if (this.bypassAllowList) {
                LOG.info("allowlist.bypass=true, allowed BSNs=<ALL>");
                LOG.warn("All bundles are allowed to use loginAdministrative due to the 'allowlist.bypass' " +
                        "configuration of this service. This is NOT RECOMMENDED, for security reasons."
                );
            }
        }
    }

    private void ensureBackwardsCompatibility(final Map<String, Object> properties, final String propertyName) {
        final AllowListFragment oldFragment = backwardsCompatibleFragments.remove(propertyName);
        
        final String[] bsns = Converters.standardConverter().convert(properties.get(propertyName)).to(String[].class);        
        if (bsns != null && bsns.length != 0) {
            LOG.warn("Using deprecated configuration property '{}'", propertyName);
            final AllowListFragment fragment = new AllowListFragment("deprecated-" + propertyName, bsns);
            bindAllowListFragment(fragment);
            backwardsCompatibleFragments.put(propertyName, fragment);
        }
        
        if (oldFragment != null) {
            unbindAllowListFragment(oldFragment);
        }
    }
}
