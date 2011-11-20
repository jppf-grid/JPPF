/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package sample.test.junit;

import java.io.Serializable;

import junit.framework.TestCase;

import org.jppf.client.*;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.server.protocol.JPPFTask;

/**
 * This class tests the remote task management features of JPPF,
 * such as cancelling or restarting a task and receiving notifications.
 * This test assumes a driver is started with the default ports, and a
 * node is started with jmx port = 12001.
 * @author Laurent Cohen
 */
public class TestRemoteTaskManagement extends TestCase implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Cancel task command.
   */
  private static final int CANCEL = 1;
  /**
   * Restart task command.
   */
  private static final int RESTART = 2;
  /**
   * The JPPF client.
   */
  private transient JPPFClient client = null;

  /**
   * Test the cancellation of a task.
   * @throws Exception if the test failed.
   */
  public void testCancelTask() throws Exception
  {
    JPPFTask task = new ManagementTestTask(5000L);
    String id = "test.cancel";
    task.setId(id);
    task = execute(task, CANCEL, id);
    assertNotNull(task);
    assertEquals("cancelled", task.getResult());
  }

  /**
   * Test the restart of a task.
   * @throws Exception if the test failed.
   */
  public void testRestartTask() throws Exception
  {
    JPPFTask task = new ManagementTestTask(5000L);
    String id = "test.restart";
    task.setId(id);
    task = execute(task, RESTART, id);
    assertNotNull(task);
    assertEquals("restarted", task.getResult());
  }

  /**
   * Execute a single JPPF task and return the results.
   * @param task the task to execute.
   * @param command the command to execute, cancel or restart.
   * @param id the id of the task to execute.
   * @return a <code>JPPFTask</code> instance.
   * @throws Exception if the execution failed.
   */
  private JPPFTask execute(final JPPFTask task, final int command, final String id) throws Exception
  {
    JPPFTask result = null;
    client = new JPPFClient();
    JPPFJob job = new JPPFJob();
    job.addTask(task);
    job.setBlocking(false);
    JPPFResultCollector c = new JPPFResultCollector(job);
    job.setResultListener(c);
    client.submit(job);
    Thread.sleep(1000L);
    JMXNodeConnectionWrapper jmxClient = new JMXNodeConnectionWrapper("localhost", 12001);
    jmxClient.connect();
    while (!jmxClient.isConnected()) Thread.sleep(100L);
    switch(command)
    {
      case CANCEL:
        jmxClient.cancelTask(id);
        break;
      case RESTART:
        jmxClient.restartTask(id);
        break;
    }
    result = c.waitForResults().get(0);
    jmxClient.close();
    client.close();
    return result;
  }

  /**
   * Simple task implementation that waits for the time specified in its constructor.
   */
  public static class ManagementTestTask extends JPPFTask
  {
    /**
     * The time in milliseconds for this task to wait before completing normally.
     */
    private long time = 0L;

    /**
     * Initialize this task with the specified time in milliseconds.
     * @param time the time in milliseconds for this task to wait before completing normally.
     */
    public ManagementTestTask(final long time)
    {
      this.time = time;
    }

    /**
     * Execute this task.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      try
      {
        Thread.sleep(time);
      }
      catch(InterruptedException e)
      {
        setException(e);
      }
    }

    /**
     * Called when the task is cancelled.
     * @see org.jppf.server.protocol.JPPFTask#onCancel()
     */
    @Override
    public void onCancel()
    {
      setResult("cancelled");
    }

    /**
     * Called when the task is restarted.
     * @see org.jppf.server.protocol.JPPFTask#onRestart()
     */
    @Override
    public void onRestart()
    {
      setResult("restarted");
    }
  }
}
