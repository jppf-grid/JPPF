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

import static org.junit.Assert.*;

import java.lang.management.*;
import java.util.Arrays;

import javax.management.*;
import javax.management.remote.JMXServiceURL;

import org.jppf.jmxremote.*;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestStandaloneConnector extends BaseTest {
  /**
   * 
   */
  private static final String[] loggerNames = { "org.jppf.jmxremote", "org.jppf.nio" };

  /**
   * 
   * @throws Exception  if any error occurs.
   */
  @BeforeClass
  public static void beforeClass() throws Exception {
    BaseSetup.setup(0, 0, false, BaseSetup.DEFAULT_CONFIG);
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.DEBUG, loggerNames);
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.INFO, "org.jppf.nio.PlainNioObject", "org.jppf.serialization");
  }

  /**
   * 
   * @throws Exception  if any error occurs.
   */
  @AfterClass
  public static void afterClass() throws Exception {
    BaseSetup.setLoggerLevel(org.apache.log4j.Level.INFO, loggerNames);
    BaseSetup.cleanup();
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  //@Test(timeout = 10000)
  @Test
  public void testConnection() throws Exception {
    ConnectorTest tc = new ConnectorTest();
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    ObjectName objectName = new ObjectName(ConnectorTestMBean.MBEAN_NAME);
    mbeanServer.registerMBean(tc, objectName);
    JMXServiceURL url = new JMXServiceURL("service:jmx:jppf://localhost:12001");
    print(false, false, "***** starting connector server *****");
    JPPFJMXConnectorServer server = new JPPFJMXConnectorServer(url, null, mbeanServer);
    server.start();
    print(false, true, "***** starting connector client *****");
    JPPFJMXConnector client = new JPPFJMXConnector(url, null);
    client.connect();
    String connectionID = client.getConnectionId();
    assertNotNull(connectionID);
    assertTrue(connectionID.startsWith("jppf://"));
    MBeanServerConnection mbsc = client.getMBeanServerConnection();
    print(false, true, "***** testing invoke *****");
    String invokeResult = (String) mbsc.invoke(objectName, "test1", new Object[] { "testing", 13 }, new String[] { String.class.getName(), int.class.getName() });
    assertEquals("[testing - 13]", invokeResult);
    print(false, true, "***** testing string attribute *****");
    String s = (String) mbsc.getAttribute(objectName, "StringParam");
    assertNull(s);
    mbsc.setAttribute(objectName, new Attribute("StringParam", "string value"));
    s = (String) mbsc.getAttribute(objectName, "StringParam");
    assertNotNull(s);
    assertEquals("string value", s);
    print(false, true, "***** testing int attribute *****");
    int n = (Integer) mbsc.getAttribute(objectName, "IntParam");
    assertEquals(0, n);
    mbsc.setAttribute(objectName, new Attribute("IntParam", 13));
    n = (Integer) mbsc.getAttribute(objectName, "IntParam");
    assertEquals(13, n);
    assertTrue(mbsc.isInstanceOf(objectName, ConnectorTestMBean.class.getName()));
    print(false, false, "***** default domain: %s *****", mbsc.getDefaultDomain());
    print(false, false, "***** domains: %s *****", Arrays.asList(mbsc.getDomains()));
    assertTrue(mbsc.isRegistered(objectName));
    mbsc.unregisterMBean(objectName);
    assertFalse(mbsc.isRegistered(objectName));
    print(false, false, "***** JPPF- thread count: %,d *****", countJMXThreads(0L));
    client.close();
    server.stop();
  }

  /**
   * @param sleepTime how long to sleep in millis before counting.
   * @return the count of live threads whose names starts with "JPPF-".
   * @throws Exception if any error occurs.
   */
  int countJMXThreads(final long sleepTime) throws Exception {
    if (sleepTime > 0L) Thread.sleep(sleepTime);
    ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
    long[] ids = mxbean.getAllThreadIds();
    ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
    int count = 0;
    for (ThreadInfo ti: allInfo) {
      if (ti == null) continue;
      String name = ti.getThreadName();
      if (name == null) continue;
      if (name.startsWith("JPPF-")) count++;
    }
    return count;
  }
}
