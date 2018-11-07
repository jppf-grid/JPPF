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

package test.org.jppf.load.balancer.persistence;

import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManagement;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.concurrent.ConcurrentUtils;
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
    try (JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1, false)) {
      final boolean b = jmx.connectAndWait(5_000L);
      print(false, false, "tearDownInstance() : jmx connected = %b", b);
      assertTrue(b);
      final LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
      if (b) mgt.deleteAll();
      assertTrue(ConcurrentUtils.awaitCondition(new ConcurrentUtils.ConditionFalseOnException() {
        @Override
        public boolean evaluateWithException() throws Exception {
          return mgt.listAllChannels().isEmpty();
        }
      }, 5000L, 250L, false));
    }
  }

  /**
   * Test that a non persistent algo (which does not implement {@link org.jppf.load.balancer.persistence.PersistentState})
   * does not result in any load-balancer state being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testNonPersistentAlgos() throws Exception {
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    final int maxJobs = pool.getMaxJobs();
    final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
    final LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    try {
      pool.setMaxJobs(1);
      final String[] algos = { "manual", "nodethreads" };
      final int nbTasks = 100;
      for (final String algo: algos) {
        jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        awaitNoMorePendingOperations(mgt);
        assertTrue(checkEmptyChannels(mgt));
      }
    } finally {
      pool.setMaxJobs(maxJobs);
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
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    final int maxJobs = pool.getMaxJobs();
    final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
    final LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    try {
      pool.setMaxJobs(1);
      final String[] algos = { "proportional", "autotuned", "rl2" };
      final int nbTasks = 100;
      for (final String algo: algos) {
        jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        awaitNoMorePendingOperations(mgt);
        final List<String> nodes = mgt.listAllChannels();
        BaseTestHelper.printToAll(jmx, true, true, true, false, false, "algo = %-12s, list of all nodes: %s", algo, nodes);
        assertNotNull(nodes);
        //assertEquals(BaseSetup.nbNodes(), nodes.size());
        for (final String node: nodes) {
          final List<String> nodeAlgos = mgt.listAlgorithms(node);
          BaseTestHelper.printToAll(jmx, true, true, true, false, false, "list of algos for node=%s : %s", node, nodeAlgos);
          assertNotNull(nodeAlgos);
          assertEquals(String.format("algo=%s, node=%s, nodeAlgos=%s", algo, node, nodeAlgos), 1, nodeAlgos.size());
          assertEquals(algo, nodeAlgos.get(0));
          BaseTestHelper.printToAll(jmx, true, true, true, false, false, "deleting records for node = %s", node);
          mgt.deleteChannel(node);
          awaitNoMorePendingOperations(mgt);
        }
        BaseTestHelper.printToAll(jmx, true, true, true, false, false, "deleting records for algo = %s", algo);
        mgt.deleteAlgorithm(algo);
        assertTrue(checkEmptyChannelsForAlgo(mgt, algo));
      }
      assertTrue(checkEmptyChannels(mgt));
    } finally {
      pool.setMaxJobs(maxJobs);
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test when the set of algorithms is different on each node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDifferentAlgosPerNode() throws Exception {
    final String method = ReflectionUtils.getCurrentMethodName();
    final JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    final int maxJobs = pool.getMaxJobs();
    final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
    final LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    try {
      pool.setMaxJobs(1);
      final String[] algos = { "proportional", "autotuned", "rl2" };
      final int nbTasks = 100;
      for (int i=0; i<algos.length; i++) {
        final String algo = algos[i];
        jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        if (i > 0) job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", true, "n" + i));
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
      }
      awaitNoMorePendingOperations(mgt);
      final Map<Integer, String> uuidToChannelID = new HashMap<>();
      for (int i=0; i<algos.length; i++) {
        final List<String> nodes = mgt.listAllChannelsWithAlgorithm(algos[i]);
        print(false, false, "[1] i = %d, nodes for algo = %-12s : %s", i, algos[i], nodes);
        assertNotNull(nodes);
        if (i == 0) {
          assertEquals(BaseSetup.nbNodes(), nodes.size());
        } else {
          assertEquals(1, nodes.size());
          uuidToChannelID.put( i, nodes.get(0));
        }
      }
      // check that node1 has algos[0] + algos[1] and node2 has algos[0] + algos[2]
      for (final Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        final List<String> nodeAlgos = mgt.listAlgorithms(entry.getValue());
        print(false, false, "[2] algos for node %s : %s", entry.getValue(), nodeAlgos);
        assertNotNull(nodeAlgos);
        assertEquals(2, nodeAlgos.size());
        assertTrue(nodeAlgos.contains(algos[0]));
        assertTrue(nodeAlgos.contains(algos[entry.getKey()])); 
      }
      // delete algos[0] from all nodes and re-check that node1 has only algos[1] and node2 has only algos[2]
      mgt.deleteAlgorithm(algos[0]);
      awaitNoMorePendingOperations(mgt);
      for (final Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        final List<String> nodeAlgos = mgt.listAlgorithms(entry.getValue());
        print(false, false, "[3] algos for node %s : %s", entry.getValue(), nodeAlgos);
        assertNotNull(nodeAlgos);
        assertEquals(1, nodeAlgos.size());
        assertFalse(nodeAlgos.contains(algos[0]));
        assertTrue(nodeAlgos.contains(algos[entry.getKey()]));
        mgt.deleteChannel(entry.getValue());
        awaitNoMorePendingOperations(mgt);
      }
      assertTrue(checkEmptyChannels(mgt));
    } finally {
      pool.setMaxJobs(maxJobs);
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test the deletion of a single alogrithm state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDeleteSingleAlgo() throws Exception {
    final String method = ReflectionUtils.getCurrentMethodName();
    final JMXDriverConnectionWrapper jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    final LoadBalancingInformation lbi = jmx.loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmx.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    try {
      final String algo = "proportional";
      final int nbTasks = 100;
      jmx.changeLoadBalancerSettings(algo, lbi.getParameters());
      final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
      final List<Task<?>> results = client.submitJob(job);
      checkJobResults(nbTasks, results, false);
      awaitNoMorePendingOperations(mgt);
      List<String> nodes = mgt.listAllChannels();
      print(false, false, "list of nodes: %s", nodes);
      assertNotNull(nodes);
      assertFalse(nodes.isEmpty());
      for (String node: nodes) {
        List<String> nodeAlgos = mgt.listAlgorithms(node);
        print(false, false, "[1] algos for node %s : %s", node, nodeAlgos);
        assertNotNull(nodeAlgos);
        assertEquals(String.format("algo=%s, node=%s", algo, node), 1, nodeAlgos.size());
        assertEquals(algo, nodeAlgos.get(0));
        assertTrue(mgt.hasAlgorithm(node, algo));
        mgt.delete(node, algo);
        awaitNoMorePendingOperations(mgt);
        assertFalse(mgt.hasAlgorithm(node, algo));
        nodeAlgos = mgt.listAlgorithms(node);
        print(false, false, "[2] algos for node %s : %s", node, nodeAlgos);
        assertNotNull(nodeAlgos);
        assertTrue(nodeAlgos.isEmpty());
      }
      final boolean empty = checkEmptyChannels(mgt);
      if (!empty) {
        nodes = mgt.listAllChannels();
        print(false, false, "list of nodes not empty: %s", nodes);
        for (final String node: nodes) print(false, false, "list of algos for node %s : %s", node, mgt.listAlgorithms(node));
      }
      assertTrue(empty);
    } finally {
      jmx.changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }
}
