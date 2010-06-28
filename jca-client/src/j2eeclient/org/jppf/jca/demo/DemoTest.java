/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import java.io.Serializable;
import java.util.*;

import javax.naming.InitialContext;
import javax.resource.cci.ConnectionFactory;

import org.jppf.client.JPPFJob;
import org.jppf.jca.cci.*;
import org.jppf.jca.work.submission.SubmissionStatus;
import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class encapsulate a simple call to the JPPF resource adapter.
 * @author Laurent Cohen
 */
public class DemoTest implements Serializable
{
	/**
	 * JNDI name of the JPPFConnectionFactory.
	 */
	private String jndiBinding = null;
	/**
	 * Reference ot the initial context.
	 */
	private transient InitialContext ctx = null;

	/**
	 * Initialize this test object with a specified jndi location for the connection factory.
	 * @param jndiBinding JNDI name of the JPPFConnectionFactory.
	 */
	public DemoTest(String jndiBinding)
	{
		this.jndiBinding = jndiBinding;
	}

	/**
	 * Perform a simple call to the JPPF resource adapter.
	 * @param duration the duration of the task to submit.
	 * @return a string reporting either the task execution result or an error message.
	 * @throws Exception if the call to JPPF failed.
	 */
	public String testConnector(int duration) throws Exception
	{
		JPPFConnection connection = null;
		String id = null;
		try
		{
			connection = getConnection();
			JPPFJob job = new JPPFJob();
			job.addTask(new DurationTask(duration));
			id = connection.submitNonBlocking(job);

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
							// process successfull completion
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
			if (connection != null) connection.close();
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
	public String testConnector(String jobId, long duration, int nbTasks) throws Exception
	{
		JPPFConnection connection = null;
		String id = null;
		try
		{
			connection = getConnection();
			JPPFJob job = new JPPFJob();
			job.setId(jobId);
			for (int i=0; i<nbTasks; i++)
			{
				DurationTask task = new DurationTask(duration);
				task.setId(jobId + " task #" + (i+1));
				job.addTask(task);
			}
			id = connection.submitNonBlocking(job);
		}
		finally
		{
			if (connection != null) connection.close();
		}
		return id;
	}

	/**
	 * Get the initial context.
	 * @return an <code>InitialContext</code> instance.
	 * @throws Exception if the context could not be obtained.
	 */
	public InitialContext getInitialContext() throws Exception
	{
		if (ctx == null) ctx = new InitialContext();
		return ctx;
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
			connection = getConnection();
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
			if (connection != null) connection.close();
		}
		return map;
	}

	/**
	 * Get the resulting message from a submission.
	 * @param id the id of the submission to retrieve.
	 * @return a string reporting either the task execution result or an error message.
	 * @throws Exception if the call to JPPF failed.
	 */
	public String getMessage(String id) throws Exception
	{
		JPPFConnection connection = null;
		String msg = null;
		try
		{
			connection = getConnection();
			List<JPPFTask> results = connection.getSubmissionResults(id);
			if (results == null) msg = "submission is not in queue anymore";
			else
			{
				StringBuffer sb = new StringBuffer();
				for (JPPFTask task: results)
				{
					if (task.getException() == null) sb.append(task.getResult());
					else sb.append("task [" + task.getId() + "] ended in error: " + task.getException().getMessage());
					sb.append("<br/>");
				}
				msg = sb.toString();
			}
		}
		finally
		{
			if (connection != null) connection.close();
		}
		return msg;
	}

	/**
	 * Obtain a JPPF connection from the resource adaper's connection pool.
	 * The obtained connection must be closed by the caller of this method, once it is done using it.
	 * @return a <code>JPPFConnection</code> instance.
	 * @throws Exception if the connection could not be obtained.
	 */
	public JPPFConnection getConnection() throws Exception
	{
		Object objref = getInitialContext().lookup(jndiBinding);
		//ConnectionFactory cf = (ConnectionFactory) PortableRemoteObject.narrow(objref, ConnectionFactory.class);
		JPPFConnectionFactory cf = (JPPFConnectionFactory) javax.rmi.PortableRemoteObject.narrow(objref, ConnectionFactory.class);
		return (JPPFConnection) cf.getConnection();
	}
}
