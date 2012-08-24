/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.io.InputStream;
import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.server.JPPFStats;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.LoadBalancingInformation;
import org.jppf.utils.TypedProperties;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.LifeCycleTask;

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
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    //waitKeyPressed();
    driver.resetStatistics();
    JPPFStats stats = driver.statistics();
    assertNotNull(stats);
    long n = stats.getNodes().getLatest();
    assertTrue("nb nodes should be 2 but is " + n, n == 2);
    assertTrue(stats.getTotalTasksExecuted() == 0);
    client.submit(BaseSetup.createJob("TestStatistics", true, false, 10, LifeCycleTask.class, 100L));
    stats = driver.statistics();
    n = stats.getIdleNodes().getLatest();
    assertTrue("nb idle nodes should be 2 but is " + n, n == 2);
    assertTrue(stats.getExecution().getAvg() > 0d);
    assertTrue(stats.getNodeExecution().getMax() > 0d);
    assertEquals(10, stats.getTotalTasksExecuted());
  }

  /**
   * Test getting statistics from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testResetStatistics() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    client.submit(BaseSetup.createJob("TestStatistics", true, false, 10, LifeCycleTask.class, 100L));
    driver.resetStatistics();
    JPPFStats stats = driver.statistics();
    assertNotNull(stats);
    long n = stats.getNodes().getLatest();
    assertTrue("nb nodes should be 2 but is " + n, n == 2);
    assertTrue(stats.getTotalTasksExecuted() == 0);
    n = stats.getIdleNodes().getLatest();
    assertTrue("nb idle nodes should be 2 but is " + n, n == 2);
    assertTrue(stats.getExecution().getAvg() == 0d);
    assertTrue(stats.getNodeExecution().getMax() == 0d);
    assertTrue(stats.getTotalTasksExecuted() == 0);
  }

  /**
   * Test getting node management information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNodesInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
    Collection<JPPFManagementInfo> coll = driver.nodesInformation();
    assertNotNull(coll);
    assertTrue("coll.size() should be 2, but is " + coll.size(), coll.size() == 2);
    int i = 0;
    for (JPPFManagementInfo info: coll)
    {
      String prefix = "node " + i;
      assertNotNull(prefix + " host is null", info.getHost());
      assertTrue(prefix + " port is <= 0", info.getPort() > 0);
      assertFalse(prefix + " is secure", info.isSecure());
      assertTrue(prefix + " isNode() should be true", info.isNode());
      assertFalse(prefix + " isDriver() should be false", info.isDriver());
      assertTrue(prefix + " type should be NODE but is " + info.getType(), info.getType() == JPPFManagementInfo.NODE);
      assertNotNull(prefix + " id is null", info.getId());
      i++;
    }
  }

  /**
   * Test getting the number of nodes attached to the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNbNodes() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
    int n = driver.nbNodes();
    assertEquals(2, n);
    JPPFJob job = BaseSetup.createJob("testNbNodes", false, false, 1, LifeCycleTask.class, 2000L);
    client.submit(job);
    Thread.sleep(500L);
    n = driver.nbNodes();
    assertEquals(2, n);
    ((JPPFResultCollector) job.getResultListener()).waitForResults();
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=15000L)
  public void testGetLoadBalancerInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
    InputStream is = getClass().getClassLoader().getResourceAsStream("config/driver.properties");
    assertNotNull(is);
    TypedProperties driverConfig = new TypedProperties();
    driverConfig.load(is);
    LoadBalancingInformation lbi = driver.loadBalancerInformation();
    assertTrue(lbi.getAlgorithmNames().contains(driverConfig.getString("jppf.load.balancing.algorithm")));
    assertEquals(lbi.getAlgorithm(), driverConfig.getString("jppf.load.balancing.algorithm"));
    TypedProperties params = lbi.getParameters();
    assertNotNull(params);
    String profile = driverConfig.getString("jppf.load.balancing.strategy");
    String prefix = "strategy." + profile + '.';
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
  @Test(timeout=15000L)
  public void testSetLoadBalancerInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
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

  /**
   * Test of matching the nodes against an execution policy.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNodesMatchingExecutionPolicy() throws Exception
  {
    JMXNodeConnectionWrapper[] nodes = null;
    JMXDriverConnectionWrapper driver = null;
    try
    {
      driver = BaseSetup.getDriverManagementProxy(client);
      assertNotNull(driver);
      Collection<JPPFManagementInfo> coll = driver.nodesInformation();
      assertNotNull(coll);
      assertTrue("coll.size() should be 2, but is " + coll.size(), coll.size() == 2);
      nodes = new JMXNodeConnectionWrapper[2];
      int count = 0;
      for (JPPFManagementInfo info: coll)
      {
        JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
        node.connectAndWait(0L);
        nodes[count++] = node;
      }
      for (JMXNodeConnectionWrapper node: nodes) node.updateThreadPoolSize(4);
      Thread.sleep(500L);
      client.submit(BaseSetup.createJob("broadcast1", true, true, 1, MyBroadcastTask.class));
      Thread.sleep(500L);
      ExecutionPolicy policy = new AtLeast("processing.threads", 4);
      int n = driver.matchingNodes(policy);
      assertTrue("n is " + n + " but should be 2", n == 2);
      nodes[1].updateThreadPoolSize(2);
      Thread.sleep(500L);
      client.submit(BaseSetup.createJob("broadcast2", true, true, 1, MyBroadcastTask.class));
      Thread.sleep(500L);
      n = driver.matchingNodes(policy);
      assertTrue("n is " + n + " but should be 1", n == 1);
    }
    finally
    {
      //if (driver != null) driver.close();
      if (nodes != null) for (JMXNodeConnectionWrapper node: nodes) node.close();
    }
  }

  /**
   * Test getting idle nodes information from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testIdleNodesInformation() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
    Collection<JPPFManagementInfo> coll = driver.idleNodesInformation();
    assertNotNull(coll);
    assertEquals(2, coll.size());
    JPPFJob job = BaseSetup.createJob("testIdleNodesInformation", false, false, 1, LifeCycleTask.class, 3000L);
    client.submit(job);
    Thread.sleep(500L);
    coll = driver.idleNodesInformation();
    assertEquals(1, coll.size());
    ((JPPFResultCollector) job.getResultListener()).waitForResults();
  }

  /**
   * Test getting the number of idle nodes from the server.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testNbIdleNodes() throws Exception
  {
    JMXDriverConnectionWrapper driver = BaseSetup.getDriverManagementProxy(client);
    assertNotNull(driver);
    int n = driver.nbIdleNodes();
    assertEquals(2, n);
    JPPFJob job = BaseSetup.createJob("testNbIdleNodes", false, false, 1, LifeCycleTask.class, 3000L);
    client.submit(job);
    Thread.sleep(500L);
    n = driver.nbIdleNodes();
    assertEquals(1, n);
    ((JPPFResultCollector) job.getResultListener()).waitForResults();
  }

  /**
   * A simple task.
   */
  public static class MyBroadcastTask extends JPPFTask
  {
    @Override
    public void run()
    {
      System.out.println("broadcast of " + getClass().getName());
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
