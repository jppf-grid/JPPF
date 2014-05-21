/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.scheduler.bundle.LoadBalancingInformation;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFDriverAdminMBean}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFDriverAdminMBean extends Setup1D2N1C
{
  /**
   * Test getting statistics from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testGetStatistics() throws Exception
  {
    int nbTasks = 10;
    long duration = 100L;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    //waitKeyPressed();
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
    double nodeAvgTime = stats.getSnapshot(NODE_EXECUTION).getAvg();
    assertTrue(nodeAvgTime > 0d);
    double serverAvgTime = stats.getSnapshot(EXECUTION).getAvg();
    assertTrue(serverAvgTime > 0);
    assertTrue(serverAvgTime >= nodeAvgTime);
    double serverMaxTime = stats.getSnapshot(EXECUTION).getMax();
    assertTrue(serverMaxTime >= stats.getSnapshot(NODE_EXECUTION).getMax());
    //assertTrue(stats.getNodeExecution().getMax() >= nodeAvgTime);
    assertTrue(nbTasks == stats.getSnapshot(TASK_DISPATCH).getTotal());
  }

  /**
   * Test resetting statistics in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testResetStatistics() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    client.submitJob(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 10, LifeCycleTask.class, 100L));
    while (driver.nbIdleNodes() < 2) Thread.sleep(10L);
    driver.resetStatistics();
    JPPFStatistics stats = driver.statistics();
    assertNotNull(stats);
    int n = (int) stats.getSnapshot(NODES).getLatest();
    assertEquals(2, n);
    assertTrue(stats.getSnapshot(TASK_DISPATCH).getTotal() == 0);
    assertTrue(stats.getSnapshot(EXECUTION).getAvg() == 0d);
    assertTrue(stats.getSnapshot(NODE_EXECUTION).getMax() == 0d);
  }

  /**
   * Test getting node management information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNodesInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Collection<JPPFManagementInfo> coll = driver.nodesInformation();
    assertNotNull(coll);
    assertEquals(2, coll.size());
    for (JPPFManagementInfo info: coll)
    {
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
   * Test getting the number of nodes attached to the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNbNodes() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    int n = driver.nbNodes();
    assertEquals(2, n);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testGetLoadBalancerInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/driver.template.properties");
    assertNotNull(is);
    TypedProperties driverConfig = new TypedProperties();
    driverConfig.load(is);
    LoadBalancingInformation lbi = driver.loadBalancerInformation();
    assertTrue(lbi.getAlgorithmNames().contains(driverConfig.getString("jppf.load.balancing.algorithm")));
    assertEquals(driverConfig.getString("jppf.load.balancing.algorithm"), lbi.getAlgorithm());
    TypedProperties params = lbi.getParameters();
    assertNotNull(params);
    String profile = driverConfig.getString("jppf.load.balancing.profile");
    String prefix = "jppf.load.balancing.profile." + profile + '.';
    for (Map.Entry entry: driverConfig.entrySet())
    {
      if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) continue;
      String name = (String) entry.getKey();
      if (!name.startsWith(prefix)) continue;
      String key = name.substring(prefix.length());
      String value = (String) entry.getValue();
      assertTrue("information does not contain '" + key + '\'', params.containsKey(key));
      String infoValue = params.getString(key);
      assertNotNull(infoValue);
      assertEquals("value of '" + key + "' should be '" + value + "' but is '" + infoValue + "'", value, infoValue);
    }
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testSetLoadBalancerInformation() throws Exception
  {
    LoadBalancingInformation oldLbi = null;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    try
    {
      assertNotNull(driver);
      oldLbi = driver.loadBalancerInformation();
      TypedProperties newConfig = new TypedProperties();
      newConfig.setProperty("size", "5");
      newConfig.setProperty("minSamplesToAnalyse", "100");
      newConfig.setProperty("minSamplesToCheckConvergence", "50");
      newConfig.setProperty("maxDeviation", "0.2");
      newConfig.setProperty("maxGuessToStable", "50");
      newConfig.setProperty("decreaseRatio", "0.2");
      driver.changeLoadBalancerSettings("autotuned", newConfig);
      LoadBalancingInformation lbi = driver.loadBalancerInformation();
      assertEquals(lbi.getAlgorithm(), "autotuned");
      TypedProperties params = lbi.getParameters();
      assertNotNull(params);
      for (Map.Entry entry: newConfig.entrySet())
      {
        if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof String)) continue;
        String name = (String) entry.getKey();
        String value = (String) entry.getValue();
        assertTrue("information does not contain '" + name + '\'', params.containsKey(name));
        String infoValue = params.getString(name);
        assertNotNull(infoValue);
        assertEquals("value of '" + name + "' should be '" + value + "' but is '" + infoValue + "'", value, infoValue);
      }
    }
    finally
    {
      if (oldLbi != null) driver.changeLoadBalancerSettings(oldLbi.getAlgorithm(), oldLbi.getParameters());
    }
  }

  /**
   * Test of matching the nodes against an execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNodesMatchingExecutionPolicy() throws Exception
  {
    int nbNodes = 2;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    int n = driver.matchingNodes(new Contains("jppf.node.uuid", false, "n"));
    assertEquals(nbNodes, n);
    n = driver.matchingNodes(new Equal("jppf.node.uuid", false, "n1"));
    assertEquals(1, n);
  }

  /**
   * Test getting idle nodes information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testIdleNodesInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Thread.sleep(500L);
    Collection<JPPFManagementInfo> coll = driver.idleNodesInformation();
    assertNotNull(coll);
    assertEquals(2, coll.size());
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, 1, LifeCycleTask.class, 2000L);
    client.submitJob(job);
    Thread.sleep(500L);
    coll = driver.idleNodesInformation();
    assertEquals(1, coll.size());
    ((JPPFResultCollector) job.getResultListener()).awaitResults();
    while (driver.nbIdleNodes() < 2) Thread.sleep(100L);
  }

  /**
   * Test getting the number of idle nodes from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNbIdleNodes() throws Exception
  {
    int nbNodes = 2;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Thread.sleep(500L);
    int n = driver.nbIdleNodes();
    assertEquals(nbNodes, n);
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, 1, LifeCycleTask.class, 2000L);
    client.submitJob(job);
    Thread.sleep(500L);
    n = driver.nbIdleNodes();
    assertEquals(nbNodes - 1, n);
    ((JPPFResultCollector) job.getResultListener()).awaitResults();
    while (driver.nbIdleNodes() < 2) Thread.sleep(100L);
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testActivateNode() throws Exception
  {
    int nbTasks = 10;
    JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    String[] nodeUuids = new String[2];
    assertNotNull(driver);
    NodeSelector selector = null;
    try
    {
      Collection<JPPFManagementInfo> nodesList = driver.nodesInformation();
      assertEquals(2, nodesList.size());
      int i = 0;
      for (JPPFManagementInfo info: nodesList) nodeUuids[i++] = info.getUuid();
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-1", true, false, nbTasks, LifeCycleTask.class, 0L);
      Set<String> executedOnUuids = new HashSet<>();
      List<Task<?>> results = client.submitJob(job);
      for (Task<?> t: results)
      {
        LifeCycleTask task = (LifeCycleTask) t;
        if (!executedOnUuids.contains(task.getNodeUuid())) executedOnUuids.add(task.getNodeUuid());
      }
      assertTrue(executedOnUuids.contains(nodeUuids[0]));
      assertTrue(executedOnUuids.contains(nodeUuids[1]));
      assertEquals(2, executedOnUuids.size());
      executedOnUuids.clear();
      // deactivate one node and make sure the job is only executed on the other node
      selector = new NodeSelector.UuidSelector(nodeUuids[1]);
      driver.toggleActiveState(selector);
      job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-2", true, false, nbTasks, LifeCycleTask.class, 0L);
      results = client.submitJob(job);
      for (Task<?> t: results)
      {
        LifeCycleTask task = (LifeCycleTask) t;
        if (!executedOnUuids.contains(task.getNodeUuid())) executedOnUuids.add(task.getNodeUuid());
      }
      assertTrue(executedOnUuids.contains(nodeUuids[0]));
      assertFalse(executedOnUuids.contains(nodeUuids[1]));
      assertEquals(1, executedOnUuids.size());
    }
    finally
    {
      for (String uuid: nodeUuids)
      {
        if (uuid != null) driver.toggleActiveState(selector);
      }
    }
  }

  /**
   * Display a message and wait until a key is pressed.
   * @throws Exception if any I/O error occurs.
   */
  public static void waitKeyPressed() throws Exception
  {
    System.out.println("press any key to continue ...");
    System.in.read();
  }
}
