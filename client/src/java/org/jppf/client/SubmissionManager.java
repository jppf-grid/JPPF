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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * This task provides asynchronous management of tasks submitted through the resource adapter.
 * It relies on a queue where job are first added, then submitted when a driver connection becomes available.
 * It also provides methods to check the status of a submission and retrieve the results.
 * @author Laurent Cohen
 */
public class SubmissionManager extends ThreadSynchronization implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(SubmissionManager.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The queue of submissions pending execution.
	 */
	private ConcurrentLinkedQueue<JPPFJob> execQueue = new ConcurrentLinkedQueue<JPPFJob>();
	/**
	 * The JPPF client that manages connections to the JPPF drivers.
	 */
	JPPFClient client = null;

	/**
	 * Initialize this submission manager with the specified JPPF client.
	 * @param client the JPPF client that manages connections to the JPPF drivers.
	 * JPPF submissions.
	 */
	public SubmissionManager(JPPFClient client)
	{
		this.client = client;
	}

	/**
	 * Run the loop of this submission manager, watching for the queue and starting a job
	 * when the queue has one and a connnection is available.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		JPPFClientConnectionImpl c = null;
		JPPFJob job = null;
		while (!isStopped())
		{
			while (((execQueue.peek() == null) || !client.hasAvailableConnection()) && !isStopped())
			{
				goToSleep();
			}
			if (isStopped()) break;
			synchronized(this)
			{
				job = execQueue.poll();
				c = (JPPFClientConnectionImpl) client.getClientConnection(true);
			}
			if (c != null) c.setStatus(JPPFClientConnectionStatus.EXECUTING);
			JobSubmission submission = new JobSubmission(job, c, this);
			client.getExecutor().submit(submission);
		}
	}

	/**
	 * Add a task submission to the execution queue.
	 * @param job encapsulation of the execution data.
	 * @return the unique id of the submission.
	 */
	public String submitJob(JPPFJob job)
	{
		if (debugEnabled) log.debug("adding new submission: jobId=" + job.getId());
		execQueue.add(job);
		wakeUp();
		return job.getId();
	}

	/**
	 * Add an existing submission back into the execution queue.
	 * @param job encapsulation of the execution data.
	 * @return the unique id of the submission.
	 */
	public String resubmitJob(JPPFJob job)
	{
		if (debugEnabled) log.debug("resubmitting job with id=" + job.getId());
		execQueue.add(job);
		wakeUp();
		return job.getId();
	}
}
