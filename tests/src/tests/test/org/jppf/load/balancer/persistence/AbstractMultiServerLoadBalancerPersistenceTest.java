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
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.persistence.AbstractDatabaseSetup;
import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test load-balancer state perisstence on the server site in a muli-server topology.
 * @author Laurent Cohen
 */
public abstract class AbstractMultiServerLoadBalancerPersistenceTest extends AbstractDatabaseSetup {
  /** */
  private final static int NB_TASKS = 2 * 50;
  /** */
  private final List<JPPFConnectionPool> pools = new ArrayList<>();
  /** */
  private final List<JMXDriverConnectionWrapper> jmxList = new ArrayList<>();
  /** */
  private final List<Integer> maxJobs = new ArrayList<>();
  /** */
  private final List<LoadBalancingInformation> lbis = new ArrayList<>();

  /**
   * Start the DB server and JPPF grid.
   * @param driverConfigFile the name of the driver configuration fie to use.
   * @param useDB whether to start the database server.
   * @throws Exception if any error occurs.
   */
  static void setupConfig(final String driverConfigFile, final boolean useDB) throws Exception {
    final String prefix = "lb_persistence_p2p";
    final TestConfiguration config = dbSetup(prefix, useDB);
    config.driver.jppf = "classes/tests/config/" + prefix + "/" + driverConfigFile;
    client = BaseSetup.setup(2, 2, true, false, config);
    checkPeers(15_000L, false, true);
  }

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
    try (final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("localhost", DRIVER_MANAGEMENT_PORT_BASE + 1, false)) {
      jmx.connectAndWait(5_000L);
      final boolean b = jmx.isConnected();
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
    capturePoolsStates();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String[] algos = { "manual", "nodethreads" };
      for (final String algo: algos) {
        for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(algo, lbis.get(i).getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, NB_TASKS, LifeCycleTask.class, 1L);
        job.getClientSLA().setMaxChannels(2);
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(NB_TASKS, results, false);
        assertTrue(checkEmptyChannels(mgt));
      }
    } finally {
      restorePoolsStates();
    }
  }

  /**
   * Test that a persistent algo (which implements {@link org.jppf.load.balancer.persistence.PersistentState})
   * results in load-balancer states being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testPersistentAlgos() throws Exception {
    capturePoolsStates();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String[] algos = { "proportional", "autotuned", "rl2" };
      for (final String algo: algos) {
        for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(algo, lbis.get(i).getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, NB_TASKS, LifeCycleTask.class, 1L);
        job.getClientSLA().setMaxChannels(2);
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(NB_TASKS, results, false);
        awaitNoMorePendingOperations(mgt);
        final List<String> channels = mgt.listAllChannels();
        print(true, false, ">>> list of nodes for algo=%-12s : %s", algo, channels);
        assertNotNull(channels);
        assertFalse(channels.isEmpty());
        for (final String channel: channels) {
          final List<String> channelAlgos = mgt.listAlgorithms(channel);
          BaseTestHelper.printToAll(jmxList, true, true, true, false, false, ">>> algo = %-12s, list of algos for channel %s = %s", algo, channel, channelAlgos);
          assertNotNull(channelAlgos);
          assertTrue(String.format("algo=%s, channelAlgos=%s, channel=%s", algo, channelAlgos, channel), channelAlgos.size() >= 1);
          mgt.deleteChannel(channel);
          awaitNoMorePendingOperations(mgt);
          assertTrue(mgt.listAlgorithms(channel).isEmpty());
        }
        mgt.deleteAlgorithm(algo);
      }
      assertTrue(checkEmptyChannels(mgt));
      final List<String> channels = mgt.listAllChannels();
      BaseTestHelper.printToAll(jmxList, true, true, true, false, false, ">>> remaining list of channels = %s", channels);
      assertNotNull(channels);
      assertTrue("channels = " + channels, channels.isEmpty());
    } finally {
      restorePoolsStates();
    }
  }

  /**
   * Test when the set of algorithms is different on each channel.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDifferentAlgosPerNode() throws Exception {
    capturePoolsStates();
    final LoadBalancingInformation clientLbi = client.getLoadBalancerSettings();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      client.setLoadBalancerSettings("manual", new TypedProperties().setInt("size", 10));
      final String[] algos = { "proportional", "autotuned", "rl2" };
      for (int i=0; i<algos.length; i++) {
        final String algo = algos[i];
        print(false, false, ">>> algo=%-12s", algo);
        for (int j=0; j<jmxList.size(); j++) jmxList.get(j).changeLoadBalancerSettings(algo, lbis.get(j).getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, NB_TASKS, LifeCycleTask.class, 1L);
        job.getClientSLA().setMaxChannels(2);
        if (i > 0) job.getClientSLA().setExecutionPolicy(new Equal("jppf.driver.uuid", true, "d" + (2 - i % 2)));
        print(false, false, ">>> submitting job %s", job.getName());
        final List<Task<?>> results = client.submitJob(job);
        print(false, false, ">>> checking job results");
        checkJobResults(NB_TASKS, results, false);
      }
      print(false, false, ">>> check 1");
      awaitNoMorePendingOperations(mgt);
      final Map<Integer, String> uuidToChannelID = new HashMap<>();
      for (int i=0; i<algos.length; i++) {
        final List<String> channels = mgt.listAllChannelsWithAlgorithm(algos[i]);
        assertNotNull(channels);
        if (i == 0) {
          assertCompare(Operator.AT_LEAST, 3, channels.size());
        } else {
          assertCompare(Operator.MORE_THAN, 0, channels.size());
          uuidToChannelID.put(i, channels.get(0));
        }
      }
      print(false, false, ">>> check 2");
      // check that channel1 has algos[0] + algos[1] and channel2 has algos[0] + algos[2]
      for (final Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        final List<String> channelAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(channelAlgos);
        assertTrue(channelAlgos.size() >= 1);
      }
      // delete algos[0] from all nodes and re-check that node1 has only algos[1] and node2 has only algos[2]
      mgt.deleteAlgorithm(algos[0]);
      awaitNoMorePendingOperations(mgt);
      print(false, false, ">>> check 3");
      for (final Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        final List<String> channelAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(channelAlgos);
        assertFalse(channelAlgos.contains(algos[0]));
      }
      final List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
    } finally {
      client.setLoadBalancerSettings(clientLbi.getAlgorithm(), clientLbi.getParameters());
      restorePoolsStates();
    }
  }

  /**
   * Test the deletion of a single alogrithm state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDeleteSingleAlgo() throws Exception {
    capturePoolsStates();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String algo = "proportional";
      for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(algo, lbis.get(i).getParameters());
      final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, NB_TASKS, LifeCycleTask.class, 1L);
      job.getClientSLA().setMaxChannels(2);
      final List<Task<?>> results = client.submitJob(job);
      checkJobResults(NB_TASKS, results, false);
      awaitNoMorePendingOperations(mgt);
      final List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertFalse(channels.isEmpty());
      for (final String channel: channels) {
        List<String> channelAlgos = mgt.listAlgorithms(channel);
        assertNotNull(channelAlgos);
        assertEquals(String.format("algo=%s, node=%s", algo, channel), 1, channelAlgos.size());
        assertEquals(algo, channelAlgos.get(0));
        assertTrue(mgt.hasAlgorithm(channel, algo));
        mgt.delete(channel, algo);
        awaitNoMorePendingOperations(mgt);
        assertFalse(mgt.hasAlgorithm(channel, algo));
        channelAlgos = mgt.listAlgorithms(channel);
        assertNotNull(channelAlgos);
        assertTrue(channelAlgos.isEmpty());
      }
      mgt.deleteAlgorithm(algo);
      assertTrue(checkEmptyChannels(mgt));
    } finally {
      restorePoolsStates();
    }
  }

  /**
   * @throws Exception if any error occurs.
   */
  private void capturePoolsStates() throws Exception {
    pools.clear();
    jmxList.clear();
    lbis.clear();
    maxJobs.clear();
    pools.addAll(client.awaitConnectionPools(Operator.EQUAL, BaseSetup.nbDrivers(), Operator.EQUAL, 1, 10_000L, JPPFClientConnectionStatus.ACTIVE));
    for (final JPPFConnectionPool pool: pools) {
      final JMXDriverConnectionWrapper jmx = pool.awaitWorkingJMXConnection();
      jmxList.add(jmx);
      lbis.add(jmx.loadBalancerInformation());
      maxJobs.add(pool.getMaxJobs());
      pool.setMaxJobs(1);
    };
  }

  /**
   * @throws Exception if any error occurs.
   */
  private void restorePoolsStates() throws Exception {
    for (int i=0; i<pools.size(); i++) {
      pools.get(i).setMaxJobs(maxJobs.get(i));
      final LoadBalancingInformation lbi = lbis.get(i);
      jmxList.get(i).changeLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
    pools.clear();
    jmxList.clear();
    lbis.clear();
    maxJobs.clear();
  }
}
