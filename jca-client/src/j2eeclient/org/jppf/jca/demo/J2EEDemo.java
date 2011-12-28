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

package org.jppf.jca.demo;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.jca.cci.JPPFConnection;
import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class encapsulate a simple call to the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class J2EEDemo
{
  /**
   * JNDI name of the JPPFConnectionFactory.
   */
  private final String jndiBinding;

  /**
   * Initialize this test object with a specified jndi location for the connection factory.
   * @param jndiBinding JNDI name of the JPPFConnectionFactory.
   */
  public J2EEDemo(final String jndiBinding)
  {
    this.jndiBinding = jndiBinding;
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * @param duration the duration of the task to submit.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testConnector(final int duration) throws Exception
  {
    JPPFConnection connection = null;
    String id = null;
    try
    {
      connection = JPPFHelper.getConnection(jndiBinding);
      JPPFJob job = new JPPFJob();
      job.addTask(new DemoTask(duration));
      id = connection.submit(job);

      /*
			// submit with an execution policy
			ExecutionPolicy policy = PolicyParser.parsePolicy("ExecutionPolicy.xml");
			id = connection.submitNonBlocking(policy, list, null, null);
       */

      /*
			id = connection.submitNonBlocking(job, new SubmissionStatusListener()
			{
				public void submissionStatusChanged(SubmissionStatusEvent event)
				{
					System.out.println("*** 1 *** submission ["+event.getSubmissionId()+"] changed to '"+event.getStatus()+"'");
				}
			});

			connection.addSubmissionStatusListener(id, new SubmissionStatusListener()
			{
				public void submissionStatusChanged(SubmissionStatusEvent event)
				{
					String id = event.getSubmissionId();
					SubmissionStatus status = event.getStatus();
					switch(status)
					{
						case COMPLETE:
							// process successful completion
							break;
						case FAILED:
							// process failure
							break;
						default:
							System.out.println("submission [" + id +
								"] changed to '" + status + "'");
							break;
					}
				}
			});
       */
    }
    finally
    {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return id;
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * @param jobId the name given to the job.
   * @param duration the duration of the task to submit.
   * @param nbTasks the number of tasks to submit.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testConnector(final String jobId, final long duration, final int nbTasks) throws Exception
  {
    JPPFConnection connection = null;
    String id = null;
    try
    {
      connection = JPPFHelper.getConnection(jndiBinding);
      JPPFJob job = new JPPFJob();
      job.setName(jobId);
      for (int i=0; i<nbTasks; i++)
      {
        DemoTask task = new DemoTask(duration);
        task.setId(jobId + " task #" + (i+1));
        job.addTask(task);
      }
      id = connection.submit(job);
    }
    finally
    {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return id;
  }

  /**
   * Perform a simple call to the JPPF resource adapter.
   * This method blocks until all job results have been received.
   * @param jobId the name given to the job.
   * @param duration the duration of the task to submit.
   * @param nbTasks the number of tasks to submit.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String testConnectorBlocking(final String jobId, final long duration, final int nbTasks) throws Exception
  {
    JPPFConnection connection = null;
    String id = null;
    try
    {
      connection = JPPFHelper.getConnection(jndiBinding);
      JPPFJob job = new JPPFJob();
      job.setName(jobId);
      for (int i=0; i<nbTasks; i++)
      {
        DemoTask task = new DemoTask(duration);
        task.setId(jobId + " task #" + (i+1));
        job.addTask(task);
      }
      id = connection.submit(job);
      List<JPPFTask> results = connection.waitForResults(id);
      System.out.println("received " + results.size() + " results for job '" + job.getName() + "'");
    }
    finally
    {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return id;
  }

  /**
   * Get the map of submissions ids to their corresponding submission status.
   * @return a map of ids to statuses as strings.
   * @throws Exception if the call to JPPF failed.
   */
  public Map getStatusMap() throws Exception
  {
    Map<String, String> map = new HashMap<String, String>();
    JPPFConnection connection = null;
    try
    {
      connection = JPPFHelper.getConnection(jndiBinding);
      Collection<String> coll = connection.getAllSubmissionIds();
      for (String id: coll)
      {
        SubmissionStatus status = connection.getSubmissionStatus(id);
        String s = (status == null) ? "Unknown" : status.toString();
        map.put(id, s);
      }
    }
    finally
    {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return map;
  }

  /**
   * Get the resulting message from a submission.
   * @param id the id of the submission to retrieve.
   * @return a string reporting either the task execution result or an error message.
   * @throws Exception if the call to JPPF failed.
   */
  public String getMessage(final String id) throws Exception
  {
    JPPFConnection connection = null;
    String msg = null;
    try
    {
      connection = JPPFHelper.getConnection(jndiBinding);
      List<JPPFTask> results = connection.getSubmissionResults(id);
      if (results == null) msg = "submission is not in queue anymore";
      else
      {
        StringBuilder sb = new StringBuilder();
        for (JPPFTask task: results)
        {
          if (task.getException() == null) sb.append(task.getResult());
          else sb.append("task [").append(task.getId()).append("] ended in error: ").append(task.getException().getMessage());
          sb.append("<br/>");
        }
        msg = sb.toString();
      }
    }
    finally
    {
      if (connection != null) JPPFHelper.closeConnection(connection);
    }
    return msg;
  }
}
