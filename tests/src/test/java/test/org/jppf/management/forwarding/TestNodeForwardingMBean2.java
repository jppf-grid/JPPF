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

package test.org.jppf.management.forwarding;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Notification;

import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.test.addons.mbeans.*;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.collections.CollectionUtils;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFNodeForwardingMBean}.
 * In this class, we test that the notifications mechanism provided by {@code JPPFNodeForwardingMBean}.
 * @author Laurent Cohen
 */
public class TestNodeForwardingMBean2 extends AbstractTestNodeForwardingMBean {
  /**
   * Test getting notifications with an {@code AllNodesSelector} selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsAllNodes() throws Exception {
    testNotifications(new AllNodesSelector(), "n1", "n2");
  }

  /**
   * Test getting notifications with an {@code ExecutionPolicySelector} selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsExecutionPolicySelector() throws Exception {
    testNotifications(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), "n1");
  }

  /**
   * Test getting notifications with an {@code UuidSelector} selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsUuidSelector() throws Exception {
    testNotifications(new UuidSelector("n2"), "n2");
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resilve to.
   * @throws Exception if any error occurs
   */
  private static void testNotifications(final NodeSelector selector, final String...expectedNodes) throws Exception {
    final int nbNodes = allNodes.size();
    final int nbTasks = 5 * nbNodes;
    NotifyingTaskListener listener = null;
    String listenerID = null;
    try {
      listener = new NotifyingTaskListener();
      print("registering notification listener with selector = %s", selector);
      listenerID = driverJmx.registerForwardingNotificationListener(selector, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      //listenerID = driverJmx.registerForwardingNotificationListener(selector, JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, "testing");
      print("registered notification listener with listenerID = %s", listenerID);
      final String jobName = ReflectionUtils.getCurrentMethodName() + ':' + selector.getClass().getSimpleName();
      final JPPFJob job = BaseTestHelper.createJob(jobName, false, nbTasks, NotifyingTask.class, 100L);
      print("submitting job");
      client.submit(job);
      Thread.sleep(1500L);
      print("checking notifications");
      checkNotifs(listener.notifs, nbTasks, expectedNodes);
    } finally {
      nodeForwarder.resetTaskCounter(selector);
      driverJmx.unregisterForwardingNotificationListener(listenerID);
    }
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a {@code AllNodesSelector} selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedAllNodesSelector() throws Exception {
    testNoNotifcationReceived(new AllNodesSelector(), "n1", "n2");
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a {@code ExecutionPolicySelector} selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedExecutionPolicySelector() throws Exception {
    testNoNotifcationReceived(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), "n1");
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a {@code UuidSelector} selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedUuidSelector() throws Exception {
    testNoNotifcationReceived(new UuidSelector("n2"), "n2");
  }

  /**
   * Test that multiple notification listeners are registered and each receives expected notifications.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testMultipleNotificationListeners() throws Exception {
    final Map<String, SimpleNotificationListener> listenerMap = new HashMap<>();
    final List<String> ids = new ArrayList<>();
    try {
      for (int i=1; i<=2; i++) {
        final SimpleNotificationListener listener = new SimpleNotificationListener();
        final String listenerID = driverJmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, "testing-" + i);
        listenerMap.put(listenerID, listener);
        ids.add(listenerID);
        print(false, false, "registered listenerID = %s", listenerID);
      }
      client.submit(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, MyNotifyingTask.class, 100L));
      print(false, false, "got job results");
      final ConcurrentUtils.Condition condition = () -> {
        for (final SimpleNotificationListener lsnr: listenerMap.values()) {
          if (lsnr.userNotifs.get() < 1) return false;
        }
        return true;
      };
      assertTrue(ConcurrentUtils.awaitCondition(condition, 5000L, 100L, false));
      
      final String id = ids.remove(1);
      print(false, false, "removing listenerID = %s", id);
      assertNotNull(listenerMap.remove(id));
      driverJmx.unregisterForwardingNotificationListener(id);
      final SimpleNotificationListener listener = listenerMap.get(ids.get(0));
      client.submit(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-2", false, 1, MyNotifyingTask.class, 100L));
      assertTrue(ConcurrentUtils.awaitCondition(() -> listener.userNotifs.get() == 2, 5000L, 100L, false));
    } finally {
      for (final String id: listenerMap.keySet()) driverJmx.unregisterForwardingNotificationListener(id);
      listenerMap.clear();
    }
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resilve to.
   * @throws Exception if any error occurs
   */
  private static void testNoNotifcationReceived(final NodeSelector selector, final String...expectedNodes) throws Exception {
    final int nbNodes = expectedNodes.length;
    final int nbTasks = 5 * nbNodes;
    NotifyingTaskListener listener = null;
    String listenerID = null;
    try {
      listener = new NotifyingTaskListener();
      listenerID = driverJmx.registerForwardingNotificationListener(selector, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      driverJmx.unregisterForwardingNotificationListener(listenerID);
      final String jobName = ReflectionUtils.getCurrentMethodName() + ':' + selector.getClass().getSimpleName();
      final JPPFJob job = BaseTestHelper.createJob(jobName, false, nbTasks, NotifyingTask.class, 100L);
      client.submit(job);
      assertTrue(listener.notifs.isEmpty());
      assertNull(listener.exception);
    } finally {
      nodeForwarder.resetTaskCounter(selector);
    }
  }

  /**
   * Check that the received notifications are the epxected ones.
   * Here we expect that:
   * <ul>
   * <li>the number of tasks is a multiple of the total number of nodes</li>
   * <li>the tasks are evenly spread among the nodes</li>
   * </ul>
   * @param notifs the notifications that were received.
   * @param nbTasks the total number of takss that were executed.
   * @param expectedNodes the nodes from which notifications should have been received.
   * @throws Exception if any error occurs.
   */
  private static void checkNotifs(final List<Notification> notifs, final int nbTasks, final String...expectedNodes) throws Exception {
    assertNotNull(expectedNodes);
    assertTrue(expectedNodes.length > 0);
    final Set<String> expectedNodesSet = CollectionUtils.set(expectedNodes);
    assertNotNull(notifs);
    final int nbNotifsPerNode = nbTasks / allNodes.size();
    final StringBuilder sb = new StringBuilder("notifications: {\n");
    notifs.forEach(notif -> sb.append("  ").append(notif).append('\n'));
    sb.append('}');
    print(sb.toString());
    assertEquals(expectedNodes.length * nbNotifsPerNode, notifs.size());
    final Map<String, AtomicInteger> notifCounts = new HashMap<>();
    for (final String uuid: expectedNodes) notifCounts.put(uuid, new AtomicInteger(0));
    for (final Notification notification: notifs) {
      assertTrue(notification instanceof JPPFNodeForwardingNotification);
      final JPPFNodeForwardingNotification outerNotif = (JPPFNodeForwardingNotification) notification;
      assertEquals(NodeTestMBean.MBEAN_NAME, outerNotif.getMBeanName());
      final Notification notif = outerNotif.getNotification();
      assertTrue(notif.getUserData() instanceof UserObject);
      final UserObject userObject = (UserObject) notif.getUserData();
      assertNotNull(userObject.nodeUuid);
      assertTrue(expectedNodesSet.contains(userObject.nodeUuid));
      assertEquals(outerNotif.getNodeUuid(), userObject.nodeUuid);
      notifCounts.get(userObject.nodeUuid).incrementAndGet();
    }
    for (final Map.Entry<String, AtomicInteger> entry: notifCounts.entrySet()) {
      assertEquals(nbNotifsPerNode, entry.getValue().get());
    }
  }

  /**
   * A simple test class.
   */
  public static class MyNotifyingTask extends AbstractTask<String> {
    /** */
    private final long duration;

    /**
     * @param duration .
     */
    public MyNotifyingTask(final long duration) {
      this.duration = duration;
    }

    @Override
    public void run() {
      fireNotification("start of " + getId(), true);
      try {
        if (duration > 0L) Thread.sleep(duration);
      } catch (final Exception e) {
        setThrowable(e);
      }
    }
  }

  /** */
  public static class SimpleNotificationListener implements ForwardingNotificationListener {
    /** */
    public AtomicInteger userNotifs = new AtomicInteger(0);

    @Override
    public void handleNotification(final JPPFNodeForwardingNotification notification, final Object handback) {
      final Notification realNotif = notification.getNotification();
      if (realNotif instanceof TaskExecutionNotification) {
        final TaskExecutionNotification notif = (TaskExecutionNotification) realNotif;
        if (notif.isUserNotification()) {
          userNotifs.incrementAndGet();
          print(false, false, "received notif %s, user object = %s", notification, notif.getUserData());
        }
      }
    }
  }
}
