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

import static org.jppf.utils.stats.JPPFStatisticsHelper.*;
import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
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
    client.submitJob(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, duration));
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
    client.submitJob(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 10, LifeCycleTask.class, 100L));
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
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    final Collection<JPPFManagementInfo> coll = driver.nodesInformation();
    assertNotNull(coll);
    assertEquals(2, coll.size());
    for (final JPPFManagementInfo info : coll) {
      assertNotNull(info.getHost());
      assertTrue(info.getPort() > 0);
      assertFalse(info.isSecure());
      assertTrue(info.isNode());
      assertFalse(info.isDriver());
      assertFalse(info.isPeer());
      assertNotNull(info.getUuid());
    }
  }

  /**
   * Test getting idle nodes information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testIdleNodesInformation() throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Thread.sleep(500L);
    Collection<JPPFManagementInfo> coll = driver.idleNodesInformation();
    assertNotNull(coll);
    assertEquals(2, coll.size());
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, 1, LifeCycleTask.class, 2000L);
    client.submitJob(job);
    Thread.sleep(500L);
    coll = driver.idleNodesInformation();
    assertEquals(1, coll.size());
    job.awaitResults();
    while (driver.nbIdleNodes() < 2)
      Thread.sleep(100L);
  }

  /**
   * Test getting the number of nodes attached to the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNbNodes() throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    final int n = driver.nbNodes();
    assertEquals(2, n);
  }

  /**
   * Test getting the number of idle nodes from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNbIdleNodes() throws Exception {
    final int nbNodes = 2;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Thread.sleep(500L);
    int n = driver.nbIdleNodes();
    assertEquals(nbNodes, n);
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, 1, LifeCycleTask.class, 2000L);
    client.submitJob(job);
    Thread.sleep(1000L);
    n = driver.nbIdleNodes();
    assertEquals(nbNodes - 1, n);
    job.awaitResults();
    while (driver.nbIdleNodes() < 2)
      Thread.sleep(100L);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testGetLoadBalancerInformation() throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    final InputStream is = getClass().getClassLoader().getResourceAsStream("config/driver.template.properties");
    assertNotNull(is);
    final TypedProperties driverConfig = new TypedProperties();
    driverConfig.load(is);
    final LoadBalancingInformation lbi = driver.loadBalancerInformation();
    assertTrue(lbi.getAlgorithmNames().contains(driverConfig.get(JPPFProperties.LOAD_BALANCING_ALGORITHM)));
    assertEquals(driverConfig.get(JPPFProperties.LOAD_BALANCING_ALGORITHM), lbi.getAlgorithm());
    final TypedProperties params = lbi.getParameters();
    assertNotNull(params);
    final String profile = driverConfig.get(JPPFProperties.LOAD_BALANCING_PROFILE);
    final String prefix = JPPFProperties.LOAD_BALANCING_PROFILE.getName() + "." + profile + '.';
    for (final Map.Entry<Object, Object> entry : driverConfig.entrySet()) {
      if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) continue;
      final String name = (String) entry.getKey();
      if (!name.startsWith(prefix)) continue;
      final String key = name.substring(prefix.length());
      final String value = (String) entry.getValue();
      assertTrue("information does not contain '" + key + '\'', params.containsKey(key));
      final String infoValue = params.getString(key);
      assertNotNull(infoValue);
      assertEquals("value of '" + key + "' should be '" + value + "' but is '" + infoValue + "'", value, infoValue);
    }
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testSetLoadBalancerInformation() throws Exception {
    LoadBalancingInformation oldLbi = null;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    try {
      assertNotNull(driver);
      oldLbi = driver.loadBalancerInformation();
      final TypedProperties newConfig = new TypedProperties();
      newConfig.setInt("size", 5)
        .setInt("minSamplesToAnalyse", 100)
        .setInt("minSamplesToCheckConvergence", 50)
        .setDouble("maxDeviation", 0.2d)
        .setInt("maxGuessToStable", 50)
        .setDouble("decreaseRatio", 0.2d);
      driver.changeLoadBalancerSettings("autotuned", newConfig);
      final LoadBalancingInformation lbi = driver.loadBalancerInformation();
      assertEquals(lbi.getAlgorithm(), "autotuned");
      final TypedProperties params = lbi.getParameters();
      assertNotNull(params);
      for (final Map.Entry<Object, Object> entry : newConfig.entrySet()) {
        if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) continue;
        final String name = (String) entry.getKey();
        final String value = (String) entry.getValue();
        assertTrue("information does not contain '" + name + '\'', params.containsKey(name));
        final String infoValue = params.getString(name);
        assertNotNull(infoValue);
        assertEquals("value of '" + name + "' should be '" + value + "' but is '" + infoValue + "'", value, infoValue);
      }
    } finally {
      if (oldLbi != null) driver.changeLoadBalancerSettings(oldLbi.getAlgorithm(), oldLbi.getParameters());
    }
  }

  /**
   * Test of matching the nodes against an execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testNodesMatchingExecutionPolicy() throws Exception {
    final int nbNodes = 2;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    int n = driver.nbNodes(new ExecutionPolicySelector(new Contains("jppf.node.uuid", false, "n")));
    assertEquals(nbNodes, n);
    n = driver.nbNodes(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")));
    assertEquals(1, n);
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testToggleActiveState() throws Exception {
    final int nbTasks = 10;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    final String[] nodeUuids = new String[2];
    assertNotNull(driver);
    try {
      final Collection<JPPFManagementInfo> nodesList = driver.nodesInformation();
      assertEquals(2, nodesList.size());
      int i = 0;
      for (final JPPFManagementInfo info : nodesList) nodeUuids[i++] = info.getUuid();
      // recheck that the job is executed on all nodes
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-1", true, false, nbTasks, LifeCycleTask.class, 0L);
      final Set<String> executedOnUuids = new HashSet<>();
      List<Task<?>> results = client.submitJob(job);
      for (final Task<?> t : results) {
        final LifeCycleTask task = (LifeCycleTask) t;
        if (!executedOnUuids.contains(task.getNodeUuid())) executedOnUuids.add(task.getNodeUuid());
      }
      assertEquals(2, executedOnUuids.size());
      for (String uuid: nodeUuids) assertTrue(executedOnUuids.contains(uuid));
      // deactivate one node and make sure the job is only executed on the other node
      executedOnUuids.clear();
      final NodeSelector selector = new UuidSelector(nodeUuids[1]);
      driver.toggleActiveState(selector);
      job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-2", true, false, nbTasks, LifeCycleTask.class, 0L);
      results = client.submitJob(job);
      for (final Task<?> t : results) {
        final LifeCycleTask task = (LifeCycleTask) t;
        if (!executedOnUuids.contains(task.getNodeUuid())) executedOnUuids.add(task.getNodeUuid());
      }
      assertTrue(executedOnUuids.contains(nodeUuids[0]));
      assertFalse(executedOnUuids.contains(nodeUuids[1]));
      assertEquals(1, executedOnUuids.size());
    } finally {
      driver.setActiveState(null, true);
    }
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000L)
  public void testGetAndSetActiveState() throws Exception {
    final int nbTasks = 10;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    String[] nodeUuids = null;
    assertNotNull(driver);
    try {
      final Set<String> executedOnUuids = new HashSet<>();
      final Collection<JPPFManagementInfo> nodesList = driver.nodesInformation();
      assertEquals(2, nodesList.size());
      nodeUuids = new String[nodesList.size()];
      int i = 0;
      for (final JPPFManagementInfo info : nodesList) nodeUuids[i++] = info.getUuid();
      // deactivate one node and make sure the job is only executed on the other node
      final NodeSelector selector = new UuidSelector(nodeUuids[0]);
      driver.setActiveState(selector, false);
      final Map<String, Boolean> map = driver.getActiveState(null);
      assertEquals(nodeUuids.length, map.size());
      assertTrue(map.containsKey(nodeUuids[0]));
      assertFalse(map.get(nodeUuids[0]));
      assertTrue(map.containsKey(nodeUuids[1]));
      assertTrue(map.get(nodeUuids[1]));
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-1", true, false, nbTasks, LifeCycleTask.class, 0L);
      List<Task<?>> results = client.submitJob(job);
      for (final Task<?> t : results) {
        final LifeCycleTask task = (LifeCycleTask) t;
        if (!executedOnUuids.contains(task.getNodeUuid())) executedOnUuids.add(task.getNodeUuid());
      }
      assertTrue(executedOnUuids.contains(nodeUuids[1]));
      assertFalse(executedOnUuids.contains(nodeUuids[0]));
      assertEquals(1, executedOnUuids.size());
      // re-activate the node and check that the job is executed on all nodes
      executedOnUuids.clear();
      driver.setActiveState(selector, true);
      job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-2", true, false, nbTasks, LifeCycleTask.class, 0L);
      results = client.submitJob(job);
      for (final Task<?> t : results) {
        final LifeCycleTask task = (LifeCycleTask) t;
        if (!executedOnUuids.contains(task.getNodeUuid())) executedOnUuids.add(task.getNodeUuid());
      }
      assertEquals(nodeUuids.length, executedOnUuids.size());
      for (String uuid: nodeUuids) assertTrue(executedOnUuids.contains(uuid));
    } finally {
       driver.setActiveState(null, true);
    }
  }
}
