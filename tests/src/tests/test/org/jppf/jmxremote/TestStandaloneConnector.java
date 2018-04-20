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
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.jmxremote.*;
import org.jppf.nio.NioHelper;
import org.jppf.utils.StringUtils;
import org.jppf.utils.collections.*;
import org.junit.*;

/**
 * Tests for the jmxremote-nio connector.
 * @author Laurent Cohen
 */
public class TestStandaloneConnector extends AbstractTestStandaloneConnector {
  /**
   * Performs setup before each test.
   * @throws Exception if any error occurs.
   */
  @Before
  public void beforeInstance() throws Exception {
    print(false, false, "***** starting connector server *****");
    server = createConnectorServer();
    print(false, false, "***** starting connector client *****");
    clientConnector = createConnectorClient();
    registerMBeans();
  }

  /**
   * Test connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testConnection() throws Exception {
    final String connectionID = clientConnector.getConnectionId();
    assertNotNull(connectionID);
    assertTrue(connectionID.startsWith("jppf://"));
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    assertTrue(mbsc instanceof JPPFMBeanServerConnection);
  }

  /**
   * Test invoking MBean methods.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testInvoke() throws Exception {
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    assertTrue(mbsc instanceof JPPFMBeanServerConnection);
    print(false, true, "***** testing invoke *****");
    final String invokeResult = (String) mbsc.invoke(connectorTestName, "test1", new Object[] { "testing", 13 }, new String[] { String.class.getName(), int.class.getName() });
    assertEquals("[testing - 13]", invokeResult);
  }

  /**
   * Test getting and setting MBean attributes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testAttributes() throws Exception {
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    print(false, true, "***** testing string attribute *****");
    String s = (String) mbsc.getAttribute(connectorTestName, "StringParam");
    assertEquals("initial_value", s);
    mbsc.setAttribute(connectorTestName, new Attribute("StringParam", "string value"));
    s = (String) mbsc.getAttribute(connectorTestName, "StringParam");
    assertNotNull(s);
    assertEquals("string value", s);
    print(false, true, "***** testing int attribute *****");
    int n = (Integer) mbsc.getAttribute(connectorTestName, "IntParam");
    assertEquals(0, n);
    mbsc.setAttribute(connectorTestName, new Attribute("IntParam", 13));
    n = (Integer) mbsc.getAttribute(connectorTestName, "IntParam");
    assertEquals(13, n);
    assertTrue(mbsc.isInstanceOf(connectorTestName, ConnectorTestMBean.class.getName()));
  }

  /**
   * Test MBean domains.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDomains() throws Exception {
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    assertTrue(mbsc.isInstanceOf(connectorTestName, ConnectorTestMBean.class.getName()));
    print(false, false, "***** default domain: %s *****", mbsc.getDefaultDomain());
    final String[] domains = mbsc.getDomains();
    print(false, false, "***** domains: %s *****", Arrays.asList(domains));
    assertNotNull(domains);
    assertTrue(StringUtils.isOneOf("org.jppf", false, domains));
  }

  /**
   * Test adding and removing notification listeners, receiving notifications.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testNotifications() throws Exception {
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    print(false, false, "***** testing notifications *****");
    final MyListener listener = new MyListener();
    mbsc.addNotificationListener(connectorTestName, listener, null, "l1");
    mbsc.addNotificationListener(connectorTestName, listener, new StartsWithFilter("a"), "l2");
    final String[] messages = { "a1", "b2", "a3" };
    mbsc.invoke(connectorTestName, "triggerNotifications", new Object[] { messages }, new String[] { String[].class.getName() });
    Thread.sleep(250L);
    final CollectionMap<Object, String> infos = listener.infos;
    assertNotNull(infos);
    assertEquals(5, infos.size());
    assertEquals(2, infos.keySet().size());
    assertTrue(infos.containsKey("l1"));
    assertTrue(infos.containsKey("l2"));
    List<String> list = new ArrayList<>(infos.getValues("l1"));
    assertEquals(3, list.size());
    assertTrue(list.contains("a1"));
    assertTrue(list.contains("b2"));
    assertTrue(list.contains("a3"));
    list = new ArrayList<>(infos.getValues("l2"));
    assertEquals(2, list.size());
    assertTrue(list.contains("a1"));
    assertFalse(list.contains("b2"));
    assertTrue(list.contains("a3"));
    infos.clear();
    mbsc.removeNotificationListener(connectorTestName, listener);
    mbsc.invoke(connectorTestName, "triggerNotifications", new Object[] { messages }, new String[] { String[].class.getName() });
    Thread.sleep(250L);
    assertTrue(infos.isEmpty());
  }

  /**
   * Test MBean registration and unregistration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testRegistration() throws Exception {
    final MBeanServerConnection mbsc = clientConnector.getMBeanServerConnection();
    print(false, false, "***** testing registration *****");
    assertTrue(mbsc.isRegistered(connectorTestName));
    mbsc.unregisterMBean(connectorTestName);
    assertFalse(mbsc.isRegistered(connectorTestName));
    print(false, false, "***** JPPF- thread count: %,d *****", countJMXThreads(0L));
  }

  /**
   * Test connector client connection status notifications.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testClientConnectionNotifications() throws Exception {
    final MyClientConnectionListener listener = new MyClientConnectionListener();
    print(false, false, "***** closing connector client before testing *****");
    clientConnector.close();
    print(false, false, "***** creating connector client *****");
    clientConnector = JMXConnectorFactory.newJMXConnector(url, null);
    clientConnector.addConnectionNotificationListener(listener, null, null);
    print(false, false, "***** connecting connector client *****");
    clientConnector.connect();
    print(false, false, "***** closing connector client *****");
    clientConnector.close();
    print(false, false, "***** re-creating connector client *****");
    clientConnector = JMXConnectorFactory.newJMXConnector(url, null);
    clientConnector.addConnectionNotificationListener(listener, null, null);
    print(false, false, "***** re-connecting connector client *****");
    clientConnector.connect();
    print(false, false, "***** stopping connector server *****");
    server.stop();
    Thread.sleep(500L);
    //clientConnector.close();
    print(false, false, "***** checking connection notifications *****");
    assertEquals(4, listener.notifs.size());
    for (int i=0; i<listener.notifs.size(); i++) {
      final Notification notification = listener.notifs.get(i);
      assertTrue("notifs[" + i + "]", notification instanceof JMXConnectionNotification);
      final JMXConnectionNotification notif = (JMXConnectionNotification) notification;
      switch(i) {
        case 0:
        case 2:
          assertEquals(JMXConnectionNotification.OPENED, notif.getType());
          break;

        case 1:
          assertEquals(JMXConnectionNotification.CLOSED, notif.getType());
          break;

        case 3:
          assertEquals(JMXConnectionNotification.FAILED, notif.getType());
          break;
      }
    }
  }

  /**
   * @param sleepTime how long to sleep in millis before counting.
   * @return the count of live threads whose names starts with "JPPF-".
   * @throws Exception if any error occurs.
   */
  int countJMXThreads(final long sleepTime) throws Exception {
    if (sleepTime > 0L) Thread.sleep(sleepTime);
    final ThreadMXBean mxbean = ManagementFactory.getThreadMXBean();
    final long[] ids = mxbean.getAllThreadIds();
    final ThreadInfo[] allInfo = mxbean.getThreadInfo(ids);
    int count = 0;
    final String prefix = NioHelper.NIO_THREAD_NAME_PREFIX + "-";
    for (final ThreadInfo ti: allInfo) {
      if (ti == null) continue;
      final String name = ti.getThreadName();
      if (name == null) continue;
      if (name.startsWith(prefix)) count++;
    }
    return count;
  }

  /** */
  static class MyListener implements NotificationListener {
    /** */
    CollectionMap<Object, String> infos = new ArrayListHashMap<>();

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      final String msg = (String) notification.getUserData();
      synchronized(infos) {
        infos.putValue(handback, msg);
      }
    }
  }

  /** */
  static class MyClientConnectionListener implements NotificationListener {
    /** */
    final List<Notification> notifs = new ArrayList<>();

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      synchronized(notifs) {
        notifs.add(notification);
      }
    }
  }

  /** */
  static class StartsWithFilter implements NotificationFilter {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    private final String start;

    /**
     * @param start .
     */
    StartsWithFilter(final String start) {
      this.start = start;
    }

    @Override
    public boolean isNotificationEnabled(final Notification notification) {
      final String msg = (String) notification.getUserData();
      return msg.startsWith(start);
    }
  }
}
