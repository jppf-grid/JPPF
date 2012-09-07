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

import static org.jppf.client.submission.SubmissionStatus.*;
import static org.junit.Assert.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.jca.cci.JPPFConnection;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.ExceptionUtils;
import org.junit.Test;

import test.org.jppf.test.setup.*;
import test.org.jppf.test.setup.common.*;

/**
 * Unit tests for <code>org.jppf.jca.cci.JPPFConnection</code>.
 * @author Laurent Cohen
 */
public class TestJPPFConnection
{
  /**
   * A connection to the JPPF connector.
   */
  private JPPFConnection connection = null;

  /**
   * Test submitting a simple job and getting the results.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testSubmit() throws Exception
  {
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = BaseSetup.createJob("JCA job", true, false, 1, LifeCycleTask.class);
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
      assertEquals(task.getResult(), BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting a job and retrieving the results after closing the JCA connection and getting a new one.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testSubmitAndRetrieve() throws Exception
  {
    JPPFConnection connection = null;
    int nbTasks = 10;
    String id = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = BaseSetup.createJob("JCA job", true, false, nbTasks, LifeCycleTask.class, 500L);
      id = connection.submit(job);
      assertNotNull(id);
    }
    finally
    {
      if (connection != null) connection.close();
    }
    Thread.sleep(1000L);
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      List<JPPFTask> results = connection.waitForResults(id);
      assertNotNull(results);
      int n = results.size();
      assertEquals("results size should be " + nbTasks + " but is " + n, nbTasks, n);
      int count = 0;
      for (JPPFTask task: results)
      {
        assertNotNull("task" + count + " is null", task);
        Exception e = task.getException();
        assertNull(task.getId() + " exception should be null but is '" + (e == null ? "" : ExceptionUtils.getMessage(e)) + "'", e);
        assertNotNull(task.getId() + " result is null", task.getResult());
        assertEquals(task.getResult(), BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      }
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
  @Test(timeout=10000L)
  public void testStatusListenerFromSubmit() throws Exception
  {
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = BaseSetup.createJob("JCA job", true, false, 1, LifeCycleTask.class);
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
   * Test cancelling at job after it is submiited and before its completion.
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
      JPPFJob job = BaseSetup.createJob("JCA job", true, false, 1, LifeCycleTask.class, 5000L);
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
      assertNull("task result should be null but is '" + task.getResult() + "'" , task.getResult());
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test cancelling at job after it is submiited and after its completion.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testCancelJobAfterCompletion() throws Exception
  {
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = BaseSetup.createJob("JCA job", true, false, 1, LifeCycleTask.class, 100L);
      String id = connection.submit(job);
      assertNotNull(id);
      Thread.sleep(3000L);
      connection.cancelJob(id);
      List<JPPFTask> results = connection.waitForResults(id);
      assertNotNull(results);
      int n = results.size();
      assertTrue("results size should be 1 but is " + n, n == 1);
      JPPFTask task = results.get(0);
      assertNotNull(task);
      assertNull(task.getException());
      assertNotNull(task.getResult());
      assertEquals(BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting a simple job and getting the results.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testSubmissionResults() throws Exception
  {
    JPPFConnection connection = null;
    int nbTasks = 1;
    String id = null;
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      JPPFJob job = BaseSetup.createJob("JCA job", true, false, nbTasks, LifeCycleTask.class, 100L);
      id = connection.submit(job);
      assertNotNull(id);
    }
    finally
    {
      if (connection != null) connection.close();
    }
    Thread.sleep(2000L);
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      List<JPPFTask> results = connection.getSubmissionResults(id);
      assertNotNull(results);
      int n = results.size();
      assertEquals("results size should be " + nbTasks + " but is " + n, nbTasks, n);
      int count = 0;
      for (JPPFTask task: results)
      {
        assertNotNull("task" + count + " is null", task);
        Exception e = task.getException();
        assertNull(task.getId() + " exception should be null but is '" + (e == null ? "" : ExceptionUtils.getMessage(e)) + "'", e);
        assertNotNull(task.getId() + " result is null", task.getResult());
        assertEquals(task.getResult(), BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE);
      }
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }

  /**
   * Test submitting multiple jobs ad retrieving their submission ids from a separate JCA connection.
   * @throws Exception if any error occurs.
   */
  @Test(timeout=10000L)
  public void testGetAllSubmissionIds() throws Exception
  {
    JPPFConnection connection = null;
    String prefix = "JCA Test Job ";
    int nbJobs = 2;
    JPPFJob[] jobs = new JPPFJob[nbJobs];
    String[] ids= new String[nbJobs];
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      assertNotNull(connection);
      // remove existing submissions
      for (String id: connection.getAllSubmissionIds()) connection.getSubmissionResults(id);
      for (int i=0; i<nbJobs; i++)
      {
        JPPFJob job = new JPPFJob(prefix + i);
        job.addTask(new LifeCycleTask(1000L));
        jobs[i] = job;
        ids[i] = connection.submit(job);
        assertNotNull(ids[i]);
        assertEquals(prefix + i, ids[i]);
      }
    }
    finally
    {
      if (connection != null) connection.close();
    }
    Thread.sleep(500L);
    try
    {
      connection = JPPFHelper.getConnection("java:eis/JPPFConnectionFactory");
      Collection<String> coll = connection.getAllSubmissionIds();
      assertNotNull(coll);
      assertEquals(nbJobs, coll.size());
      for (int i=0; i<nbJobs; i++) assertTrue(coll.contains(ids[i]));
      for (int i=0; i<nbJobs; i++)
      {
        List<JPPFTask> results = connection.waitForResults(ids[i]);
        assertNotNull(results);
      }
    }
    finally
    {
      if (connection != null) connection.close();
    }
  }
}
