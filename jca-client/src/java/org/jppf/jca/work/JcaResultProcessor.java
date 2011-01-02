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
package org.jppf.jca.work;

import static org.jppf.jca.work.submission.SubmissionStatus.*;

import java.io.NotSerializableException;

import javax.resource.spi.work.Work;

import org.jppf.client.*;
import org.jppf.jca.work.submission.*;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Instances of this class send tasks to a JPPF driver and collect the results.
 */
public class JcaResultProcessor implements Work
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JcaResultProcessor.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Client connection owning this results processor.
	 */
	private final JPPFJcaClientConnection connection;
	/**
	 * The execution processed by this task.
	 */
	private JPPFJob job = null;

	/**
	 * Initialize this result processor with a specified list of tasks, data provider and result listener.
	 * @param connection the client connection owning this results processor.
	 * @param job the execution processed by this task.
	 */
	public JcaResultProcessor(JPPFJcaClientConnection connection, JPPFJob job)
	{
		this.connection = connection;
		this.job = job;
	}

	/**
	 * This method executes until all partial results have been received.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		boolean error = false;
		JPPFSubmissionResult result = (JPPFSubmissionResult) job.getResultListener();
		result.setStatus(EXECUTING);
		try
		{
			connection.setCurrentJob(job);
			int count = 0;
			//for (JPPFTask task : job.getTasks()) task.setPosition(count++);
			boolean completed = false;
			while (!completed)
			{
				try
				{
					completed = performSubmission(result);
				}
				catch(NotSerializableException e)
				{
					throw e;
				}
				catch(InterruptedException e)
				{
					throw e;
				}
				catch(ClassNotFoundException e)
				{
					throw e;
				}
				catch(Exception e)
				{
					if (debugEnabled) log.debug("["+connection.getName()+"] "+e.getMessage(), e);
					connection.setStatus(JPPFClientConnectionStatus.DISCONNECTED);
					JPPFSubmissionManager mgr = connection.getClient().getSubmissionManager();
					mgr.addExistingSubmission(job);
					connection.getTaskServerConnection().init();
					break;
				}
			}
		}
		catch(Exception e)
		{
			error = true;
			log.error("["+connection.getName()+"] "+e.getMessage(), e);
		}
		finally
		{
			if (!error) connection.setCurrentJob(null);
			else result.setStatus(FAILED);
		}
	}

	/**
	 * Perform the actual tasks submission.
	 * @param result the submission result.
	 * @return true if the submission is successfull.
	 * @throws Exception if an error is raised while submitting the tasks or receiving the results.
	 */
	private boolean performSubmission(JPPFSubmissionResult result) throws Exception
	{
		int count = 0;
		String requestUuid = job.getJobUuid();
		AbstractGenericClient client = connection.getClient();
		ClassLoader cl = null;
		ClassLoader oldCl = null;
		if (!job.getTasks().isEmpty())
		{
			JPPFTask task = job.getTasks().get(0);
			cl = task.getClass().getClassLoader();
			client.addRequestClassLoader(requestUuid, cl);
			if (log.isDebugEnabled()) log.debug("adding request class loader=" + cl + " for uuid=" + requestUuid);
		}
		try
		{
			if (cl != null)
			{
				oldCl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(cl);
			}
			connection.getClient().getLoadBalancer().execute(job, connection);
			result.waitForResults(0);
			client.removeRequestClassLoader(requestUuid);
			result.setStatus(COMPLETE);
			connection.setStatus(JPPFClientConnectionStatus.ACTIVE);
		}
		finally
		{
			if (cl != null) Thread.currentThread().setContextClassLoader(oldCl);
		}
		return true;
	}

	/**
	 * Get all the data pertaining to the execution for this task.
	 * @return a <code>JPPFJob</code> instance.
	 */
	public JPPFJob getJob()
	{
		return job;
	}

	/**
	 * Stop this result processor.
	 * @see javax.resource.spi.work.Work#release()
	 */
	public void release()
	{
	}
}
