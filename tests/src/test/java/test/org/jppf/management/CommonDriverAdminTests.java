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
import static test.org.jppf.test.setup.BaseTest.*;

import java.io.InputStream;
import java.util.*;

import org.jppf.client.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.jppf.utils.configuration.JPPFProperties;

import test.org.jppf.client.CommonClientTests;
import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * 
 * @author Laurent Cohen
 */
public final class CommonDriverAdminTests {
  /**
   * Instatiation not permitted.
   */
  private CommonDriverAdminTests() {
  }

  /**
   * Test getting node management information from the server.
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testNodesInformation(final JPPFClient client, final int nbNodes) throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    final Collection<JPPFManagementInfo> coll = driver.nodesInformation();
    assertNotNull(coll);
    assertEquals(nbNodes, coll.size());
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
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testIdleNodesInformation(final JPPFClient client, final int nbNodes) throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Thread.sleep(500L);
    Collection<JPPFManagementInfo> coll = driver.idleNodesInformation();
    assertNotNull(coll);
    assertEquals(2, coll.size());
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, 2000L);
    client.submitAsync(job);
    Thread.sleep(500L);
    coll = driver.idleNodesInformation();
    assertEquals(1, coll.size());
    job.awaitResults();
    while (driver.nbIdleNodes() < nbNodes) Thread.sleep(100L);
  }

  /**
   * Test getting the number of nodes attached to the server.
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testNbNodes(final JPPFClient client, final int nbNodes) throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    final int n = driver.nbNodes();
    assertEquals(nbNodes, n);
  }

  /**
   * Test getting the number of idle nodes from the server.
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testNbIdleNodes(final JPPFClient client, final int nbNodes) throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    Thread.sleep(500L);
    int n = driver.nbIdleNodes();
    assertEquals(nbNodes, n);
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, 2000L);
    client.submitAsync(job);
    Thread.sleep(1000L);
    n = driver.nbIdleNodes();
    assertEquals(nbNodes - 1, n);
    job.awaitResults();
    while (driver.nbIdleNodes() < 2) Thread.sleep(100L);
  }

  /**
   * Test getting and setting load-balancer information in the server.
   * @param client the JPPF client to use.
   * @throws Exception if any error occurs.
   */
  public static void testGetLoadBalancerInformation(final JPPFClient client) throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    assertNotNull(driver);
    final InputStream is = CommonClientTests.class.getClassLoader().getResourceAsStream("config/driver.template.properties");
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
   * @param client the JPPF client to use.
   * @throws Exception if any error occurs.
   */
  public static void testSetLoadBalancerInformation(final JPPFClient client) throws Exception {
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
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testNodesMatchingExecutionPolicy(final JPPFClient client, final int nbNodes) throws Exception {
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    int n = driver.nbNodes(new ExecutionPolicySelector(new Contains("jppf.node.uuid", false, "n")));
    assertEquals(nbNodes, n);
    n = driver.nbNodes(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")));
    assertEquals(1, n);
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testToggleActiveState(final JPPFClient client, final int nbNodes) throws Exception {
    assertTrue("there must be at least 2 nodes in the grid!", nbNodes >= 2);
    final int nbTasks = 5 * nbNodes;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    final String[] nodeUuids = new String[nbNodes];
    assertNotNull(driver);
    try {
      final Collection<JPPFManagementInfo> nodesList = driver.nodesInformation();
      assertEquals(nbNodes, nodesList.size());
      int i = 0;
      for (final JPPFManagementInfo info : nodesList) nodeUuids[i++] = info.getUuid();
      // recheck that the job is executed on all nodes
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-1", false, nbTasks, LifeCycleTask.class, 0L);
      final Set<String> executedOnUuids = new HashSet<>();
      List<Task<?>> results = client.submit(job);
      for (final Task<?> t : results) {
        final String uuid = ((LifeCycleTask) t).getUuidFromNode();
        if (!executedOnUuids.contains(uuid)) executedOnUuids.add(uuid);
      }
      assertCompare(Operator.AT_LEAST, 2, executedOnUuids.size());
      //for (String uuid: nodeUuids) assertTrue(executedOnUuids.contains(uuid));
      // deactivate one node and make sure the job is only executed on the other node
      executedOnUuids.clear();
      final NodeSelector selector = new UuidSelector(Arrays.asList(nodeUuids).subList(1, nodeUuids.length));
      driver.toggleActiveState(selector);
      job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod() + "-2", false, nbTasks, LifeCycleTask.class, 0L);
      results = client.submit(job);
      for (final Task<?> t : results) {
        final String uuid = ((LifeCycleTask) t).getUuidFromNode();
        if (!executedOnUuids.contains(uuid)) executedOnUuids.add(uuid);
      }
      assertTrue(executedOnUuids.contains(nodeUuids[0]));
      for (i=1; i<nodeUuids.length; i++) assertFalse(executedOnUuids.contains(nodeUuids[i]));
      assertEquals(1, executedOnUuids.size());
    } finally {
      driver.setActiveState(null, true);
    }
  }

  /**
   * Test activating and deactivating one or more nodes.
   * @param client the JPPF client to use.
   * @param nbNodes the number of nodes in the grid.
   * @throws Exception if any error occurs.
   */
  public static void testGetAndSetActiveState(final JPPFClient client, final int nbNodes) throws Exception {
    assertTrue("there must be at least 2 nodes in the grid!", nbNodes >= 2);
    print(false, false, "+++ client config:\n%s", client.getConfig().asString());
    final String jobNamePrefix = ReflectionUtils.getCurrentClassAndMethod();
    final int nbTasks = 5 * nbNodes;
    print(false, false, "+++ checking driver JMX connection");
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    String[] nodeUuids = null;
    assertNotNull(driver);
    try {
      print(false, false, "+++ checking number of nodes = %d", nbNodes);
      final Collection<JPPFManagementInfo> nodesList = driver.nodesInformation();
      assertEquals(nbNodes, nodesList.size());
      nodeUuids = new String[nodesList.size()];
      int i = 0;
      for (final JPPFManagementInfo info : nodesList) nodeUuids[i++] = info.getUuid();
      // deactivate one node and make sure the job is only executed on the other node
      final NodeSelector selector = new UuidSelector(nodeUuids[0]);
      print(false, false, "+++ setting active state of node %s to false", nodeUuids[0]);
      driver.setActiveState(selector, false);
      waitForNodeInState(driver, nodeUuids[0], false);
      final Map<String, Boolean> map = driver.getActiveState(null);
      assertEquals(nodeUuids.length, map.size());
      assertTrue(map.containsKey(nodeUuids[0]));
      assertFalse(map.get(nodeUuids[0]));
      for (i=1; i<nodeUuids.length; i++) {
        assertTrue(map.containsKey(nodeUuids[i]));
        assertTrue(map.get(nodeUuids[i]));
      }
      JPPFJob job = BaseTestHelper.createJob(jobNamePrefix + "-1", false, nbTasks, LifeCycleTask.class, 0L);
      print(false, false, "+++ executing job %s", job.getName());
      List<Task<?>> results = client.submit(job);
      print(false, false, "+++ checking results for job %s", job.getName());
      final Set<String> executedOnUuids = new HashSet<>();
      for (final Task<?> t : results) {
        final String uuid = ((LifeCycleTask) t).getUuidFromNode();
        if (!executedOnUuids.contains(uuid)) executedOnUuids.add(uuid);
      }
      print(false, false, "+++ executedOnUuids = %s", executedOnUuids);
      assertFalse(executedOnUuids.isEmpty());
      assertFalse(executedOnUuids.contains(nodeUuids[0]));
      assertCompare(Operator.AT_LEAST, 1, executedOnUuids.size());
      // re-activate the node and check that the job is executed on all nodes
      executedOnUuids.clear();
      print(false, false, "+++ setting active state of node %s to true", nodeUuids[0]);
      driver.setActiveState(selector, true);
      waitForNodeInState(driver, nodeUuids[0], true);
      job = BaseTestHelper.createJob(jobNamePrefix + "-2", false, nbTasks, LifeCycleTask.class, 0L);
      print(false, false, "+++ executing job %s", job.getName());
      results = client.submit(job);
      print(false, false, "+++ checking results for job %s", job.getName());
      for (final Task<?> t : results) {
        final String uuid = ((LifeCycleTask) t).getUuidFromNode();
        if (!executedOnUuids.contains(uuid)) executedOnUuids.add(uuid);
      }
      print(false, false, "+++ executedOnUuids = %s", executedOnUuids);
      assertCompare(Operator.AT_LEAST, 2, executedOnUuids.size());
      for (final String uuid: nodeUuids) assertTrue(executedOnUuids.contains(uuid));
    } finally {
       driver.setActiveState(null, true);
    }
  }

  /**
   * 
   * @param driver .
   * @param nodeUuid .
   * @param active .
   * @throws Exception .
   */
  private static void waitForNodeInState(final JMXDriverConnectionWrapper driver, final String nodeUuid, final boolean active) throws Exception {
    final NodeSelector selector = new UuidSelector(nodeUuid);
    ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> {
      final Map<String, Boolean> map = driver.getActiveState(selector);
      final Boolean result = map.get(nodeUuid);
      return (result != null) && (result.booleanValue() == active);
    }, 5000L, 100L, false);
  }
}
