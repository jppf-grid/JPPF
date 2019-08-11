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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.jppf.client.JPPFJob;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.test.addons.mbeans.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link org.jppf.node.protocol.JobSLA JobSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFJobSLA2 extends Setup1D2N1C {
  /**
   * Test that a job dispatch expires after a given duration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testDispatchExpirationSchedule() throws Exception {
    String listenerId = null;
    checkNodes();
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    final LoadBalancingInformation lbInfo = jmx.loadBalancerInformation();
    try {
      jmx.changeLoadBalancerSettings("manual", new TypedProperties().setInt("size", 1));
      final NotifyingTaskListener listener = new NotifyingTaskListener();
      listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      final JPPFJob job = BaseTestHelper.createJob2(ReflectionUtils.getCurrentMethodName(), false, new NotifyingTask(100L), new NotifyingTask(5000L));
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(2000L));
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), 2);
      Task<?> task = results.get(0);
      assertNotNull(task.getResult());
      assertEquals(NotifyingTask.SUCCESS, task.getResult());
      assertNull(task.getThrowable());
      task = results.get(1);
      assertNull(task.getResult());
      assertNull(task.getThrowable());
      Thread.sleep(1000L);
      assertNotNull(listener.notifs);
      assertEquals(1, listener.notifs.size());
      final Notification notification = listener.notifs.get(0);
      assertTrue(notification instanceof JPPFNodeForwardingNotification);
      final JPPFNodeForwardingNotification outerNotif = (JPPFNodeForwardingNotification) notification;
      assertEquals(NodeTestMBean.MBEAN_NAME, outerNotif.getMBeanName());
      final Notification notif = outerNotif.getNotification();
      assertTrue(notif.getUserData() instanceof UserObject);
      final UserObject userObject = (UserObject) notif.getUserData();
      assertNotNull(userObject.nodeUuid);
      task = job.getJobTasks().get(0);
      assertEquals(NotifyingTask.END_PREFIX + task.getId(), userObject.taskId);
    } finally {
      jmx.changeLoadBalancerSettings(lbInfo.getAlgorithm(), lbInfo.getParameters());
      if (listenerId != null) jmx.unregisterForwardingNotificationListener(listenerId);
    }
  }

  /**
   * Test that a job dispatch expires 2 times after a given duration, and that its tasks are cancelled after the second expiration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testMaxDispatchExpirations() throws Exception {
    String listenerId = null;
    final int maxExpirations = 2;
    checkNodes();
    BaseTestHelper.printToAll(client, false, "trace 0");
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    try {
      BaseTestHelper.printToAll(client, false, "trace 1");
      final NotifyingTaskListener listener = new NotifyingTaskListener();
      listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      BaseTestHelper.printToAll(client, false, "trace 2");
      //Thread.sleep(250);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, NotifyingTask.class, 5000L, true, true);
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(750L)).setMaxDispatchExpirations(maxExpirations);
      final List<Task<?>> results = client.submit(job);
      BaseTestHelper.printToAll(client, false, "trace 3");
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNull(task.getResult());
      assertNull(task.getThrowable());
      Thread.sleep(1000L);
      assertNotNull(listener.notifs);
      BaseTestHelper.printToAll(client, false, "trace 4");
      assertEquals(maxExpirations + 1, listener.notifs.size());
      for (final Notification notification: listener.notifs) {
        assertTrue(notification instanceof JPPFNodeForwardingNotification);
        final JPPFNodeForwardingNotification outerNotif = (JPPFNodeForwardingNotification) notification;
        assertEquals(NodeTestMBean.MBEAN_NAME, outerNotif.getMBeanName());
        final Notification notif = outerNotif.getNotification();
        assertTrue(notif.getUserData() instanceof UserObject);
        final UserObject userObject = (UserObject) notif.getUserData();
        assertNotNull(userObject.nodeUuid);
        task = job.getJobTasks().get(0);
        assertEquals(NotifyingTask.START_PREFIX + task.getId(), userObject.taskId);
      }
      BaseTestHelper.printToAll(client, false, "trace 5");
    } finally {
      if (listenerId != null) jmx.unregisterForwardingNotificationListener(listenerId);
      BaseTestHelper.printToAll(client, false, "trace 6");
    }
  }

  /**
   * Wait until all nodes are connected to the driver via JMX.
   * @throws Exception if any error occurs.
   */
  private static void checkNodes() throws Exception {
    final int nbNodes = BaseSetup.nbNodes();
    final JMXDriverConnectionWrapper driverJmx = BaseSetup.getJMXConnection(client);
    final JPPFNodeForwardingMBean nodeForwarder = driverJmx.getNodeForwarder();
    while (true) {
      final Map<String, Object> result = nodeForwarder.state(NodeSelector.ALL_NODES);
      if (result.size() == nbNodes) {
        int count = 0;
        for (final Map.Entry<String, Object>entry: result.entrySet()) {
          if (entry.getValue() instanceof JPPFNodeState) count++;
          else break;
        }
        if (count == nbNodes) break;
      }
      Thread.sleep(100L);
    }
  }

  /**
   * Test that a job dispatch expires after a given duration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testSimpleJob() throws Exception {
    String listenerId = null;
    checkNodes();
    final int nbTasks = 20;
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection();
    try {
      final MyTaskListener listener = new MyTaskListener();
      listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, "testing");
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks, LifeCycleTask.class, 0L, true, "notif");
      final List<Task<?>> results = client.submit(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      print(false, false, "received so far %d notifications", listener.getTotalNotificationCount());
      assertTrue(ConcurrentUtils.awaitCondition(() -> listener.getTotalNotificationCount() == 2 * nbTasks, 5000L, 250L, false));
      assertEquals(nbTasks, listener.taskExecutionUserNotificationCount);
      assertEquals(nbTasks, listener.startNotifMap.size());
      assertEquals(nbTasks, listener.taskExecutionJppfNotificationCount);
      assertEquals(nbTasks, listener.endNotifMap.size());
      for (int i=0; i<nbTasks; i++) {
        assertTrue(results.get(i) instanceof LifeCycleTask);
        final LifeCycleTask task = (LifeCycleTask) results.get(i);
        assertEquals(job.getJobTasks().get(i).getId(), task.getId());
        assertNotNull(task.getResult());
        assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
        final Collection<TaskExecutionNotification> startNotifs = listener.startNotifMap.getValues(task.getId());
        assertNotNull(startNotifs);
        assertEquals(1, startNotifs.size());
        final Collection<TaskExecutionNotification> endNotifs = listener.endNotifMap.getValues(task.getId());
        assertNotNull(endNotifs);
        assertEquals(1, endNotifs.size());
      }
    } finally {
      if (listenerId != null) jmx.unregisterForwardingNotificationListener(listenerId);
    }
  }

  /**
   * A JMX {@link NotificationListener} which simply accumulates the notifications it receives.
   * @author Laurent Cohen
   */
  static class MyTaskListener implements NotificationListener {
    /**
     * Collects the start notifications for all the tasks.
     */
    public final CollectionMap<String, TaskExecutionNotification> startNotifMap = new ArrayListHashMap<>();
    /**
     * Collects the start notifications for all the tasks.
     */
    public final CollectionMap<String, TaskExecutionNotification> endNotifMap = new ArrayListHashMap<>();
    /**
     * An eventual exception that occurred in the {@link #handleNotification(Notification, Object)} method.
     */
    public Exception exception;
    /**
     * The total count of notifications.
     */
    private int totalNotificationCount;
    /**
     * The count of user notifications sent via {@code Task.fireNotification()}.
     */
    public int taskExecutionUserNotificationCount;
    /**
     * The count of JPPF notifications sent via {@code Task.fireNotification()}.
     */
    public int taskExecutionJppfNotificationCount;

    @Override
    public synchronized void handleNotification(final Notification notification, final Object handback) {
      try {
        totalNotificationCount++;
        if (notification instanceof JPPFNodeForwardingNotification) {
          final JPPFNodeForwardingNotification forwardingNotif = (JPPFNodeForwardingNotification) notification;
          final String node = forwardingNotif.getNodeUuid();
          final Notification realNotif = forwardingNotif.getNotification();
          if (realNotif instanceof TaskExecutionNotification) {
            print(false, false, "received notification from node %s: %s", node, realNotif);
            final TaskExecutionNotification notif = (TaskExecutionNotification) realNotif;
            if (notif.isUserNotification()) {
              startNotifMap.putValue(notif.getTaskInformation().getId(), notif);
              taskExecutionUserNotificationCount++;
            } else {
              taskExecutionJppfNotificationCount++;
              endNotifMap.putValue(notif.getTaskInformation().getId(), notif);
            }
          }
        }
      } catch (final Exception e) {
        print(false, false, "exception in listener: %s", e);
        if (exception == null) exception = e;
      }
    }

    /**
     * @return the total count of notifications.
     */
    public synchronized int getTotalNotificationCount() {
      return totalNotificationCount;
    }
  }
}
