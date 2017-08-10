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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.List;

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
public class TestJPPFRunnable extends Setup1D1N1C {
  /**
   * Test a POJO task with a static method annotated with &#64;{@link JPPFRunnable}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testAnnotatedStaticMethod() throws Exception {
    int nbTasks = 1;
    JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(AnnotatedStaticMethodTask.class, "testParam");
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    Task<?> task = results.get(0);
    Throwable t = task.getThrowable();
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
    int nbTasks = 1;
    JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(new AnnotatedInstanceMethodTask(), "testParam");
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    Task<?> task = results.get(0);
    Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertNotNull(task.getResult());
    assertEquals("task ended for param testParam", task.getResult());
    assertTrue(task instanceof JPPFAnnotatedTask);
    assertTrue(task.getTaskObject() instanceof AnnotatedInstanceMethodTask);
    AnnotatedInstanceMethodTask aimt = (AnnotatedInstanceMethodTask) task.getTaskObject();
    assertNotNull(aimt.result);
    assertEquals("task ended for param testParam", aimt.result);
  }

  /**
   * Test a POJO task with a constructor annotated with &#64;{@link JPPFRunnable}.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testAnnotatedConstructor() throws Exception {
    int nbTasks = 1;
    JPPFJob job = new JPPFJob();
    job.setName(ReflectionUtils.getCurrentMethodName());
    job.add(AnnotatedConstructorTask.class, "testParam");
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    Task<?> task = results.get(0);
    Throwable t = task.getThrowable();
    assertNull("task exception: " + ExceptionUtils.getStackTrace(t), t);
    assertTrue(task.getResult() instanceof AnnotatedConstructorTask);
    assertTrue(task instanceof JPPFAnnotatedTask);
    print(false, false, "task object: %s", task.getTaskObject());
    assertTrue(task.getTaskObject() instanceof AnnotatedConstructorTask);
    AnnotatedConstructorTask act = (AnnotatedConstructorTask) task.getTaskObject();
    assertNotNull(act.result);
    assertEquals("task ended for param testParam", act.result);
    assertTrue(act == task.getResult());
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
}
