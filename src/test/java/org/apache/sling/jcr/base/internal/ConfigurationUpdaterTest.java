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

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUpdaterTest {

    @Mock
    private ConfigurationAdmin mockConfigurationAdmin;

    @Mock
    private Configuration mockSourceConfiguration;
    @Mock
    private Configuration mockTargetConfiguration;

    @Captor
    private ArgumentCaptor<Dictionary<String, ?>> targetPropertiesCaptor;

    private final String targetConfigPid = "someConfigPid";
    private final String sourceConfigPid = "anotherConfigPid";

    private final Map<String, String> mappings = new HashMap<>();
    private Dictionary<String, Object> targetProperties;

    @Before
    public void setup() throws IOException {
        when(mockConfigurationAdmin.getConfiguration(sourceConfigPid, null)).thenReturn(mockSourceConfiguration);
        when(mockConfigurationAdmin.getConfiguration(targetConfigPid, null)).thenReturn(mockTargetConfiguration);
        targetProperties = new Hashtable<>();
        when(mockTargetConfiguration.getProperties()).thenReturn(targetProperties);
        mappings.put("whitelist.name", "allowlist.name");
        mappings.put("whitelist.bundles", "allowlist.bundles");
    }

    @Test
    public void testUpdateProps_whenFragmentOldPropertiesAreProvided_thenNewPropertiesAreConfigured() throws IOException {
        Dictionary<String, Object> sourceProperties = new Hashtable<>();
        final String whitelistNameValue = "whitelistNameValue";
        final String whitelistBundlesValue = "whitelistBundleValue";
        sourceProperties.put("whitelist.name", whitelistNameValue);
        sourceProperties.put("whitelist.bundles", whitelistBundlesValue);
        targetProperties.put(mappings.get("whitelist.name"), whitelistNameValue);
        targetProperties.put(mappings.get("whitelist.bundles"), whitelistBundlesValue);

        when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);

        ConfigurationUpdater configurationUpdater = ConfigurationUpdater.forPid(sourceConfigPid, targetConfigPid, mappings);
        configurationUpdater.updateProps(mockConfigurationAdmin);

        verify(mockTargetConfiguration).update(targetPropertiesCaptor.capture());
        assertEquals(targetPropertiesCaptor.getValue(), targetProperties);
        verify(mockSourceConfiguration).delete();
    }

    @Test
    public void testUpdateProps_whenFragmentOldPropertiesAreNotProvided_thenNewPropertiesAreNotConfigured() throws IOException {
        Dictionary<String, Object> sourceProperties = new Hashtable<>();
        sourceProperties.put("some.random.property", "value");
        when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);

        ConfigurationUpdater configurationUpdater = ConfigurationUpdater.forPid(sourceConfigPid, targetConfigPid, mappings);
        configurationUpdater.updateProps(mockConfigurationAdmin);

        assertNull(targetProperties.get("allowlist.name"));
        assertNull(targetProperties.get("allowlist.bundles"));
        verify(mockSourceConfiguration, never()).delete();
    }

    @Test
    public void testUpdatePropsForFactoryPid_whenFragmentOldPropertiesAreProvided_thenNewPropertiesAreConfigured() throws InvalidSyntaxException, IOException {
        Dictionary<String, Object> sourceProperties = new Hashtable<>();
        final String whitelistNameValue = "whitelistNameValue";
        final String whitelistBundlesValue = "whitelistBundleValue";
        sourceProperties.put("whitelist.name", whitelistNameValue);
        sourceProperties.put("whitelist.bundles", whitelistBundlesValue);
        targetProperties.put(mappings.get("whitelist.name"), whitelistNameValue);
        targetProperties.put(mappings.get("whitelist.bundles"), whitelistBundlesValue);
        when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);
        when(mockConfigurationAdmin.listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)"))
            .thenReturn(new Configuration[]{mockSourceConfiguration});
        final String sourceConfigPid = "org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment~somefragment";
        when(mockSourceConfiguration.getPid()).thenReturn(sourceConfigPid);
        when(mockConfigurationAdmin.getConfiguration("org.apache.sling.jcr.base.internal.LoginAdminAllowlistlist.fragment~somefragment", null))
            .thenReturn(mockTargetConfiguration);

        ConfigurationUpdater configurationUpdater = ConfigurationUpdater.forFactoryPid("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment", "org.apache.sling.jcr.base.internal.LoginAdminAllowlistlist.fragment", mappings);
        configurationUpdater.updateProps(mockConfigurationAdmin);

        verify(mockTargetConfiguration).update(targetPropertiesCaptor.capture());
        assertEquals(targetPropertiesCaptor.getValue(), targetProperties);
        verify(mockSourceConfiguration).delete();
    }
}