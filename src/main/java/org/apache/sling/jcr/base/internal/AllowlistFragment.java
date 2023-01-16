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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import static java.util.Arrays.asList;

@ObjectClassDefinition(
        name = "Apache Sling Login Admin Allowlist Configuration Fragment",
        description = "Allowlist configuration fragments contribute a list of allowlisted bundle symbolic " +
                "names to the Login Admin Whitelist. This allows for modularisation of the allowlist." +
            " Used only if a Whitelist Configuration Fragment is not defined."
)
@interface AllowListConfiguration {

    @AttributeDefinition(
        name = "Allowlist Name",
        description = "Optional name to disambiguate configurations."
    )
    String allowlist_name() default "[unnamed]";

    @AttributeDefinition(
        name = "Allow listed BSNs",
        description = "A list of bundle symbolic names allowed to use loginAdministrative()."
    )
    String[] allowlist_bundles();

    @SuppressWarnings("unused")
    String webconsole_configurationFactory_nameHint() default "{whitelist.name}: [{whitelist.bundles}]";
}

@Component(
        configurationPid = {
            "org.apache.sling.jcr.base.internal.LoginAdminAllowlist.fragment"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AllowlistFragment.class
)
@Designate(ocd = AllowListConfiguration.class, factory = true)
public class AllowlistFragment {

    private String name;

    private Set<String> bundles;

    @SuppressWarnings("unused")
    public AllowlistFragment() {
        // default constructor for SCR
    }

    // constructor for tests and for backwards compatible deprecated fragments
    AllowlistFragment(String name, String[] bundles) {
        this.name = name;
        this.bundles = asSet(bundles);
    }

    @Activate
    @SuppressWarnings("unused")
    void activate(AllowListConfiguration config) {
        name = config.allowlist_name();
        bundles = asSet(config.allowlist_bundles());
    }

    public boolean allows(String bsn) {
        return bundles.contains(bsn);
    }

    @Override
    public String toString() {
        return name + ": " + bundles + "";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AllowlistFragment)) {
            return false;
        }
        final AllowlistFragment that = (AllowlistFragment) o;
        return name.equals(that.name)
                && bundles.equals(that.bundles);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + bundles.hashCode();
        return result;
    }

    private Set<String> asSet(final String[] values) {
        return Collections.unmodifiableSet(new HashSet<>(asList(values)));
    }
}
