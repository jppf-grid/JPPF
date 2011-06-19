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

package org.jppf.client;

import java.io.NotSerializableException;

import org.slf4j.*;

/**
 * This class encapsulates the results of a job submission.
 * @author Laurent Cohen
 */
public class JobSubmission implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JobSubmission.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The status of this submission.
	 */
	protected SubmissionStatus status = SubmissionStatus.SUBMITTED;
	/**
	 * The submitted job.
	 */
	protected JPPFJob job = null;
	/**
	 * The connection to execute the job on.
	 */
	protected AbstractJPPFClientConnection connection;
	/**
	 * The submission manager.
	 */
	protected SubmissionManager submissionManager;
	/**
	 * Flag indicating whether the job will be executed locally, at least partially.
	 */
	protected final boolean locallyExecuting;

	/**
	 * Initialize this job submission. 
	 * @param job the submitted job.
	 * @param connection the connection to execute the job on.
	 * @param submissionManager the submission manager.
	 * @param locallyExecuting determines whether the job will be executed locally, at least partially.
	 */
	JobSubmission(JPPFJob job, AbstractJPPFClientConnection connection, SubmissionManager submissionManager, boolean locallyExecuting)
	{
		this.job = job;
		this.connection = connection;
		this.submissionManager = submissionManager;
		this.locallyExecuting = locallyExecuting;
	}

	/**
	 * This method executes until all partial results have been received.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		boolean error = false;
		try
		{
			if (connection != null) connection.job = job;
			try
			{
				submissionManager.client.getLoadBalancer().execute(job, connection, locallyExecuting);
			}
			catch(NotSerializableException e)
			{
				throw e;
			}
			catch(InterruptedException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				String src = (connection == null) ? "local execution" : connection.getName();
				log.error("[" + src + "] " + e.getClass().getName() + ": " + e.getMessage(), e);
				if (connection != null)
				{
					connection.setCurrentJob(null);
					connection.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.DISCONNECTED);
				}
				submissionManager.resubmitJob(job);
				if (connection != null)
				{
					try
					{
						connection.getTaskServerConnection().init();
					}
					catch(Exception e2)
					{
						log.error(e2.getMessage(), e2);
					}
				}
			}
		}
		catch(Exception e)
		{
			error = true;
			log.error("["+connection.getName()+"] "+ e.getClass().getName() + ": " + e.getMessage(), e);
		}
		finally
		{
			//if (debugEnabled) log.debug("job id '" + job.getId()  + "' ended with error = " + error);
			if (!error && (connection != null)) connection.job = null;
		}
	}

	/**
	 * Get the status of this submission.
	 * @return a {@link SubmissionStatus} enumerated value.
	 */
	public synchronized SubmissionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this submission.
	 * @param status a {@link SubmissionStatus} enumerated value.
	 */
	public synchronized void setStatus(SubmissionStatus status)
	{
		if (debugEnabled) log.debug("submission [" + job.getJobUuid() + "] status changing from '" + this.status + "' to '" + status + "'");
		this.status = status;
	}

	/**
	 * Get the unique id of this submission.
	 * @return the id as a string.
	 */
	public String getId()
	{
		return job.getJobUuid();
	}

	/**
	 * Get the submitted job.
	 * @return  a {@link JPPFJob} instance.
	 */
	public JPPFJob getJob()
	{
		return job;
	}

	/**
	 * Get the connection to execute the job on.
	 * @return a {@link AbstractJPPFClientConnection} instance.
	 */
	public AbstractJPPFClientConnection getConnection()
	{
		return connection;
	}

	/**
	 * Set the connection to execute the job on.
	 * @param connection a {@link AbstractJPPFClientConnection} instance.
	 */
	public void setConnection(AbstractJPPFClientConnection connection)
	{
		this.connection = connection;
	}

	/**
	 * Determine whether the job will be executed locally, at least partially.
	 * @return <code>true</code> if the job is executed locally, <code>false</code> otherwise.
	 */
	public boolean isLocallyExecuting()
	{
		return locallyExecuting;
	}
}
