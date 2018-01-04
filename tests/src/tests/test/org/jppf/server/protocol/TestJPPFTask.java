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

import static org.jppf.utils.configuration.JPPFProperties.*;
import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFJob;
import org.jppf.management.*;
import org.jppf.node.NodeRunner;
import org.jppf.node.policy.Equal;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link Task}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestJPPFTask extends Setup1D1N1C {
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1000L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 3000L;
  /**
   * The date format used in the tests.
   */
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  /**
   * Used to test JPPFTask.compute(JPPFCallable) in method {@link #testComputeCallable()}.
   */
  static String callableResult = "";

  /**
   * Test the timeout of task with a timeout duration set.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTaskTimeout() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, TIME_LONG);
    final List<Task<?>> tasks = job.getJobTasks();
    final JPPFSchedule schedule = new JPPFSchedule(TIME_SHORT);
    tasks.get(nbTasks-1).setTimeoutSchedule(schedule);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertTrue(task.isTimedout());
  }

  /**
   * Test that the timeout countdown for a task starts when the task execution starts.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testTaskTimeoutStart() throws Exception {
    final int nbTasks = 2;
    final long timeout = 200L;
    final JPPFJob job = new JPPFJob(ReflectionUtils.getCurrentMethodName());
    job.add(new LifeCycleTask(2*timeout)).setId("task 1");
    MyTask task = new MyTask(2*timeout);
    task.setTimeoutSchedule(new JPPFSchedule(timeout));
    job.add(task).setId("task 2");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    task = (MyTask) results.get(1);
    assertNotNull(task.getResult());
    assertEquals("result is set", task.getResult());
    assertTrue(task.isTimedout());
  }

  /**
   * Test that a task expires at a specified date.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTaskExpirationDate() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, TIME_LONG);
    final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    final Date date = new Date(System.currentTimeMillis() + TIME_SHORT + 10L);
    final JPPFSchedule schedule = new JPPFSchedule(sdf.format(date), DATE_FORMAT);
    final List<Task<?>> tasks = job.getJobTasks();
    tasks.get(0).setTimeoutSchedule(schedule);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertTrue(task.isTimedout());
  }

  /**
   * Test the execution of a JPPFCallable via <code>Task.compute()</code>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testComputeCallable() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class, MyComputeCallable.class.getName());
    callableResult = "test successful";
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
    assertNotNull(task.getResult());
    assertEquals("test successful", task.getResult());
  }

  /**
   * Test the exception handling of a JPPFCallable which calls its <code>Task.compute()</code> method.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testComputeCallableThrowingException() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class, MyExceptionalCallable.class.getName());
    callableResult = "test successful";
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    assertNull(task.getResult());
    final Throwable t = task.getThrowable();
    assertNotNull(t);
    assertTrue("throwable class is " + t.getClass().getName(), t instanceof UnsupportedOperationException);
  }

  /**
   * Test the execution of a JPPFCallable via <code>Task.compute()</code> in the client.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testComputeCallableInClient() throws Exception {
    try {
      configure();
      final int nbTasks = 1;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class, MyComputeCallable.class.getName());
      callableResult = "test successful";
      final List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      final MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
      if (task.getThrowable() != null) throw new Exception(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals("test successful", task.getResult());
    } finally {
      reset();
    }
  }

  /**
   * Test the value of <code>Task.isInNode()</code> for a task executing in a node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testIsInNodeTrue() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
    assertNotNull(task.getResult());
    assertTrue((Boolean) task.getResult());
    assertNotNull(task.nodeUuid);
    assertNotNull(task.uuidFromNode);
    assertEquals(task.nodeUuid, task.uuidFromNode);
  }

  /**
   * Test the value of <code>Task.isInNode()</code> for a task executing locally in the client.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testIsInNodeFalse() throws Exception {
    try {
      configure();
      final int nbTasks = 1;
      final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class);
      final List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      final MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
      assertNotNull(task.getResult());
      assertFalse((Boolean) task.getResult());
      assertNotNull(task.nodeUuid);
      assertEquals("local_client", task.nodeUuid);
      assertNull(task.uuidFromNode);
    } finally {
      reset();
    }
  }

  /**
   * Test that notifications sent by a task are received by a locally registered listener.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTaskLocalNotifications() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotifyingTask.class);
    int count = 0;
    for (Task<?> task: job) task.setId("NotifyingTask " + ++count);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final NotifyingTask task = (NotifyingTask) results.get(0);
    assertTrue(task.getResult() instanceof List);
    final List<?> list = (List<?>) task.getResult();
    assertEquals(3, list.size());
    for (int i=0; i<3; i++) assertEquals("task notification " + (i+1), list.get(i));
  }

  /**
   * Test that JMX notifications sent by a task are received by a fowarding JMX notification listener.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTaskJMXNotifications() throws Exception {
    final NodeSelector selector = NodeSelector.ALL_NODES;
    final int nbTasks = 20;
    final NotifyingTaskListener listener = new NotifyingTaskListener();
    String listenerID = null;
    final JMXDriverConnectionWrapper driverJmx = BaseSetup.getJMXConnection();
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, NotifyingTask2.class);
    try {
      listenerID = driverJmx.registerForwardingNotificationListener(selector, JPPFNodeTaskMonitorMBean.MBEAN_NAME, listener, null, "testing");
      client.submitJob(job);
      Thread.sleep(1500L);
    } finally {
      driverJmx.unregisterForwardingNotificationListener(listenerID);
    }
    assertEquals(2*nbTasks, listener.taskExecutionUserNotificationCount);
    assertEquals(nbTasks, listener.taskExecutionJppfNotificationCount);
    for (Task<?> task: job) {
      assertTrue(listener.userObjects.contains(task.getId()  + "#1"));
      assertFalse(listener.userObjects.contains(task.getId() + "#2"));
      assertTrue(listener.userObjects.contains(task.getId()  + "#3"));
    }
  }
  
  /**
   * Test that the {@link AbstractTask#setResubmit(boolean)} method works properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTaskResubmit() throws Exception {
    final int nbTasks = 1;
    final int nbRuns = 5;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, ResubmittingTask.class, nbRuns);
    // ensure the job is only executed in a single specific node
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final Collection<JPPFManagementInfo> coll = jmx.nodesInformation();
    final String nodeUuid = coll.iterator().next().getUuid();
    job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", true, nodeUuid));
    job.getSLA().setMaxTaskResubmits(Integer.MAX_VALUE);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final ResubmittingTask task = (ResubmittingTask) results.get(0);
    assertNotNull(task.getResult());
    assertEquals(Integer.valueOf(nbRuns), task.getResult());
  }

  /**
   * Test that the max number of task resubmits set in the job SLA works properly.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testMaxTaskResubmits() throws Exception {
    final int nbTasks = 1;
    final int maxResubmits = 2;
    final int nbRuns = 5;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, ResubmittingTask.class, nbRuns);
    // ensure the job is only executed in a single specific node
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final Collection<JPPFManagementInfo> coll = jmx.nodesInformation();
    final String nodeUuid = coll.iterator().next().getUuid();
    job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", true, nodeUuid));
    job.getSLA().setMaxTaskResubmits(maxResubmits);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final ResubmittingTask task = (ResubmittingTask) results.get(0);
    assertNotNull(task.getResult());
    assertEquals(Integer.valueOf(maxResubmits + 1), task.getResult());
  }

  /**
   * Test that the max number of task resubmits set in the task works properly and overrides the one in the job SLA.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testMaxTaskResubmitsWithTaskOverride() throws Exception {
    final int nbTasks = 1;
    final int slaMaxResubmits = 2;
    final int taskMaxResubmits = slaMaxResubmits + 1;
    final int nbRuns = 5;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), true, false, nbTasks, ResubmittingTask.class, nbRuns);
    job.getJobTasks().get(0).setMaxResubmits(taskMaxResubmits);
    // ensure the job is only executed in a single specific node
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final Collection<JPPFManagementInfo> coll = jmx.nodesInformation();
    final String nodeUuid = coll.iterator().next().getUuid();
    job.getSLA().setExecutionPolicy(new Equal("jppf.node.uuid", true, nodeUuid));
    job.getSLA().setMaxTaskResubmits(slaMaxResubmits);
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final ResubmittingTask task = (ResubmittingTask) results.get(0);
    assertNotNull(task.getResult());
    assertEquals(Integer.valueOf(taskMaxResubmits + 1), task.getResult());
  }

  /**
   * A simple Task which calls its <code>compute()</code> method.
   */
  public static class MyComputeCallableTask extends AbstractTask<Object> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * The class name for the callable to invoke.
     */
    private final String callableClassName;
    /**
     * The uuid of the node obtained via {@code Task.getNode().getUuid()}.
     */
    public String uuidFromNode = null, nodeUuid = null;

    /**
     * Initialize this task.
     */
    public MyComputeCallableTask() {
      this.callableClassName = null;
    }

    /**
     * Iitialize this task.
     * @param callableClassName the class name for the callable to invoke.
     */
    public MyComputeCallableTask(final String callableClassName) {
      this.callableClassName = callableClassName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {
      try {
        printOut("this task's class loader = %s", getClass().getClassLoader());
        if (callableClassName != null) {
          final Class<?> clazz = Class.forName(callableClassName);
          final JPPFCallable<String> callable = (JPPFCallable<String>) clazz.newInstance();
          final String s = compute(callable);
          printOut("result of MyCallable.call() = %s", s);
          setResult(s);
        } else {
          final boolean b = isInNode();
          printOut("isInNode() = %b", b);
          setResult(b);
        }
        nodeUuid = JPPFConfiguration.getProperties().getString("jppf.node.uuid");
        if (isInNode()) uuidFromNode = getNode().getUuid();
        printOut("this task's nodeUuid = %s, uuidFromNode = %s", nodeUuid, uuidFromNode);
      } catch (final Exception e) {
        setThrowable(e);
      }
    }
  }

  /**
   * A simple <code>JPPFCallable</code>.
   */
  public static class MyComputeCallable implements JPPFCallable<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String call() throws Exception {
      printOut("result of MyCallable.call() = %s", callableResult);
      return callableResult;
    }
  }

  /**
   * A <code>JPPFCallable</code> whixh throws an exception in its <code>call()</code> method..
   */
  public static class MyExceptionalCallable implements JPPFCallable<String> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public String call() throws Exception {
      throw new UnsupportedOperationException("this exception is thrown intentionally");
    }
  }

  /**
   * Configure the client for a local execution.
   * @throws Exception if any error occurs.
   */
  private static void configure() throws Exception {
    client.close();
    // enable only local execution
    JPPFConfiguration.set(REMOTE_EXECUTION_ENABLED, false).set(LOCAL_EXECUTION_ENABLED, true);
    client = BaseSetup.createClient(null, false);
  }

  /**
   * Reset the confiugration.
   * @throws Exception if any error occurs.
   */
  private static void reset() throws Exception {
    // reset the client and config
    client.close();
    client = BaseSetup.createClient(null, true);
  }

  /**
   * An extension of LifeCycleTask which sets the result before calling {@link super.run()}.
   */
  public static class MyTask extends LifeCycleTask {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this task.
     * @param duration the  task duration.
     */
    public MyTask(final long duration) {
      super(duration);
    }

    @Override
    public void run() {
      setResult("result is set");
      super.run();
    }
  }

  /** */
  public static class NotifyingTask extends AbstractTask<Object> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      for (int i=1; i<=3; i++) {
        final int n = i;
        final Callable<Object> callable = new Callable<Object>() {
          @Override
          public Object call() throws Exception {
            return "task notification " + n;
          }
        };
        fireNotification(callable, false);
      }
    }
  }

  /** */
  public static class NotifyingTask2 extends AbstractTask<Object> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public void run() {
      fireNotification(getId() + "#1", true);
      fireNotification(getId() + "#2", false);
      fireNotification(getId() + "#3", true);
    }
  }

  /**
   * This task maintains a counter in the node and resubmits itself
   * until the counter has reached a specified value.
   */
  public static class ResubmittingTask extends AbstractTask<Integer> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Maximum number of runs for this task = max resubmit + 1.
     */
    private final int nbRuns;

    /**
     * Initialie this task.
     * @param nbRuns the maximum number of runs for this task.
     */
    public ResubmittingTask(final int nbRuns) {
      this.nbRuns = nbRuns;
    }

    @Override
    public void run() {
      final String key = getId() + "counter";
      AtomicInteger counter = (AtomicInteger) NodeRunner.getPersistentData(key);
      if (counter == null) {
        counter = new AtomicInteger(0);
        NodeRunner.setPersistentData(key, counter);
      }
      final int n = counter.incrementAndGet();
      if (n < nbRuns) setResubmit(true);
      else NodeRunner.removePersistentData(key);
      setResult(n);
    }
  }
}
