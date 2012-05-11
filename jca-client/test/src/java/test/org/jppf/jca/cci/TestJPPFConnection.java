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

package test.org.jppf.jca.cci;

import static org.junit.Assert.*;
import static org.jppf.client.submission.SubmissionStatus.*;

import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.jca.cci.JPPFConnection;
import org.jppf.server.protocol.JPPFTask;
import org.junit.*;

import test.org.jppf.test.setup.JPPFHelper;
import test.org.jppf.test.setup.jca.*;

/**
 * Unit tests for <code>org.jppf.jca.cci.JPPFConnection</code>.
 * @author Laurent Cohen
 */
public class TestJPPFConnection
{
  /**
   * A coonection to the JPPF connector.
   */
  private JPPFConnection connection = null;

  /**
   * Launches a driver and node and start the client.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupTest() throws Exception
  {
  }

  /**
   * Stops the driver and node and close the client.
   * @throws Exception if any error occurs.
   */
  @After
  public void cleanupTest() throws Exception
  {
  }

  /**
   * Test submitting a simple job and getting the results.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testSubmit() throws Exception
  {
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = JCATestHelper.createJob("JCA job", true, false, 1, LifeCycleTask.class);
      String id = connection.submit(job);
      assertNotNull(id);
      List<JPPFTask> results = connection.waitForResults(id);
      assertNotNull(results);
      int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      JPPFTask task = results.get(0);
      assertNotNull(task);
      assertNull(task.getException());
      assertNotNull(task.getResult());
      assertEquals(task.getResult(), JCATestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting a simple job with a status listener.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testStatusListenerFromSubmit() throws Exception
  {
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = JCATestHelper.createJob("JCA job", true, false, 1, LifeCycleTask.class);
      GatheringStatusListener listener = new GatheringStatusListener();
      String id = connection.submit(job, listener);
      assertNotNull(id);
      List<JPPFTask> results = connection.waitForResults(id);
      assertNotNull(results);
      int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      List<SubmissionStatus> statuses = listener.getStatuses();
      assertNotNull(statuses);
      assertArrayEquals(new SubmissionStatus[] { PENDING, EXECUTING, COMPLETE }, statuses.toArray(new SubmissionStatus[statuses.size()]));
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test test cancelling at job after it is submiited and before its completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testCancelJob() throws Exception
  {
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = JCATestHelper.createJob("JCA job", true, false, 1, LifeCycleTask.class, 5000L);
      String id = connection.submit(job);
      assertNotNull(id);
      Thread.sleep(1000L);
      connection.cancelJob(id);
      List<JPPFTask> results = connection.waitForResults(id);
      assertNotNull(results);
      int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      JPPFTask task = results.get(0);
      assertNotNull(task);
      assertNull(task.getException());
      assertNull(task.getResult());
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }
}
