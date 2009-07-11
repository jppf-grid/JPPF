/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.job;

import java.nio.channels.SelectableChannel;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.management.NodeManagementInfo;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.utils.EventEmitter;

/**
 * Instances of this class manage and monitor the jobs thoughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager extends EventEmitter<JobManagerListener> implements QueueListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFJobManager.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Mapping of jobs to the nodes they are executing on.
	 */
	private Map<String, List<SelectableChannel>> jobMap = new HashMap<String, List<SelectableChannel>>();
	/**
	 * Mapping of job ids to the corresponding <code>JPPFTaskBundle</code>.
	 */
	private Map<String, JPPFTaskBundle> bundleMap = new HashMap<String, JPPFTaskBundle>();

	/**
	 * Default constructor.
	 */
	public JPPFJobManager()
	{
	}

	/**
	 * Called when all or part of a job is dispatched to a node.
	 * @param bundleWrapper - the dispatched job.
	 * @param channel - the node to which the job is dispatched.
	 */
	public synchronized void jobDispatched(BundleWrapper bundleWrapper, SelectableChannel channel)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		List<SelectableChannel> list = jobMap.get(jobId);
		if (list == null)
		{
			list = new ArrayList<SelectableChannel>();
			jobMap.put(jobId, list);
		}
		list.add(channel);
	}

	/**
	 * Called when all or part of a job has returned from a node.
	 * @param bundleWrapper - the returned job.
	 * @param channel - the node to which the job is dispatched.
	 */
	public synchronized void jobReturned(BundleWrapper bundleWrapper, SelectableChannel channel)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		List<SelectableChannel> list = jobMap.get(jobId);
		if (list == null)
		{
			log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + jobId);
			return;
		}
		list.remove(channel);
	}

	/**
	 * Called when a job is added to the server queue.
	 * @param bundleWrapper - the queued job.
	 */
	public synchronized void jobQueued(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		bundleMap.put(jobId, bundle);
		jobMap.put(jobId, new ArrayList<SelectableChannel>());
	}

	/**
	 * Called when a job is complete and returned to the client.
	 * @param bundleWrapper - the completed job.
	 */
	public synchronized void jobEnded(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		jobMap.remove(jobId);
		bundleMap.remove(jobId);
	}

	/**
	 * Called when a queue event occurred.
	 * @param event - a queue event.
	 * @see org.jppf.server.queue.QueueListener#newBundle(org.jppf.server.queue.QueueEvent)
	 */
	public void newBundle(QueueEvent event)
	{
		jobQueued(event.getBundleWrapper());
	}

	/**
	 * Instances of this class are submitted into an event queue and generate actual
	 * job manager events that are then dispatched to registered listeners.
	 */
	private class JobManagerEventTask implements Runnable
	{
		/**
		 * The type of event to generate.
		 */
		private JobManagerEventType eventType = null;
		/**
		 * The id of the job source of the event.
		 */
		private String jobId = null;
		/**
		 * The node, if any, for which the event happened.
		 */
		private SelectableChannel channel = null;
		/**
		 * The job data.
		 */
		private JPPFTaskBundle bundle = null;

		/**
		 * .
		 * @param eventType - the type of event to generate.
		 * @param bundle - the job data.
		 * @param channel - the id of the job source of the event.
		 */
		public JobManagerEventTask(JobManagerEventType eventType, JPPFTaskBundle bundle, SelectableChannel channel)
		{
			this.eventType = eventType;
			this.channel = channel;
			this.bundle = bundle;
		}

		/**
		 * Execute this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			JobInformation jobInfo = new JobInformation((String) bundle.getParameter(BundleParameter.JOB_ID),
				bundle.getTaskCount(), bundle.getInitialTaskCount(), bundle.getPriority());
			NodeManagementInfo nodeInfo = (channel == null) ? null : JPPFDriver.getInstance().getNodeInformation(channel);
			JobManagerEvent event = new JobManagerEvent(jobInfo, nodeInfo);
			switch (eventType)
			{
				case JOB_QUEUED:
					
					break;

				case JOB_ENDED:
					break;

				case JOB_DISPATCHED:
					break;

				case JOB_RETURNED:
					break;
			}
		}
	}
}
