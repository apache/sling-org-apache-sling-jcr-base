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
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import org.apache.sling.jcr.base.util.ConfigAnnotationUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class LoginAdminAllowlisttTest {

    private LoginAdminAllowlist allowlist;

    @Before
    public void setup() {
        allowlist = new LoginAdminAllowlist();
    }
    
    private void assertAdminLogin(final String bundleSymbolicName, boolean expected) {
        final Bundle b = Mockito.mock(Bundle.class);
        when(b.getSymbolicName()).thenReturn(bundleSymbolicName);
        final boolean actual = allowlist.allowLoginAdministrative(b);
        assertEquals("For bundle " + bundleSymbolicName + ", expected admin login=" + expected, expected, actual);
    }
    
    private List<String> randomBsn() {
        final List<String> result = new ArrayList<String>();
        for(int i=0; i < 5; i++) {
            result.add("random.bsn." + UUID.randomUUID());
        }
        return result;
    }

    @Test
    public void testBypassWhitelist() throws ConfigurationException {
        configure(allowlist, true, null, null, null);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, true);
        }
    }
    
    @Test
    public void testDefaultConfigOnly() throws ConfigurationException {
        final String [] allowed = {
                "bundle1", "bundle2"
        };
        configure(allowlist, null, null, allowed, null);

        assertAdminLogin("foo.1.bar", false);

        for(String bsn : allowed) {
            assertAdminLogin(bsn, true);
        }

        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testAdditionalConfigOnly() throws ConfigurationException {
        final String [] allowed = {
                "bundle5", "bundle6"
        };
        configure(allowlist, null, null, null, allowed);

        assertAdminLogin("foo.1.bar", false);

        for(String bsn : allowed) {
            assertAdminLogin(bsn, true);
        }

        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testDefaultAndAdditionalConfig() throws ConfigurationException {
        configure(allowlist, null, null, new String [] { "defB"}, new String [] { "addB"});
        
        assertAdminLogin("defB", true);
        assertAdminLogin("addB", true);
        assertAdminLogin("foo.1.bar", false);
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }
    
    @Test
    public void testRegexpWhitelist() throws ConfigurationException {
        final String [] allowed = {
                "bundle3", "bundle4"
        };
        configure(allowlist, null, "foo.*bar", allowed, null);

        assertAdminLogin("foo.2.bar", true);
        assertAdminLogin("foo.somethingElse.bar", true);

        for(String bsn : allowed) {
            assertAdminLogin(bsn, true);
        }
        
        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }
    }


    @Test
    public void testWhitelistFragment() throws ConfigurationException {
        final String [] allowed1 = randomBsn().toArray(new String[0]);
        final String [] allowed2 = randomBsn().toArray(new String[0]);

        final AllowlistFragment testFragment1 = new AllowlistFragment("test1", allowed1);
        final AllowlistFragment testFragment2 = new AllowlistFragment("test2", allowed2);

        configure(allowlist, null, null, null, null);
        allowlist.bindWAllowlistFragment(testFragment1);
        allowlist.bindWAllowlistFragment(testFragment2);

        for(String bsn : allowed1) {
            assertAdminLogin(bsn, true);
        }

        for(String bsn : allowed2) {
            assertAdminLogin(bsn, true);
        }

        for(String bsn : randomBsn()) {
            assertAdminLogin(bsn, false);
        }

        allowlist.unbindWAllowlistFragment(testFragment1);

        for(String bsn : allowed1) {
            assertAdminLogin(bsn, false);
        }

        for(String bsn : allowed2) {
            assertAdminLogin(bsn, true);
        }
    }

    private void configure(final LoginAdminAllowlist allowlist, final Boolean bypass, final String regexp, final String[] defaultBSNs, final String[] additionalBSNs) throws ConfigurationException {
        final Hashtable<String, Object> props = new Hashtable<>();
        if (bypass != null) {
            props.put("allowlist.bypass", bypass);
        }
        if (regexp != null) {
            props.put("allowlist.bundles.regexp", regexp);
        }
        if (defaultBSNs != null) {
            props.put("whitelist.bundles.default", defaultBSNs);
        }
        if (additionalBSNs != null) {
            props.put("whitelist.bundles.additional", additionalBSNs);
        }
        LoginAdminAllowlistConfiguration configuration =
                ConfigAnnotationUtil.fromDictionary(LoginAdminAllowlistConfiguration.class, props);
        allowlist.configure(configuration, props);
    }
}