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

import org.jppf.client.*;
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
 *
 * @author Laurent Cohen
 */
public abstract class AbstractMuliServerLoadBalancerPersistenceTest extends AbstractDatabaseSetup {
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
    final List<JMXDriverConnectionWrapper> jmxList = getJMXConnections();
    final LoadBalancingInformation[] lbi = new LoadBalancingInformation[jmxList.size()];
    for (int i=0; i<jmxList.size(); i++) lbi[i] = jmxList.get(i).loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String[] algos = { "manual", "nodethreads" };
      final int nbTasks = 100;
      for (final String algo: algos) {
        for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(algo, lbi[i].getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        job.getClientSLA().setMaxChannels(2);
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        final List<String> channels = mgt.listAllChannels();
        assertNotNull(channels);
        assertTrue(channels.isEmpty());
      }
    } finally {
      for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(lbi[i].getAlgorithm(), lbi[i].getParameters());
    }
  }

  /**
   * Test that a persistent algo (which implements {@link org.jppf.load.balancer.persistence.PersistentState})
   * results in load-balancer states being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testPersistentAlgos() throws Exception {
    final List<JMXDriverConnectionWrapper> jmxList = getJMXConnections();
    final LoadBalancingInformation[] lbi = new LoadBalancingInformation[jmxList.size()];
    for (int i=0; i<jmxList.size(); i++) lbi[i] = jmxList.get(i).loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String[] algos = { "proportional", "autotuned", "rl2" };
      final int nbTasks = 100;
      for (String algo: algos) {
        for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(algo, lbi[i].getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        job.getClientSLA().setMaxChannels(2);
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        final List<String> channels = mgt.listAllChannels();
        print(true, false, "list of nodes for algo=%s : %s", algo, channels);
        assertNotNull(channels);
        assertEquals(BaseSetup.nbNodes(), channels.size());
        for (final String channel: channels) {
          List<String> channelAlgos = mgt.listAlgorithms(channel);
          assertNotNull(channelAlgos);
          assertEquals(String.format("algo=%s, node=%s", algo, channel), 1, channelAlgos.size());
          assertEquals(algo, channelAlgos.get(0));
          mgt.deleteChannel(channel);
          channelAlgos = mgt.listAlgorithms(channel);
          assertNotNull(channelAlgos);
          assertTrue(channelAlgos.isEmpty());
        }
      }
      final List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertTrue(channels.isEmpty());
    } finally {
      for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(lbi[i].getAlgorithm(), lbi[i].getParameters());
    }
  }

  /**
   * Test when the set of algorithms is different on each channel.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDifferentAlgosPerNode() throws Exception {
    final List<JMXDriverConnectionWrapper> jmxList = getJMXConnections();
    final LoadBalancingInformation[] lbi = new LoadBalancingInformation[jmxList.size()];
    for (int i=0; i<jmxList.size(); i++) lbi[i] = jmxList.get(i).loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String[] algos = { "proportional", "autotuned", "rl2" };
      final int nbTasks = 100;
      for (int i=0; i<algos.length; i++) {
        final String algo = algos[i];
        for (int j=0; j<jmxList.size(); j++) jmxList.get(j).changeLoadBalancerSettings(algo, lbi[j].getParameters());
        final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        job.getClientSLA().setMaxChannels(2);
        if (i > 0) job.getClientSLA().setExecutionPolicy(new Equal("jppf.driver.uuid", true, "d" + i));
        final List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
      }
      final Map<Integer, String> uuidToChannelID = new HashMap<>();
      for (int i=0; i<algos.length; i++) {
        final List<String> channels = mgt.listAllChannelsWithAlgorithm(algos[i]);
        assertNotNull(channels);
        if (i == 0) {
          assertEquals(BaseSetup.nbNodes(), channels.size());
        } else {
          assertEquals(1, channels.size());
          uuidToChannelID.put(i, channels.get(0));
        }
      }
      // check that channel1 has algos[0] + algos[1] and channel2 has algos[0] + algos[2]
      for (final Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        final List<String> channelAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(channelAlgos);
        assertEquals(2, channelAlgos.size());
        assertTrue(channelAlgos.contains(algos[0]));
        assertTrue(channelAlgos.contains(algos[entry.getKey()])); 
      }
      // delete algos[0] from all nodes and re-check that node1 has only algos[1] and node2 has only algos[2]
      mgt.deleteAlgorithm(algos[0]);
      for (final Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        final List<String> channelAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(channelAlgos);
        assertEquals(1, channelAlgos.size());
        assertFalse(channelAlgos.contains(algos[0]));
        assertTrue(channelAlgos.contains(algos[entry.getKey()]));
        mgt.deleteChannel(entry.getValue());
      }
      final List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertTrue(channels.isEmpty());
    } finally {
      for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(lbi[i].getAlgorithm(), lbi[i].getParameters());
    }
  }

  /**
   * Test the deletion of a single alogrithm state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDeleteSingleAlgo() throws Exception {
    final List<JMXDriverConnectionWrapper> jmxList = getJMXConnections();
    final LoadBalancingInformation[] lbi = new LoadBalancingInformation[jmxList.size()];
    for (int i=0; i<jmxList.size(); i++) lbi[i] = jmxList.get(i).loadBalancerInformation();
    final LoadBalancerPersistenceManagement mgt = jmxList.get(0).getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    final String method = ReflectionUtils.getCurrentMethodName();
    try {
      final String algo = "proportional";
      final int nbTasks = 100;
      for (int j=0; j<jmxList.size(); j++) jmxList.get(j).changeLoadBalancerSettings(algo, lbi[j].getParameters());
      final JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
      job.getClientSLA().setMaxChannels(2);
      final List<Task<?>> results = client.submitJob(job);
      checkJobResults(nbTasks, results, false);
      List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertEquals(BaseSetup.nbNodes(), channels.size());
      for (final String channel: channels) {
        List<String> channelAlgos = mgt.listAlgorithms(channel);
        assertNotNull(channelAlgos);
        assertEquals(String.format("algo=%s, node=%s", algo, channel), 1, channelAlgos.size());
        assertEquals(algo, channelAlgos.get(0));
        assertTrue(mgt.hasAlgorithm(channel, algo));
        mgt.delete(channel, algo);
        assertFalse(mgt.hasAlgorithm(channel, algo));
        channelAlgos = mgt.listAlgorithms(channel);
        assertNotNull(channelAlgos);
        assertTrue(channelAlgos.isEmpty());
      }
      channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertTrue(channels.isEmpty());
    } finally {
      for (int i=0; i<jmxList.size(); i++) jmxList.get(i).changeLoadBalancerSettings(lbi[i].getAlgorithm(), lbi[i].getParameters());
    }
  }

  /**
   * 
   * @return a list of jmx connections, one for each pool.
   * @throws Exception if any error occurs.
   */
  private static List<JMXDriverConnectionWrapper> getJMXConnections() throws Exception {
    final List<JPPFConnectionPool> pools = client.awaitConnectionPools(Operator.EQUAL, 2, Operator.EQUAL, 1, 10_000L, JPPFClientConnectionStatus.ACTIVE);
    final List<JMXDriverConnectionWrapper> result = new ArrayList<>();
    for (final JPPFConnectionPool pool: pools) result.add(pool.awaitWorkingJMXConnection());
    return result;
  }
}
