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

package test.org.jppf.discovery;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.*;

import org.jppf.client.*;
import org.jppf.discovery.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to each server and 1 client.
 * @author Laurent Cohen
 */
public class TestDriverDiscovery extends AbstractNonStandardSetup {
  /**
   * Path to the folder containng the scripts.
   */
  private final String resourcePath = getClass().getPackage().getName().replace(".", "/");
  /**
   * JMX connections to all drivers.
   */
  private static final JMXDriverConnectionWrapper[] JMX = new JMXDriverConnectionWrapper[2];
  /** */
  @Rule
  public TestWatcher testDriverDiscoveryInstanceWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      try {
        final String msg = String.format( "***** start of method %s() *****", description.getMethodName());
        final String banner = StringUtils.padLeft("", '*', msg.length(), false);
        for (final JMXDriverConnectionWrapper jmx: JMX) logInServer(jmx, banner, msg, banner);
      } catch(final Exception e) {
        e.printStackTrace();
      }
    }
  };

  /**
   * Launches 2 drivers with 1 node attached to each.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = createConfig("discovery");
    config.driver.log4j = "classes/tests/config/discovery/log4j-driver.properties";
    BaseSetup.setup(2, 2, false, true, config);
    final long start = System.currentTimeMillis();
    final long timeout = 60_000L;
    for (int i=0; i<JMX.length; i++) {
      BaseTest.print(false, false, "connecting to server %d", (i + 1));
      JMX[i] = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1 + i);
      final JMXDriverConnectionWrapper jmx = JMX[i];
      BaseTest.print(false, false, "connecting to %s", jmx);
      jmx.connectAndWait(timeout - (System.currentTimeMillis() - start));
      assertTrue("failed to connect to " + jmx, jmx.isConnected());
      BaseTest.print(false, false, "connected to %s", jmx);
      awaitNbIdleNodes(jmx, Operator.EQUAL, 1, 5000L);
    }
  }

  /**
   * Close the JMX connections.
   * @throws Exception if a process could not be started.
   */
  @AfterClass
  public static void teardown() throws Exception {
    BaseSetup.generateDriverThreadDump(JMX);
    for (int i=0; i<JMX.length; i++) {
      if (JMX[i] != null) JMX[i].close();
    }
  }

  /**
   * Test that we can add a {@link org.jppf.discovery.PeerDriverDiscovery PeerDriverDiscovery} to each driver, allowing them to discover each other.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testServerSide() throws Exception {
    for (int i=0; i<2; i++) {
      assertEquals("ok", executeScriptOnServer(JMX[i], FileUtils.readTextFile(resourcePath + "/SetPeerDiscovery.js")));
    }
    final String[] results = new String[2];
    boolean good = false;
    while (!good) {
      good = true;
      for (int i=0; i<2; i++) {
        final String result = (String) executeScriptOnServer(JMX[i], FileUtils.readTextFile(resourcePath + "/RetrievePeerDiscovery.js"));
        if (result.startsWith("ko")) {
          good = false;
          BaseTest.print(false, false, "driver response: %s", result);
          Thread.sleep(500L);
          break;
        } else {
          results[i] = result;
        }
      }
    }
    for (int i=0; i<2; i++) {
      BaseTest.print(false, false, "checking server %d", (i + 1));
      final TypedProperties props = new TypedProperties();
      props.load(new StringReader(results[i]));
      assertEquals("localhost", props.getString("host"));
      assertEquals(11102 - i, props.getInt("port"));
      assertFalse(props.getBoolean("secure"));
      assertEquals("custom_discovery", props.getString("name"));
      assertEquals(11101 + i, props.getInt("channel.local.port"));
      assertFalse(props.getBoolean("channel.secure"));
    }
  }

  /**
   * Test there there are 2 distinct connection pools, with 1 driver connection each.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testClientSide() throws Exception {
    JPPFConfiguration.set(JPPFProperties.REMOTE_EXECUTION_ENABLED, false);
    final ClientDiscovery discovery = new ClientDiscovery();
    try (JPPFClient client = new JPPFClient()) {
      AbstractNonStandardSetup.client = client;
      client.addDriverDiscovery(discovery);
      List<JPPFConnectionPool> pools = null;
      boolean end = false;
      while (!end) {
        pools = client.getConnectionPools();
        if (pools.size() < 2) Thread.sleep(100L);
        else end = true;
      }
      for (final JPPFConnectionPool pool: pools) pool.awaitActiveConnection();
      Collections.sort(pools, new Comparator<JPPFConnectionPool>() {
        @Override
        public int compare(final JPPFConnectionPool p1, final JPPFConnectionPool p2) {
          return p1.getName().compareTo(p2.getName());
        }
      });
      assertNotNull(pools);
      assertEquals(2, pools.size());
      for (int i=0; i<2; i++) {
        final JPPFConnectionPool pool = pools.get(i);
        assertNotNull(pool);
        final int port = 11101 + i;
        assertEquals("ClientDiscovery_" + port, pool.getName());
        assertEquals(port, pool.getDriverPort());
        assertEquals("localhost", pool.getDriverHost());
        assertFalse(pool.isSslEnabled());
        assertEquals(2 - i, pool.getPriority());
        assertEquals(1, pool.getSize());
        assertEquals(1, pool.getJMXPoolSize());
      }
      final int tasksPerNode = 5;
      final int nbNodes = BaseSetup.nbNodes();
      final int nbTasks = tasksPerNode * nbNodes;
      final String name = ReflectionUtils.getCurrentMethodName();
      final JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 1L);
      final List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      for (final Task<?> t: results) {
        assertTrue("task = " + t, t instanceof LifeCycleTask);
        assertNull(t.getThrowable());
        assertNotNull(t.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, t.getResult());
      }
      //AbstractNonStandardSetup.client = null;
    }
    assertTrue(discovery.shutdownFlag);
  }

  /** */
  public static class ClientDiscovery extends ClientDriverDiscovery {
    /**
     * Whether this discovery was shutdown.
     */
    boolean shutdownFlag = false;
  
    /** */
    public ClientDiscovery() {
      BaseTest.printOut("in %s() contructor", getClass().getSimpleName());
    }
  
    @Override
    public void discover() {
      final String className = getClass().getSimpleName();
      for (int i=0; i<2; i++) {
        final int port = 11101 + i;
        final ClientConnectionPoolInfo info = new ClientConnectionPoolInfo("ClientDiscovery_" + port, false, "localhost", port, 2 - i, 1, 1);
        BaseTest.printOut("%s 'discovering' %s", className, info);
        newConnection(info);
      }
    }
  
    @Override
    public void shutdown() {
      synchronized(this) {
        shutdownFlag = true;
      }
    }
  }
}
