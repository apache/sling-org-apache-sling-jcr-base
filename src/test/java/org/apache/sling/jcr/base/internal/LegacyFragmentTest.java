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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.util.converter.Converters;

public class LegacyFragmentTest {

    private final LegacyFragment.Configuration configuration;
    {
        final Map<String, Object> props = new HashMap<>();
        props.put("whitelist.name", "test");
        props.put("whitelist.bundles", new String[] { "org.apache.sling.test" });
        configuration = Converters.standardConverter()
                .convert(props)
                .to(LegacyFragment.Configuration.class);
    }

    @Test
    public void testFragmentBinding() {
        final AllowListFragment fragment = new LegacyFragment(configuration);
        assertEquals("test", fragment.name);
        assertTrue(fragment.allows("org.apache.sling.test"));
        assertFalse(fragment.allows("org.apache.sling.test.not.allowed"));
        assertEquals(1, fragment.bundles.size());
    }

    @Test
    public void testEquality() {
        final LegacyFragment legacyFragment = new LegacyFragment(configuration);
        final AllowListFragment fragment = new AllowListFragment(configuration.whitelist_name(), configuration.whitelist_bundles());

        assertEquals(fragment, legacyFragment);
    }
}