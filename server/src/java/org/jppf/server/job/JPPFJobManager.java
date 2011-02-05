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

package org.jppf.server.job;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.job.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * Instances of this class manage and monitor the jobs throughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager implements QueueListener
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFJobManager.class);
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
	 * The list of registered listeners.
	 */
	protected List<JobListener> eventListeners = new ArrayList<JobListener>();

	/**
	 * Default constructor.
	 */
	public JPPFJobManager()
	{
		executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobManager"));
	}

	/**
	 * Get all the nodes to which a all or part of a job is dispatched.
	 * @param jobUuid the id of the job.
	 * @return a list of <code>SelectableChannel</code> instances.
	 */
	public synchronized List<ChannelBundlePair> getNodesForJob(String jobUuid)
	{
		if (jobUuid == null) return null;
		List<ChannelBundlePair> list = jobMap.get(jobUuid);
		return list == null ? null : Collections.unmodifiableList(list);
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
	 * Get the queued bundle wrapper for the specified job.
	 * @param jobUuid the id of the job to look for.
	 * @return a <code>BundleWrapper</code> instance, or null if the job is not queued anymore.
	 */
	public synchronized BundleWrapper getBundleForJob(String jobUuid)
	{
		return bundleMap.get(jobUuid);
	}

	/**
	 * Called when all or part of a job is dispatched to a node.
	 * @param bundleWrapper the dispatched job.
	 * @param channel the node to which the job is dispatched.
	 */
	public synchronized void jobDispatched(BundleWrapper bundleWrapper, ChannelWrapper channel)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobUuid = bundle.getJobUuid();
		List<ChannelBundlePair> list = jobMap.get(jobUuid);
		if (list == null)
		{
			list = new ArrayList<ChannelBundlePair>();
			jobMap.put(jobUuid, list);
		}
		list.add(new ChannelBundlePair(channel, bundleWrapper));
		if (debugEnabled) log.debug("jobId '" + bundle.getId() + "' : added node " + channel);
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
		String jobUuid = bundle.getJobUuid();
		List<ChannelBundlePair> list = jobMap.get(jobUuid);
		if (list == null)
		{
			log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + bundle.getId());
			return;
		}
		list.remove(new ChannelBundlePair(channel, bundleWrapper));
		if (debugEnabled) log.debug("jobId '" + bundle.getId() + "' : removed node " + channel);
		submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
	}

	/**
	 * Called when a job is added to the server queue.
	 * @param bundleWrapper the queued job.
	 */
	public synchronized void jobQueued(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobUuid = bundle.getJobUuid();
		bundleMap.put(jobUuid, bundleWrapper);
		jobMap.put(jobUuid, new ArrayList<ChannelBundlePair>());
		if (debugEnabled) log.debug("jobId '" + bundle.getId() + "' queued");
		submitEvent(JobEventType.JOB_QUEUED, bundle, null);
	}

	/**
	 * Called when a job is complete and returned to the client.
	 * @param bundleWrapper the completed job.
	 */
	public synchronized void jobEnded(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		String jobUuid = bundle.getJobUuid();
		jobMap.remove(jobUuid);
		bundleMap.remove(jobUuid);
		((JPPFPriorityQueue) JPPFDriver.getInstance().getQueue()).clearSchedules(jobUuid);
		if (debugEnabled) log.debug("jobId '" + bundle.getId() + "' ended");
		submitEvent(JobEventType.JOB_ENDED, bundle, null);
	}

	/**
	 * Called when a job is added to the server queue.
	 * @param bundleWrapper the queued job.
	 */
	public synchronized void jobUpdated(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		if (debugEnabled) log.debug("jobId '" + bundle.getId() + "' updated");
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

	/**
	 * Add a listener to the list of listeners.
	 * @param listener the listener to add to the list.
	 */
	public void addJobListener(JobListener listener)
	{
		synchronized(eventListeners)
		{
			eventListeners.add(listener);
		}
	}

	/**
	 * Remove a listener from the list of listeners.
	 * @param listener the listener to rmeove from the list.
	 */
	public void removeJobListener(JobListener listener)
	{
		synchronized(eventListeners)
		{
			eventListeners.remove(listener);
		}
	}

	/**
	 * return a list of all the registered listee ners.
	 * This list is not thread safe and must bmanually synchronized against concurrent modifications.
	 * @return a list of listener instances.
	 */
	public List<JobListener> getJobListeners()
	{
		return eventListeners;
	}
}
