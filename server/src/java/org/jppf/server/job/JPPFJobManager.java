/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.job;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.job.*;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.utils.*;

/**
 * Instances of this class manage and monitor the jobs throughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager extends EventEmitter<JobListener> implements QueueListener
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
	private Map<String, List<ChannelBundlePair>> jobMap = new HashMap<String, List<ChannelBundlePair>>();
	/**
	 * Mapping of job ids to the corresponding <code>JPPFTaskBundle</code>.
	 */
	private Map<String, BundleWrapper> bundleMap = new HashMap<String, BundleWrapper>();
	/**
	 * Processes the event queue asynchronously.
	 */
	private ExecutorService executor = null;

	/**
	 * Default constructor.
	 */
	public JPPFJobManager()
	{
		executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("Job manager event thread"));
	}

	/**
	 * Get all the nodes to which a all or part of a job is dispatched.
	 * @param jobId the id of the job.
	 * @return a list of <code>SelectableChannel</code> instances.
	 */
	public synchronized List<ChannelBundlePair> getNodesForJob(String jobId)
	{
		return Collections.unmodifiableList(jobMap.get(jobId));
	}

	/**
	 * Get the set of ids for all the jobs currently queued or executing.
	 * @return a set of ids as strings.
	 */
	public synchronized Set<String> getAllJobIds()
	{
		return Collections.unmodifiableSet(jobMap.keySet());
	}

	/**
	 * Get the queueed bundle wrapper for the specified job.
	 * @param jobId the id of the job to look for.
	 * @return a <code>BundleWrapper</code> instance, or null if the job is not queued anymore.
	 */
	public synchronized BundleWrapper getBundleForJob(String jobId)
	{
		return bundleMap.get(jobId);
	}

	/**
	 * Called when all or part of a job is dispatched to a node.
	 * @param bundleWrapper the dispatched job.
	 * @param channel the node to which the job is dispatched.
	 */
	public synchronized void jobDispatched(BundleWrapper bundleWrapper, ChannelWrapper channel)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		List<ChannelBundlePair> list = jobMap.get(jobId);
		if (list == null)
		{
			list = new ArrayList<ChannelBundlePair>();
			jobMap.put(jobId, list);
		}
		list.add(new ChannelBundlePair(channel, bundleWrapper));
		if (debugEnabled) log.debug("jobId '" + jobId + "' : added node " + channel);
		submitEvent(JobEventType.JOB_DISPATCHED, bundle, channel);
	}

	/**
	 * Called when all or part of a job has returned from a node.
	 * @param bundleWrapper the returned job.
	 * @param channel the node to which the job is dispatched.
	 */
	public synchronized void jobReturned(BundleWrapper bundleWrapper, ChannelWrapper channel)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		List<ChannelBundlePair> list = jobMap.get(jobId);
		if (list == null)
		{
			log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + jobId);
			return;
		}
		list.remove(new ChannelBundlePair(channel, bundleWrapper));
		if (debugEnabled) log.debug("jobId '" + jobId + "' : removed node " + channel);
		submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
	}

	/**
	 * Called when a job is added to the server queue.
	 * @param bundleWrapper the queued job.
	 */
	public synchronized void jobQueued(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		bundleMap.put(jobId, bundleWrapper);
		jobMap.put(jobId, new ArrayList<ChannelBundlePair>());
		if (debugEnabled) log.debug("jobId '" + jobId + "' queued");
		submitEvent(JobEventType.JOB_QUEUED, bundle, null);
	}

	/**
	 * Called when a job is complete and returned to the client.
	 * @param bundleWrapper the completed job.
	 */
	public synchronized void jobEnded(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		jobMap.remove(jobId);
		bundleMap.remove(jobId);
		if (debugEnabled) log.debug("jobId '" + jobId + "' ended");
		submitEvent(JobEventType.JOB_ENDED, bundle, null);
	}

	/**
	 * Called when a job is added to the server queue.
	 * @param bundleWrapper the queued job.
	 */
	public synchronized void jobUpdated(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
		if (debugEnabled) log.debug("jobId '" + jobId + "' updated");
		submitEvent(JobEventType.JOB_UPDATED, bundle, null);
	}

	/**
	 * Called when a queue event occurrs.
	 * @param event a queue event.
	 * @see org.jppf.server.queue.QueueListener#newBundle(org.jppf.server.queue.QueueEvent)
	 */
	public void newBundle(QueueEvent event)
	{
		if (!event.isRequeued()) jobQueued(event.getBundleWrapper());
		else jobUpdated(event.getBundleWrapper());
	}

	/**
	 * Submit an event to the event queue.
	 * @param eventType the type of event to generate.
	 * @param bundle the job data.
	 * @param channel the id of the job source of the event.
	 */
	private void submitEvent(JobEventType eventType, JPPFTaskBundle bundle, ChannelWrapper channel)
	{
		executor.submit(new JobEventTask(this, eventType, bundle, channel));
	}

	/**
	 * Close this job manager and release its resources.
	 */
	public synchronized void close()
	{
		executor.shutdownNow();
		jobMap.clear();
		bundleMap.clear();
	}
}
