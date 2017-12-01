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

package test.org.jppf.load.balancer.persistence;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManagement;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ReflectionUtils;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.persistence.AbstractDatabaseSetup;
import test.org.jppf.test.setup.BaseSetup;
import test.org.jppf.test.setup.common.*;

/**
 * Base class for load-balancer persistence testing, independantly of the configured persistence.
 * @author Laurent Cohen
 */
public abstract class AbstractDriverLoadBalancerPersistenceTest extends AbstractDatabaseSetup {
  /** */
  @Rule
  public TestWatcher setup1D1N1CWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, true, "start of method %s()", description.getMethodName());
    }
  };

  /**
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDownInstance() throws Exception {
    try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", 11201, false)) {
      boolean b = jmx.connectAndWait(5_000L);
      print(false, false, "tearDownInstance() : jmx connected = %b", b);
      if (b) jmx.getLoadBalancerPersistenceManagement().deleteAll();
    }
  }

  /**
   * Test that a non persistent algo (which does not implement {@link org.jppf.load.balancer.persistence.PersistentState})
   * does not result in any load-balancer state being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testNonPersistentAlgos() throws Exception {
    JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String[] algos = { "manual", "nodethreads" };
      int nbTasks = 100;
      for (String algo: algos) {
        jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
        JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        List<String> nodes = mgt.listAllChannels();
        assertNotNull(nodes);
        assertTrue(nodes.isEmpty());
      }
    } finally {
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test that a persistent algo (which implements {@link org.jppf.load.balancer.persistence.PersistentState})
   * results in load-balancer states being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testPersistentAlgos() throws Exception {
    JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String[] algos = { "proportional", "autotuned", "rl2" };
      int nbTasks = 100;
      for (String algo: algos) {
        jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
        JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        List<String> nodes = mgt.listAllChannels();
        print(true, false, "list of nodes for algo=%s : %s", algo, nodes);
        assertNotNull(nodes);
        assertEquals(BaseSetup.nbNodes(), nodes.size());
        for (String node: nodes) {
          List<String> nodeAlgos = mgt.listAlgorithms(node);
          assertNotNull(nodeAlgos);
          assertEquals(String.format("algo=%s, node=%s", algo, node), 1, nodeAlgos.size());
          assertEquals(algo, nodeAlgos.get(0));
          mgt.deleteChannel(node);
          nodeAlgos = mgt.listAlgorithms(node);
          assertNotNull(nodeAlgos);
          assertTrue(nodeAlgos.isEmpty());
        }
      }
      Thread.sleep(500L);
      List<String> nodes = mgt.listAllChannels();
      assertNotNull(nodes);
      assertTrue("nodes should be empty but is " + nodes, nodes.isEmpty());
    } finally {
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test when the set of algorithms is different on each node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDifferentAlgosPerNode() throws Exception {
    JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String[] algos = { "proportional", "autotuned", "rl2" };
      int nbTasks = 100;
      for (int i=0; i<algos.length; i++) {
        String algo = algos[i];
        jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
        JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        if (i > 0) job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", true, "n" + i));
        List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
      }
      Map<Integer, String> uuidToChannelID = new HashMap<>();
      for (int i=0; i<algos.length; i++) {
        List<String> nodes = mgt.listAllChannelsWithAlgorithm(algos[i]);
        assertNotNull(nodes);
        if (i == 0) {
          assertEquals(BaseSetup.nbNodes(), nodes.size());
        } else {
          assertEquals(1, nodes.size());
          uuidToChannelID.put( i, nodes.get(0));
        }
      }
      // check that node1 has algos[0] + algos[1] and node2 has algos[0] + algos[2]
      for (Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        List<String> nodeAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(nodeAlgos);
        assertEquals(2, nodeAlgos.size());
        assertTrue(nodeAlgos.contains(algos[0]));
        assertTrue(nodeAlgos.contains(algos[entry.getKey()])); 
      }
      // delete algos[0] from all nodes and re-check that node1 has only algos[1] and node2 has only algos[2]
      mgt.deleteAlgorithm(algos[0]);
      for (Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        List<String> nodeAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(nodeAlgos);
        assertEquals(1, nodeAlgos.size());
        assertFalse(nodeAlgos.contains(algos[0]));
        assertTrue(nodeAlgos.contains(algos[entry.getKey()]));
        mgt.deleteChannel(entry.getValue());
      }
      List<String> nodes = mgt.listAllChannels();
      assertNotNull(nodes);
      assertTrue(nodes.isEmpty());
    } finally {
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test the deletion of a single alogrithm state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDeleteSingleAlgo() throws Exception {
    JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String algo = "proportional";
      int nbTasks = 100;
      jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
      JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
      List<Task<?>> results = client.submitJob(job);
      checkJobResults(nbTasks, results, false);
      List<String> nodes = mgt.listAllChannels();
      assertNotNull(nodes);
      assertEquals(BaseSetup.nbNodes(), nodes.size());
      for (String node: nodes) {
        List<String> nodeAlgos = mgt.listAlgorithms(node);
        assertNotNull(nodeAlgos);
        assertEquals(String.format("algo=%s, node=%s", algo, node), 1, nodeAlgos.size());
        assertEquals(algo, nodeAlgos.get(0));
        assertTrue(mgt.hasAlgorithm(node, algo));
        mgt.delete(node, algo);
        assertFalse(mgt.hasAlgorithm(node, algo));
        nodeAlgos = mgt.listAlgorithms(node);
        assertNotNull(nodeAlgos);
        assertTrue(nodeAlgos.isEmpty());
      }
      nodes = mgt.listAllChannels();
      assertNotNull(nodes);
      assertTrue(nodes.isEmpty());
    } finally {
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }
}
