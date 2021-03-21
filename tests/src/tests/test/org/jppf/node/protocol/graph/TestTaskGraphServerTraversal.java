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
import static test.org.jppf.test.setup.common.TaskDependenciesHelper.*;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ConcurrentUtils;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestTaskGraphServerTraversal extends Setup1D2N1C {
  /**
   * Test the submission of a job with tasks dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testGraphSubmission() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final int layers = 3, tasksPerLayer = 10, nbTasks = layers * tasksPerLayer;
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final ServerDispatchListener serverListener = new ServerDispatchListener();
    jmx.getJobManager().addNotificationListener(serverListener, null, null);
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final MyTask[] tasks = createLayeredTasks(layers, tasksPerLayer);
      final JPPFJob job = new JPPFJob();
      for (int i=0; i<tasksPerLayer; i++) job.add(tasks[i]);
      assertEquals(nbTasks, job.unexecutedTaskCount());
      assertTrue(job.hasTaskGraph());
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
      assertEquals(1, dispatches.size());
      assertEquals(nbTasks, (int) dispatches.get(0));

      assertTrue(ConcurrentUtils.awaitCondition(() -> serverListener.getNbDispatches() == 6, 5000L, 250L, true));
      final List<Integer> serverDispatches = serverListener.dispatches;
      for (int i=0; i<6; i++) assertEquals(5, (int) serverDispatches.get(i));
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
      jmx.getJobManager().removeNotificationListener(serverListener);
    }
  }

  /**
   * Test the submission and cancellation of a job with tasks dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testCancelGraphSubmission() throws Exception {
    int oldMaxJobs = 1;
    JPPFConnectionPool pool = null;
    final JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
    final ServerDispatchListener serverListener = new ServerDispatchListener();
    jmx.getJobManager().addNotificationListener(serverListener, null, null);
    try {
      pool = client.awaitWorkingConnectionPool();
      oldMaxJobs = pool.getMaxJobs();
      pool.setMaxJobs(Integer.MAX_VALUE);
      final MyTask[] tasks = createDiamondTasks();
      final Map<String, MyTask> taskMap = new HashMap<>();
      for (final MyTask task: tasks) taskMap.put(task.getId(), task);
      taskMap.get("T1").setDuration(5000L).setStartNotif("start");
      taskMap.get("T2").setDuration(5000L).setStartNotif("start");
      final JPPFJob job = new JPPFJob().setName(ReflectionUtils.getCurrentMethodName());
      job.add(tasks[0]);
      assertTrue(job.hasTaskGraph());
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
      assertEquals(1, dispatches.size());
      assertEquals(4, (int) dispatches.get(0));

      assertTrue(ConcurrentUtils.awaitCondition(() -> serverListener.getNbDispatches() > 1, 5000L, 250L, true));
      final List<Integer> serverDispatches = serverListener.dispatches;
      assertEquals(2, serverDispatches.size());
      assertEquals(1, (int) serverDispatches.get(0));
      assertEquals(2, (int) serverDispatches.get(1));
    } finally {
      if (pool != null) pool.setMaxJobs(oldMaxJobs);
      jmx.getJobManager().removeNotificationListener(serverListener);
    }
  }

  /**
   * Test that a task with dependencies can reuse the results of its dependencies.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10_000L)
  public void testSimpleResultDependency() throws Exception {
    TaskDependenciesHelper.testResultDependency(client, false);
  }
}
