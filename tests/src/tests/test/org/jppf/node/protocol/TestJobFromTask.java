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

package test.org.jppf.node.protocol;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D2N1C;
import test.org.jppf.test.setup.common.*;

/**
 * Test that a job can be submitted from a JPPF task.
 * @author Laurent Cohen
 */
public class TestJobFromTask extends Setup1D2N1C {
  /**
   * Submit a job with a task that submits another job while executing in a node. 
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testJobFromTask() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, MyTask1.class, 0L, 0L);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final MyTask1 task = (MyTask1) results.get(0);
    assertTrue(task.isCompleted());
    final Throwable t = task.getThrowable();
    if (t != null) {
      print(false, false, "task raised an exception: %s", ExceptionUtils.getStackTrace(t));
      fail("task raised an exception: " + t);
    }
    assertNotNull(task.getResult());
    assertEquals(MyTask2.HELLO_WORLD, task.getResult());
  }

  /**
   * Submit a job with a task that submits another job and cancels it. 
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testCancelSecondaryJob() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, MyTask1.class, 5000L, 1000L);
    final List<Task<?>> results = client.submit(job);
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final MyTask1 task = (MyTask1) results.get(0);
    assertTrue(task.isCompleted());
    final Throwable t = task.getThrowable();
    if (t != null) {
      print(false, false, "task raised an exception: %s", ExceptionUtils.getStackTrace(t));
      fail("task raised an exception: " + t);
    }
    assertNull(task.getResult());
  }

  /**
   * Submit a job with a task that submits another job,  and cancel it. 
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testCancelPrimaryJob() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, MyTask1.class, 5000L, 0L);
    client.submitAsync(job);
    Thread.sleep(1000L);
    job.cancel();
    final List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), 1);
    final MyTask1 task = (MyTask1) results.get(0);
    assertFalse(task.isCompleted());
    final Throwable t = task.getThrowable();
    if (t != null) {
      print(false, false, "task raised an exception: %s", ExceptionUtils.getStackTrace(t));
      fail("task raised an exception: " + t);
    }
    assertNull(task.getResult());
  }

  /**
   * Submit a job with a task that submits another job, then competes wihtout processing the second job's results. 
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testSubmitAndLeave() throws Exception {
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, 1, MyTask1.class, 1000L, 0L, true);
    final AwaitTaskNotificationListener listener = new AwaitTaskNotificationListener(client, MyTask2.END_NOTIF);
    print(false, false, "submitting job %s", job.getName());
    client.submitAsync(job);
    print(false, false, "awaiting '%s' notification", MyTask2.END_NOTIF);
    assertTrue(listener.await(5000L));
    final List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(results.size(), 1);
    print(false, false, "got job results");
    final MyTask1 task = (MyTask1) results.get(0);
    assertTrue(task.isCompleted());
    final Throwable t = task.getThrowable();
    if (t != null) {
      print(false, false, "task raised an exception: %s", ExceptionUtils.getStackTrace(t));
      fail("task raised an exception: " + t);
    }
    assertNull(task.getResult());
  }
}
