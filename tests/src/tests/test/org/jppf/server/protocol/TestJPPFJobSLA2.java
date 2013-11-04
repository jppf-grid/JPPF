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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.util.*;

import javax.management.Notification;

import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.management.forwarding.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.test.addons.mbeans.*;
import org.jppf.utils.ReflectionUtils;
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
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 750L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 3000L;
  /**
   * A the date format used in the tests.
   */
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

  /**
   * Test that a job dispatch expires after a given duration.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=8000)
  public void testDispatchExpirationSchedule() throws Exception {
    String listenerId = null;
    checkNodes();
    JMXDriverConnectionWrapper jmx = BaseSetup.getDriverManagementProxy();
    try {
      NotifyingTaskListener listener = new NotifyingTaskListener();
      listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      JPPFJob job = BaseTestHelper.createJob2(ReflectionUtils.getCurrentMethodName(), true, false, new NotifyingTask(100L), new NotifyingTask(5000L));
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(2000L));
      List<Task<?>> results = client.submitJob(job);
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
      Notification notification = listener.notifs.get(0);
      assertTrue(notification instanceof JPPFNodeForwardingNotification);
      JPPFNodeForwardingNotification outerNotif = (JPPFNodeForwardingNotification) notification;
      assertEquals(NodeTestMBean.MBEAN_NAME, outerNotif.getMBeanName());
      Notification notif = outerNotif.getNotification();
      assertTrue(notif.getUserData() instanceof UserObject);
      UserObject userObject = (UserObject) notif.getUserData();
      assertNotNull(userObject.nodeUuid);
      task = (Task<?>) job.getJobTasks().get(0);
      assertEquals(NotifyingTask.END_PREFIX + task.getId(), userObject.taskId);
    } finally {
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
    checkNodes();
    JMXDriverConnectionWrapper jmx = BaseSetup.getDriverManagementProxy();
    try {
      NotifyingTaskListener listener = new NotifyingTaskListener();
      listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, NodeTestMBean.MBEAN_NAME, listener, null, "testing");
      JPPFJob job = BaseTestHelper.createJob2(ReflectionUtils.getCurrentMethodName(), true, false, new NotifyingTask(5000L, true, true));
      job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(1000L));
      job.getSLA().setMaxDispatchExpirations(2);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), 1);
      Task<?> task = results.get(0);
      assertNull(task.getResult());
      assertNull(task.getThrowable());
      Thread.sleep(1000L);
      assertNotNull(listener.notifs);
      assertEquals(3, listener.notifs.size());
      for (Notification notification: listener.notifs)
      {
        assertTrue(notification instanceof JPPFNodeForwardingNotification);
        JPPFNodeForwardingNotification outerNotif = (JPPFNodeForwardingNotification) notification;
        assertEquals(NodeTestMBean.MBEAN_NAME, outerNotif.getMBeanName());
        Notification notif = outerNotif.getNotification();
        assertTrue(notif.getUserData() instanceof UserObject);
        UserObject userObject = (UserObject) notif.getUserData();
        assertNotNull(userObject.nodeUuid);
        task = (JPPFTask) job.getJobTasks().get(0);
        assertEquals(NotifyingTask.START_PREFIX + task.getId(), userObject.taskId);
      }
    } finally {
      if (listenerId != null) jmx.unregisterForwardingNotificationListener(listenerId);
    }
  }

  /**
   * Wait until all nodes are connected to the driver via JMX.
   * @throws Exception if any error occurs.
   */
  private void checkNodes() throws Exception {
    int nbNodes = BaseSetup.nbNodes();
    JMXDriverConnectionWrapper driverJmx = BaseSetup.getDriverManagementProxy(client);
    JPPFNodeForwardingMBean nodeForwarder = driverJmx.getProxy(JPPFNodeForwardingMBean.MBEAN_NAME, JPPFNodeForwardingMBean.class);
    while (true) {
      Map<String, Object> result = nodeForwarder.state(NodeSelector.ALL_NODES);
      if (result.size() == nbNodes) {
        int count = 0;
        for (Map.Entry<String, Object>entry: result.entrySet()) {
          if (entry.getValue() instanceof JPPFNodeState) count++;
          else break;
        }
        if (count == nbNodes) break;
      }
      Thread.sleep(100L);
    }
  }
}
