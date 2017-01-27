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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;
import javax.management.remote.MBeanServerForwarder;

import org.jppf.management.*;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.collections.CollectionUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.BaseTest;

/**
 * Unit tests for the the JMXMPServer class.
 * @author Laurent Cohen
 */
public class TestJMXMPServer extends BaseTest {
  /** */
  private static final String[] PARAMS = {"param1", "param2", "param3"};
  /** */
  private final static int PORT = 4444;

  /**
   * Register the test mbean.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void setup() throws Exception {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.registerMBean(new StandardMBean(new MyTest(), MyTestMBean.class), new ObjectName(MyTestMBean.MBEAN_NAME));
  }

  /**
   * Unregister the test mbean.
   * @throws Exception if any error occurs.
   */
  @AfterClass
  public static void teardown() throws Exception {
    JPPFConfiguration.remove(JPPFProperties.MANAGEMENT_SERVER_FORWARDER);
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    server.unregisterMBean(new ObjectName(MyTestMBean.MBEAN_NAME));
  }

  /**
   * Test without an MBeanServerForwarder.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testNoMBeanForwarder() throws Exception {
    JMXServer server = null;
    try {
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, PORT);
      JPPFConfiguration.remove(JPPFProperties.MANAGEMENT_SERVER_FORWARDER);
      server = new JMXMPServer("clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      MBeanServerForwarder mbsf = server.getMBeanServerForwarder();
      assertNull(mbsf);
    } finally {
      if (server != null) {
        try {
          server.stop();
        } catch (Exception e) {
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
    JMXServer server = null;
    JMXConnectionWrapper client = null;
    try {
      JPPFConfiguration.set(JPPFProperties.MANAGEMENT_PORT, PORT)
      .set(JPPFProperties.MANAGEMENT_SERVER_FORWARDER, CollectionUtils.concatArrays(new String[] {Forwarder1.class.getName()}, PARAMS));
      server = new JMXMPServer("clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      MBeanServerForwarder mbsf = server.getMBeanServerForwarder();
      assertTrue(mbsf instanceof Forwarder1);
      Forwarder1 forwarder = (Forwarder1) mbsf;
      String[] params = forwarder.getParameters();
      assertTrue(Arrays.equals(PARAMS, params));
      client = new JMXConnectionWrapper("localhost", PORT, false);
      client.connectAndWait(3000L);
      assertTrue(client.isConnected());
      MyTestMBean mbean = client.getProxy(MyTestMBean.MBEAN_NAME, MyTestMBean.class);
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
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (server != null) {
        try {
          server.stop();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * Test an MBeanServerForwarder with a no-arg constructor and a {@code setParameters(String[])} method.
   * @throws Exception if any error occurs
   */
  //@Test(timeout = 10000)
  @Test
  public void testMBeanForwarderWithSetParameters() throws Exception {
    JMXServer server = null;
    JMXConnectionWrapper client = null;
    try {
      JPPFConfiguration
        .set(JPPFProperties.MANAGEMENT_PORT, PORT)
        .set(JPPFProperties.MANAGEMENT_SERVER_FORWARDER, CollectionUtils.concatArrays(new String[] {Forwarder2.class.getName()}, PARAMS));
      server = new JMXMPServer("clientTest", false, JPPFProperties.MANAGEMENT_PORT);
      server.start(getClass().getClassLoader());
      MBeanServerForwarder mbsf = server.getMBeanServerForwarder();
      assertTrue(mbsf instanceof Forwarder2);
      Forwarder2 forwarder = (Forwarder2) mbsf;
      String[] params = forwarder.getParameters();
      assertTrue(Arrays.equals(PARAMS, params));
      client = new JMXConnectionWrapper("localhost", PORT, false);
      client.connectAndWait(3000L);
      assertTrue(client.isConnected());
      MyTestMBean mbean = client.getProxy(MyTestMBean.MBEAN_NAME, MyTestMBean.class);
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
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      if (server != null) {
        try {
          server.stop();
        } catch (Exception e) {
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
  private void checkCounter(final Map<String, AtomicInteger> map, final String key, final int expected) throws Exception {
    if (expected == 0) assertNull(map.get(key));
    else {
      AtomicInteger n = map.get(key);
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
    Map<String, AtomicInteger> getMap = new HashMap<>(), setMap = new HashMap<>(), invokeMap = new HashMap<>();

    @Override
    public Object getAttribute(final ObjectName name, final String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException {
      inc(getMap, attribute);
      return super.getAttribute(name, attribute);
    }

    @Override
    public void setAttribute(final ObjectName name, final Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
      inc(setMap, attribute.getName());
      super.setAttribute(name, attribute);
    }

    @Override
    public Object invoke(final ObjectName name, final String operationName, final Object[] params, final String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException {
      inc(invokeMap, operationName);
      return super.invoke(name, operationName, params, signature);
    }

    /**
     * Incrementer the counter for the specified key in he specified map.
     * @param map the map where the counter is found or created.
     * @param key the key for which to increment or create the counter.
     */
    private void inc(final Map<String, AtomicInteger> map, final String key) {
      AtomicInteger n = map.get(key);
      if (n == null) map.put(key, n = new AtomicInteger(0));
      n.incrementAndGet();
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
