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

import java.util.concurrent.*;

import org.jppf.client.concurrent.*;
import org.junit.*;

import test.org.jppf.test.setup.Setup1D1N1C;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFExecutorService}.
 * @author Laurent Cohen
 */
public class TestJPPFCompletionService extends Setup1D1N1C
{
  /**
   * Default duration for tasks that use a duration. Adjust the value for slow hardware.
   */
  protected static final long TASK_DURATION = 100L;
  /**
   * The executor we are testing.
   */
  private JPPFExecutorService executor = null;

  /**
   * Instantiates a {@link JPPFExecutorService}.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupTest() throws Exception
  {
    executor = new JPPFExecutorService(client);
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if any error occurs.
   */
  @After
  public void cleanupTest() throws Exception
  {
    if ((executor != null) && !executor.isShutdown()) executor.shutdownNow();
  }

  /**
   * Test the invocation of <code>JPPFCompletionService.submit(Callable)</code>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSubmitCallables() throws Exception
  {
    int nbTasks = 5;
    executor.setBatchSize(3);
    executor.setBatchTimeout(100L);
    CompletionService cs = new JPPFCompletionService(executor);
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> f = cs.submit(new SimpleCallable(i, TASK_DURATION));
      assertNotNull(f);
      f = executor.submit(new SimpleCallable(100+i, TASK_DURATION));
      assertNotNull(f);
    }
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> future = cs.take();
      assertNotNull(future);
      assertTrue(future.isDone());
      assertFalse(future.isCancelled());
      TaskResult result = future.get();
      assertNotNull(result);
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, result.message);
      assertTrue(result.position < nbTasks);
    }
    executor.shutdown();
  }

  /**
   * Test the invocation of <code>JPPFCompletionService.submit(Runnable, T result)</code>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testSubmitRunnables() throws Exception
  {
    int nbTasks = 5;
    executor.setBatchSize(3);
    executor.setBatchTimeout(100L);
    CompletionService cs = new JPPFCompletionService(executor);
    for (int i=0; i<nbTasks; i++)
    {
      TaskResult result = new TaskResult();
      Future<TaskResult> f = cs.submit(new SimpleRunnable(i, result), result);
      assertNotNull(f);
      f = executor.submit(new SimpleCallable(100+i, TASK_DURATION));
      assertNotNull(f);
    }
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> future = cs.take();
      assertNotNull(future);
      assertTrue(future.isDone());
      assertFalse(future.isCancelled());
      TaskResult result = future.get();
      assertNotNull(result);
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, result.message);
      assertTrue(result.position < nbTasks);
    }
    executor.shutdown();
  }

  /**
   * Test using <code>JPPFCompletionService.poll()</code> to retrieve results.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testPoll() throws Exception
  {
    int nbTasks = 5;
    executor.setBatchSize(3);
    executor.setBatchTimeout(100L);
    CompletionService<TaskResult> cs = new JPPFCompletionService<TaskResult>(executor);
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> f = cs.submit(new SimpleCallable(i, TASK_DURATION));
      assertNotNull(f);
      f = executor.submit(new SimpleCallable(100+i, TASK_DURATION));
      assertNotNull(f);
    }
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> future = null;
      while ((future = cs.poll()) == null) Thread.sleep(10L);
      assertNotNull(future);
      assertTrue(future.isDone());
      assertFalse(future.isCancelled());
      TaskResult result = future.get();
      assertNotNull(result);
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, result.message);
      assertTrue(result.position < nbTasks);
    }
    assertNull(cs.poll());
    executor.shutdown();
  }

  /**
   * Test using <code>JPPFCompletionService.poll(long, TimeUnit)</code> to retrieve results.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testPollWithTimeout() throws Exception
  {
    int nbTasks = 5;
    executor.setBatchSize(3);
    executor.setBatchTimeout(100L);
    CompletionService cs = new JPPFCompletionService(executor);
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> f = cs.submit(new SimpleCallable(i, TASK_DURATION));
      assertNotNull(f);
      f = executor.submit(new SimpleCallable(100+i, TASK_DURATION));
      assertNotNull(f);
    }
    for (int i=0; i<nbTasks; i++)
    {
      Future<TaskResult> future = null;
      while ((future = cs.poll(10L, TimeUnit.MILLISECONDS)) == null);
      assertNotNull(future);
      assertTrue(future.isDone());
      assertFalse(future.isCancelled());
      TaskResult result = future.get();
      assertNotNull(result);
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, result.message);
      assertTrue(result.position < nbTasks);
    }
    assertNull(cs.poll());
    executor.shutdown();
  }
}
