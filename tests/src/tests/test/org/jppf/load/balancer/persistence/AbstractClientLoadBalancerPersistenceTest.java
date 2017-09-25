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
import org.jppf.client.balancer.JobManagerClient;
import org.jppf.client.event.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManagement;
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
public abstract class AbstractClientLoadBalancerPersistenceTest extends AbstractDatabaseSetup {
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
    if (client != null) {
      client.getLoadBalancerPersistenceManagement().deleteAll();
    }
  }

  /**
   * Test that a non persistent algo (which does not implement {@link org.jppf.load.balancer.persistence.PersistentState})
   * does not result in any load-balancer state being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testNonPersistentAlgos() throws Exception {
    LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    LoadBalancerPersistenceManagement mgt = client.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String[] algos = { "manual", "nodethreads" };
      int nbTasks = 100;
      for (String algo: algos) {
        client.setLoadBalancerSettings(algo, lbi.getParameters());
        JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        job.getClientSLA().setMaxChannels(2);
        List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        List<String> channels = mgt.listAllChannels();
        assertNotNull(channels);
        assertTrue(channels.isEmpty());
      }
    } finally {
      client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test that a persistent algo (which implements {@link org.jppf.load.balancer.persistence.PersistentState})
   * results in load-balancer states being persisted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testPersistentAlgos() throws Exception {
    LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    LoadBalancerPersistenceManagement mgt = client.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String[] algos = { "proportional", "autotuned", "rl2" };
      int nbTasks = 100;
      for (String algo: algos) {
        client.setLoadBalancerSettings(algo, lbi.getParameters());
        JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        job.getClientSLA().setMaxChannels(2);
        List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
        List<String> channels = mgt.listAllChannels();
        print(true, false, "list of nodes for algo=%s : %s", algo, channels);
        assertNotNull(channels);
        assertEquals(BaseSetup.nbDrivers() + 1, channels.size());
        for (String channel: channels) {
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
      List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertTrue(channels.isEmpty());
    } finally {
      client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test when the set of algorithms is different on each channel.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDifferentAlgosPerNode() throws Exception {
    LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    LoadBalancerPersistenceManagement mgt = client.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String[] algos = { "proportional", "autotuned", "rl2" };
      int nbTasks = 100;
      for (int i=0; i<algos.length; i++) {
        String algo = algos[i];
        client.setLoadBalancerSettings(algo, lbi.getParameters());
        JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
        job.getClientSLA().setMaxChannels(2);
        if (i > 0) job.getClientSLA().setExecutionPolicy(new Equal("jppf.channel.local", i == 1));
        List<Task<?>> results = client.submitJob(job);
        checkJobResults(nbTasks, results, false);
      }
      Map<Integer, String> uuidToChannelID = new HashMap<>();
      for (int i=0; i<algos.length; i++) {
        List<String> channels = mgt.listAllChannelsWithAlgorithm(algos[i]);
        assertNotNull(channels);
        if (i == 0) {
          assertEquals(BaseSetup.nbDrivers() + 1, channels.size());
        } else {
          assertEquals(1, channels.size());
          uuidToChannelID.put(i, channels.get(0));
        }
      }
      // check that channel1 has algos[0] + algos[1] and channel2 has algos[0] + algos[2]
      for (Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        List<String> channelAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(channelAlgos);
        assertEquals(2, channelAlgos.size());
        assertTrue(channelAlgos.contains(algos[0]));
        assertTrue(channelAlgos.contains(algos[entry.getKey()]));
      }
      // delete algos[0] from all nodes and re-check that node1 has only algos[1] and node2 has only algos[2]
      mgt.deleteAlgorithm(algos[0]);
      for (Map.Entry<Integer, String> entry: uuidToChannelID.entrySet()) {
        List<String> channelAlgos = mgt.listAlgorithms(entry.getValue());
        assertNotNull(channelAlgos);
        assertEquals(1, channelAlgos.size());
        assertFalse(channelAlgos.contains(algos[0]));
        assertTrue(channelAlgos.contains(algos[entry.getKey()]));
        mgt.deleteChannel(entry.getValue());
      }
      List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertTrue(channels.isEmpty());
    } finally {
      client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test the deletion of a single alogrithm state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testDeleteSingleAlgo() throws Exception {
    LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    LoadBalancerPersistenceManagement mgt = client.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    try {
      String algo = "proportional";
      int nbTasks = 100;
      client.setLoadBalancerSettings(algo, lbi.getParameters());
      JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
      job.getClientSLA().setMaxChannels(2);
      List<Task<?>> results = client.submitJob(job);
      checkJobResults(nbTasks, results, false);
      List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertEquals(BaseSetup.nbDrivers() + 1, channels.size());
      for (String channel: channels) {
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
      client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
    }
  }

  /**
   * Test that loab-balancer works with multiple connections to a single driver.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testSingleDriverMultipleConnections() throws Exception {
    boolean localEnabled = client.isLocalExecutionEnabled();
    LoadBalancingInformation lbi = client.getLoadBalancerSettings();
    LoadBalancerPersistenceManagement mgt = client.getLoadBalancerPersistenceManagement();
    assertNotNull(mgt);
    String method = ReflectionUtils.getCurrentMethodName();
    JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
    try {
      client.setLocalExecutionEnabled(false);
      pool.setSize(2);
      pool.awaitWorkingConnections(Operator.EQUAL, 2);
      JobManagerClient jmc = (JobManagerClient) client.getJobManager();
      while (jmc.getTaskQueueChecker().getNbIdleChannels() < 2) Thread.sleep(10L);
      //Thread.sleep(100L);
      String algo = "proportional";
      int nbTasks = 100;
      client.setLoadBalancerSettings(algo, lbi.getParameters());
      JPPFJob job = BaseTestHelper.createJob(method + "-" + algo, true, false, nbTasks, LifeCycleTask.class, 0L);
      job.getClientSLA().setMaxChannels(2);
      job.addJobListener(new JobListenerAdapter() {
        @Override
        public void jobDispatched(final JobEvent event) {
          print(false, false, "job '%s' dispatching %d tasks to %s", event.getJob().getName(), event.getJobTasks().size(), event.getConnection());
        }
      });
      List<Task<?>> results = client.submitJob(job);
      checkJobResults(nbTasks, results, false);
      List<String> channels = mgt.listAllChannels();
      assertNotNull(channels);
      assertEquals(pool.getSize(), channels.size());
      for (String channel: channels) {
        List<String> channelAlgos = mgt.listAlgorithms(channel);
        assertNotNull(channelAlgos);
        assertEquals(String.format("algo=%s, channel=%s", algo, channel), 1, channelAlgos.size());
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
      client.setLoadBalancerSettings(lbi.getAlgorithm(), lbi.getParameters());
      pool.setSize(1);
      pool.awaitWorkingConnections(Operator.EQUAL, 1);
      client.setLocalExecutionEnabled(localEnabled);
    }
  }

  @Override
  protected void checkJobResults(final int nbTasks, final Collection<Task<?>> results, final boolean cancelled) throws Exception {
    super.checkJobResults(nbTasks, results, cancelled);
    Thread.sleep(100L);
  }
}
