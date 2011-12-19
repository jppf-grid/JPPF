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

package org.jppf.jca.work.submission;

import static org.jppf.client.SubmissionStatus.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.resource.spi.work.*;

import org.jppf.client.*;
import org.jppf.jca.work.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This task provides asynchronous management of tasks submitted through the resource adapter.
 * It relies on a queue where job are first added, then submitted when a driver connection becomes available.
 * It also provides methods to check the status of a submission and retrieve the results.
 * @author Laurent Cohen
 */
public class JPPFSubmissionManager extends ThreadSynchronization implements Work
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFSubmissionManager.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The queue of submissions pending execution.
	 */
	private ConcurrentLinkedQueue<JPPFJob> execQueue = new ConcurrentLinkedQueue<JPPFJob>();
	/**
	 * Mapping of submissions to their submission id.
	 */
	private Map<String, JPPFSubmissionResult> submissionMap = new Hashtable<String, JPPFSubmissionResult>();
	/**
	 * The JPPF client that manages connections to the JPPF drivers.
	 */
	private JPPFJcaClient client = null;

	/**
	 * Initialize this submission worker with the specified JPPF client.
	 * @param client the JPPF client that manages connections to the JPPF drivers.
	 */
	public JPPFSubmissionManager(JPPFJcaClient client)
	{
		this.client = client;
	}

	/**
	 * Stop this submission manager.
	 * @see javax.resource.spi.work.Work#release()
	 */
	public void release()
	{
		setStopped(true);
		wakeUp();
	}

	/**
	 * Run the loop of this submission manager, watching for the queue and starting a job
	 * when the queue has one and a connnection is available.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		while (!isStopped())
		{
			Pair<Boolean, Boolean> execFlags = null;
			while (((execQueue.peek() == null) || !(execFlags = client.handleAvailableConnection()).first()) && !isStopped())
			{
				goToSleep();
			}
			if (isStopped()) break;
			synchronized(client)
			{
        JPPFJob job = execQueue.peek();
        boolean isBroadcast = job.getJobSLA().isBroadcastJob();
        AbstractJPPFClientConnection c = (AbstractJPPFClientConnection) client.getClientConnection(true);
        if ((c == null) && isBroadcast)
        {
          if (execFlags.second()) client.getLoadBalancer().setLocallyExecuting(false);
          continue;
        }
        job = execQueue.poll();
        if (debugEnabled) log.debug("submitting jobId=" + job.getId());
        if (c != null) c.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.EXECUTING);
        JobSubmission submission = new JobSubmission(job, c, isBroadcast ? false : execFlags.second());
        client.getExecutor().submit(submission);
			}
		}
	}

	/**
	 * Add a task submission to the execution queue.
	 * @param job encapsulation of the execution data.
	 * @return the unique id of the submission.
	 */
	public String addSubmission(JPPFJob job)
	{
		return addSubmission(job, null);
	}

	/**
	 * Add a task submission to the execution queue.
	 * @param job encapsulation of the execution data.
	 * @param listener an optional listener to receive submission status change notifications, may be null.
	 * @return the unique id of the submission.
	 */
	public String addSubmission(JPPFJob job, SubmissionStatusListener listener)
	{
		int count = job.getTasks().size();
		JPPFSubmissionResult submission = new JPPFSubmissionResult(count, job.getJobUuid());
		if (debugEnabled) log.debug("adding new submission: jobId=" + job.getId() + ", nbTasks=" + count + ", submission id=" + submission.getId());
		if (listener != null) submission.addSubmissionStatusListener(listener);
		job.setResultListener(submission);
		submission.setStatus(PENDING);
		execQueue.add(job);
		submissionMap.put(submission.getId(), submission);
		wakeUp();
		return submission.getId();
	}

	/**
	 * Add an existing submission back into the execution queue.
	 * @param job encapsulation of the execution data.
	 * @return the unique id of the submission.
	 */
	public String addExistingSubmission(JPPFJob job)
	{
		JPPFSubmissionResult submission = (JPPFSubmissionResult) job.getResultListener();
		if (debugEnabled) log.debug("resubmitting: jobId=" + job.getId() + ", nbTasks=" + job.getTasks().size() + ", submission id=" + submission.getId());
		submission.reset();
		submission.setStatus(PENDING);
		execQueue.add(job);
		submissionMap.put(submission.getId(), submission);
		wakeUp();
		return submission.getId();
	}

	/**
	 * Get a submission given its id, without removing it from this submissison manager.
	 * @param id the id of the submission to find.
	 * @return the submisison corresponding to the id, or null if the submission could not be found.
	 */
	public JPPFSubmissionResult peekSubmission(String id)
	{
		return submissionMap.get(id);
	}

	/**
	 * Get a submission given its id, and remove it from this submissison manager.
	 * @param id the id of the submission to find.
	 * @return the submisison corresponding to the id, or null if the submission could not be found.
	 */
	public JPPFSubmissionResult pollSubmission(String id)
	{
		return submissionMap.remove(id);
	}

	/**
	 * Get the ids of all currently available submissions.
	 * @return a collection of ids as strings.
	 */
	public Collection<String> getAllSubmissionIds()
	{
		return Collections.unmodifiableSet(submissionMap.keySet());
	}

	/**
	 * Wrapper for submitting a job.
	 */
	public class JobSubmission implements Runnable
	{
		/**
		 * The job to execute.
		 */
		private JPPFJob job;
		/**
		 * Flag indicating whether the job will be executed locally, at least partially.
		 */
		private boolean locallyExecuting = false;
		/**
		 * The connection to execute the job on.
		 */
		protected AbstractJPPFClientConnection connection;

		/**
		 * Initialize this job submission. 
		 * @param job the submitted job.
		 * @param connection the connection to execute the job on.
		 * @param locallyExecuting determines whether the job will be executed locally, at least partially.
		 */
		JobSubmission(JPPFJob job, AbstractJPPFClientConnection connection, boolean locallyExecuting)
		{
			this.job = job;
			this.connection = connection;
			this.locallyExecuting = locallyExecuting;
		}

		/**
		 * {@inheritDoc}
		 */
		public void run()
		{
			JPPFSubmissionResult result = (JPPFSubmissionResult) job.getResultListener();
			String requestUuid = job.getJobUuid();
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
				client.getLoadBalancer().execute(job, connection, locallyExecuting);
				result.waitForResults(0);
				client.removeRequestClassLoader(requestUuid);
				result.setStatus(COMPLETE);
			}
			catch (Exception e)
			{
				result.setStatus(FAILED);
				log.error(e.getMessage(), e);
				addExistingSubmission(job);
			}
			finally
			{
				if (connection != null) connection.setStatus(JPPFClientConnectionStatus.ACTIVE);
				if (cl != null) Thread.currentThread().setContextClassLoader(oldCl);
			}
		}

		/**
		 * Get the flag indicating whether the job will be executed locally, at least partially.
		 * @return <code>true</code> if the job will execute locally, <code>false</code> otherwise.
		 */
		public boolean isLocallyExecuting()
		{
			return locallyExecuting;
		}

		/**
		 * Set the flag indicating whether the job will be executed locally, at least partially.
		 * @param locallyExecuting <code>true</code> if the job will execute locally, <code>false</code> otherwise.
		 */
		public void setLocallyExecuting(boolean locallyExecuting)
		{
			this.locallyExecuting = locallyExecuting;
		}
	}
}
