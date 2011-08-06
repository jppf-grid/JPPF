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

package org.jppf.server.queue;

import static org.jppf.utils.CollectionUtils.*;

import java.text.ParseException;
import java.util.*;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.*;
import org.jppf.scheduling.*;
import org.jppf.server.*;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.protocol.*;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFPriorityQueue.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * An of task bundles, ordered by descending priority.
	 */
	private TreeMap<JPPFPriority, List<BundleWrapper>> priorityMap = new TreeMap<JPPFPriority, List<BundleWrapper>>();
	/**
	 * Contains the ids of all queued jobs.
	 */
	private Map<String, BundleWrapper> jobMap = new HashMap<String, BundleWrapper>();
	/**
	 * The driver stats manager.
	 */
	protected JPPFDriverStatsManager statsManager = null;
	/**
	 * The job manager.
	 */
	protected JPPFJobManager jobManager = null;
	/**
	 * Handles the schedule of each job that has one.
	 */
	private JPPFScheduleHandler jobScheduleHandler = new JPPFScheduleHandler("Job Schedule Handler");
	/**
	 * Handles the expiration schedule of each job that has one.
	 */
	private JPPFScheduleHandler jobExpirationHandler = new JPPFScheduleHandler("Job Expiration Handler");

	/**
	 * Initialize this queue.
	 */
	public JPPFPriorityQueue()
	{
		statsManager = JPPFDriver.getInstance().getStatsManager();
		jobManager = JPPFDriver.getInstance().getJobManager();
	}

	/**
	 * Add an object to the queue, and notify all listeners about it.
	 * @param bundleWrapper the object to add to the queue.
	 * @see org.jppf.server.queue.JPPFQueue#addBundle(org.jppf.server.protocol.BundleWrapper)
	 */
	public void addBundle(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
		JPPFJobSLA sla = bundle.getJobSLA();
		if (sla.isBroadcastJob() && (bundle.getParameter(BundleParameter.NODE_BROADCAST_UUID) == null))
		{
			if (debugEnabled) log.debug("before processing broadcast job with id=" + bundle.getId() + ", uuid=" + bundle.getJobUuid() + ", task count=" + bundle.getTaskCount());
			processBroadcastJob(bundleWrapper);
			return;
		}
		String jobUuid = bundle.getJobUuid();
		try
		{
			lock.lock();
			BundleWrapper other = jobMap.get(jobUuid);
			if (other != null)
			{
				other.merge(bundleWrapper, false);
				if (debugEnabled) log.debug("re-submitting bundle with " + bundle);
				bundle.setParameter("real.task.count", bundle.getTaskCount());
				fireQueueEvent(new QueueEvent(this, other, true));
			}
			else
			{
				bundle.setQueueEntryTime(System.currentTimeMillis());
				putInListMap(new JPPFPriority(sla.getPriority()), bundleWrapper, priorityMap);
				putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
				Boolean requeued = (Boolean) bundle.removeParameter(BundleParameter.JOB_REQUEUE);
				if (requeued == null) requeued = false;
				if (debugEnabled) log.debug("adding bundle with " + bundle);
				if (!requeued)
				{
					handleStartJobSchedule(bundleWrapper);
					handleExpirationJobSchedule(bundleWrapper);
				}
				jobMap.put(jobUuid, bundleWrapper);
				fireQueueEvent(new QueueEvent(this, bundleWrapper, requeued));
			}
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
		statsManager.taskInQueue(bundle.getTaskCount());
	}

	/**
	 * Get the next object in the queue.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(int)
	 */
	public ServerJob nextBundle(int nbTasks)
	{
		Iterator<BundleWrapper> it = iterator();
		return it.hasNext() ? nextBundle(it.next(),  nbTasks) : null;
	}

	/**
	 * Get the next object in the queue.
	 * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(org.jppf.server.protocol.BundleWrapper, int)
	 */
	public BundleWrapper nextBundle(BundleWrapper bundleWrapper, int nbTasks)
	{
		JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
		BundleWrapper result = null;
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks, next bundle has " + bundle.getTaskCount() + " tasks");
			int size = getSize(bundleWrapper);
			removeFromListMap(size, bundleWrapper, sizeMap);
			if (nbTasks >= bundle.getTaskCount())
			{
				result = bundleWrapper;
				removeBundle(bundleWrapper);
				bundle.setParameter("real.task.count", 0);
			}
			else
			{
				if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
				result = bundleWrapper.copy(nbTasks);
				int newSize = bundle.getTaskCount();
				List<BundleWrapper> list = sizeMap.get(newSize);
				if (list == null)
				{
					list = new ArrayList<BundleWrapper>();
					//sizeMap.put(newSize, list);
					sizeMap.put(size, list);
				}
				list.add(bundleWrapper);
				bundle.setParameter("real.task.count", bundle.getTaskCount());
				List<BundleWrapper> bundleList = priorityMap.get(new JPPFPriority(bundle.getJobSLA().getPriority()));
				bundleList.remove(bundleWrapper);
				bundleList.add(bundleWrapper);
			}
			jobManager.jobUpdated(bundleWrapper);
			//result.getBundle().setExecutionStartTime(System.currentTimeMillis());
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " +
			formatSizeMapInfo("sizeMap", sizeMap));
		JPPFTaskBundle resultJob = (JPPFTaskBundle) result.getJob();
		statsManager.taskOutOfQueue(resultJob.getTaskCount(), System.currentTimeMillis() - resultJob.getQueueEntryTime());
		return result;
	}

	/**
	 * Determine whether the queue is empty or not.
	 * @return true if the queue is empty, false otherwise.
	 * @see org.jppf.server.queue.JPPFQueue#isEmpty()
	 */
	public boolean isEmpty()
	{
		lock.lock();
		try
		{
			return priorityMap.isEmpty();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get the maximum bundle size for the bundles present in the queue.
	 * @return the bundle size as an int.
	 * @see org.jppf.server.queue.JPPFQueue#getMaxBundleSize()
	 */
	public int getMaxBundleSize()
	{
		lock.lock();
		try
		{
			latestMaxSize = sizeMap.isEmpty() ? latestMaxSize : sizeMap.lastKey();
			return latestMaxSize;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public ServerJob removeBundle(BundleWrapper bundleWrapper)
	{
		lock.lock();
		try
		{
			JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
			if (debugEnabled) log.debug("removing bundle from queue, jobId=" + bundle.getId());
			removeFromListMap(new JPPFPriority(bundle.getJobSLA().getPriority()), bundleWrapper, priorityMap);
			return jobMap.remove(bundle.getJobUuid());
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Get an iterator on the task bundles in this queue.
	 * @return an iterator.
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<BundleWrapper> iterator()
	{
		return new BundleIterator(priorityMap, lock);
	}

	/**
	 * Process the start schedule specified in the job SLA.
	 * @param bundleWrapper the job to process.
	 */
	private void handleStartJobSchedule(ServerJob bundleWrapper)
	{
		JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
		JPPFSchedule schedule = bundle.getJobSLA().getJobSchedule();
		if (schedule != null)
		{
			bundle.setParameter(BundleParameter.JOB_PENDING, true);
			String jobId = bundle.getId();
			String uuid = bundle.getJobUuid();
			if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + jobId);
			try
			{
				long dt = (Long) bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME_MILLIS);
				jobScheduleHandler.scheduleAction(uuid, schedule, new JobScheduleAction(bundleWrapper), dt);
			}
			catch(ParseException e)
			{
				bundle.setParameter(BundleParameter.JOB_PENDING, false);
				log.error("Unparseable start date for job id " + jobId + " : date = " + schedule.getDate() +
					", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
			}
		}
		else bundle.setParameter(BundleParameter.JOB_PENDING, false);
	}

	/**
	 * Process the expiration schedule specified in the job SLA.
	 * @param bundleWrapper the job to process.
	 */
	private void handleExpirationJobSchedule(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
		bundle.setParameter(BundleParameter.JOB_EXPIRED, false);
		JPPFSchedule schedule = bundle.getJobSLA().getJobExpirationSchedule();
		if (schedule != null)
		{
			String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
			String uuid = bundle.getJobUuid();
			if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + jobId);
			long dt = (Long) bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME_MILLIS);
			try
			{
				jobExpirationHandler.scheduleAction(uuid, schedule, new JobExpirationAction(bundleWrapper), dt);
			}
			catch(ParseException e)
			{
				bundle.setParameter(BundleParameter.JOB_EXPIRED, false);
				log.error("Unparseable expiration date for job id " + jobId + " : date = " + schedule.getDate() +
					", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
			}
		}
	}

	/**
	 * Clear all the scheduled actions associated with a job.
	 * This method should normally only be called when a job has completed.
	 * @param jobUuid the job uuid.
	 */
	public void clearSchedules(String jobUuid)
	{
		jobScheduleHandler.cancelAction(jobUuid);
		jobExpirationHandler.cancelAction(jobUuid);
	}

	/**
	 * Process the specified broadcast job.
	 * <b/>This consists in creating one job per node, each containing the same tasks,
	 * and with an execution policy that enforces its execution ont he designated node only.
	 * @param bundleWrapper the broadcast job to process.
	 */
	private void processBroadcastJob(BundleWrapper bundleWrapper)
	{
		Map<String, JPPFManagementInfo> uuidMap = JPPFDriver.getInstance().getNodeHandler().getUuidMap();
		if (uuidMap.isEmpty()) return;
		BroadcastJobCompletionListener completionListener = new BroadcastJobCompletionListener(bundleWrapper, uuidMap.keySet());
		JPPFDistributedJob bundle = bundleWrapper.getJob();
		JPPFJobSLA sla = bundle.getJobSLA();
		ExecutionPolicy policy = sla.getExecutionPolicy();
		List<BundleWrapper> jobList = new ArrayList<BundleWrapper>();
		for (Map.Entry<String, JPPFManagementInfo> entry: uuidMap.entrySet())
		{
			BundleWrapper job = bundleWrapper.copy();
			JPPFTaskBundle newBundle = (JPPFTaskBundle) job.getJob();
			String uuid = entry.getKey();
			JPPFManagementInfo info = entry.getValue();
			newBundle.setParameter(BundleParameter.NODE_BROADCAST_UUID, uuid);
			if ((policy != null) && !policy.accepts(info.getSystemInfo())) continue;
			ExecutionPolicy broadcastPolicy = new Equal("jppf.uuid", true, uuid);
			if (policy != null) broadcastPolicy = broadcastPolicy.and(policy);
			newBundle.setJobSLA(sla.copy());
			newBundle.getJobSLA().setExecutionPolicy(broadcastPolicy);
			newBundle.setCompletionListener(completionListener);
			newBundle.setParameter(BundleParameter.JOB_ID, bundle.getId() + " [node: " + info.toString() + "]");
			newBundle.setParameter(BundleParameter.JOB_UUID, new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString());
			if (debugEnabled) log.debug("Execution policy for job uuid=" + newBundle.getJobUuid() + " :\n" + broadcastPolicy);
			jobList.add(job);
		}
		for (BundleWrapper job: jobList) addBundle(job);
	}
}
