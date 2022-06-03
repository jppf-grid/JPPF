/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.Map;

import javax.management.remote.*;

import org.jppf.jmx.JMXHelper;
import org.jppf.jmxremote.JPPFJMXConnector;
import org.jppf.management.*;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for the the JMX environment providers.
 * @author Laurent Cohen
 */
public class TestEnvironmentProviders extends BaseTest {
  /**
   * Test that the client environment provider adds the expected proeprties.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testClientProvider() throws Exception {
    final int port = 4444;
    JMXServer server = null;
    JMXConnectionWrapper client = null;
    try {
      TestClientEnvironmentProvider.active = true;
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, port).set(JPPFProperties.JMX_REMOTE_PROTOCOL, JMXHelper.JPPF_JMX_PROTOCOL);
      server = JMXServerFactory.createServer(JPPFConfiguration.getProperties(), "clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      client = new JMXConnectionWrapper("localhost", port, false);
      client.connectAndWait(3000L);
      assertTrue(client.isConnected());
      final JMXConnector jmxc = client.getJmxconnector();
      assertTrue(jmxc instanceof JPPFJMXConnector);
      final Map<String, ?> actual = ((JPPFJMXConnector) jmxc).getEnvironment();
      final Map<String, ?> expected = TestClientEnvironmentProvider.env;
      for (final Map.Entry<String, ?> entry: expected.entrySet()) {
        final String key = entry.getKey();
        final Object expectedValue = entry.getValue();
        assertTrue(String.format("env does not contain %s", key), actual.containsKey(key));
        final Object actualValue = actual.get(key);
        assertEquals(String.format("value for key=%s should be %s but is %s", key, expectedValue, actualValue), expectedValue, actualValue);
      }
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
      if (server != null) {
        try {
          server.stop();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
      TestClientEnvironmentProvider.active = false;
    }
  }

  /**
   * Test that the client environment provider adds the expected proeprties.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testServerProvider() throws Exception {
    final int port = 4445;
    JMXServer server = null;
    JMXConnectionWrapper client = null;
    try {
      TestServerEnvironmentProvider.active = true;
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, port).set(JPPFProperties.JMX_REMOTE_PROTOCOL, JMXHelper.JPPF_JMX_PROTOCOL);
      server = JMXServerFactory.createServer(JPPFConfiguration.getProperties(), "serverTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      client = new JMXConnectionWrapper("localhost", port, false);
      client.connectAndWait(3000L);
      assertTrue(client.isConnected());
      final JMXConnectorServer connector = server.getConnectorServer();
      final Map<String, ?> actual = connector.getAttributes();
      final Map<String, ?> expected = TestServerEnvironmentProvider.env;
      for (final Map.Entry<String, ?> entry: expected.entrySet()) {
        final String key = entry.getKey();
        final Object expectedValue = entry.getValue();
        assertTrue(String.format("env does not contain %s", key), actual.containsKey(key));
        final Object actualValue = actual.get(key);
        assertEquals(String.format("value for key=%s should be %s but is %s", key, expectedValue, actualValue), expectedValue, actualValue);
      }
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
      if (server != null) {
        try {
          server.stop();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
      TestServerEnvironmentProvider.active = false;
    }
  }
}
