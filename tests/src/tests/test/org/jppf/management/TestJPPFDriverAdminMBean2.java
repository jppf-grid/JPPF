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

package test.org.jppf.management;

import static org.junit.Assert.*;

import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.JobEvent;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ReflectionUtils;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFDriverAdminMBean}.
 * In this class, we test that the functionality of the DriverJobManagementMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFDriverAdminMBean2 extends Setup1D1N1C {
  /**
   * Test restarting the driver via JMX when the client is idle (not executing any job).
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testRestartDriverWhenIdle() throws Exception {
    int nbTasks = 1;
    long duration = 1L;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    print(false, false, "submitting job 1");
    List<Task<?>> results = client.submitJob(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-1", true, false, nbTasks, LifeCycleTask.class, duration));
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results) {
      assertNotNull(task);
      assertNotNull(task.getResult());
      assertNull(task.getThrowable());
    }
    print(false, false, "restarting driver");
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          driver.restartShutdown(1L, 1000L);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
    print(false, false, "waiting for 0 connection");
    while (!client.findConnectionPools(JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING).isEmpty()) Thread.sleep(10L);
    print(false, false, "waiting for 1 connection");
    while (client.awaitWorkingConnectionPool() == null) Thread.sleep(10L);
    print(false, false, "submitting job 2");
    results = client.submitJob(BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName() + "-2", true, false, nbTasks, LifeCycleTask.class, duration));
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results) {
      assertNotNull(task);
      assertNotNull(task.getResult());
      assertNull(task.getThrowable());
    }
  }

  /**
   * Test restarting the driver via JMX while the client is executing a job.
   * @throws Exception if any error occurs.
   */
  @Test(timeout = 10000)
  public void testRestartDriverWhenBusy() throws Exception {
    int nbTasks = 1;
    long duration = 1000L;
    final JMXDriverConnectionWrapper driver = BaseSetup.getJMXConnection(client);
    print(false, false, "submitting job");
    JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), false, false, nbTasks, LifeCycleTask.class, duration);
    AwaitJobListener listener = new AwaitJobListener(job, JobEvent.Type.JOB_DISPATCH);
    client.submitJob(job);
    print(false, false, "waiting for JOB_DISPATCH notification");
    listener.await();
    print(false, false, "restarting driver");
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          driver.restartShutdown(1L, 250L);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }).start();
    Thread.sleep(100L);
    print(false, false, "getting job results");
    List<Task<?>> results = job.awaitResults();
    assertNotNull(results);
    assertEquals(nbTasks, results.size());
    for (Task<?> task: results) {
      assertNotNull(task);
      assertNotNull(task.getResult());
      assertNull(task.getThrowable());
    }
  }
}
