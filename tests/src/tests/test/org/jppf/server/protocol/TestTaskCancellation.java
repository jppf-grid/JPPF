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

import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.client.*;
import org.jppf.client.taskwrapper.*;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.BaseTest;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link Task} canellation.
 * In this class, we test that the behavior is the expected one, when a task is cancelled.
 * @author Laurent Cohen
 */
public class TestTaskCancellation extends BaseTest {
  /**
   * A "short" duration for this test.
   */
  private static final long TIME_SHORT = 1500L;
  /**
   * The result for a tzsk that completes successfully.
   */
  private static final String SUCCESS = "success";
  /**
   * Used to test JPPFTask.compute(JPPFCallable) in method {@link #testComputeCallable()}.
   */
  static String callableResult = "";
  /**
   * The JPPF client to use.
   */
  private static JPPFClient client;
  /** */
  private final ExecutionPolicy localPolicy = new Equal("jppf.channel.local", true);

  /**
   * Setup the configuration and create the client.
   * @throws Exception if any error occurs.
   */
  @BeforeClass
  public static void config() throws Exception {
    JPPFConfiguration.set(JPPFProperties.REMOTE_EXECUTION_ENABLED, false).set(JPPFProperties.LOCAL_EXECUTION_ENABLED, true);
    client = new JPPFClient();
  }

  /**
   * Setup the configuration and create the client.
   * @throws Exception if any error occurs.
   */
  @AfterClass
  public static void cleanup() throws Exception {
    if (client != null) client.close();
  }

  /**
   * Test the thread of a task with its interruptible flag set to {@code true} is effectively interrupted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testInterruptibleJPPFTask() throws Exception {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, MyTask.class, TIME_SHORT, true);
    job.getClientSLA().setExecutionPolicy(localPolicy);
    client.submitJob(job);
    Thread.sleep(TIME_SHORT / 2L);
    job.cancel();
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    MyTask task = (MyTask) results.get(0);
    assertNotNull(task);
    assertTrue(task.getElapsedOnCancel() >= 0L);
    assertTrue(task.getElapsedOnCancel() < TIME_SHORT * 1_000_000L);
    assertTrue(task.getElapsedDoCancelAction() >= 0L);
    assertTrue(task.getElapsedDoCancelAction() <= task.getElapsedOnCancel());
    assertNull(task.getResult());
    assertNull(task.getThrowable());
  }

  /**
   * Test the thread of a task with its interruptible flag set to {@code false} is effectively not interrupted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testUninterruptibleJPPFTask() throws Exception {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, MyTask.class, TIME_SHORT, false);
    job.getClientSLA().setExecutionPolicy(localPolicy);
    client.submitJob(job);
    Thread.sleep(TIME_SHORT / 2L);
    job.cancel();
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    MyTask task = (MyTask) results.get(0);
    assertNotNull(task);
    assertTrue(task.getElapsedOnCancel() >= 0L);
    assertTrue(task.getElapsedOnCancel() >= TIME_SHORT * 1_000_000L);
    assertTrue(task.getElapsedDoCancelAction() >= 0L);
    assertTrue(task.getElapsedDoCancelAction() <= task.getElapsedOnCancel());
    assertNotNull(task.getResult());
    assertEquals(SUCCESS, task.getResult());
    assertNull(task.getThrowable());
  }

  /**
   * Test the thread of a callable task with its interruptible flag set to {@code true} is effectively interrupted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testInterruptibleCallable() throws Exception {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, MyCallable.class, TIME_SHORT, true);
    for (Task<?> task: job) ((JPPFAnnotatedTask) task).setCancelCallback(new MyCancelCallback());
    job.getClientSLA().setExecutionPolicy(localPolicy);
    client.submitJob(job);
    Thread.sleep(TIME_SHORT / 2L);
    job.cancel();
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    Task<?> task = results.get(0);
    MyCallable callable = (MyCallable) task.getTaskObject();
    assertNotNull(callable);
    assertTrue(callable.getElapsedOnCancel() >= 0L);
    assertTrue(callable.getElapsedOnCancel() < TIME_SHORT * 1_000_000L);
    assertTrue(callable.getElapsedDoCancelAction() >= 0L);
    assertTrue(callable.getElapsedDoCancelAction() <= callable.getElapsedOnCancel());
    assertNull(task.getResult());
    assertNull(task.getThrowable());
  }

  /**
   * Test the thread of a callable task with its interruptible flag set to {@code false} is effectively not interrupted.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000)
  public void testUninterruptibleCallable() throws Exception {
    int nbTasks = 1;
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, false, nbTasks, MyCallable.class, TIME_SHORT, false);
    for (Task<?> task: job) ((JPPFAnnotatedTask) task).setCancelCallback(new MyCancelCallback());
    job.getClientSLA().setExecutionPolicy(localPolicy);
    client.submitJob(job);
    Thread.sleep(TIME_SHORT / 2L);
    job.cancel();
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), nbTasks);
    Task<?> task = results.get(0);
    MyCallable callable = (MyCallable) task.getTaskObject();
    assertNotNull(callable);
    assertTrue(callable.getElapsedOnCancel() >= 0L);
    assertTrue(callable.getElapsedOnCancel() >= TIME_SHORT * 1_000_000L);
    assertTrue(callable.getElapsedDoCancelAction() >= 0L);
    assertTrue(callable.getElapsedDoCancelAction() <= callable.getElapsedOnCancel());
    assertNotNull(task.getResult());
    assertEquals(SUCCESS, task.getResult());
    assertNull(task.getThrowable());
  }

  /** */
  public static class MyTask extends AbstractTask<String> implements CancellationHandler {
    /** */
    protected long start, duration, elapsedOnCancel=-1L, elapsedDoCancelAction=-1L;
    /** */
    protected final boolean interruptible;
    /** */
    private final ThreadSynchronization lock = new ThreadSynchronization();

    /**
     * @param duration .
     * @param interruptible .
     */
    public MyTask(final long duration, final boolean interruptible) {
      this.duration = duration;
      this.interruptible = interruptible;
    }

    @Override
    public void run() {
      start = System.nanoTime();
      try {
        synchronized(lock) {
          lock.wait(duration + 16L);
        }
        setResult(SUCCESS);
      } catch (Exception e) {
        setThrowable(e);
      }
    }

    @Override
    public boolean isInterruptible() {
      return interruptible;
    }

    @Override
    public void onCancel() {
      elapsedOnCancel = System.nanoTime() - start;
      System.out.printf("onCancel() : elapsed = %,d ns%n", elapsedOnCancel);
    }

    @Override
    public void doCancelAction() throws Exception {
      elapsedDoCancelAction = System.nanoTime() - start;
      System.out.printf("doCancelAction() : elapsed = %,d ns%n", elapsedDoCancelAction);
    }

    /**
     * @return .
     */
    public long getElapsedOnCancel() {
      return elapsedOnCancel;
    }

    /**
     * @return .
     */
    public long getElapsedDoCancelAction() {
      return elapsedDoCancelAction;
    }
  }


  /** */
  public static class MyCallable implements Callable<String>, Interruptibility, CancellationHandler {
    /** */
    protected long start, duration, elapsedOnCancel=-1L, elapsedDoCancelAction=-1L;
    /** */
    protected final boolean interruptible;
    /** */
    private final ThreadSynchronization lock = new ThreadSynchronization();

    /**
     * @param duration .
     * @param interruptible .
     */
    public MyCallable(final long duration, final boolean interruptible) {
      this.duration = duration;
      this.interruptible = interruptible;
    }

    @Override
    public String call() throws Exception {
      start = System.nanoTime();
      synchronized(lock) {
        lock.wait(duration + 16L);
      }
      return SUCCESS;
    }

    @Override
    public boolean isInterruptible() {
      return interruptible;
    }

    
    /** */
    public void onCancel() {
      elapsedOnCancel = System.nanoTime() - start;
      System.out.printf("onCancel() : elapsed = %,d ns%n", elapsedOnCancel);
    }

    @Override
    public void doCancelAction() throws Exception {
      elapsedDoCancelAction = System.nanoTime() - start;
      System.out.printf("doCancelAction() : elapsed = %,d ns%n", elapsedDoCancelAction);
    }

    /**
     * @return .
     */
    public long getElapsedOnCancel() {
      return elapsedOnCancel;
    }

    /**
     * @return .
     */
    public long getElapsedDoCancelAction() {
      return elapsedDoCancelAction;
    }
  }

  /** */
  public static class MyCancelCallback extends JPPFTaskCallback<Object> {
    @Override
    public void run() {
      MyCallable callable = (MyCallable) getTask().getTaskObject();
      callable.onCancel();
    }
  }
}
