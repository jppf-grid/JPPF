/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package test.org.jppf.server.peer;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
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
public class TestMultiServer extends AbstractNonStandardSetup {
  /** */
  private static final long TIMEOUT = 10_000L;
  /** */
  @Rule
  public TestWatcher testMultiServerWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * Launches 2 drivers with 1 node attached to each and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = createConfig("p2p");
    config.driver.log4j = "classes/tests/config/p2p/log4j-driver.properties";
    client = BaseSetup.setup(2, 2, true, true, config);
  }

  /**
   * Wait until each driver has 1 idle node.
   * @throws Exception if any error occurs.
   */
  @Before
  public void instanceSetup() throws Exception {
    awaitPeersInitialized(15_000L);
  }

  @Override
  @Test(timeout = TIMEOUT)
  public void testCancelJob() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testCancelJob();
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TIMEOUT)
  public void testSimpleJob() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testSimpleJob(new Equal(JPPFProperties.SERVER_PORT.getName(), 11101));
  }

  @Override
  @Test(timeout = 15000)
  public void testMultipleJobs() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testMultipleJobs();
  }

  @Override
  @Test(timeout = TIMEOUT)
  public void testNotSerializableExceptionFromNode() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testNotSerializableExceptionFromNode();
  }

  /**
   * Test there there are 2 distinct connection pools, with 1 driver connection each.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TIMEOUT)
  public void testServerConnections() throws Exception {
    final List<JPPFConnectionPool> pools = client.getConnectionPools();
    Collections.sort(pools, new Comparator<JPPFConnectionPool>() {
      @Override
      public int compare(final JPPFConnectionPool o1, final JPPFConnectionPool o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    assertNotNull(pools);
    assertEquals(2, pools.size());
    for (int i=1; i<=2; i++) {
      final JPPFConnectionPool pool = pools.get(i-1);
      assertNotNull(pool);
      assertEquals("driver" + i, pool.getName());
      assertEquals(11100 + i, pool.getDriverPort());
      final List<JPPFClientConnection> connections = pool.getConnections();
      assertNotNull(connections);
      assertEquals(1, connections.size());
      for (final JPPFClientConnection c: connections) {
        assertNotNull(c);
        assertNotNull(c.getStatus());
        assertEquals(JPPFClientConnectionStatus.ACTIVE, c.getStatus());
        assertTrue(c.getStatus().isWorkingStatus());
      }
    }
  }

  /**
   * Test that the topology monuitoring API detects the peer conenctions.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TIMEOUT)
  public void testTopologyMonitoring() throws Exception {
    client.awaitConnectionPools(Operator.EQUAL, 2, Operator.AT_LEAST, 1, TIMEOUT - 500, JPPFClientConnectionStatus.workingStatuses());
    try (final TopologyManager mgr = new TopologyManager(client)) {
      final ConcurrentUtils.Condition cond = new ConcurrentUtils.Condition() {
        @Override
        public boolean evaluate() {
          final List<TopologyDriver> drivers = mgr.getDrivers();
          if ((drivers != null) && (drivers.size() == 2)) {
            for (final TopologyDriver driver: drivers) {
              final List<TopologyPeer> peers = driver.getPeers();
              if ((peers == null) || (peers.size() != 1)) return false;
            }
            return true;
          }
          return false;
        }
      };
      do {
        Thread.sleep(50L);
      } while (!cond.evaluate());
    }
  }

  /**
   * Test a job that is submitted via multiple channels on the client side in a p2p topology.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 15_000L)
  public void testJobMultipleChannels() throws Exception {
    final LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    final Map<JMXDriverConnectionWrapper, LoadBalancingInformation> lbiMap = new HashMap<>();
    try {
      //final TypedProperties props = new TypedProperties().setInt("performanceCacheSize", 1000).setInt("proportionalityFactor", 1).setInt("initialSize", 10).setDouble("initialMeanTime", 3e10);
      //client.setLoadBalancerSettings("proportional", props);
      client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 10));
      final TypedProperties propsDriver = new TypedProperties().setInt("size", 5);
      final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.EQUAL, 2, Operator.AT_LEAST, 1, TIMEOUT - 500, JPPFClientConnectionStatus.workingStatuses());
      for (final JPPFConnectionPool pool: pools) {
        final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
        final LoadBalancingInformation driverLbi = jmx.loadBalancerInformation();
        lbiMap.put(jmx, driverLbi);
        System.out.printf("load balancing config for driver %s: %s%n", jmx.getDisplayName(), driverLbi);
        jmx.changeLoadBalancerSettings("manual", propsDriver);
      }
      final int nbNodes = getNbNodes();
      final int nbTasks = 100;
      final String name = ReflectionUtils.getCurrentClassAndMethod();
      final JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 1L);
      job.getClientSLA().setMaxChannels(2);
      final List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      final CollectionMap<String, Task<?>> map = new ArrayListHashMap<>();
      for (final Task<?> t: results) {
        assertTrue("task = " + t, t instanceof LifeCycleTask);
        final LifeCycleTask task = (LifeCycleTask) t;
        map.putValue(task.getNodeUuid(), task);
        final Throwable throwable = t.getThrowable();
        assertNull("throwable for task '" + t.getId() + "' : " + ExceptionUtils.getStackTrace(throwable), throwable);
        assertNotNull(t.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, t.getResult());
      }
      BaseTest.printOut("%s : map = %s", name , CollectionUtils.prettyPrint(map));
      assertEquals(nbNodes, map.keySet().size());
    } finally {
      if (lbi != null) client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
      for (final Map.Entry<JMXDriverConnectionWrapper, LoadBalancingInformation> entry: lbiMap.entrySet()) {
        final LoadBalancingInformation driverLbi = entry.getValue();
        if (driverLbi != null) entry.getKey().changeLoadBalancerSettings(driverLbi.getAlgorithm(), driverLbi.getParameters());
      }
    }
  }
}
