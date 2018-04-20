/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package test.org.jppf.jmxremote;

import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.jmxremote.*;
import org.jppf.management.ObjectNameCache;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Tests for the jmxremote-nio connector.
 * @author Laurent Cohen
 */
public abstract class AbstractTestStandaloneConnector extends BaseTest {
  /**
   * Object name of the ConnectorTestMBean.
   */
  static ObjectName connectorTestName;
  /**
   * The MBean server used in the tests.
   */
  static final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
  /**
   * Address of the JMX connector server.
   */
  static JMXServiceURL url;
  /**
   * The server-side JMX connector.
   */
  JMXConnectorServer server; 
  /**
   * The clientr-side JMX connector.
   */
  JMXConnector clientConnector; 

  /**
   * Performs cleanup after each test.
   * @throws Exception if any error occurs.
   */
  @After
  public void afterInstance() throws Exception {
    if (clientConnector != null) {
      clientConnector.close();
      clientConnector = null;
    }
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  /**
   * @throws Exception  if any error occurs.
   */
  @BeforeClass
  public static void beforeClass() throws Exception {
    BaseSetup.setup(0, 0, false, BaseSetup.DEFAULT_CONFIG);
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.DEBUG, "org.jppf.jmxremote", "org.jppf.nio");
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.INFO, "org.jppf.nio.PlainNioObject", "org.jppf.serialization");
    url = new JMXServiceURL("service:jmx:jppf://localhost:12001");
    connectorTestName = ObjectNameCache.getObjectName(ConnectorTestMBean.MBEAN_NAME);
    registerMBeans();
  }

  /**
   * @throws Exception  if any error occurs.
   */
  @AfterClass
  public static void afterClass() throws Exception {
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.INFO, "org.jppf.jmxremote", "org.jppf.nio");
    BaseSetup.cleanup();
  }

  /**
   * Register the LBeans used in the tests.
   * @throws Exception if any error occurs.
   */
  static void registerMBeans() throws Exception {
    if (!mbeanServer.isRegistered(connectorTestName)) mbeanServer.registerMBean(new ConnectorTest(), connectorTestName);
  }

  /**
   * Create a connector server.
   * @param env the environment ot pass on to the connector server.
   * @return a new started {@link JMXConnectorServer}.
   * @throws Exception if any error occurs.
   */
  static JMXConnectorServer createConnectorServer(final Map<String, ?> env) throws Exception {
    final JMXConnectorServer server = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbeanServer);
    assertTrue(server instanceof JPPFJMXConnectorServer);
    server.start();
    return server;
  }

  /**
   * Create a connector server.
   * @return a new started {@link JMXConnectorServer}.
   * @throws Exception if any error occurs.
   */
  static JMXConnectorServer createConnectorServer() throws Exception {
    return createConnectorServer(null);
  }

  /**
   * Create a connector client.
   * @param env the environment ot pass on to the connector client.
   * @return a new connected {@link JMXConnector}.
   * @throws Exception if any error occurs.
   */
  static JMXConnector createConnectorClient(final Map<String, ?> env) throws Exception {
    final JMXConnector client = JMXConnectorFactory.connect(url, env);
    assertTrue(client instanceof JPPFJMXConnector);
    return client;
  }

  /**
   * Create a connector client.
   * @return a new connected {@link JMXConnector}.
   * @throws Exception if any error occurs.
   */
  static JMXConnector createConnectorClient() throws Exception {
    return createConnectorClient(null);
  }
}
