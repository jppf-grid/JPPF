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

package test.org.jppf.client.event;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.job.JobEventType;
import org.jppf.node.protocol.Task;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.junit.*;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.BaseSetup.Configuration;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>JobListener</code> using multiple connections to the same server
 * (connection pool size > 1).
 * @author Laurent Cohen
 */
public class TestJobListener extends AbstractNonStandardSetup {
  /**
   * The JPPF client.
   */
  private JPPFClient client = null;

  /**
   * Launches 1 driver with 3 nodes and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception {
    Configuration cfg = createConfig(null);
    cfg.driverLog4j = "classes/tests/config/job_reservation/log4j-driver-TestJobListener.properties";
    BaseSetup.setup(1, 1, false, cfg);
  }

  /**
   * Test the <code>JobListener</code> notifications with <code>jppf.pool.size = 1</code>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testJobListenerSingleLocalConnection() throws Exception {
    try {
      configure(false, true, 1);
      CountingJobListener listener = new CountingJobListener();
      int nbTasks = 20;
      List<Task<?>> results = runJob(ReflectionUtils.getCurrentMethodName(), listener, nbTasks);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
      assertEquals(4, listener.dispatchedCount.get());
      assertEquals(4, listener.returnedCount.get());
    } finally {
      reset();
    }
  }

  /**
   * Test the <code>JobListener</code> notifications with <code>jppf.pool.size = 2</code>.
   * @throws Exception if any error occurs
   */
  @Test(timeout=10000)
  public void testJobListenerMultipleRemoteConnections() throws Exception {
    try {
      configure(true, false, 2);
      CountingJobListener listener = new CountingJobListener();
      int nbTasks = 20;
      List<Task<?>> results = runJob(ReflectionUtils.getCurrentMethodName(), listener, nbTasks);
      assertEquals(1, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
      assertEquals(4, listener.dispatchedCount.get());
      assertEquals(4, listener.returnedCount.get());
    } finally {
      reset();
    }
  }

  /**
   * Test that the <code>JobListener</code> receives a jobStarted() notification when a job is requeued.
   * @throws Exception if any error occurs
   */
  @Test(timeout=20000)
  public void testJobListenerNotificationsUponRequeue() throws Exception {
    try {
      configure(true, false, 1);
      client = BaseSetup.createClient(null, false);
      CountingJobListener listener = new CountingJobListener();
      AwaitJobNotificationListener jmxListener = new AwaitJobNotificationListener();
      DriverJobManagementMBean jobManager = BaseSetup.getJobManagementProxy(client);
      jobManager.addNotificationListener(jmxListener, null, null);
      int nbTasks = 1;
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, 3000L);
      job.addJobListener(listener);
      jmxListener.await(JobEventType.JOB_DISPATCHED);
      jobManager.removeNotificationListener(jmxListener);
      client.reset();
      List<Task<?>> results = job.awaitResults();
      assertNotNull(results);
      assertEquals(nbTasks, results.size());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, results.get(0).getResult());
      assertEquals(2, listener.startedCount.get());
      assertEquals(1, listener.endedCount.get());
      assertEquals(2, listener.dispatchedCount.get());
      assertEquals(1, listener.returnedCount.get());
    } finally {
      reset();
    }
  }

  /**
   * submit the job with the specified listener and number of tasks.
   * @param name the name of the job to run.
   * @param listener the listener to use for the test.
   * @param nbTasks the number of tasks
   * @return the execution results.
   * @throws Exception if any error occurs
   */
  private List<Task<?>> runJob(final String name, final CountingJobListener listener, final int nbTasks) throws Exception {
    client = BaseSetup.createClient(null, false);
    JPPFJob job = BaseTestHelper.createJob(name, true, false, nbTasks, LifeCycleTask.class, 0L);
    if (listener != null) job.addJobListener(listener);
    List<Task<?>> results = client.submitJob(job);
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    Thread.sleep(250L);
    return results;
  }

  /**
   * Configure the client for a connection pool.
   * @param remoteEnabled specifies whether remote execution is enabled.
   * @param localEnabled specifies whether local execution is enabled.
   * @param poolSize the size of the connection pool.
   */
  private void configure(final boolean remoteEnabled, final boolean localEnabled, final int poolSize) {
    JPPFConfiguration.getProperties()
    .set(JPPFProperties.REMOTE_EXECUTION_ENABLED, remoteEnabled)
    .set(JPPFProperties.LOCAL_EXECUTION_ENABLED, localEnabled)
    .set(JPPFProperties.LOCAL_EXECUTION_THREADS, 4)
    .set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "manual")
    .set(JPPFProperties.LOAD_BALANCING_PROFILE, "manual")
    .setInt(JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".manual.size", 5)
    .set(JPPFProperties.POOL_SIZE, poolSize);
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
}
