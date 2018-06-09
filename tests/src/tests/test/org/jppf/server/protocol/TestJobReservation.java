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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.client.JPPFJob;
import org.jppf.job.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Test a job whose SLA mandates the configuration of the nodes it runs on.
 * @author Laurent Cohen
 */
public class TestJobReservation extends AbstractNonStandardSetup {
  /**
   * Listens for and counts nodes connections and disconnections.
   */
  private static final MyNodeConnectionListener myNodeListener = new MyNodeConnectionListener();
  /**
   * Test timeout.
   */
  private static final long TEST_TIMEOUT = 20_000L;
  /**
   * 
   */
  private static final String NODE_RESET_SCRIPT = new StringBuilder()
    .append("org.jppf.utils.JPPFConfiguration.reset();\n")
    .append("org.jppf.node.NodeRunner.getNode().triggerConfigChanged();\n")
    .append("java.lang.System.out.println(\"reset configuration on node \" + org.jppf.node.NodeRunner.getUuid());\n")
    .toString();
  /**
   * Waits for a JOB_DISPATCHED notification.
   */
  private AwaitJobNotificationListener jobNotificationListener;
  /**
   *
   */
  private static JMXDriverConnectionWrapper jmx;

  /**
   * Launch 1 driver with 3 nodes and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    final TestConfiguration cfg = createConfig("job_reservation");
    cfg.driver.log4j = "classes/tests/config/job_reservation/log4j-driver.properties";
    cfg.node.log4j = "classes/tests/config/job_reservation/log4j-node.properties";
    client = BaseSetup.setup(1, 3, true, cfg);
    jmx = client.awaitWorkingConnectionPool().awaitWorkingJMXConnection();
    jmx.addNotificationListener(JPPFNodeConnectionNotifierMBean.MBEAN_NAME, myNodeListener);
  }

  /** */
  @Rule
  public TestWatcher testJobReservationWatcher = new MyWatcher();

  /**
   * @throws Exception if any error occurs.
   */
  @Before
  public void showIdleNodes() throws Exception {
    BaseTest.print(false, "nb idle nodes = %d", jmx.nbIdleNodes());
    RetryUtils.runWithRetryTimeout(5000L, 500L, new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        final int n;
        if ((n = jmx.nbIdleNodes()) != BaseSetup.nbNodes()) throw new IllegalStateException(String.format("expected <%d> nodes but got <%d>", BaseSetup.nbNodes(), n));
        return true;
      }
    });
  }

  /**
   * Remove the jmx notification listener.
   * @throws Exception if any error occurs.
   */
  @After
  public void tearDown() throws Exception {
    if ((jobNotificationListener != null) && !jobNotificationListener.isListenerRemoved()) jmx.getJobManager().removeNotificationListener(jobNotificationListener);
    jobNotificationListener = null;
    myNodeListener.reset();
  }

  /**
   * Test that a job is executed on the node closest ot its desired node config spec after restart of this node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testJobReservationSingleNodeWithRestart() throws Exception {
    final  int nbTasks = 5 * BaseSetup.nbNodes();
    print(false, false, ">>> creating job");
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 1L);
    final TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(1);
    print(false, false, ">>> submitting job");
    final List<Task<?>> result = client.submitJob(job);
    print(false, false, ">>> checking job results");
    assertNotNull(result);
    assertFalse(result.isEmpty());
    for (final Task<?> tsk: result) {
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals("n3", task.getNodeUuid());
    }
    Thread.sleep(500L);
    while (myNodeListener.total.get() < BaseSetup.nbNodes()) Thread.sleep(100L);
    assertEquals(1, myNodeListener.map.size());
    final AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertTrue(String.format("expected at least <%d> but was <%d>", BaseSetup.nbNodes(), n.get()), n.get() >= BaseSetup.nbNodes());
    checkDriverReservations();
  }

  /**
   * Test that when a job with reservation is cancelled, all node reservations are removed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testCancelledJobReservationSingleNodeWithRestart() throws Exception {
    final int nbTasksPerNode = 5;
    final int nbTasks = nbTasksPerNode * BaseSetup.nbNodes();
    print(false, false, ">>> creating job");
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 500L);
    final TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(1);
    jobNotificationListener = new AwaitJobNotificationListener(client, JobEventType.JOB_RETURNED);
    print(false, false, ">>> submitting job");
    client.submitJob(job);
    print(false, false, ">>> awaiting JOB_RETURNED notfication");
    jobNotificationListener.await();
    print(false, false, ">>> cancelling job");
    job.cancel();
    print(false, false, ">>> awaiting job results");
    final List<Task<?>> result = job.awaitResults();
    print(false, false, ">>> checking job results");
    assertNotNull(result);
    assertFalse(result.isEmpty());
    int nbSuccessful = 0;
    int nbCancelled = 0;
    for (final Task<?> tsk: result) {
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      if (task.getResult() != null) {
        assertEquals("n3", task.getNodeUuid());
        nbSuccessful++;
      }
      else nbCancelled++;
    }
    assertEquals(nbTasksPerNode, nbSuccessful);
    assertEquals((BaseSetup.nbNodes() - 1) * nbTasksPerNode, nbCancelled);
    Thread.sleep(500L);
    assertEquals(1, myNodeListener.map.size());
    final AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertEquals(1, n.get());
    checkDriverReservations();
  }

  /**
   * Test that when a job with reservation expires, all node reservations are removed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testExpiredJobReservationSingleNodeWithRestart() throws Exception {
    print(false, false, ">>> creating job");
    final int nbTasksPerNode = 5;
    final int nbTasks = nbTasksPerNode * BaseSetup.nbNodes();
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 600L);
    final TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(1);
    jobNotificationListener = new AwaitJobNotificationListener(client, JobEventType.JOB_RETURNED);
    print(false, false, ">>> submitting job");
    client.submitJob(job);
    print(false, false, ">>> awaiting JOB_RETURNED notfication");
    jobNotificationListener.await();
    job.getSLA().setJobExpirationSchedule(new JPPFSchedule(1000L));
    print(false, false, ">>> updating job sla");
    jmx.getJobManager().updateJobs(new JobUuidSelector(job.getUuid()), job.getSLA(), null);
    print(false, false, ">>> awaiting job results");
    final List<Task<?>> result = job.awaitResults();
    print(false, false, ">>> got job results");
    assertNotNull(result);
    assertFalse(result.isEmpty());
    int nbSuccessful = 0;
    int nbCancelled = 0;
    for (final Task<?> tsk: result) {
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      if (task.getResult() != null) {
        assertEquals("n3", task.getNodeUuid());
        nbSuccessful++;
      }
      else nbCancelled++;
    }
    assertEquals(nbTasksPerNode, nbSuccessful);
    assertEquals((BaseSetup.nbNodes() - 1) * nbTasksPerNode, nbCancelled);
    Thread.sleep(500L);
    assertEquals(1, myNodeListener.map.size());
    final AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertTrue(n.get() < BaseSetup.nbNodes());
    checkDriverReservations();
  }

  /**
   * Test that a job is executed on the 2 nodes closest to its desired node config spec after restart of these nodes.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testJobReservationTwoNodesWithRestart() throws Exception {
    print(false, false, ">>> creating job");
    final int nbTasks = 5 * BaseSetup.nbNodes();
    final Set<String> expectedNodes = new TreeSet<>(Arrays.asList("n2", "n3"));
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 1L);
    // Levenshtein scores: node1: 10 ; node2 : 6 ; node3 : 2
    final TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props));
    job.getSLA().setMaxNodes(expectedNodes.size());
    print(false, false, ">>> submiting job");
    final List<Task<?>> result = client.submitJob(job);
    print(false, false, ">>> checking results");
    assertNotNull(result);
    assertFalse(result.isEmpty());
    final Set<String> actualNodes = new HashSet<>();
    for (final Task<?> t: result) {
      final LifeCycleTask task = (LifeCycleTask) t;
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      final String nodeUuid = task.getNodeUuid();
      assertTrue(String.format("node %s is not one of %s", nodeUuid, expectedNodes), expectedNodes.contains(nodeUuid));
      if (!actualNodes.contains(nodeUuid)) actualNodes.add(nodeUuid);
    }
    assertFalse(actualNodes.isEmpty());
    assertTrue(expectedNodes.containsAll(actualNodes));
    Thread.sleep(500L);
    print(false, false, ">>> waiting for %d nodes", BaseSetup.nbNodes());
    while (myNodeListener.total.get() < BaseSetup.nbNodes()) Thread.sleep(100L);
    assertFalse(myNodeListener.map.isEmpty());
    assertTrue(expectedNodes.containsAll(myNodeListener.map.keySet()));
    checkDriverReservations();
  }

  /**
   * Test that a job is executed on the node closest to its desired node config spec,
   * with this node being restarted only when its configuration does not match the desired one.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = TEST_TIMEOUT)
  public void testJobReservationSingleNodeNoRestart() throws Exception {
    print(false, false, ">>> creating job");
    final int nbTasks = 5 * BaseSetup.nbNodes();
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 1L);
    final TypedProperties props = new TypedProperties()
      .setString("reservation.prop.1", "123456")  // node1 : "1" ; node2 : "123" ; node3 : "12345"
      .setString("reservation.prop.2", "abcdef"); // node1 : "a" ; node2 : "abc" ; node3 : "abcde"
    job.getSLA().setDesiredNodeConfiguration(new JPPFNodeConfigSpec(props, false));
    job.getSLA().setMaxNodes(1);
    print(false, false, ">>> submiting job");
    final List<Task<?>> result = client.submitJob(job);
    print(false, false, ">>> checking results");
    assertNotNull(result);
    assertFalse(result.isEmpty());
    for (final Task<?> tsk: result) {
      final LifeCycleTask task = (LifeCycleTask) tsk;
      assertNull(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals("n3", task.getNodeUuid());
    }
    Thread.sleep(500L);
    assertEquals(1, myNodeListener.map.size());
    final AtomicInteger n = myNodeListener.map.get("n3");
    assertNotNull(n);
    assertEquals(1, n.get());
    checkDriverReservations();
  }

  /**
   * @throws Exception if any error occurs.
   */
  private static void checkDriverReservations() throws Exception {
    print(false, false, ">>> checking remaining reservations");
    @SuppressWarnings("unchecked")
    final Map<String, Set<String>> map = (Map<String, Set<String>>) jmx.getAttribute("org.jppf:name=debug,type=driver", "AllReservations");
    print(false, false, "reservations map = %s", map);
    final String[] keys = { "pendingJobs", "readyJobs", "pendingNodes", "readyNodes" };
    for (final String key: keys) {
      final Set<String> values = map.get(key);
      assertNotNull(key + " has null value set", values);
      assertTrue(key + " should be empty but has " + values.size() + " values", values.isEmpty());
    }
  }

  /**
   * Listener to node connection events, counts the number of times each node is restarted.
   */
  public static class MyNodeConnectionListener implements NotificationListener {
    /**
     * Map of node uuids to number of restarts.
     */
    final Map<String, AtomicInteger> map = new ConcurrentHashMap<>();
    /**
     * Total number of connection notifications.
     */
    final AtomicInteger total = new AtomicInteger(0);

    @Override
    public void handleNotification(final Notification notif, final Object handback) {
      final JPPFManagementInfo info = (JPPFManagementInfo) notif.getUserData();
      if (JPPFNodeConnectionNotifierMBean.CONNECTED.equals(notif.getType())) {
        final String uuid = info.getUuid();
        synchronized (this) {
          AtomicInteger n = map.get(uuid);
          if (n == null) map.put(uuid, n = new AtomicInteger(0));
          n.incrementAndGet();
          total.incrementAndGet();
          print(false, "got notification from node %s, timestamp = [%s], current count = %d, total = %d", uuid, BaseTest.getFormattedTimestamp(notif.getTimeStamp()), n.get(), total.get());
        }
      }
    }

    /**
     * Reset the state of this listener.
     */
    public synchronized void reset() {
      BaseTest.print(false, "resetting node connection listener");
      map.clear();
      total.set(0);
    }
  }

  /**
   * 
   */
  public class MyWatcher extends TestWatcher {
    @Override
    protected void starting(final Description description) {
      try {
        final int n = BaseSetup.nbNodes();
        //BaseSetup.checkDriverAndNodesInitialized(1, n);
        BaseTestHelper.printToAll(BaseSetup.getClient(), false, "checking idle nodes");
        RetryUtils.runWithRetryTimeout(5000L, 500L, new Callable<Void>() {
          @Override
          public Void call() throws Exception {
            if (jmx.nbIdleNodes() < BaseSetup.nbNodes()) throw new IllegalStateException("not enough idle nodes"); 
            return null;
          }
        });
        BaseTestHelper.printToAll(BaseSetup.getClient(), false, "before resetting nodes configurations");
        final JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
        final Map<String, Object> result = forwarder.forwardInvoke(NodeSelector.ALL_NODES, "org.jppf:name=debug,type=node", "executeScript",
          new Object[] { "javascript", NODE_RESET_SCRIPT }, new String[] { "java.lang.String", "java.lang.String" });
        assertEquals(n, result.size());
        final String[] uuids = new String[n];
        for (int i=0; i<n; i++) uuids[i] = "n" + (i + 1);
        for (final Map.Entry<String, Object> entry: result.entrySet()) {
          assertTrue(StringUtils.isOneOf(entry.getKey(), false, uuids));
          if (entry.getValue() instanceof Throwable) {
            print(false, false, "throwable raised by node %s: %s", entry.getKey(), ExceptionUtils.getStackTrace((Throwable) entry.getValue()));
          }
          assertFalse(entry.getValue() instanceof Throwable);
        }
        BaseTestHelper.printToAll(BaseSetup.getClient(), false, "after resetting nodes configurations");
        //while (jmx.nbIdleNodes() < BaseSetup.nbNodes()) Thread.sleep(10L);
        myNodeListener.map.clear();
      } catch (final Exception e) {
        e.printStackTrace();
        throw (e instanceof IllegalStateException) ? (IllegalStateException) e : new IllegalStateException(e);
      }
      BaseTestHelper.printToServersAndNodes(client, true, true, "start of method %s()", description.getMethodName());
    }

    @Override
    protected void finished(final Description description) {
      BaseTestHelper.printToAll(client, false, false, true, true, "end of method %s()", description.getMethodName());
    }
  };
}
