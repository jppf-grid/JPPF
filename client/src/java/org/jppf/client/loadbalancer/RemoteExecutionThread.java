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
package org.jppf.client.loadbalancer;

import java.util.List;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.Pair;
import org.slf4j.*;

/**
 * Instances of this class are intended to perform remote task executions concurrently.
 */
class RemoteExecutionThread extends ExecutionThread
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(RemoteExecutionThread.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The connection to the driver to use.
	 */
	private AbstractJPPFClientConnection connection = null;

	/**
	 * Initialize this execution thread for remote excution.
	 * @param tasks the tasks to execute.
	 * @param job the execution to perform.
	 * @param connection the connection to the driver to use.
	 * @param loadBalancer the load balancer for which this thread is working.
	 */
	public RemoteExecutionThread(List<JPPFTask> tasks, JPPFJob job, AbstractJPPFClientConnection connection, LoadBalancer loadBalancer)
	{
		super(tasks, job, loadBalancer);
		this.connection = connection;
		setName("RemoteExecution");
	}

	/**
	 * Perform the execution.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		try
		{
			long start = System.nanoTime();
			int count = 0;
			boolean completed = false;
			JPPFJob newJob = createNewJob(job);
			for (JPPFTask task: tasks)
			{
				// needed as JPPFJob.addTask() resets the position
				int pos = task.getPosition();
				newJob.addTask(task);
				task.setPosition(pos);
			}
			while (!completed)
			{
				JPPFTaskBundle bundle = createBundle(newJob);
				connection.sendTasks(bundle, newJob);
				ClassLoader cl = connection.getClient().getRequestClassLoader(bundle.getRequestUuid());
				while (count < tasks.size())
				{
					Pair<List<JPPFTask>, Integer> p = connection.receiveResults(cl);
					int n = p.first().size();
					count += n;
					if (debugEnabled) log.debug("received " + n + " tasks from server" + (n > 0 ? ", first position=" + p.first().get(0).getPosition() : ""));
					TaskResultListener listener = newJob.getResultListener();
					if (listener != null)
					{
						synchronized(listener)
						{
							listener.resultsReceived(new TaskResultEvent(p.first()));
						}
					}
					else log.warn("result listener is null for job " + newJob);
				}
				completed = true;
			}

			if (loadBalancer.isLocalEnabled())
			{
				double elapsed = System.nanoTime() - start;
				loadBalancer.getBundlers()[LoadBalancer.REMOTE].feedback(tasks.size(), elapsed);
			}
			if (connection != null) connection.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.ACTIVE);
		}
		catch(Throwable t)
		{
			log.error(t.getMessage(), t);
			exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
			if (job.getResultListener() != null)
			{
				synchronized(job.getResultListener())
				{
					job.getResultListener().resultsReceived(new TaskResultEvent(t));
				}
			}
		}
	}

	/**
	 * Create a new job based on the initial one.
	 * @param job the initial job.
	 * @return a new {@link JPPFJob} with the same characteristics as the initial one, except for the tasks.
	 */
	private JPPFJob createNewJob(JPPFJob job)
	{
		JPPFJob newJob = new JPPFJob(job.getJobUuid());
		newJob.setDataProvider(job.getDataProvider());
		newJob.setJobSLA(job.getJobSLA());
		newJob.setJobMetadata(job.getJobMetadata());
		newJob.setBlocking(job.isBlocking());
		newJob.setResultListener(job.getResultListener());
		newJob.setName(job.getName());
		return newJob;
	}

	/**
	 * Create a task bundle for the specified job.
	 * @param job the job to use as a base.
	 * @return a JPPFTaskBundle instance.
	 */
	private JPPFTaskBundle createBundle(JPPFJob job)
	{
		String requestUuid = job.getJobUuid();
		JPPFTaskBundle bundle = new JPPFTaskBundle();
		bundle.setRequestUuid(requestUuid);
		ClassLoader cl = null;
		ClassLoader oldCl = null;
		if (!job.getTasks().isEmpty())
		{
			JPPFTask task = job.getTasks().get(0);
			cl = task.getClass().getClassLoader();
			connection.getClient().addRequestClassLoader(requestUuid, cl);
			if (log.isDebugEnabled()) log.debug("adding request class loader=" + cl + " for uuid=" + requestUuid);
		}
		return bundle;
	}
}