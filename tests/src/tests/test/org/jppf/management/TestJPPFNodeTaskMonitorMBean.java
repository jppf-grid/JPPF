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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ReflectionUtils;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFNodeAdminMBean}.
 * In this class, we test that the functionality of the JPPFNodeAdminMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFNodeTaskMonitorMBean extends BaseTest {
  /**
   * Connection to the node's JMX server.
   */
  private static JMXNodeConnectionWrapper nodeJmx = null;
  /**
   * Connection to the driver's JMX server.
   */
  private static JMXDriverConnectionWrapper driverJmx = null;
  /**
   * 
   */
  private static JPPFNodeTaskMonitorMBean nodeMonitorProxy = null;
  /**
   * The jppf client to use.
   */
  protected static JPPFClient client = null;

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    client = BaseSetup.setup(1);
    driverJmx = BaseSetup.getJMXConnection(client);
    Collection<JPPFManagementInfo> coll = driverJmx.nodesInformation();
    JPPFManagementInfo info = coll.iterator().next();
    nodeJmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
    nodeJmx.connectAndWait(5000L);
    if (!nodeJmx.isConnected()) {
      nodeJmx = null;
      throw new Exception("could not connect to the node's JMX server");
    }
    nodeMonitorProxy = nodeJmx.getJPPFNodeTaskMonitorProxy();
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    if (nodeJmx != null) {
      try {
        nodeJmx.close();
      } finally {
        nodeJmx = null;
        nodeMonitorProxy = null;
      }
    }
    BaseSetup.cleanup();
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSnapshot() throws Exception {
    long duration = 100L;
    try {
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksSucessfull());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskCpuTime());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskElapsedTime());
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, duration);
      job.add(new ErrorLifeCycleTask(duration, true)).setId(job.getName() + " - task 2");
      List<Task<?>> result = client.submitJob(job);
      assertEquals(Integer.valueOf(2), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksSucessfull());
      Long n = nodeMonitorProxy.getTotalTaskCpuTime();
      assertTrue("cpu time is only " + n, n > 0L);
      n = nodeMonitorProxy.getTotalTaskElapsedTime();
      assertTrue("elapsed time is only " + n, n >= duration - 1L);
    } finally {
      nodeMonitorProxy.reset();
      nodeJmx.resetTaskCounter();
    }
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testReset() throws Exception {
    long duration = 100L;
    try {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, duration);
      job.add(new ErrorLifeCycleTask(duration, true)).setId(job.getName() + " - task 2");
      List<Task<?>> result = client.submitJob(job);
      assertEquals(Integer.valueOf(2), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksSucessfull());
      Long n = nodeMonitorProxy.getTotalTaskCpuTime();
      assertTrue("cpu time is only " + n, n > 0L);
      n = nodeMonitorProxy.getTotalTaskElapsedTime();
      assertTrue("elapsed time is only " + n, n >= duration - 1L);
      nodeMonitorProxy.reset();
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksSucessfull());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskCpuTime());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskElapsedTime());
    } finally {
      nodeJmx.resetTaskCounter();
    }
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testNotifications() throws Exception {
    long duration = 100L;
    int nbTasks = 5;
    NodeNotificationListener listener = new NodeNotificationListener();
    try {
      nodeMonitorProxy.addNotificationListener(listener, null, null);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks - 1, LifeCycleTask.class, duration);
      job.add(new ErrorLifeCycleTask(duration, true)).setId(job.getName() + "-task " + nbTasks);
      List<Task<?>> result = client.submitJob(job);
      assertNull(listener.exception);
      assertEquals(nbTasks + 1, listener.notifs.size());
      assertEquals(1, listener.userObjects.size());
      Collections.sort(listener.notifs, new Comparator<TaskInformation>() {
        @Override
        public int compare(final TaskInformation o1, final TaskInformation o2) {
          return o1.getId().compareTo(o2.getId());
        }
      });
      for (int i=0; i<nbTasks; i++) {
        Task<?> task = result.get(i);
        TaskInformation ti = listener.notifs.get(i);
        assertEquals(job.getUuid(), ti.getJobId());
        assertEquals(task.getId(), ti.getId()); //
        Long n = ti.getElapsedTime();
        if (i < nbTasks - 1) assertFalse(ti.hasError());
        else {
          assertFalse(ti.hasError());
          n = ti.getCpuTime();
          assertTrue(n < 0L);
          assertEquals("starting task " + task.getId(), listener.userObjects.get(0));
          ti = listener.notifs.get(i + 1);
          n = ti.getCpuTime();
          assertTrue("task " + i + " cpu time is only " + n, n > 0L);
        }
      }
      nodeMonitorProxy.removeNotificationListener(listener);
    } finally {
      nodeMonitorProxy.reset();
      nodeJmx.resetTaskCounter();
    }
  }

  /** */
  public class NodeNotificationListener implements NotificationListener {
    /** The task information received as notifications from the node. */
    public List<TaskInformation> notifs = new Vector<>();
    /** A user-defined object. */
    public List<Object> userObjects = new Vector<>();
    /** */
    public Exception exception = null;

    @Override
    public synchronized void handleNotification(final Notification notification, final Object handback) {
      try {
        TaskExecutionNotification notif = (TaskExecutionNotification) notification;
        notifs.add(notif.getTaskInformation());
        System.out.println("got task notification for task " + notif.getTaskInformation().getId());
        if (notif.getUserData() != null) userObjects.add(notif.getUserData());
      } catch (Exception e) {
        if (exception == null) exception = e;
      }
    }
  }

  /**
   * This class throws an {@link Error} in its <code>run()</code> method.
   */
  public static class ErrorLifeCycleTask extends LifeCycleTask {
    /**
     * if true, then raise an exception at the end of execution.
     */
    private final boolean raiseException;

    /**
     * Initialize this task.
     * @param duration specifies the duration of this task.
     * @param raiseException if true, then raise an exception at the end of execution.
     */
    public ErrorLifeCycleTask(final long duration, final boolean raiseException) {
      super(duration);
      this.raiseException = raiseException;
    }

    @Override
    public void run() {
      long start = System.nanoTime();
      fireNotification("starting task " + getId(), true);
      fireNotification("non-JMX notification for " + getId(), false);
      Random rand = new Random(start);
      long elapsed = 0L;
      String s = "";
      while ((elapsed = System.nanoTime() - start) < duration * 1_000_000L) {
        double d = Math.exp(35525.36789d * rand.nextDouble());
        s = String.valueOf(d) + (d < 100d ? " < 100" : " >= 100");
      }
      if (raiseException) throw new IllegalStateException("this error is thrown deliberately");
    }
  }
}
