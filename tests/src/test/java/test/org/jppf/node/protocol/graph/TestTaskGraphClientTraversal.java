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

package test.org.jppf.node.protocol.graph;

import static org.junit.Assert.*;
import static test.org.jppf.test.setup.common.TaskDependenciesHelper.createDiamondTasks;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.junit.Test;

import test.org.jppf.test.setup.Setup1D2N1C;
import test.org.jppf.test.setup.common.*;
import test.org.jppf.test.setup.common.TaskDependenciesHelper.*;

/**
 * Test graphs of tasks with graph traversal on the client side.
 * @author Laurent Cohen
 */
public class TestTaskGraphClientTraversal extends Setup1D2N1C {
  /**
   * Test the submission of a job with tasks dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000L)
  public void testGraphSubmission() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final MyTask[] tasks = createDiamondTasks();
      final JPPFJob job = new JPPFJob();
      job.add(tasks[0]);
      assertTrue(job.hasTaskGraph());
      job.getClientSLA().setGraphTraversalInClient(true);
      final DispatchListener listener = new DispatchListener();
      job.addJobListener(listener);
      final List<Task<?>> result = client.submit(job);
      assertNotNull(result);
      assertEquals(tasks.length, result.size());
      for (final Task<?> task: result) {
        assertTrue(task instanceof MyTask);
        final MyTask myTask = (MyTask) task;
        assertNull(myTask.getThrowable());
        assertNotNull(myTask.getResult());
        assertEquals("executed " + myTask.getId(), myTask.getResult());
      }
      final List<Integer> dispatches = listener.dispatches;
      assertEquals(3, dispatches.size());
      assertEquals(1, (int) dispatches.get(0));
      assertEquals(2, (int) dispatches.get(1));
      assertEquals(1, (int) dispatches.get(2));
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * Test the submission and cancellation of a job with tasks dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 5000L)
  public void testCancelGraphSubmission() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final MyTask[] tasks = createDiamondTasks();
      final Map<String, MyTask> taskMap = new HashMap<>();
      for (final MyTask task: tasks) taskMap.put(task.getId(), task);
      taskMap.get("T1").setDuration(5000L).setStartNotif("start");
      taskMap.get("T2").setDuration(5000L).setStartNotif("start");
      final JPPFJob job = new JPPFJob();
      job.add(tasks[0]);
      assertTrue(job.hasTaskGraph());
      job.getClientSLA().setGraphTraversalInClient(true);
      final DispatchListener listener = new DispatchListener();
      job.addJobListener(listener);
      final AwaitTaskNotificationListener notifListener = new AwaitTaskNotificationListener(client, "start");
      BaseTestHelper.printToAll(client, false, "submitting job");
      client.submitAsync(job);
      notifListener.await();
      BaseTestHelper.printToAll(client, false, "cancelling job");
      job.cancel();
      BaseTestHelper.printToAll(client, false, "awaiting job results");
      final List<Task<?>> result = job.awaitResults();
      BaseTestHelper.printToAll(client, false, "got job results");
      assertNotNull(result);
      assertEquals(tasks.length, result.size());
      for (final Task<?> task: result) {
        print(false, false, "checking %s", task);
        assertTrue(task instanceof MyTask);
        final MyTask myTask = (MyTask) task;
        assertNull(myTask.getThrowable());
        if ("T3".equals(myTask.getId())) {
          assertNotNull(myTask.getResult());
          assertEquals("executed " + myTask.getId(), myTask.getResult());
        } else assertNull(myTask.getResult());
      }
      final List<Integer> dispatches = listener.dispatches;
      assertEquals(2, dispatches.size());
      assertEquals(1, (int) dispatches.get(0));
      assertEquals(2, (int) dispatches.get(1));
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
    }
  }

  /**
   * Test that a task with dependencies can reuse the results of its dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testSimpleResultDependency() throws Exception {
    TaskDependenciesHelper.testResultDependency(client, true);
  }
}
