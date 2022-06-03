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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;
import static org.junit.Assert.*;

import org.jppf.management.*;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.stats.JPPFStatistics;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFDriverAdminMBean}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFDriverAdminMBean extends Setup1D2N1C {
  /**
   * Test getting statistics from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testGetStatistics() throws Exception {
    final int nbTasks = 10;
    final long duration = 100L;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    driver.resetStatistics();
    JPPFStatistics stats = driver.statistics();
    assertNotNull(stats);
    double n = stats.getSnapshot(NODES).getLatest();
    assertTrue("nb nodes should be 2 but is " + n, n == 2d);
    assertTrue(stats.getSnapshot(TASK_DISPATCH).getTotal() == 0d);
    client.submit(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, duration));
    while (driver.nbIdleNodes() < 2) Thread.sleep(10L);
    stats = driver.statistics();
    n = stats.getSnapshot(IDLE_NODES).getLatest();
    assertTrue("nb idle nodes should be 2 but is " + n, n == 2d);
    final double nodeAvgTime = stats.getSnapshot(NODE_EXECUTION).getAvg();
    assertTrue(nodeAvgTime > 0d);
    final double serverAvgTime = stats.getSnapshot(EXECUTION).getAvg();
    assertTrue(serverAvgTime > 0);
    assertTrue(serverAvgTime >= nodeAvgTime);
    final double serverMaxTime = stats.getSnapshot(EXECUTION).getMax();
    assertTrue(serverMaxTime >= stats.getSnapshot(NODE_EXECUTION).getMax());
    assertTrue(nbTasks == stats.getSnapshot(TASK_DISPATCH).getTotal());
  }

  /**
   * Test resetting statistics in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testResetStatistics() throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    client.submit(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 10, LifeCycleTask.class, 100L));
    while (driver.nbIdleNodes() < 2) Thread.sleep(10L);
    driver.resetStatistics();
    final JPPFStatistics stats = driver.statistics();
    assertNotNull(stats);
    final int n = (int) stats.getSnapshot(NODES).getLatest();
    assertEquals(2, n);
    assertTrue(stats.getSnapshot(TASK_DISPATCH).getTotal() == 0);
    assertTrue(stats.getSnapshot(EXECUTION).getAvg() == 0d);
    assertTrue(stats.getSnapshot(NODE_EXECUTION).getMax() == 0d);
  }

  /**
   * Test getting node management information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNodesInformation() throws Exception {
    CommonDriverAdminTests.testNodesInformation(client, BaseSetup.nbNodes());
  }

  /**
   * Test getting idle nodes information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testIdleNodesInformation() throws Exception {
    CommonDriverAdminTests.testIdleNodesInformation(client, BaseSetup.nbNodes());
  }

  /**
   * Test getting the number of nodes attached to the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNbNodes() throws Exception {
    CommonDriverAdminTests.testNbNodes(client, BaseSetup.nbNodes());
  }

  /**
   * Test getting the number of idle nodes from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNbIdleNodes() throws Exception {
    CommonDriverAdminTests.testNbIdleNodes(client, BaseSetup.nbNodes());
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testGetLoadBalancerInformation() throws Exception {
    CommonDriverAdminTests.testGetLoadBalancerInformation(client);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testSetLoadBalancerInformation() throws Exception {
    CommonDriverAdminTests.testSetLoadBalancerInformation(client);
  }

  /**
   * Test of matching the nodes against an execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNodesMatchingExecutionPolicy() throws Exception {
    CommonDriverAdminTests.testNodesMatchingExecutionPolicy(client, BaseSetup.nbNodes());
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testToggleActiveState() throws Exception {
    CommonDriverAdminTests.testToggleActiveState(client, BaseSetup.nbNodes());
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testGetAndSetActiveState() throws Exception {
    CommonDriverAdminTests.testGetAndSetActiveState(client, BaseSetup.nbNodes());
  }
}
