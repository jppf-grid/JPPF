/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package test.org.jppf.client.concurrent;

import static org.junit.Assert.*;

import java.io.*;
import java.util.concurrent.*;

import org.jppf.client.concurrent.*;
import org.jppf.client.persistence.DefaultFilePersistenceManager;
import org.jppf.client.taskwrapper.JPPFTaskCallback;
import org.jppf.scheduling.JPPFSchedule;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * @author Laurent Cohen
 */
public class TestExecutorServiceConfiguration extends Setup1D1N1C
{
  /**
   * Default duration for tasks that use a duration. Adjust the value for slow hardware.
   */
  protected static final long TASK_DURATION = 10000L;
  /**
   * Message set as task result when the task is cancelled.
   */
  protected static final String CANCELLED_MESSAGE = "this task has been cancelled";
  /**
   * Message set as task result when the task is cancelled.
   */
  protected static final String TIMEOUT_MESSAGE = "this task has timed out";
  /**
   * The executor we are testing.
   */
  private JPPFExecutorService executor = null;

  /**
   * Launches a driver and node and start the client.
   * @throws IOException if a process could not be started.
   */
  @Before
  public void setupTest() throws IOException
  {
    executor = new JPPFExecutorService(client);
  }

  /**
   * Stops the driver and node and close the client.
   * @throws IOException if a process could not be stopped.
   */
  @After
  public void cleanupTest() throws IOException
  {
    if ((executor != null) && !executor.isShutdown()) executor.shutdownNow();
  }

  /**
   * Submit a Callable task with a timeout.
   * @throws Exception if any error occurs
   */
  @Test
  public void testSubmitCallableWithTimeout() throws Exception
  {
    client.setLocalExecutionEnabled(false);
    executor.getConfiguration().getTaskConfiguration().setOnTimeoutCallback(new MyTaskCallback(TIMEOUT_MESSAGE));
    executor.getConfiguration().getTaskConfiguration().setTimeoutSchedule(new JPPFSchedule(3000L));
    Callable<String> task = new MyCallableTask(TASK_DURATION);
    Future<String> future = executor.submit(task);
    String s = future.get();
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertNotNull(s);
    assertEquals(TIMEOUT_MESSAGE, s);
  }

  /**
   * Submit a Callable task with a timeout.
   * @throws Exception if any error occurs
   */
  @Test
  public void testSubmitCallableWithJobTimeout() throws Exception
  {
    client.setLocalExecutionEnabled(false);
    executor.getConfiguration().getJobConfiguration().getSLA().setJobExpirationSchedule(new JPPFSchedule(3000L));
    executor.getConfiguration().getTaskConfiguration().setOnCancelCallback(new MyTaskCallback(CANCELLED_MESSAGE));
    Callable<String> task = new MyCallableTask(TASK_DURATION);
    Future<String> future = executor.submit(task);
    String s = future.get();
    assertTrue(future.isDone());
    assertFalse(future.isCancelled());
    assertNull(s);
    //assertEquals(CANCELLED_MESSAGE, s);
  }

  /**
   * Submit a Callable task with a timeout.
   * @throws Exception if any error occurs
   */
  @Test
  public void testResetConfiguration() throws Exception
  {
    client.setLocalExecutionEnabled(false);
    ExecutorServiceConfiguration config = executor.getConfiguration();
    config.getTaskConfiguration().setOnCancelCallback(new MyTaskCallback(CANCELLED_MESSAGE));
    config.getTaskConfiguration().setOnTimeoutCallback(new MyTaskCallback(TIMEOUT_MESSAGE));
    config.getTaskConfiguration().setTimeoutSchedule(new JPPFSchedule(3000L));
    config.getJobConfiguration().getSLA().setJobExpirationSchedule(new JPPFSchedule(3000L));
    config.getJobConfiguration().getMetadata().setParameter("job.metadata", "some value");
    config.getJobConfiguration().setPersistenceManager(new DefaultFilePersistenceManager("root"));
    config = executor.resetConfiguration();
    assertNotNull(config.getTaskConfiguration());
    assertNull(config.getTaskConfiguration().getOnCancelCallback());
    assertNull(config.getTaskConfiguration().getOnTimeoutCallback());
    assertNull(config.getTaskConfiguration().getTimeoutSchedule());
    assertNotNull(config.getJobConfiguration());
    assertNotNull(config.getJobConfiguration().getSLA());
    assertNull(config.getJobConfiguration().getSLA().getJobExpirationSchedule());
    assertNotNull(config.getJobConfiguration().getMetadata());
    assertNull(config.getJobConfiguration().getMetadata().getParameter("job.metadata"));
    assertNull(config.getJobConfiguration().getPersistenceManager());
  }

  /**
   * A callback used in lieu of JPPFTask.onCancel() and JPPFTask.onTimeout().
   */
  private static class MyTaskCallback extends JPPFTaskCallback
  {
    /**
     * A message that will be set as the task's result.
     */
    private String message = null;

    /**
     * Initialize this callback with a message that will be set as the task's result and printed to the node console.
     * @param message a message that will be set as the task's result.
     */
    public MyTaskCallback(final String message)
    {
      this.message = message;
    }

    @Override
    public void run()
    {
      getTask().setResult(message);
      System.out.println(message);
    }
  }

  /**
   * 
   */
  private static class MyCallableTask implements Callable<String>, Serializable
  {
    /**
     * the duration of this task.
     */
    private long duration = 1L;

    /**
     * Initialize this task with the specified duration.
     * @param duration the duration of this task.
     */
    public MyCallableTask(final long duration)
    {
      this.duration = duration;
    }

    @Override
    public String call() throws Exception
    {
      Thread.sleep(duration);
      System.out.println("task executed");
      return BaseSetup.EXECUTION_SUCCESSFUL_MESSAGE;
    }
  }
}
