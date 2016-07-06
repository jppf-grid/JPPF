/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.client.JPPFJob;
import org.jppf.job.JobEventType;
import org.jppf.management.*;
import org.jppf.node.NodeRunner;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.BaseSetup.Configuration;
import test.org.jppf.test.setup.common.*;

/**
 * Test a job whose SLA mandates the configuration of the nodes it runs on.
 * @author Laurent Cohen
 */
public class TestJobReservation extends AbstractNonStandardSetup {
  /**
   * Listens for and counts nodes connections and disconnections.
   */
  private final MyNodeConnectionListener myNodeListener = new MyNodeConnectionListener();
  /**
   * Waits for a JOB_DISPATCHED notification.
   */
  private final AwaitJobNotificationListener jobNotificationListener = new AwaitJobNotificationListener();
  /**
   * 
   */
  private JMXDriverConnectionWrapper jmx;

  /**
   * Launches 1 driver with 3 nodes and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    Configuration cfg = createConfig("job_reservation");
    cfg.driverLog4j = "classes/tests/config/job_reservation/log4j-driver.properties";
    client = BaseSetup.setup(1, 3, true, cfg);
  }

  /**
   * Reset the configuration of all nodes.
   * @throws Exception if any error occurs.
   */
  @Before
  public void resetNodes() throws Exception {
    BaseSetup.checkDriverAndNodesInitialized(1, 3);
    JPPFJob job = new JPPFJob();
    job.setName("broadcast reset node config");
    job.getSLA().setBroadcastJob(true);
    job.add(new BroadcastTask());
    client.submitJob(job);
    jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    jmx.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, myNodeListener);
    jmx.getJobManager().addNotificationListener(jobNotificationListener, null, null);
  }

  /**
   * Remove the jmx notification listener.
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDown() throws Exception {
    jmx.removeNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, myNodeListener);
    jmx.getJobManager().removeNotificationListener(jobNotificationListener);
    myNodeListener.map.clear();
  }

  /**
   * Test that a job is executed on the node closest ot its desired node config spec after restart of this node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testJobReservationSingleNodeWithRestart() throws Exception {
    int nbTasks = 5 * BaseSetup.nbNodes();
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 1L);
    TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(1);
    List<Task<?>> result = client.submitJob(job);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    for (Task<?> tsk: result) {
      LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals("n3", task.getNodeUuid());
    }
    assertEquals(1, myNodeListener.map.size());
    AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertEquals(BaseSetup.nbNodes(), n.get());
  }

  /**
   * Test that when a job with reservation is cancelled, all node reservations are removed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testCancelledJobReservationSingleNodeWithRestart() throws Exception {
    int nbTasksPerNode = 5;
    int nbTasks = nbTasksPerNode * BaseSetup.nbNodes();
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 500L);
    TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(1);
    client.submitJob(job);
    jobNotificationListener.await(JobEventType.JOB_RETURNED);
    job.cancel();
    List<Task<?>> result = job.awaitResults();
    assertNotNull(result);
    assertFalse(result.isEmpty());
    int nbSuccessful = 0;
    int nbCancelled = 0;
    for (Task<?> tsk: result) {
      LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      if (task.getResult() != null) {
        assertEquals("n3", task.getNodeUuid());
        nbSuccessful++;
      }
      else nbCancelled++;
    }
    assertEquals(nbTasksPerNode, nbSuccessful);
    assertEquals((BaseSetup.nbNodes() - 1) * nbTasksPerNode, nbCancelled);
    assertEquals(1, myNodeListener.map.size());
    AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertEquals(1, n.get());
    String[]  reservedJobs = (String[]) jmx.getAttribute("org.jppf:name=debug,type=driver", "ReservedJobs");
    assertNotNull(reservedJobs);
    assertEquals(0, reservedJobs.length);
    String[]  reservedNodes = (String[]) jmx.getAttribute("org.jppf:name=debug,type=driver", "ReservedNodes");
    assertNotNull(reservedNodes);
    assertEquals(0, reservedNodes.length);
  }

  /**
   * Test that when a job with reservation expires, all node reservations are removed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testExpiredJobReservationSingleNodeWithRestart() throws Exception {
    int nbTasksPerNode = 5;
    int nbTasks = nbTasksPerNode * BaseSetup.nbNodes();
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 500L);
    TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(1);
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(5000L));
    List<Task<?>> result = client.submitJob(job);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    int nbSuccessful = 0;
    int nbCancelled = 0;
    for (Task<?> tsk: result) {
      LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      if (task.getResult() != null) {
        assertEquals("n3", task.getNodeUuid());
        nbSuccessful++;
      }
      else nbCancelled++;
    }
    assertEquals(nbTasksPerNode, nbSuccessful);
    assertEquals((BaseSetup.nbNodes() - 1) * nbTasksPerNode, nbCancelled);
    assertEquals(1, myNodeListener.map.size());
    AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertEquals(1, n.get());
    String[]  reservedJobs = (String[]) jmx.getAttribute("org.jppf:name=debug,type=driver", "ReservedJobs");
    assertNotNull(reservedJobs);
    assertEquals(0, reservedJobs.length);
    String[]  reservedNodes = (String[]) jmx.getAttribute("org.jppf:name=debug,type=driver", "ReservedNodes");
    assertNotNull(reservedNodes);
    assertEquals(0, reservedNodes.length);
  }

  /**
   * Test that a job is executed on the 2 nodes closest ot its desired node config spec after restart of these nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testJobReservationTwoNodesWithRestart() throws Exception {
    int nbTasks = 5 * BaseSetup.nbNodes();
    Set<String> expectedNodes = new TreeSet<>(Arrays.asList("n2", "n3"));
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 1L);
    // Levenshtein scores: node1: 10 ; node2 : 6 ; node3 : 2
    TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(expectedNodes.size());
    List<Task<?>> result = client.submitJob(job);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    Set<String> actualNodes = new HashSet<>();
    for (Task<?> t: result) {
      LifeCycleTask task = (LifeCycleTask) t;
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      String nodeUuid = task.getNodeUuid();
      assertTrue(String.format("node %s is not one of %s", nodeUuid, expectedNodes), expectedNodes.contains(nodeUuid));
      if (!actualNodes.contains(nodeUuid)) actualNodes.add(nodeUuid);
    }
    assertEquals(expectedNodes, actualNodes);
    assertEquals(2, myNodeListener.map.size());
    Set<String> set = myNodeListener.map.keySet();
    assertEquals(expectedNodes, set);
    int sum = 0;
    for (String uuid: set) {
      AtomicInteger n = myNodeListener.map.get(uuid);
      assertNotNull(n);
      assertTrue(n.get() >= 1);
      sum += n.get();
    }
    assertEquals(BaseSetup.nbNodes(), sum);
  }

  /**
   * Test that a job is executed on the node closest to its desired node config spec,
   * with this node being restarted only when its configuration does not match the desired one.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 15000)
  public void testJobReservationSingleNodeNoRestart() throws Exception {
    int nbTasks = 5 * BaseSetup.nbNodes();
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 1L);
    TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props, false));
    job.getSLA().setMaxNodes(1);
    List<Task<?>> result = client.submitJob(job);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    for (Task<?> tsk: result) {
      LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals("n3", task.getNodeUuid());
    }
    assertEquals(1, myNodeListener.map.size());
    AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertEquals(1, n.get());
  }

  /**
   * Task which resets the node configuration.
   */
  public static class BroadcastTask extends AbstractTask<String> {
    @Override
    public void run() {
      try {
        JPPFConfiguration.reset();
        ((JPPFNode) NodeRunner.getNode()).triggerConfigChanged();
        System.out.println("reset configuration on node " + NodeRunner.getUuid());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Listener to node connection events, counts the number of times each node is restarted.
   */
  public class MyNodeConnectionListener implements NotificationListener {
    /**
     * Map of node uuids to number of restarts.
     */
    final Map<String, AtomicInteger> map = new ConcurrentHashMap<>();

    @Override
    public synchronized void handleNotification(final Notification notif, final Object handback) {
      JPPFManagementInfo info = (JPPFManagementInfo) notif.getUserData();
      switch (notif.getType()) {
        case JPPFNodeConnectionNotifierMBean.CONNECTED:
          String uuid = info.getUuid();
          AtomicInteger n = map.get(uuid);
          if (n == null) {
            n = new AtomicInteger(0);
            map.put(uuid, n);
          }
          n.incrementAndGet();
          break;

        case JPPFNodeConnectionNotifierMBean.DISCONNECTED:
          break;
      }
    }
  }
}
