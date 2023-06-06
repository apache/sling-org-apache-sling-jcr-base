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
import java.util.Hashtable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationUpdaterTest {

    @Test
    public void testUpdateProps_whenFragmentOldPropertiesAreProvided_thenNewPropertiesAreConfigured() throws IOException, InvalidSyntaxException {
        final ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        final Configuration mockSourceConfiguration = mock(Configuration.class);
        final Configuration mockTargetConfiguration = mock(Configuration.class);
        final Dictionary<String, Object> sourceProperties = new Hashtable<>();
        sourceProperties.put("whitelist.bypass", "whitelistNameValue");
        sourceProperties.put("whitelist.bundles.regexp", "whitelistBundleValue");
        sourceProperties.put("prop", "value");
        when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);

        when(mockConfigurationAdmin.listConfigurations("(service.pid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist)")).thenReturn(new Configuration[] {mockSourceConfiguration});
        when(mockConfigurationAdmin.getConfiguration(LoginAdminAllowList.PID, null)).thenReturn(mockTargetConfiguration);

        final Dictionary<String, Object> expectedProperties = new Hashtable<>();
        expectedProperties.put("allowlist.bypass", "whitelistNameValue");
        expectedProperties.put("allowlist.bundles.regexp", "whitelistBundleValue");
        expectedProperties.put("prop", "value");

        new ConfigurationUpdater(mockConfigurationAdmin); 

        verify(mockConfigurationAdmin).listConfigurations("(service.pid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist)");
        verify(mockConfigurationAdmin).getConfiguration(LoginAdminAllowList.PID, null);
        final ArgumentCaptor<Dictionary> targetPropertiesCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(mockTargetConfiguration).update(targetPropertiesCaptor.capture());
        assertEquals(expectedProperties, targetPropertiesCaptor.getValue());
        verify(mockSourceConfiguration).delete();
    }

    @Test
    public void testUpdateProps_whenNewConfigExists() throws IOException, InvalidSyntaxException {
        final ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        final Configuration mockSourceConfiguration = mock(Configuration.class);
        final Configuration mockTargetConfiguration = mock(Configuration.class);
        final Dictionary<String, Object> sourceProperties = new Hashtable<>();
        sourceProperties.put("whitelist.bypass", "whitelistNameValue");
        sourceProperties.put("whitelist.bundles.regexp", "whitelistBundleValue");
        sourceProperties.put("prop", "value");
        when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);

        when(mockConfigurationAdmin.listConfigurations("(service.pid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist)")).thenReturn(new Configuration[] {mockSourceConfiguration});
        when(mockConfigurationAdmin.listConfigurations("(service.pid=" + LoginAdminAllowList.PID + ")")).thenReturn(new Configuration[] {mockTargetConfiguration});

        new ConfigurationUpdater(mockConfigurationAdmin); 

        verify(mockConfigurationAdmin).listConfigurations("(service.pid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist)");
        verify(mockConfigurationAdmin).listConfigurations("(service.pid=" + LoginAdminAllowList.PID + ")");
        verify(mockConfigurationAdmin).listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)");
        verifyNoMoreInteractions(mockConfigurationAdmin);
    }

    @Test
    public void testUpdatePropsForNamedFactoryPid_whenFragmentOldPropertiesAreProvided_thenNewPropertiesAreConfigured() throws InvalidSyntaxException, IOException {
        final ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        final Configuration mockSourceConfiguration = mock(Configuration.class);
        final Configuration mockTargetConfiguration = mock(Configuration.class);
        final Dictionary<String, Object> sourceProperties = new Hashtable<>();
        sourceProperties.put("whitelist.name", "whitelistNameValue");
        sourceProperties.put("whitelist.bundles", "whitelistBundleValue");
        sourceProperties.put("prop", "value");
        when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);
        when(mockSourceConfiguration.getFactoryPid()).thenReturn("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment");
        when(mockSourceConfiguration.getPid()).thenReturn("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment~foo");
        when(mockConfigurationAdmin.listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)")).thenReturn(new Configuration[] {mockSourceConfiguration});
        when(mockConfigurationAdmin.getFactoryConfiguration(AllowListFragment.FACTORY_PID, "foo", null)).thenReturn(mockTargetConfiguration);

        final Dictionary<String, Object> expectedProperties = new Hashtable<>();
        expectedProperties.put("allowlist.name", "whitelistNameValue");
        expectedProperties.put("allowlist.bundles", "whitelistBundleValue");
        expectedProperties.put("prop", "value");

        new ConfigurationUpdater(mockConfigurationAdmin); 

        verify(mockConfigurationAdmin).listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)");
        verify(mockConfigurationAdmin).getFactoryConfiguration(AllowListFragment.FACTORY_PID, "foo", null);
        final ArgumentCaptor<Dictionary> targetPropertiesCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(mockTargetConfiguration).update(targetPropertiesCaptor.capture());
        assertEquals(expectedProperties, targetPropertiesCaptor.getValue());
        verify(mockSourceConfiguration).delete();
   }

   @Test
   public void testUpdateNamedFactoryConfig_whenNewConfigExists() throws IOException, InvalidSyntaxException {
       final ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
       final Configuration mockSourceConfiguration = mock(Configuration.class);
       final Configuration mockTargetConfiguration = mock(Configuration.class);
       final Dictionary<String, Object> sourceProperties = new Hashtable<>();
       sourceProperties.put("whitelist.bypass", "whitelistNameValue");
       sourceProperties.put("whitelist.bundles.regexp", "whitelistBundleValue");
       sourceProperties.put("prop", "value");
       when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);
       when(mockSourceConfiguration.getFactoryPid()).thenReturn("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment");
       when(mockSourceConfiguration.getPid()).thenReturn("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment~foo");

       when(mockConfigurationAdmin.listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)")).thenReturn(new Configuration[] {mockSourceConfiguration});
       when(mockConfigurationAdmin.listConfigurations("(service.pid=" + AllowListFragment.FACTORY_PID + "~foo)")).thenReturn(new Configuration[] {mockTargetConfiguration});

       new ConfigurationUpdater(mockConfigurationAdmin); 

       verify(mockConfigurationAdmin).listConfigurations("(service.pid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist)");
       verify(mockConfigurationAdmin).listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)");
       verify(mockConfigurationAdmin).listConfigurations("(service.pid=" + AllowListFragment.FACTORY_PID + "~foo)");
       verifyNoMoreInteractions(mockConfigurationAdmin);
   }

   @Test
   public void testUpdatePropsForOldFactoryPid_whenFragmentOldPropertiesAreProvided_thenNewPropertiesAreConfigured() throws InvalidSyntaxException, IOException {
       final ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
       final Configuration mockSourceConfiguration = mock(Configuration.class);
       final Configuration mockTargetConfiguration = mock(Configuration.class);
       final Dictionary<String, Object> sourceProperties = new Hashtable<>();
       sourceProperties.put("whitelist.name", "whitelistNameValue");
       sourceProperties.put("whitelist.bundles", "whitelistBundleValue");
       sourceProperties.put("prop", "value");
       when(mockSourceConfiguration.getProperties()).thenReturn(sourceProperties);
       when(mockSourceConfiguration.getFactoryPid()).thenReturn("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment");
       when(mockSourceConfiguration.getPid()).thenReturn("org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment-randomid");
       when(mockConfigurationAdmin.listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)")).thenReturn(new Configuration[] {mockSourceConfiguration});
       when(mockConfigurationAdmin.createFactoryConfiguration(AllowListFragment.FACTORY_PID, null)).thenReturn(mockTargetConfiguration);

       final Dictionary<String, Object> expectedProperties = new Hashtable<>();
       expectedProperties.put("allowlist.name", "whitelistNameValue");
       expectedProperties.put("allowlist.bundles", "whitelistBundleValue");
       expectedProperties.put("prop", "value");

       new ConfigurationUpdater(mockConfigurationAdmin); 

       verify(mockConfigurationAdmin).listConfigurations("(service.factoryPid=org.apache.sling.jcr.base.internal.LoginAdminWhitelist.fragment)");
       verify(mockConfigurationAdmin).createFactoryConfiguration(AllowListFragment.FACTORY_PID, null);
       final ArgumentCaptor<Dictionary> targetPropertiesCaptor = ArgumentCaptor.forClass(Dictionary.class);
       verify(mockTargetConfiguration).update(targetPropertiesCaptor.capture());
       assertEquals(expectedProperties, targetPropertiesCaptor.getValue());
       verify(mockSourceConfiguration).delete();
    }
}