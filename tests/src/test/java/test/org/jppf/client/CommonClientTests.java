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

package test.org.jppf.client;

import static org.junit.Assert.*;
import static test.org.jppf.test.setup.BaseTest.print;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

import test.org.jppf.test.setup.common.*;

/**
 * Common unit tests methods for testing a JPPF client.
 * @author Laurent Cohen
 */
public final class CommonClientTests {
  /**
   * Instantiation not permitted.
   */
  private CommonClientTests() {
  }

  /**
   * Test the submission of a job.
   * @param clientConfig the client configuration to use.
   * @throws Exception if any error occurs.
   */
  public static void testSubmit(final TypedProperties clientConfig) throws Exception {
    try (final JPPFClient client = new JPPFClient(clientConfig)) {
      testSubmit(client);
    }
  }

  /**
   * Test the submission of a job.
   * @param client the client to use.
   * @throws Exception if any error occurs.
   */
  public static void testSubmit(final JPPFClient client) throws Exception {
    print(false, false, "waiting for working connection");
    assertNotNull(client.awaitConnectionPool(5000L, JPPFClientConnectionStatus.workingStatuses()));
    print(false, false, "got working connection");
    final int nbTasks = 50;
    final JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentClassAndMethod(), false, nbTasks, LifeCycleTask.class, 0L);
    int i = 0;
    for (final Task<?> task: job.getJobTasks()) task.setId("" + i++);
    print(false, false, "submitting job");
    final List<Task<?>> results = client.submit(job);
    print(false, false, "got job results");
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    final String msg = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
    for (i=0; i<nbTasks; i++) {
      final Task<?> task = results.get(i);
      final Throwable t = task.getThrowable();
      assertNull("task " + i +" has an exception " + t, t);
      assertEquals("result of task " + i + " should be " + msg + " but is " + task.getResult(), msg, task.getResult());
      assertEquals(job.getJobTasks().get(i).getId(), task.getId());
      assertEquals(i, task.getPosition());
    }
  }

  /**
   * Test the cancellation of a job.
   * @param clientConfig the client configuration to use.
   * @throws Exception if any error occurs
   */
  public static void testCancelJob(final TypedProperties clientConfig) throws Exception {
    try (final JPPFClient client = new JPPFClient(clientConfig)) {
      testCancelJob(client);
    }
  }

  /**
   * Test the cancellation of a job.
   * @param client the client to use.
   * @throws Exception if any error occurs
   */
  public static void testCancelJob(final JPPFClient client) throws Exception {
    final String name = ReflectionUtils.getCurrentMethodName();
    final int nbTasks = 10;
    final AwaitTaskNotificationListener listener = new AwaitTaskNotificationListener(client, "start notif");
    final JPPFJob job = BaseTestHelper.createJob(name + "-1", false, nbTasks, LifeCycleTask.class, 5000L, true, "start notif");
    print(false, false, "submitting job 1");
    client.submitAsync(job);
    print(false, false, "awaiting JOB_DISPATCHED notification");
    listener.await();
    print(false, false, "cancelling job 1");
    client.cancelJob(job.getUuid());
    print(false, false, "awaiting job 1 results");
    List<Task<?>> results = job.awaitResults();
    print(false, false, "got job 1 results");
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    int count = 0;
    for (final Task<?> task: results) {
      if (task.getResult() == null) count++;
    }
    assertTrue(count > 0);
    final JPPFJob job2 = BaseTestHelper.createJob(name + "-2", false, nbTasks, LifeCycleTask.class, 1L);
    print(false, false, "submitting job 2");
    results = client.submit(job2);
    print(false, false, "got job 2 results");
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (final Task<?> task: results) {
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
  }
}
