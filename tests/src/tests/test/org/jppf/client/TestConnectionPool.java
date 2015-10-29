/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JPPFClient</code> using multiple connections to the same server
 * (connection pool size > 1).
 * @author Laurent Cohen
 */
public class TestConnectionPool extends Setup1D1N {
  /**
   * The JPPF client.
   */
  private JPPFClient client = null;

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and local execution disabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleConnections() throws Exception {
    try {
      configure(0);
      createClient();
      int nbTasks = 100;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
      List<Task<?>> results = client.submitJob(job);
      testJobResults(nbTasks, results);
    } finally {
      reset();
    }
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and local execution enabled.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleConnectionsAndLocalExec() throws Exception {
    try {
      configure(2);
      createClient();
      int nbTasks = 100;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
      List<Task<?>> results = client.submitJob(job);
      testJobResults(nbTasks, results);
    } finally {
      reset();
    }
  }

  /**
   * Test job submission with <code>jppf.pool.size = 2</code> and getMachChannels() > 1.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSubmitJobMultipleRemoteChannels() throws Exception {
    try {
      configure(0);
      createClient();
      while (client.getAllConnections().size() < 2) Thread.sleep(10L);
      int nbTasks = 100;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks, LifeCycleTask.class, 0L);
      // default max channels is 1 for backward compatibility with previous versions of the client.
      job.getClientSLA().setMaxChannels(10);
      List<Task<?>> results = client.submitJob(job);
      testJobResults(nbTasks, results);
    } finally {
      reset();
    }
  }

  /**
   * Test a sequence of {@link JPPFConnectionPool#setSize(int)} calls.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 25000)
  public void testSetPoolSizeByAPI() throws Exception {
    long start = System.nanoTime();
    createClient();
    long elapsedSetup = System.nanoTime() - start;
    System.out.printf("test %s: client setup took %s%n", ReflectionUtils.getCurrentClassAndMethod(), StringUtils.toStringDuration(elapsedSetup/1_000_000L));
    try {
      for (int i=1; i<=3; i++) {
        JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
        try {
          pool.setMaxSize(2);
          pool.awaitWorkingConnections(Operator.EQUAL, 2);
          assertEquals(2, pool.getMaxSize());
        } finally {
          pool.setMaxSize(1);
          pool.awaitWorkingConnections(Operator.EQUAL, 1);
          assertEquals(1, pool.getMaxSize());
        }
      }
    } finally {
      reset();
      long elapsed = System.nanoTime() - start;
      System.out.printf("test %s took %s%n", ReflectionUtils.getCurrentClassAndMethod(), StringUtils.toStringDuration(elapsed/1_000_000L));
    }
  }

  /**
   * Create the JPPF client.
   * @throws Exception if any error occurs.
   */
  private void createClient() throws Exception {
    //client = BaseSetup.createClient(null, false);
    (client = new JPPFClient()).awaitWorkingConnectionPool();
  }

  /**
   * Configure the client for a connection pool.
   * @param localThreads a value greater than 0 to enable local execution with this number of threads, 0 or less otherwise.
   */
  private void configure(final int localThreads) {
    TypedProperties config = JPPFConfiguration.getProperties();
    config.setProperty("jppf.load.balancing.algorithm", "proportional");
    config.setProperty("jppf.load.balancing.profile", "test");
    config.setInt("jppf.load.balancing.profile.test.initialSize", 10);
    config.setInt("jppf.pool.size", 2);
    if (localThreads > 0) {
      config.setBoolean("jppf.local.execution.enabled", true);
      config.setInt("jppf.local.execution.threads", localThreads);
    }
  }

  /**
   * Reset the confiugration.
   */
  private void reset() {
    if (client != null) {
      client.close();
      client = null;
    }
    JPPFConfiguration.reset();
  }

  /**
   * Test the results of a job execution.
   * @param nbTasks the expected number of tasks in the results.
   * @param results the results.
   * @throws Exception if any error occurs.
   */
  private void testJobResults(final int nbTasks, final List<Task<?>> results) throws Exception {
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    int count = 0;
    for (Task<?> task : results) {
      String prefix = "task " + count + " ";
      Throwable t = task.getThrowable();
      assertNull(prefix + "has an exception", t);
      assertNotNull(prefix + "result is null", task.getResult());
    }
  }
}
