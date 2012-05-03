/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.io.IOException;
import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.junit.*;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for <code>JPPFClient</code>.
 * @author Laurent Cohen
 */
public class TestJPPFClient extends Setup1D1N
{
  /**
   * Launches a driver and node and start the client.
   * @throws IOException if a process could not be started.
   */
  @Before
  public void setupTest() throws IOException
  {
  }

  /**
   * Stops the driver and node and close the client.
   * @throws IOException if a process could not be stopped.
   */
  @After
  public void cleanupTest() throws IOException
  {
  }

  /**
   * Invocation of the <code>JPPFClient()</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test
  public void testDefaultConstructor() throws Exception
  {
    JPPFClient client = BaseSetup.createClient(null);
    client.close();
  }

  /**
   * Invocation of the <code>JPPFClient(String uuid)</code> constructor.
   * @throws Exception if any error occurs
   */
  @Test
  public void testConstructorWithUuid() throws Exception
  {
    JPPFClient client = BaseSetup.createClient("some_uuid");
    client.close();
  }

  /**
   * Test the submission of a job.
   * @throws Exception if any error occurs
   */
  @Test
  public void testSubmit() throws Exception
  {
    JPPFClient client = BaseSetup.createClient(null);
    int nbTasks = 10;
    JPPFJob job = BaseSetup.createJob("TestSubmit", true, false, nbTasks, MyTask.class, 0L);
    int i = 0;
    for (JPPFTask task: job.getTasks()) task.setId("" + i++);
    List<JPPFTask> results = client.submit(job);
    assertNotNull(results);
    assertTrue("results size should be " + nbTasks + " but is " + results.size(), results.size() == nbTasks);
    for (i=0; i<nbTasks; i++)
    {
      JPPFTask t = results.get(i);
      Exception e = t.getException();
      assertNull("task " + i +" has an exception " + e, e);
      String s = "success: " + i;
      assertEquals("result of task " + i + " should be " + s + " but is " + t.getResult(), s, t.getResult());
    }
    client.close();
  }

  /**
   * Test the cancellation of a job.
   * @throws Exception if any error occurs
   */
  @Test
  public void testCancelJob() throws Exception
  {
    JPPFClient client = BaseSetup.createClient(null);
    int nbTasks = 10;
    JPPFJob job = BaseSetup.createJob("TestJPPFClientCancelJob", false, false, nbTasks, MyTask.class, 5000L);
    JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
    int i = 0;
    for (JPPFTask task: job.getTasks()) task.setId("" + i++);
    client.submit(job);
    Thread.sleep(1500L);
    client.cancelJob(job.getUuid());
    List<JPPFTask> results = collector.waitForResults();
    assertNotNull(results);
    assertTrue("results size should be " + nbTasks + " but is " + results.size(), results.size() == nbTasks);
    int count = 0;
    for (JPPFTask t: results)
    {
      if (t.getResult() == null) count++;
    }
    assertTrue(count > 0);
    client.close();
  }

  /**
   * A simple JPPF task for unit-testing.
   */
  public static class MyTask extends JPPFTask
  {
    /**
     * The duration of this task;
     */
    private long duration = 0L;

    /**
     * Initialize this task.
     * @param duration specifies the duration of this task.
     */
    public MyTask(final long duration)
    {
      this.duration = duration;
    }

    @Override
    public void run()
    {
      try
      {
        if (duration > 0) Thread.sleep(duration);
        setResult("success: " + getId());
      }
      catch(Exception e)
      {
        setException(e);
      }
    }
  }
}
