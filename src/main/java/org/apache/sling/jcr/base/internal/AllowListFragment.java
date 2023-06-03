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
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

@ObjectClassDefinition(
        name = "Apache Sling Login Admin Allow List Configuration Fragment",
        description = "Allow list configuration fragments contribute a list of allow listed bundle symbolic " +
                "names to the Login Admin Allow List. This allows for modularisation of the allow list."
)
@interface Configuration {

    @AttributeDefinition(
            name = "Name",
            description = "Optional name to disambiguate configurations."
    )
    String allowlist_name() default "[unnamed]";

    @AttributeDefinition(
            name = "Allow listed BSNs",
            description = "A list of bundle symbolic names allowed to use loginAdministrative()."
    )
    String[] allowlist_bundles();

    @SuppressWarnings("unused")
    String webconsole_configurationFactory_nameHint() default "{allowlist.name}: [{allowlist.bundles}]";
}

@Component(
        configurationPid = "org.apache.sling.jcr.base.internal.LoginAdminAllowList.fragment",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = AllowListFragment.class
)
@Designate(ocd = Configuration.class, factory = true)
public class AllowListFragment {

    private final String name;

    private final Set<String> bundles;

    /**
     * Constructor for SCR
     * @param config Configuration
     */
    @Activate
    public AllowListFragment(final Configuration config) {
        this.name = config.allowlist_name();
        this.bundles = asSet(config.allowlist_bundles());
    }

    // constructor for tests and for backwards compatible deprecated fragments
    AllowListFragment(String name, String[] bundles) {
        this.name = name;
        this.bundles = asSet(bundles);
    }

    boolean allows(final String bsn) {
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
        if (!(o instanceof AllowListFragment)) {
            return false;
        }
        final AllowListFragment that = (AllowListFragment) o;
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
        return Collections.unmodifiableSet(new HashSet<String>(asList(values)));
    }
}
