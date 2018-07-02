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

package test.org.jppf.server;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Test a topology with 1 servers with a local node and 1 client.
 * @author Laurent Cohen
 */
public class TestLocalNode extends AbstractNonStandardSetup {
  /**
   * Launches 1 drivers with a local node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration config = createConfig("localnode");
    config.driver.log4j = "classes/tests/config/localnode/log4j-driver.properties";
    client = BaseSetup.setup(1, 0, true, false, config);
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1, false)) {
      jmx.connectAndWait(5_000L);
      final ConcurrentUtils.Condition cond = new ConcurrentUtils.ConditionFalseOnException() {
        @Override
        public boolean evaluateWithException() throws Exception {
          return jmx.nbNodes() > 0;
        }
      };
      print(false, false, "before wait for interruptible condition");
      assertTrue(ConcurrentUtils.awaitCondition(cond, 10_000L, 100L, true));
    }
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
    super.testSimpleJob(null, "ln");
  }

  @Override
  @Test(timeout = 15000)
  public void testMultipleJobs() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testMultipleJobs();
  }

  @Override
  @Test(timeout = 10000)
  public void testNotSerializableExceptionFromNode() throws Exception {
    BaseTestHelper.printToServers(client, "start of %s()", ReflectionUtils.getCurrentMethodName());
    super.testNotSerializableExceptionFromNode();
  }

  /**
   * Test there there are 2 distinct connection pools, with 1 driver connection each.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testServerConnections() throws Exception {
    Thread.sleep(200L);
    final List<JPPFConnectionPool> pools = client.getConnectionPools();
    assertNotNull(pools);
    assertEquals(1, pools.size());
    final JPPFConnectionPool pool = pools.get(0);
    assertNotNull(pool);
    assertEquals("driver1", pool.getName());
    assertEquals(11101, pool.getDriverPort());
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

  @Override
  @Test(timeout = 10000)
  public void testForwardingMBean() throws Exception {
    super.testForwardingMBean();
  }

  @Override
  protected int getNbNodes() {
    return 1;
  }
}
