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

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;
import javax.management.remote.MBeanServerForwarder;

import org.apache.log4j.Level;
import org.jppf.jmx.JMXHelper;
import org.jppf.management.*;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.collections.CollectionUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for the the JMXMPServer class.
 * @author Laurent Cohen
 */
public class TestJMXServer extends BaseTest {
  /** */
  private static final String[] PARAMS = {"param1", "param2", "param3"};
  /** */
  private final static String[] debugLoggers = { "org.jppf.management", "org.jppf.utils.ReflectionHelper", "org.jppf.jmxremote.JPPFJMXConnectorServer" };

  /**
   * Register the test mbean.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.registerMBean(new StandardMBean(new MyTest(), MyTestMBean.class), ObjectNameCache.getObjectName(MyTestMBean.MBEAN_NAME));
    ConfigurationHelper.setLoggerLevel(Level.DEBUG, debugLoggers);
  }

  /**
   * Unregister the test mbean.
   * @throws Exception if any error occurs.
   */
  @AfterClass
  public static void teardown() throws Exception {
    JPPFConfiguration.remove(JPPFProperties.MANAGEMENT_SERVER_FORWARDER);
    final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.unregisterMBean(ObjectNameCache.getObjectName(MyTestMBean.MBEAN_NAME));
    ConfigurationHelper.setLoggerLevel(Level.INFO, debugLoggers);
  }

  /**
   * Test without an MBeanServerForwarder.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testNoMBeanForwarder() throws Exception {
    final int port = 4444;
    JMXServer server = null;
    try {
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, port)
        .set(JPPFProperties.JMX_REMOTE_PROTOCOL, JMXHelper.JPPF_JMX_PROTOCOL)
        .remove(JPPFProperties.MANAGEMENT_SERVER_FORWARDER);
      server = JMXServerFactory.createServer(JPPFConfiguration.getProperties(), "clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      final MBeanServerForwarder mbsf = server.getMBeanServerForwarder();
      assertNull(mbsf);
    } finally {
      if (server != null) {
        try {
          server.stop();
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Test an MBeanServerForwarder with a constructor that takes a {@code String[]} parameter.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testMBeanForwarderWithStringArrayConstructor() throws Exception {
    final int port = 4445;
    JMXServer server = null;
    JMXConnectionWrapper client = null;
    try {
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, port)
        .set(JPPFProperties.MANAGEMENT_SERVER_FORWARDER, CollectionUtils.concatArrays(new String[] {Forwarder1.class.getName()}, PARAMS));
      server = JMXServerFactory.createServer(JPPFConfiguration.getProperties(), "clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      final MBeanServerForwarder mbsf = server.getMBeanServerForwarder();
      assertTrue(mbsf instanceof Forwarder1);
      final Forwarder1 forwarder = (Forwarder1) mbsf;
      final String[] params = forwarder.getParameters();
      assertTrue(Arrays.equals(PARAMS, params));
      client = new JMXConnectionWrapper("localhost", port, false);
      assertTrue(client.connectAndWait(5000L));
      final MyTestMBean mbean = client.getProxy(MyTestMBean.MBEAN_NAME, MyTestMBean.class);
      assertNotNull(mbean);
      mbean.reset();
      assertNull(mbean.getStringAttribute());
      assertEquals(0, mbean.getIntAttribute());
      mbean.setStringAttribute("test string");
      assertEquals("test string", mbean.getStringAttribute());
      mbean.setIntAttribute(3);
      assertEquals(3, mbean.getIntAttribute());
      assertEquals("myMethod1", mbean.myMethod1());
      assertEquals(6, mbean.myMethod2(3));
      assertEquals(8, mbean.myMethod2(4));
      Thread.sleep(500L);
      checkCounter(forwarder.getMap, "StringAttribute", 2);
      checkCounter(forwarder.setMap, "StringAttribute", 1);
      checkCounter(forwarder.getMap, "IntAttribute", 2);
      checkCounter(forwarder.setMap, "IntAttribute", 1);
      checkCounter(forwarder.invokeMap, "myMethod1", 1);
      checkCounter(forwarder.invokeMap, "myMethod2", 2);
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
    }
  }

  /**
   * Test an MBeanServerForwarder with a no-arg constructor and a {@code setParameters(String[])} method.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testMBeanForwarderWithSetParameters() throws Exception {
    final int port = 4446;
    JMXServer server = null;
    JMXConnectionWrapper client = null;
    try {
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, port)
        .set(JPPFProperties.MANAGEMENT_SERVER_FORWARDER, CollectionUtils.concatArrays(new String[] {Forwarder2.class.getName()}, PARAMS));
      server = JMXServerFactory.createServer(JPPFConfiguration.getProperties(), "clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      final MBeanServerForwarder mbsf = server.getMBeanServerForwarder();
      assertTrue(mbsf instanceof Forwarder2);
      final Forwarder2 forwarder = (Forwarder2) mbsf;
      final String[] params = forwarder.getParameters();
      assertTrue(Arrays.equals(PARAMS, params));
      client = new JMXConnectionWrapper("localhost", port, false);
      assertTrue(client.connectAndWait(5000L));
      final MyTestMBean mbean = client.getProxy(MyTestMBean.MBEAN_NAME, MyTestMBean.class);
      assertNotNull(mbean);
      mbean.reset();
      assertNull(mbean.getStringAttribute());
      assertEquals(0, mbean.getIntAttribute());
      mbean.setStringAttribute("test string");
      assertEquals("test string", mbean.getStringAttribute());
      mbean.setIntAttribute(3);
      assertEquals(3, mbean.getIntAttribute());
      assertEquals("myMethod1", mbean.myMethod1());
      assertEquals(6, mbean.myMethod2(3));
      assertEquals(8, mbean.myMethod2(4));
      checkCounter(forwarder.getMap, "StringAttribute", 2);
      checkCounter(forwarder.setMap, "StringAttribute", 1);
      checkCounter(forwarder.getMap, "IntAttribute", 2);
      checkCounter(forwarder.setMap, "IntAttribute", 1);
      checkCounter(forwarder.invokeMap, "myMethod1", 1);
      checkCounter(forwarder.invokeMap, "myMethod2", 2);
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
    }
  }

  /**
   * Check the counter value for the specified key in the specified map.
   * @param map the map in which to lookup the value.
   * @param key the key to lookup.
   * @param expected the expectd value.
   * @throws Exception if any error occurs.
   */
  private static void checkCounter(final Map<String, AtomicInteger> map, final String key, final int expected) throws Exception {
    print(false, false, "checking that counter for %s == %d", key, expected);
    if (expected == 0) assertNull(map.get(key));
    else {
      final AtomicInteger n = map.get(key);
      assertNotNull(n);
      assertEquals(expected, n.get());
    }
  }

  /**
   *
   */
  public abstract static class AbstractForwarder extends MBeanServerForwarderAdapter {
    /**
     * These maps hold counters for getAttribute(), setAttribute() or invoke() calls, where the key is an attribute or method name.
     */
    final Map<String, AtomicInteger> getMap = new HashMap<>(), setMap = new HashMap<>(), invokeMap = new HashMap<>();

    @Override
    public Object getAttribute(final ObjectName name, final String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
      final int n = inc(getMap, attribute);
      print(false, false, "counter for getter %s incremented to %d", attribute, n);
      return super.getAttribute(name, attribute);
    }

    @Override
    public void setAttribute(final ObjectName name, final Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
      final int n = inc(setMap, attribute.getName());
      print(false, false, "counter for setter %s incremented to %d", attribute, n);
      super.setAttribute(name, attribute);
    }

    @Override
    public Object invoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
      final int n = inc(invokeMap, operationName);
      print(false, false, "counter for invoke %s incremented to %d", operationName, n);
      return super.invoke(name, operationName, params, signature);
    }

    /**
     * Incrementer the counter for the specified key in he specified map.
     * @param map the map where the counter is found or created.
     * @param key the key for which to increment or create the counter.
     * @return the new counter value.
     */
    private static int inc(final Map<String, AtomicInteger> map, final String key) {
      AtomicInteger n;
      synchronized(map) {
        n = map.get(key);
        if (n == null) map.put(key, n = new AtomicInteger(0));
      }
      return n.incrementAndGet();
    }

    /**
     * Clear all the maps.
     */
    void reset() {
      getMap.clear();
      setMap.clear();
      invokeMap.clear();
    }
  }

  /** */
  public static class Forwarder1 extends AbstractForwarder {
    /**
     * Initialize with the specified parameters.
     * @param params the parameters.
     */
    public Forwarder1(final String[] params) {
      this.parameters = params;
    }
  }

  /** */
  public static class Forwarder2 extends AbstractForwarder {
  }

  /**
   * Test MBean interface.
   */
  public interface MyTestMBean {
    /**
     * Name of this MBean.
     */
    String MBEAN_NAME = "org.jppf:name=MyTestMBean,type=test";

    /**
     * Get the string attribute.
     * @return a String.
     */
    String getStringAttribute();

    /**
     * set the string attribute.
     * @param value a String.
     */
    void setStringAttribute(String value);

    /**
     * Get the int attribute.
     * @return an int.
     */
    int getIntAttribute();

    /**
     * set the int attribute.
     * @param value an int.
     */
    void setIntAttribute(int value);

    /**
     * A simple method.
     * @return the string {@code "myMethod1"}.
     */
    String myMethod1();

    /**
     * A simple method.
     * @param param an int.
     * @return the value {@code 2 * param}.
     */
    int myMethod2(int param);

    /**
     * Reset all attributes to their initial values.
     */
    void reset();
  }

  /**
   * MBean implementation.
   */
  public static class MyTest implements MyTestMBean {
    /** */
    private String stringAttribute;
    /** */
    private int intAttribute;

    @Override
    public String getStringAttribute() {
      return stringAttribute;
    }

    @Override
    public void setStringAttribute(final String value) {
      this.stringAttribute = value;
    }

    @Override
    public int getIntAttribute() {
      return intAttribute;
    }

    @Override
    public void setIntAttribute(final int value) {
      this.intAttribute = value;
    }

    @Override
    public String myMethod1() {
      return "myMethod1";
    }

    @Override
    public int myMethod2(final int param) {
      return 2 * param;
    }

    @Override
    public void reset() {
      stringAttribute = null;
      intAttribute = 0;
    }
  }
}
