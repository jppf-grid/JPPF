/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.*;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

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
  private static JMXNodeConnectionWrapper nodeJmx;
  /**
   * Connection to the driver's JMX server.
   */
  private static JMXDriverConnectionWrapper driverJmx;
  /**
   * Proxy to the node task monitor MBean.
   */
  private static JPPFNodeTaskMonitorMBean taskMonitor;

  /** */
  @Rule
  public final TestWatcher testJPPFNodeTaskMonitorMBeanWatcher = new TestWatcher() {
    @Override
    protected void starting(final Description description) {
      BaseTestHelper.printToAll(client, false, true, true, true, "***** start of method %s() *****", description.getMethodName());
    }
  };

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    client = BaseSetup.setup(1);
    driverJmx = BaseSetup.getJMXConnection(client);
    final Collection<JPPFManagementInfo> coll = driverJmx.nodesInformation();
    final JPPFManagementInfo info = coll.iterator().next();
    nodeJmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
    nodeJmx.connectAndWait(5000L);
    if (!nodeJmx.isConnected()) {
      nodeJmx = null;
      throw new Exception("could not connect to the node's JMX server");
    }
    taskMonitor = nodeJmx.getJPPFNodeTaskMonitorProxy();
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    Exception e = null;
    if (nodeJmx != null) {
      try {
        nodeJmx.close();
      } catch(final Exception e2) {
        e = e2;
      } finally {
        nodeJmx = null;
        taskMonitor = null;
      }
    }
    BaseSetup.cleanup();
    if (e != null) throw e;
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testSnapshot() throws Exception {
    print(false, false, "classpath: %s", System.getProperty("java.class.path"));
    final long duration = 100L;
    try {
      assertEquals(Integer.valueOf(0), taskMonitor.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(0), taskMonitor.getTotalTasksInError());
      assertEquals(Integer.valueOf(0), taskMonitor.getTotalTasksSucessfull());
      assertEquals(Long.valueOf(0L), taskMonitor.getTotalTaskCpuTime());
      assertEquals(Long.valueOf(0L), taskMonitor.getTotalTaskElapsedTime());
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, duration);
      job.add(new ErrorLifeCycleTask(duration, true)).setId(job.getName() + "-task_2");
      client.submit(job);
      assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> taskMonitor.getTotalTasksExecuted() == 2, 3000L, 250L, false));
      assertEquals(Integer.valueOf(1), taskMonitor.getTotalTasksInError());
      assertEquals(Integer.valueOf(1), taskMonitor.getTotalTasksSucessfull());
      Long n = taskMonitor.getTotalTaskCpuTime();
      assertTrue("cpu time is only " + n, n > 0L);
      n = taskMonitor.getTotalTaskElapsedTime();
      assertTrue("elapsed time is only " + n, n >= duration - 1L);
    } finally {
      taskMonitor.reset();
      nodeJmx.resetTaskCounter();
    }
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000)
  public void testReset() throws Exception {
    final long duration = 100L;
    try {
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, LifeCycleTask.class, duration);
      job.add(new ErrorLifeCycleTask(duration, true)).setId(job.getName() + "-task_2");
      client.submit(job);
      assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> taskMonitor.getTotalTasksExecuted() == 2, 3000L, 250L, false));
      assertEquals(Integer.valueOf(1), taskMonitor.getTotalTasksInError());
      assertEquals(Integer.valueOf(1), taskMonitor.getTotalTasksSucessfull());
      Long n = taskMonitor.getTotalTaskCpuTime();
      assertTrue("cpu time is only " + n, n > 0L);
      n = taskMonitor.getTotalTaskElapsedTime();
      assertTrue("elapsed time is only " + n, n >= duration - 1L);
      taskMonitor.reset();
      assertTrue(ConcurrentUtils.awaitCondition((ConditionFalseOnException) () -> taskMonitor.getTotalTasksExecuted() == 0, 3000L, 250L, false));
      assertEquals(Integer.valueOf(0), taskMonitor.getTotalTasksInError());
      assertEquals(Integer.valueOf(0), taskMonitor.getTotalTasksSucessfull());
      assertEquals(Long.valueOf(0L), taskMonitor.getTotalTaskCpuTime());
      assertEquals(Long.valueOf(0L), taskMonitor.getTotalTaskElapsedTime());
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
    final long duration = 100L;
    final int nbTasks = 5;
    final NodeNotificationListener listener = new NodeNotificationListener();
    try {
      taskMonitor.addNotificationListener(listener, null, null);
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, nbTasks - 1, LifeCycleTask.class, duration);
      job.add(new ErrorLifeCycleTask(duration, true)).setId(job.getName() + "-task_" + nbTasks);
      final List<Task<?>> result = client.submit(job);
      assertTrue(ConcurrentUtils.awaitCondition(() -> listener.notifs.size() == nbTasks + 1, 3000L, 250L, false));
      assertNull(listener.exception);
      assertEquals(1, listener.userObjects.size());
      Collections.sort(listener.notifs, (o1, o2) -> o1.getId().compareTo(o2.getId()));
      for (int i=0; i<nbTasks; i++) {
        final Task<?> task = result.get(i);
        TaskInformation ti = listener.notifs.get(i);
        assertEquals(job.getUuid(), ti.getJobId());
        assertEquals(task.getId(), ti.getId()); //
        Long n = ti.getElapsedTime();
        if (i < nbTasks - 1) assertFalse(ti.hasError());
        else {
          assertFalse(ti.hasError());
          n = ti.getCpuTime();
          assertTrue(n < 0L);
          assertEquals(ErrorLifeCycleTask.ERROR_TASK_RESULT_PREFIX + task.getId(), listener.userObjects.get(0));
          ti = listener.notifs.get(i + 1);
          n = ti.getCpuTime();
          assertTrue("task " + i + " cpu time is only " + n, n > 0L);
        }
      }
      taskMonitor.removeNotificationListener(listener);
    } finally {
      taskMonitor.reset();
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
        final TaskExecutionNotification notif = (TaskExecutionNotification) notification;
        notifs.add(notif.getTaskInformation());
        printOut("got task notification for task %s", notif.getTaskInformation().getId());
        if (notif.getUserData() != null) userObjects.add(notif.getUserData());
      } catch (final Exception e) {
        if (exception == null) exception = e;
      }
    }
  }

  /**
   * This class throws an {@link Exception} in its {@code run()} method.
   */
  public static class ErrorLifeCycleTask extends LifeCycleTask {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * 
     */
    private static final String ERROR_TASK_RESULT_PREFIX = "starting task ";
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
      final long start = System.nanoTime();
      fireNotification(ERROR_TASK_RESULT_PREFIX + getId(), true);
      fireNotification("non-JMX notification for " + getId(), false);
      final Random rand = new Random(start);
      while ((elapsed = System.nanoTime() - start) < duration * 1_000_000L) {
        final double d = Math.exp(35525.36789d * rand.nextDouble());
        final String s = String.valueOf(d) + (d < 100d ? " < 100" : " >= 100");
        s.toString();
      }
      if (raiseException) throw new IllegalStateException("this error is thrown deliberately");
    }
  }
}
