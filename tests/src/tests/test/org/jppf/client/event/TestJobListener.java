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

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingNotification;
import org.jppf.node.protocol.Task;
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
public class TestJobListener extends BaseTest {
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
    Configuration cfg = BaseSetup.DEFAULT_CONFIG.copy();
    cfg.driverLog4j = "classes/tests/config/log4j-driver.TestJobListener.properties";
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
  @Test(timeout = 10000)
  public void testJobListenerNotificationsUponRequeue() throws Exception {
    try {
      String name = ReflectionUtils.getCurrentMethodName();
      configure(true, false, 1);
      client = BaseSetup.createClient(null, false);
      BaseTestHelper.printToServers(client, "start of %s()", name);
      CountingJobListener listener = new CountingJobListener();
      String startNotification = "start notification";
      MyTaskListener taskListener = new MyTaskListener(startNotification);
      JMXDriverConnectionWrapper jmx = BaseSetup.getJMXConnection(client);
      String listenerId = jmx.registerForwardingNotificationListener(NodeSelector.ALL_NODES, JPPFNodeTaskMonitorMBean.MBEAN_NAME, taskListener, null, null);
      int nbTasks = 1;
      JPPFJob job = BaseTestHelper.createJob(name, false, false, nbTasks, LifeCycleTask.class, 3000L, true, startNotification);
      job.addJobListener(listener);
      client.submitJob(job);
      taskListener.await();
      jmx.unregisterForwardingNotificationListener(listenerId);
      client.reset();
      //client = BaseSetup.createClient(null, false);
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
    BaseTestHelper.printToServers(client, "start of %s()", name);
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
    JPPFConfiguration.getProperties().set(JPPFProperties.REMOTE_EXECUTION_ENABLED, remoteEnabled).set(JPPFProperties.LOCAL_EXECUTION_ENABLED, localEnabled)
    .set(JPPFProperties.LOCAL_EXECUTION_THREADS, 4).set(JPPFProperties.LOAD_BALANCING_ALGORITHM, "manual").set(JPPFProperties.LOAD_BALANCING_PROFILE, "manual")
    .setInt(JPPFProperties.LOAD_BALANCING_PROFILE.getName() + ".manual.size", 5).set(JPPFProperties.POOL_SIZE, poolSize);
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
   *
   */
  public static class MyTaskListener implements NotificationListener {
    /**
     * A message we expect to receive as a notification.
     */
    private final String expectedMessage;

    /**
     * Intiialize with an expected message.
     * @param expectedMessage a message we expect to receive as a notification.
     */
    public MyTaskListener(final String expectedMessage) {
      this.expectedMessage = expectedMessage;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      JPPFNodeForwardingNotification wrapping = (JPPFNodeForwardingNotification) notification;
      TaskExecutionNotification actualNotif = (TaskExecutionNotification) wrapping.getNotification();
      Object data = actualNotif.getUserData();
      if (expectedMessage.equals(data)) {
        synchronized(this) {
          notifyAll();
        }
      }
    }

    /**
     * Wait for the epxected message to be received.
     */
    public synchronized void await() {
      try {
        wait();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
