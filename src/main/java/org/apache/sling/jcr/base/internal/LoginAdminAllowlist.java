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
import org.apache.sling.jcr.base.PermissionCheckerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.apache.sling.commons.osgi.PropertiesUtil.toStringArray;

/**
 * Allowlist that defines which bundles can use the
 * {@link SlingRepository#loginAdministrative} method.
 *
 * The default configuration lets a few trusted Sling bundles
 * use the loginAdministrative method.
 */
@Component(
        service = PermissionCheckerService.class,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Apache Sling Login Admin Allowlist",
                Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
            Constants.SERVICE_RANKING + ":Integer=" + Integer.MIN_VALUE
        }
)
@Designate(
        ocd = LoginAdminAllowlistConfiguration.class
)
public class LoginAdminAllowlist implements PermissionCheckerService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginAdminAllowlist.class);

    private volatile ConfigurationState config;

    private final List<AllowlistFragment> allowlistFragments = new CopyOnWriteArrayList<>();

    // for backwards compatibility only (read properties directly to prevent them from appearing in the metatype)
    private static final String PROP_WHITELIST_BUNDLES_DEFAULT = "whitelist.bundles.default";

    private static final String PROP_WHITELIST_BUNDLES_ADDITIONAL = "whitelist.bundles.additional";

    private final Map<String, AllowlistFragment> backwardsCompatibleFragments =
        new ConcurrentHashMap<>();

    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY
    ) @SuppressWarnings("unused")
    void bindWAllowlistFragment(AllowlistFragment fragment) {
        allowlistFragments.add(fragment);
        LOG.info("AllowlistFragment added '{}'", fragment);
    }

    @SuppressWarnings("unused")
    void unbindWAllowlistFragment(AllowlistFragment fragment) {
        allowlistFragments.remove(fragment);
        LOG.info("AllowlistFragment removed '{}'", fragment);
    }

    @Activate @Modified @SuppressWarnings("unused")
    void configure(LoginAdminAllowlistConfiguration configuration, Map<String, Object> properties) {
        this.config = new ConfigurationState(configuration);
        ensureBackwardsCompatibility(properties, PROP_WHITELIST_BUNDLES_DEFAULT);
        ensureBackwardsCompatibility(properties, PROP_WHITELIST_BUNDLES_ADDITIONAL);
    }

    public boolean allowLoginAdministrative(Bundle b) {
        if (config == null) {
            throw new IllegalStateException("LoginAdminAllowlist has no configuration.");
        }
        // create local copy of ConfigurationState to avoid reading mixed configurations during an configure
        final ConfigurationState localConfig = this.config;
        if(localConfig.bypassAllowlist) {
            LOG.debug("Allowlist is bypassed, all bundles allowed to use loginAdministrative");
            return true;
        }

        final String bsn = b.getSymbolicName();

        if(localConfig.allowlistRegexp != null && localConfig.allowlistRegexp.matcher(bsn).matches()) {
            LOG.debug("{} is allowed to use loginAdministrative, by regexp", bsn);
            return true;
        }

        for (final AllowlistFragment fragment : allowlistFragments) {
            if (fragment.allows(bsn)) {
                LOG.debug("{} is allowed to use loginAdministrative, by fragment '{}'",
                        bsn, fragment);
                return true;
            }
        }

        LOG.debug("{} is not allowed to use loginAdministrative", bsn);
        return false;
    }

    // encapsulate configuration state for atomic configuration updates
    private static class ConfigurationState {

        private final boolean bypassAllowlist;

        private final Pattern allowlistRegexp;

        private ConfigurationState(final LoginAdminAllowlistConfiguration config) {
            final String regexp = config.allowlist_bundles_regexp();
            if(regexp.trim().length() > 0) {
                allowlistRegexp = Pattern.compile(regexp);
                LOG.warn("A 'allowlist_bundles_regexp' is configured, this is NOT RECOMMENDED for production: {}",
                    allowlistRegexp);
            } else {
                allowlistRegexp = null;
            }

            bypassAllowlist = config.allowlist_bypass();
            if(bypassAllowlist) {
                LOG.info("bypassAllowlist=true, allowlisted BSNs=<ALL>");
                LOG.warn("All bundles are allowed to use loginAdministrative due to the 'allowlist_bypass' " +
                        "configuration of this service. This is NOT RECOMMENDED, for security reasons."
                );
            }
        }
    }

    @SuppressWarnings("deprecated")
    private void ensureBackwardsCompatibility(final Map<String, Object> properties, final String propertyName) {
        final AllowlistFragment oldFragment = backwardsCompatibleFragments.remove(propertyName);
        
        final String[] bsns = toStringArray(properties.get(propertyName), new String[0]);
        if (bsns.length != 0) {
            LOG.warn("Using deprecated configuration property '{}'", propertyName);
            final AllowlistFragment fragment = new AllowlistFragment("deprecated-" + propertyName, bsns);
            bindWAllowlistFragment(fragment);
            backwardsCompatibleFragments.put(propertyName, fragment);
        }
        
        if (oldFragment != null) {
            unbindWAllowlistFragment(oldFragment);
        }
    }
}
