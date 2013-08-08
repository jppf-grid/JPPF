/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.*;

import javax.management.*;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ReflectionUtils;
import org.junit.*;

import test.org.jppf.test.setup.BaseSetup;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for {@link JPPFNodeAdminMBean}.
 * In this class, we test that the functionality of the JPPFNodeAdminMBean from the client point of view.
 * @author Laurent Cohen
 */
public class TestJPPFNodeTaskMonitorMBean
{
  /**
   * Connection to the node's JMX server.
   */
  private static JMXNodeConnectionWrapper nodeJmx = null;
  /**
   * Connection to the driver's JMX server.
   */
  private static JMXDriverConnectionWrapper driverJmx = null;
  /**
   * 
   */
  private static JPPFNodeTaskMonitorMBean nodeMonitorProxy = null;

  /**
   * The jppf client to use.
   */
  protected static JPPFClient client = null;

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if a process could not be started.
   */
  @BeforeClass
  public static void setup() throws Exception
  {
    client = BaseSetup.setup(1);
    driverJmx = BaseSetup.getDriverManagementProxy(client);
    Collection<JPPFManagementInfo> coll = driverJmx.nodesInformation();
    JPPFManagementInfo info = coll.iterator().next();
    nodeJmx = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
    nodeJmx.connectAndWait(5000L);
    if (!nodeJmx.isConnected())
    {
      nodeJmx = null;
      throw new Exception("could not connect to the node's JMX server");
    }
    nodeMonitorProxy = nodeJmx.getProxy(JPPFNodeTaskMonitorMBean.MBEAN_NAME, JPPFNodeTaskMonitorMBean.class);
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if a process could not be stopped.
   */
  @AfterClass
  public static void cleanup() throws Exception
  {
    if (nodeJmx != null)
    {
      try
      {
        nodeJmx.close();
      }
      finally
      {
        nodeJmx = null;
        nodeMonitorProxy = null;
      }
    }
    BaseSetup.cleanup();
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testSnapshot() throws Exception
  {
    long duration = 100L;
    try
    {
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksSucessfull());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskCpuTime());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskElapsedTime());
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, duration);
      job.addTask(new ErrorLifeCycleTask(duration)).setId(job.getName() + " - task 2");
      List<JPPFTask> result = client.submit(job);
      assertEquals(Integer.valueOf(2), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksSucessfull());
      Long n = nodeMonitorProxy.getTotalTaskCpuTime();
      assertTrue("cpu time is only " + n, n > 0L);
      n = nodeMonitorProxy.getTotalTaskElapsedTime();
      assertTrue("elapsed time is only " + n, n >= duration - 1L);
    }
    finally
    {
      nodeMonitorProxy.reset();
      nodeJmx.resetTaskCounter();
    }
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testReset() throws Exception
  {
    long duration = 100L;
    try
    {
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, 1, LifeCycleTask.class, duration);
      job.addTask(new ErrorLifeCycleTask(duration)).setId(job.getName() + " - task 2");
      List<JPPFTask> result = client.submit(job);
      assertEquals(Integer.valueOf(2), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(1), nodeMonitorProxy.getTotalTasksSucessfull());
      Long n = nodeMonitorProxy.getTotalTaskCpuTime();
      assertTrue("cpu time is only " + n, n > 0L);
      n = nodeMonitorProxy.getTotalTaskElapsedTime();
      assertTrue("elapsed time is only " + n, n >= duration - 1L);
      nodeMonitorProxy.reset();
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksExecuted());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksInError());
      assertEquals(Integer.valueOf(0), nodeMonitorProxy.getTotalTasksSucessfull());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskCpuTime());
      assertEquals(Long.valueOf(0L), nodeMonitorProxy.getTotalTaskElapsedTime());
    }
    finally
    {
      nodeJmx.resetTaskCounter();
    }
  }

  /**
   * Test getting the node state.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=5000)
  public void testNotifications() throws Exception
  {
    long duration = 100L;
    int nbTasks = 5;
    assertTrue(nbTasks > 1);
    NodeNotificationListener listener = new NodeNotificationListener();
    try
    {
      nodeMonitorProxy.addNotificationListener(listener, null, null);
      JPPFJob job = BaseTestHelper.createJob(ReflectionUtils.getCurrentMethodName(), true, false, nbTasks - 1, LifeCycleTask.class, duration);
      job.addTask(new ErrorLifeCycleTask(duration)).setId(job.getName() + " - task " + nbTasks);
      List<JPPFTask> result = client.submit(job);
      assertNull(listener.exception);
      assertEquals(nbTasks, listener.notifs.size());
      for (int i=0; i < nbTasks; i++)
      {
        JPPFTask task = result.get(i);
        TaskInformation ti = listener.notifs.get(i);
        assertEquals(job.getUuid(), ti.getJobId());
        assertEquals(task.getId(), ti.getId());
        Long n = ti.getElapsedTime();
        //assertTrue("task " + i + " elapsed time is only " + n, n >= duration - 1L);
        if (i < nbTasks - 1)
        {
          assertFalse(ti.hasError());
        }
        else
        {
          assertTrue(ti.hasError());
          n = ti.getCpuTime();
          assertTrue("task " + i + " cpu time is only " + n, n > 0L);
        }
      }
      nodeMonitorProxy.removeNotificationListener(listener);
    }
    finally
    {
      nodeMonitorProxy.reset();
      nodeJmx.resetTaskCounter();
    }
  }

  /**
   * 
   */
  public class NodeNotificationListener implements NotificationListener
  {
    /**
     * The task information received as notifications from the node.
     */
    public List<TaskInformation> notifs = new ArrayList<>();
    /**
     * 
     */
    public Exception exception = null;

    @Override
    public void handleNotification(final Notification notification, final Object handback)
    {
      try
      {
        TaskExecutionNotification notif = (TaskExecutionNotification) notification;
        notifs.add(notif.getTaskInformation());
      }
      catch (Exception e)
      {
        if (exception == null) exception = e;
      }
    }
  }

  /**
   * This class throws an {@link Error} in its <code>run()</code> method.
   */
  public static class ErrorLifeCycleTask extends LifeCycleTask
  {
    /**
     * Initialize this task.
     * @param duration specifies the duration of this task.
     */
    public ErrorLifeCycleTask(final long duration)
    {
      super(duration);
    }

    @Override
    public void run()
    {
      long start = System.currentTimeMillis();
      Random rand = new Random(start);
      long elapsed = 0L;
      String s = "";
      while ((elapsed = System.currentTimeMillis() - start) < duration)
      {
        double d = Math.exp(35525.36789d * rand.nextDouble());
        s = String.valueOf(d) + (d < 100d ? " < 100" : " >= 100");
      }
      throw new IllegalStateException("this error is thrown deliberately");
    }
  }
}
