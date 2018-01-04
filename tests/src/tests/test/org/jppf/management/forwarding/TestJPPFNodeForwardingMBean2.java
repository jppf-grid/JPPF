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

package test.org.jppf.management.forwarding;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Notification;

import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.policy.Equal;
import org.jppf.test.addons.mbeans.*;
import org.jppf.utils.ReflectionUtils;
import org.jppf.utils.collections.CollectionUtils;
import org.junit.Test;

import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFNodeForwardingMBean}.
 * In this class, we test that the notifications mechanism provided by <code>JPPFNodeForwardingMBean</code>.
 * @author Laurent Cohen
 */
public class TestJPPFNodeForwardingMBean2 extends AbstractTestJPPFNodeForwardingMBean {
  /**
   * Test getting notifications with an <code>AllNodesSelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsAllNodes() throws Exception {
    testNotifications(new AllNodesSelector(), "n1", "n2");
  }

  /**
   * Test getting notifications with an <code>ExecutionPolicySelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsExecutionPolicySelector() throws Exception {
    testNotifications(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), "n1");
  }

  /**
   * Test getting notifications with an <code>UuidSelector</code> selector.
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
      configureLoadBalancer();
      listener = new NotifyingTaskListener();
      listenerID = driverJmx.registerForwardingNotificationListener(selector, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      final String jobName = ReflectionUtils.getCurrentMethodName() + ':' + selector.getClass().getSimpleName();
      final JPPFJob job = BaseTestHelper.createJob(jobName, true, false, nbTasks, NotifyingTask.class, 100L);
      client.submitJob(job);
      Thread.sleep(1500L);
      checkNotifs(listener.notifs, nbTasks, expectedNodes);
    } finally {
      nodeForwarder.resetTaskCounter(selector);
      resetLoadBalancer();
      driverJmx.unregisterForwardingNotificationListener(listenerID);
    }
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a <code>AllNodesSelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedAllNodesSelector() throws Exception {
    testNoNotifcationReceived(new AllNodesSelector(), "n1", "n2");
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a <code>ExecutionPolicySelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedExecutionPolicySelector() throws Exception {
    testNoNotifcationReceived(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), "n1");
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a <code>UuidSelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedUuidSelector() throws Exception {
    testNoNotifcationReceived(new UuidSelector("n2"), "n2");
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
      configureLoadBalancer();
      listener = new NotifyingTaskListener();
      listenerID = driverJmx.registerForwardingNotificationListener(selector, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      driverJmx.unregisterForwardingNotificationListener(listenerID);
      final String jobName = ReflectionUtils.getCurrentMethodName() + ':' + selector.getClass().getSimpleName();
      final JPPFJob job = BaseTestHelper.createJob(jobName, true, false, nbTasks, NotifyingTask.class, 100L);
      client.submitJob(job);
      assertTrue(listener.notifs.isEmpty());
      assertNull(listener.exception);
    } finally {
      nodeForwarder.resetTaskCounter(selector);
      resetLoadBalancer();
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
}
