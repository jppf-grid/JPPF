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

import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.client.JPPFJob;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D1N1C;

/**
 * Unit tests for POJO {@link Task}s annotated with &#64;{@link JPPFRunnable} or expressed s lambdas.
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
    job.add(NonStandardTasks.AnnotatedStaticMethodTask.class, "testParam");
    final List<Task<?>> results = client.submit(job);
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
    job.add(new NonStandardTasks.AnnotatedInstanceMethodTask(), "testParam");
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals("task ended for param testParam", task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof NonStandardTasks.AnnotatedInstanceMethodTask);
    final NonStandardTasks.AnnotatedInstanceMethodTask aimt = (NonStandardTasks.AnnotatedInstanceMethodTask) task.getTaskObject();
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
    job.add(NonStandardTasks.AnnotatedConstructorTask.class, "testParam");
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task.getResult() instanceof NonStandardTasks.AnnotatedConstructorTask);
    assertTrue(task instanceof JPPFAnnotatedTask);
    print(false, false, "task object: %s", task.getTaskObject());
    assertTrue(task.getTaskObject() instanceof NonStandardTasks.AnnotatedConstructorTask);
    final NonStandardTasks.AnnotatedConstructorTask act = (NonStandardTasks.AnnotatedConstructorTask) task.getTaskObject();
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
    job.add("staticMethod", NonStandardTasks.PojoTask.class, "testParam");
    final List<Task<?>> results = client.submit(job);
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
    job.add("instanceMethod", new NonStandardTasks.PojoTask(), "testParam");
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals(endResult, task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof NonStandardTasks.PojoTask);
    final NonStandardTasks.PojoTask aimt = (NonStandardTasks.PojoTask) task.getTaskObject();
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
    job.add("PojoTask", NonStandardTasks.PojoTask.class, "testParam");
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task.getResult() instanceof NonStandardTasks.PojoTask);
    assertTrue(task instanceof JPPFAnnotatedTask);
    print(false, false, "task object: %s", task.getTaskObject());
    assertTrue("taskObject is an instance of " + task.getTaskObject().getClass(), task.getTaskObject() instanceof NonStandardTasks.PojoTask);
    final NonStandardTasks.PojoTask act = (NonStandardTasks.PojoTask) task.getTaskObject();
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
    job.add(new NonStandardTasks.RunnableTask("testParam"));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof NonStandardTasks.RunnableTask);
    final NonStandardTasks.RunnableTask aimt = (NonStandardTasks.RunnableTask) task.getTaskObject();
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
    job.add(new NonStandardTasks.CallableTask("testParam"));
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    final Task<?> task = results.get(0);
    final Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals(endResult, task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof NonStandardTasks.CallableTask);
    final NonStandardTasks.CallableTask aimt = (NonStandardTasks.CallableTask) task.getTaskObject();
    assertNotNull(aimt.result);
    assertEquals(endResult, aimt.result);
  }

  /**
   * Test tasks expressed as lambda expressions.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testTasksAsLambdas() throws Exception {
    final String resultMessage = "Hello, world";
    final JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(NonStandardTasks.getRunnableTaskAsLambda(resultMessage));
    job.add(NonStandardTasks.getCallableTaskAsLambda(resultMessage));
    final int nbTasks = job.getTaskCount();
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    results.forEach(task -> {
      assertNotNull(task);
      final Throwable t = task.getThrowable();
      if (t != null) print(false, false, "task %s has exception:\n%s", task, ExceptionUtils.getStackTrace(t));
      assertNull(t);
      if (task.getTaskObject() instanceof Callable) assertEquals(resultMessage, task.getResult());
    });
  }
}
