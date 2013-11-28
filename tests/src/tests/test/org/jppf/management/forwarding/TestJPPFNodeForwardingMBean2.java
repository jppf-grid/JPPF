/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import javax.management.*;

import org.jppf.client.JPPFJob;
import org.jppf.management.NodeSelector;
import org.jppf.management.NodeSelector.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.NodeRunner;
import org.jppf.node.policy.Equal;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.test.addons.mbeans.*;
import org.jppf.test.addons.startups.TaskNotifier;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * Unit tests for {@link JPPFNodeForwardingMBean}.
 * In this class, we test that the notifications mechanism provided by <code>JPPFNodeForwardingMBean</code>.
 * @author Laurent Cohen
 */
public class TestJPPFNodeForwardingMBean2 extends AbstractTestJPPFNodeForwardingMBean
{
  /**
   * Test getting notifications with an <code>AllNodesSelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsAllNodes() throws Exception
  {
    testNotifications(new AllNodesSelector(), "n1", "n2");
  }

  /**
   * Test getting notifications with an <code>ExecutionPolicySelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsExecutionPolicySelector() throws Exception
  {
    testNotifications(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), "n1");
  }

  /**
   * Test getting notifications with an <code>UuidSelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifcationsUuidSelector() throws Exception
  {
    testNotifications(new UuidSelector("n2"), "n2");
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resilve to.
   * @throws Exception if any error occurs
   */
  private void testNotifications(final NodeSelector selector, final String...expectedNodes) throws Exception
  {
    //int nbNodes = expectedNodes.length;
    int nbNodes = allNodes.size();
    int nbTasks = 5 * nbNodes;
    NodeNotificationListener listener = null;
    String listenerID = null;
    try
    {
      configureLoadBalancer();
      listener = new NodeNotificationListener();
      listenerID = driverJmx.registerForwardingNotificationListener(selector, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      String jobName = ReflectionUtils.getCurrentMethodName() + ':' + selector.getClass().getSimpleName();
      JPPFJob job = BaseTestHelper.createJob(jobName, true, false, nbTasks, NotificationTask.class, 100L);
      client.submit(job);
      Thread.sleep(1500L);
      checkNotifs(listener.notifs, nbTasks, expectedNodes);
    }
    finally
    {
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
  public void testNoNotifcationReceivedAllNodesSelector() throws Exception
  {
    testNoNotifcationReceived(new AllNodesSelector(), "n1", "n2");
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a <code>ExecutionPolicySelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedExecutionPolicySelector() throws Exception
  {
    testNoNotifcationReceived(new ExecutionPolicySelector(new Equal("jppf.node.uuid", false, "n1")), "n1");
  }

  /**
   * Test that no notifications are received after unregistering a notification listener with a <code>UuidSelector</code> selector.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNoNotifcationReceivedUuidSelector() throws Exception
  {
    testNoNotifcationReceived(new UuidSelector("n2"), "n2");
  }

  /**
   * Execute the tests with the specified node selector.
   * @param selector the selector to apply.
   * @param expectedNodes the set of nodes the selector is expected to resilve to.
   * @throws Exception if any error occurs
   */
  private void testNoNotifcationReceived(final NodeSelector selector, final String...expectedNodes) throws Exception
  {
    int nbNodes = expectedNodes.length;
    int nbTasks = 5 * nbNodes;
    NodeNotificationListener listener = null;
    String listenerID = null;
    try
    {
      configureLoadBalancer();
      listener = new NodeNotificationListener();
      listenerID = driverJmx.registerForwardingNotificationListener(selector, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      driverJmx.unregisterForwardingNotificationListener(listenerID);
      String jobName = ReflectionUtils.getCurrentMethodName() + ':' + selector.getClass().getSimpleName();
      JPPFJob job = BaseTestHelper.createJob(jobName, true, false, nbTasks, NotificationTask.class, 100L);
      client.submit(job);
      assertTrue(listener.notifs.isEmpty());
      assertNull(listener.exception);
    }
    finally
    {
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
  private void checkNotifs(final List<Notification> notifs, final int nbTasks, final String...expectedNodes) throws Exception
  {
    assertNotNull(expectedNodes);
    assertTrue(expectedNodes.length > 0);
    Set<String> expectedNodesSet = CollectionUtils.set(expectedNodes);
    assertNotNull(notifs);
    int nbNotifsPerNode = nbTasks / allNodes.size();
    assertEquals(expectedNodes.length * nbNotifsPerNode, notifs.size());
    Map<String, AtomicInteger> notifCounts = new HashMap<String, AtomicInteger>();
    for (String uuid: expectedNodes) notifCounts.put(uuid, new AtomicInteger(0));
    for (Notification notification: notifs)
    {
      assertTrue(notification instanceof JPPFNodeForwardingNotification);
      JPPFNodeForwardingNotification outerNotif = (JPPFNodeForwardingNotification) notification;
      assertEquals(NodeTestMBean.MBEAN_NAME, outerNotif.getMBeanName());
      Notification notif = outerNotif.getNotification();
      assertTrue(notif.getUserData() instanceof UserObject);
      UserObject userObject = (UserObject) notif.getUserData();
      assertNotNull(userObject.nodeUuid);
      assertTrue(expectedNodesSet.contains(userObject.nodeUuid));
      assertEquals(outerNotif.getNodeUuid(), userObject.nodeUuid);
      notifCounts.get(userObject.nodeUuid).incrementAndGet();
    }
    for (Map.Entry<String, AtomicInteger> entry: notifCounts.entrySet())
    {
      assertEquals(nbNotifsPerNode, entry.getValue().get());
    }
  }

  /**
   * A {@link NotificationListener} which simply accumulates the notifications it receives.
   */
  public static class NodeNotificationListener implements NotificationListener
  {
    /**
     * The task information received as notifications from the node.
     */
    public List<Notification> notifs = new Vector<Notification>();
    /**
     * 
     */
    public Exception exception = null;

    @Override
    public void handleNotification(final Notification notification, final Object handback)
    {
      try
      {
        notifs.add(notification);
      }
      catch (Exception e)
      {
        if (exception == null) exception = e;
      }
    }
  }

  /**
   * A task that sends a notification via NodeTestMBean.
   */
  public static class NotificationTask extends JPPFTask
  {
    /**
     * The duration of this task
     */
    private final long duration;

    /**
     * Initialize this task.
     * @param duration the duration of this task.
     */
    public NotificationTask(final long duration)
    {
      this.duration = duration;
    }

    @Override
    public void run()
    {
      try
      {
        Thread.sleep(duration);
        TaskNotifier.addNotification(new UserObject(NodeRunner.getUuid(), getId()));
        System.out.println("task " + getId() + " successful");
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
}
