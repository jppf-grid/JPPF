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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFJob;
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
public class TestJPPFTask extends Setup1D1N1C
{
  /**
   * Count of the number of jobs created.
   */
  private static AtomicInteger jobCount = new AtomicInteger(0);
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1000L;
  /**
   * A "long" duration for this test.
   */
  private static final long TIME_LONG = 3000L;
  /**
   * A "rest" duration for this test.
   */
  private static final long TIME_REST = 1L;
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
  public void testTaskTimeout() throws Exception
  {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, TIME_LONG);
    List<Task<?>> tasks = job.getJobTasks();
    JPPFSchedule schedule = new JPPFSchedule(TIME_SHORT);
    tasks.get(nbTasks-1).setTimeoutSchedule(schedule);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertTrue(task.isTimedout());
  }

  /**
   * Test that the timeout countdown for a task starts when the task execution starts.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testTaskTimeoutStart() throws Exception
  {
    int nbTasks = 2;
    long timeout = 200L;
    JPPFJob job = new JPPFJob(ReflectionUtils.getCurrentMethodName());
    job.add(new LifeCycleTask(2*timeout)).setId("task 1");
    MyTask task = new MyTask(2*timeout);
    task.setTimeoutSchedule(new JPPFSchedule(timeout));
    job.add(task).setId("task 2");
    List<Task<?>> results = client.submitJob(job);
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
  public void testTaskExpirationDate() throws Exception
  {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, TIME_LONG);
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date(System.currentTimeMillis() + TIME_SHORT + 10L);
    JPPFSchedule schedule = new JPPFSchedule(sdf.format(date), DATE_FORMAT);
    List<Task<?>> tasks = job.getJobTasks();
    tasks.get(0).setTimeoutSchedule(schedule);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    LifeCycleTask task = (LifeCycleTask) results.get(0);
    assertNull(task.getResult());
    assertTrue(task.isTimedout());
  }

  /**
   * Test the execution of a JPPFCallable via <code>Task.compute()</code>.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testComputeCallable() throws Exception
  {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks,
        MyComputeCallableTask.class, MyComputeCallable.class.getName());
    callableResult = "test successful";
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
    assertNotNull(task.getResult());
    assertEquals("test successful", task.getResult());
  }

  /**
   * Test the exception handling of a JPPFCallable which calls its <code>Task.compute()</code> method.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testComputeCallableThrowingException() throws Exception
  {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks,
        MyComputeCallableTask.class, MyExceptionalCallable.class.getName());
    callableResult = "test successful";
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
    assertNull(task.getResult());
    assertNotNull(task.getThrowable());
    assertTrue(task.getThrowable() instanceof UnsupportedOperationException);
  }

  /**
   * Test the execution of a JPPFCallable via <code>Task.compute()</code> in the client.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testComputeCallableInClient() throws Exception
  {
    try
    {
      configure();
      int nbTasks = 1;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks,
          MyComputeCallableTask.class, MyComputeCallable.class.getName());
      callableResult = "test successful";
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
      if (task.getThrowable() != null) throw new Exception(task.getThrowable());
      assertNotNull(task.getResult());
      assertEquals("test successful", task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * Test the value of <code>Task.isInNode()</code> for a task executing in a node.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testIsInNodeTrue() throws Exception
  {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
    assertNotNull(task.getResult());
    assertTrue((Boolean) task.getResult());
  }

  /**
   * Test the value of <code>Task.isInNode()</code> for a task executing locally in the client.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testIsInNodeFalse() throws Exception
  {
    try
    {
      configure();
      int nbTasks = 1;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, MyComputeCallableTask.class);
      List<Task<?>> results = client.submitJob(job);
      assertNotNull(results);
      assertEquals(results.size(), nbTasks);
      MyComputeCallableTask task = (MyComputeCallableTask) results.get(0);
      assertNotNull(task.getResult());
      assertFalse((Boolean) task.getResult());
    }
    finally
    {
      reset();
    }
  }

  /**
   * A simple Task which calls its <code>compute()</code> method.
   */
  public static class MyComputeCallableTask extends AbstractTask<Object>
  {
    /**
     * The class name for the callable to invoke.
     */
    private final String callableClassName;

    /**
     * Iitialize this task.
     */
    public MyComputeCallableTask()
    {
      this.callableClassName = null;
    }

    /**
     * Iitialize this task.
     * @param callableClassName the class name for the callable to invoke.
     */
    public MyComputeCallableTask(final String callableClassName)
    {
      this.callableClassName = callableClassName;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run()
    {
      try
      {

        System.out.println("this task's class loader = " + getClass().getClassLoader());
        if (callableClassName != null)
        {
          Class<?> clazz = Class.forName(callableClassName);
          JPPFCallable<String> callable = (JPPFCallable<String>) clazz.newInstance();
          String s = compute(callable);
          System.out.println("result of MyCallable.call() = " + s);
          setResult(s);
        }
        else
        {
          boolean b = isInNode();
          System.out.println("isInNode() = " + b);
          setResult(b);
        }
      }
      catch (Exception e)
      {
        setThrowable(e);
      }
    }
  }

  /**
   * A simple <code>JPPFCallable</code>.
   */
  public static class MyComputeCallable implements JPPFCallable<String>
  {
    @Override
    public String call() throws Exception
    {
      System.out.println("result of MyCallable.call() = " + callableResult);
      return callableResult;
    }
  }

  /**
   * A <code>JPPFCallable</code> whixh throws an exception in its <code>call()</code> method..
   */
  public static class MyExceptionalCallable implements JPPFCallable<String>
  {
    @Override
    public String call() throws Exception
    {
      throw new UnsupportedOperationException("this exception is thrown intentionally");
    }
  }

  /**
   * Configure the client for a local execution.
   * @throws Exception if any error occurs.
   */
  private void configure() throws Exception
  {
    client.close();
    // enable only local execution
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.remote.execution.enabled", "false");
    config.setProperty("jppf.local.execution.enabled", "true");
    client = BaseSetup.createClient(null, false);
  }

  /**
   * Reset the confiugration.
   * @throws Exception if any error occurs.
   */
  private void reset() throws Exception
  {
    // reset the client and config
    client.close();
    client = BaseSetup.createClient(null, true);
  }

  /**
   * An extension of LifeCycleTask which sets the result before calling {@link super.run()}.
   */
  public static class MyTask extends LifeCycleTask
  {
    /**
     * Initialize this task.
     * @param duration the  task duration.
     */
    public MyTask(final long duration)
    {
      super(duration);
    }

    @Override
    public void run()
    {
      setResult("result is set");
      super.run();
    }
  }
}
