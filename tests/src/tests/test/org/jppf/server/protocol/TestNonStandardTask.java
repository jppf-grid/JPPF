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

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.client.JPPFJob;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D1N1C;

/**
 * Unit tests for POJO {@link Task}s annotated with &#64;{@link JPPFRunnable}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA.
 * @author Laurent Cohen
 */
public class TestNonStandardTask extends Setup1D1N1C {
  /**
   * Test a POJO task with a static method annotated with &#64;{@link JPPFRunnable}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testAnnotatedStaticMethod() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(AnnotatedStaticMethodTask.class, "testParam");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals("task ended for param testParam", task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertNull(task.getTaskObject());
  }

  /**
   * Test a POJO task with an instance method annotated with &#64;{@link JPPFRunnable}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testAnnotatedInstanceMethod() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(new AnnotatedInstanceMethodTask(), "testParam");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals("task ended for param testParam", task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof AnnotatedInstanceMethodTask);
    final AnnotatedInstanceMethodTask aimt = (AnnotatedInstanceMethodTask) task.getTaskObject();
    assertNotNull(aimt.result);
    assertEquals("task ended for param testParam", aimt.result);
  }

  /**
   * Test a POJO task with a constructor annotated with &#64;{@link JPPFRunnable}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testAnnotatedConstructor() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(AnnotatedConstructorTask.class, "testParam");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task.getResult() instanceof AnnotatedConstructorTask);
    assertTrue(task instanceof JPPFAnnotatedTask);
    print(false, false, "task object: %s", task.getTaskObject());
    assertTrue(task.getTaskObject() instanceof AnnotatedConstructorTask);
    final AnnotatedConstructorTask act = (AnnotatedConstructorTask) task.getTaskObject();
    assertNotNull(act.result);
    assertEquals("task ended for param testParam", act.result);
    assertTrue(act == task.getResult());
  }

  /**
   * Test a POJO task of which a static method is executed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testPojoStaticMethod() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add("staticMethod", PojoTask.class, "testParam");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals("static result for param testParam", task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertNull(task.getTaskObject());
  }

  /**
   * Test a POJO task of which an instance method is executed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testPojoInstanceMethod() throws Exception {
    final int nbTasks = 1;
    final String endResult = "instance result for param testParam";
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add("instanceMethod", new PojoTask(), "testParam");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals(endResult, task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof PojoTask);
    final PojoTask aimt = (PojoTask) task.getTaskObject();
    assertNotNull(aimt.result);
    assertEquals(endResult, aimt.result);
  }

  /**
   * Test a POJO task whose constructor is executed.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testPojoConstructor() throws Exception {
    final int nbTasks = 1;
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add("PojoTask", PojoTask.class, "testParam");
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task.getResult() instanceof PojoTask);
    assertTrue(task instanceof JPPFAnnotatedTask);
    print(false, false, "task object: %s", task.getTaskObject());
    assertTrue("taskObject is an instance of " + task.getTaskObject().getClass(), task.getTaskObject() instanceof PojoTask);
    final PojoTask act = (PojoTask) task.getTaskObject();
    assertNotNull(act.result);
    assertEquals("constructor result for param testParam", act.result);
    assertTrue(act == task.getResult());
  }

  /**
   * Test a {@link Runnable} task.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testRunnableTask() throws Exception {
    final int nbTasks = 1;
    final String endResult = "runnable result for param testParam";
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(new RunnableTask("testParam"));
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof RunnableTask);
    final RunnableTask aimt = (RunnableTask) task.getTaskObject();
    assertNotNull(aimt.result);
    assertEquals(endResult, aimt.result);
  }

  /**
   * Test a {@link Callable} task.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testCallableTask() throws Exception {
    final int nbTasks = 1;
    final String endResult = "callable result for param testParam";
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(new CallableTask("testParam"));
    final List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals(endResult, task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof CallableTask);
    final CallableTask aimt = (CallableTask) task.getTaskObject();
    assertNotNull(aimt.result);
    assertEquals(endResult, aimt.result);
  }

  /** */
  public static class AnnotatedStaticMethodTask {
    /**
     * @param param .
     * @return .
     */
    @JPPFRunnable
    public static String staticMethod(final String param) {
      return "task ended for param " + param;
    }
  }

  /** */
  public static class AnnotatedInstanceMethodTask implements Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;

    /**
     * @param param .
     * @return .
     */
    @JPPFRunnable
    public String instanceMethod(final String param) {
      result = "task ended for param " + param;
      return result;
    }
  }

  /** */
  public static class AnnotatedConstructorTask implements Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    public final String result;

    /**
     * @param param .
     */
    @JPPFRunnable
    public AnnotatedConstructorTask(final String param) {
      this.result = "task ended for param " + param;
    }
  }

  /** */
  public static class PojoTask implements Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;

    /** */
    public PojoTask() {
    }

    /**
     * @param param .
     */
    public PojoTask(final String param) {
      this.result = "constructor result for param " + param;
    }

    /**
     * @param param .
     * @return .
     */
    public static String staticMethod(final String param) {
      return "static result for param " + param;
    }

    /**
     * @param param .
     * @return .
     */
    public String instanceMethod(final String param) {
      result = "instance result for param " + param;
      return result;
    }
  }

  /** */
  public static class RunnableTask implements Runnable, Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;
    /** */
    private final String param;

    /**
     * @param param .
     */
    public RunnableTask(final String param) {
      this.result = "constructor result for param " + param;
      this.param = param;
    }

    @Override
    public void run() {
      result = "runnable result for param " + param;
    }
  }


  /** */
  public static class CallableTask implements Callable<String>, Serializable {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
    /** */
    public String result;
    /** */
    private final String param;

    /**
     * @param param .
     */
    public CallableTask(final String param) {
      this.result = "constructor result for param " + param;
      this.param = param;
    }

    @Override
    public String call() {
      result = "callable result for param " + param;
      return result;
    }
  }
}
