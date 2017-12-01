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

package test.org.jppf.server.peer;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.node.policy.Equal;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Test a multi-server topology with 2 servers, 1 node attached to each server and 1 client.
 * @author Laurent Cohen
 */
public class TestMultiServer extends AbstractNonStandardSetup {
  /** */
  private static final int TIMEOUT = 10_000;

  /**
   * Launches 2 drivers with 1 node attached to each and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    TestConfiguration config = createConfig("p2p");
    config.driverLog4j = "classes/tests/config/p2p/log4j-driver.properties";
    client = BaseSetup.setup(2, 2, true, true, config);
  }

  /**
   * Wait until each driver has 1 idle node.
   * @throws Exception if any error occurs.
   */
  @Before
  public void instanceSetup() throws Exception {
    awaitPeersInitialized();
  }

  @Override
  @Test(timeout = 10000)
  public void testCancelJob() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testCancelJob();
  }

  /**
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
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
  @Test(timeout = 5000)
  public void testNotSerializableExceptionFromNode() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testNotSerializableExceptionFromNode();
  }

  /**
   * Test there there are 2 distinct connection pools, with 1 driver connection each.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testServerConnections() throws Exception {
    List<JPPFConnectionPool> pools = client.getConnectionPools();
    Collections.sort(pools, new Comparator<JPPFConnectionPool>() {
      @Override
      public int compare(final JPPFConnectionPool o1, final JPPFConnectionPool o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    assertNotNull(pools);
    assertEquals(2, pools.size());
    for (int i=1; i<=2; i++) {
      JPPFConnectionPool pool = pools.get(i-1);
      assertNotNull(pool);
      assertEquals("driver" + i, pool.getName());
      assertEquals(11100 + i, pool.getDriverPort());
      List<JPPFClientConnection> connections = pool.getConnections();
      assertNotNull(connections);
      assertEquals(1, connections.size());
      for (JPPFClientConnection c: connections) {
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
    final TopologyManager mgr = new TopologyManager(client);
    ConcurrentUtils.Condition cond = new ConcurrentUtils.Condition() {
      @Override
      public boolean evaluate() {
        List<TopologyDriver> drivers = mgr.getDrivers();
        if ((drivers != null) && (drivers.size() == 2)) {
          for (TopologyDriver driver: drivers) {
            List<TopologyPeer> peers = driver.getPeers();
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
