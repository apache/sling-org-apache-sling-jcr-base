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
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class LegacyFragmentTest {

    @Test
    public void testFragmentBinding() {
        final LoginAdminAllowList allowlist = Mockito.mock(LoginAdminAllowList.class);
        final Map<String, Object> props = new HashMap<>();
        props.put("whitelist.name", "test");
        props.put("whitelist.bundles", new String[] { "org.apache.sling.test" });
        final LegacyFragment fragment = new LegacyFragment(allowlist, props);
        final ArgumentCaptor<AllowListFragment> captor = ArgumentCaptor.forClass(AllowListFragment.class);
        Mockito.verify(allowlist).bindAllowListFragment(captor.capture());
        final AllowListFragment captured = captor.getValue();
        assertEquals("test", captured.name);
        assertTrue(captured.bundles.contains("org.apache.sling.test"));
        assertEquals(1, captured.bundles.size());

        fragment.deactivate();
        Mockito.verify(allowlist).unbindAllowListFragment(captured);
    }
}