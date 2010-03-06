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

package org.jppf.server.queue;

import static org.jppf.utils.CollectionUtils.*;

import java.text.ParseException;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.io.BundleWrapper;
import org.jppf.scheduling.*;
import org.jppf.server.*;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.protocol.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFPriorityQueue.class);
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
	 * @see org.jppf.server.queue.JPPFQueue#addBundle(org.jppf.io.BundleWrapper)
	 */
	public void addBundle(BundleWrapper bundleWrapper)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		JPPFJobSLA sla = bundle.getJobSLA();
		try
		{
			lock.lock();
			String jobId = (String) bundle.getParameter(BundleParameter.JOB_ID);
			BundleWrapper other = jobMap.get(jobId);
			if (other != null)
			{
				other.merge(bundleWrapper, false);
				if (debugEnabled) log.debug("re-submitting bundle with [priority=" + sla.getPriority()+", initialTasksCount=" +
					bundle.getInitialTaskCount() + ", taskCount=" + bundle.getTaskCount() + "]");
				bundle.setParameter("real.task.count", bundle.getTaskCount());
				fireQueueEvent(new QueueEvent(this, other, true));
			}
			else
			{
				bundle.setQueueEntryTime(System.currentTimeMillis());
				if (debugEnabled) log.debug("adding bundle with [priority=" + sla.getPriority()+", initialTasksCount=" +
					bundle.getInitialTaskCount() + ", taskCount=" + bundle.getTaskCount() + "]");
				putInListMap(new JPPFPriority(sla.getPriority()), bundleWrapper, priorityMap);
				putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
				Boolean requeued = (Boolean) bundle.removeParameter(BundleParameter.JOB_REQUEUE);
				if (requeued == null) requeued = false;
				if (!requeued)
				{
					JPPFSchedule schedule = sla.getJobSchedule();
					boolean pending = (schedule == null) ? false : true;
					bundle.setParameter(BundleParameter.JOB_PENDING, pending);
					if (pending)
					{
						try
						{
							jobScheduleHandler.scheduleAction(jobId, sla.getJobSchedule(), new JobScheduleAction(bundleWrapper));
						}
						catch(ParseException e)
						{
							bundle.setParameter(BundleParameter.JOB_PENDING, false);
							log.error("Unparseable date for job id " + jobId + " : date = " + schedule.getDate() +
								", date format = " + (schedule.getDateFormat() == null ? "null" : schedule.getDateFormat().toLocalizedPattern()), e);
						}
					}
				}
				jobMap.put(jobId, bundleWrapper);
				fireQueueEvent(new QueueEvent(this, bundleWrapper, requeued));
			}
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled) log.debug("Maps size information:\n" + formatSizeMapInfo("priorityMap", priorityMap) + "\n" +
			formatSizeMapInfo("sizeMap", sizeMap));
		statsManager.taskInQueue(bundle.getTaskCount());
	}

	/**
	 * Get the next object in the queue.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(int)
	 */
	public BundleWrapper nextBundle(int nbTasks)
	{
		Iterator<BundleWrapper> it = iterator();
		return it.hasNext() ? nextBundle(it.next(),  nbTasks) : null;
	}

	/**
	 * Get the next object in the queue.
	 * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
	 * @param nbTasks the maximum number of tasks to get out of the bundle.
	 * @return the most recent object that was added to the queue.
	 * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(org.jppf.io.BundleWrapper, int)
	 */
	public BundleWrapper nextBundle(BundleWrapper bundleWrapper, int nbTasks)
	{
		JPPFTaskBundle bundle = bundleWrapper.getBundle();
		BundleWrapper result = null;
		try
		{
			lock.lock();
			if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks");
			if (debugEnabled) log.debug("next bundle has " + bundle.getTaskCount() + " tasks");
			int size = getSize(bundleWrapper);
			removeFromListMap(size, bundleWrapper, sizeMap);
			if (nbTasks >= bundle.getTaskCount())
			{
				if (debugEnabled) log.debug("removing bundle from queue");
				result = bundleWrapper;
				removeFromListMap(new JPPFPriority(bundle.getJobSLA().getPriority()), bundleWrapper, priorityMap);
				jobMap.remove((String) bundle.getParameter(BundleParameter.JOB_ID));
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
			}
			jobManager.jobUpdated(bundleWrapper);
			//result.getBundle().setExecutionStartTime(System.currentTimeMillis());
		}
		finally
		{
			lock.unlock();
		}
		if (debugEnabled) log.debug("Maps size information:\n" + formatSizeMapInfo("priorityMap", priorityMap) + "\n" +
			formatSizeMapInfo("sizeMap", sizeMap));
		statsManager.taskOutOfQueue(result.getBundle().getTaskCount(), System.currentTimeMillis() - result.getBundle().getQueueEntryTime());
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
	 * Get an iterator on the task bundles in this queue.
	 * @return an iterator.
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<BundleWrapper> iterator()
	{
		return new BundleIterator();
	}

	/**
	 * Iterator that traverses the collection of task bundles in descending order of their priority.
	 * This iterator is read-only and does not support the <code>remove()</code> operation.
	 */
	private class BundleIterator implements Iterator<BundleWrapper>
	{
		/**
		 * Iterator over the entries in the priority map.
		 */
		private Iterator<Map.Entry<JPPFPriority, List<BundleWrapper>>> entryIterator = null;
		/**
		 * Iterator over the task bundles in the map entry specified by <code>entryIterator</code>.
		 */
		private Iterator<BundleWrapper> listIterator = null;

		/**
		 * Initialize this iterator.
		 */
		public BundleIterator()
		{
			lock.lock();
			try
			{
				entryIterator = priorityMap.entrySet().iterator();
				if (entryIterator.hasNext()) listIterator = entryIterator.next().getValue().iterator();
			}
			finally
			{
				lock.unlock();
			}
		}

		/**
		 * Determines whether an element remains to visit.
		 * @return true if there is at least one element that hasn't been visited, false otherwise.
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext()
		{
			lock.lock();
			try
			{
				return entryIterator.hasNext() || ((listIterator != null) && listIterator.hasNext());
			}
			finally
			{
				lock.unlock();
			}
		}

		/**
		 * Get the next element for this iterator.
		 * @return the next element as a <code>JPPFTaskBundle</code> instance.
		 * @see java.util.Iterator#next()
		 */
		public BundleWrapper next()
		{
			lock.lock();
			try
			{
				if (listIterator != null)
				{
					if (listIterator.hasNext()) return listIterator.next();
					if (entryIterator.hasNext())
					{
						listIterator = entryIterator.next().getValue().iterator();
						if (listIterator.hasNext()) return listIterator.next();
					}
				}
				throw new NoSuchElementException("no more element in this BundleIterator");
			}
			finally
			{
				lock.unlock();
			}
		}

		/**
		 * This operation is not supported and throws an <code>UnsupportedOperationException</code>.
		 * @throws UnsupportedOperationException as this operation is not supported.
		 * @see java.util.Iterator#remove()
		 */
		public void remove() throws UnsupportedOperationException
		{
			throw new UnsupportedOperationException("remove() is not supported on a BundleIterator");
		}
	}

	/**
	 * Action triggered when a job reaches its scheduled execution date.
	 */
	public class JobScheduleAction implements Runnable
	{
		/**
		 * The bundle wrapper encapsulating the job.
		 */
		private BundleWrapper bundleWrapper = null;

		/**
		 * Initialize this action witht he specified bundle wrapper.
		 * @param bundleWrapper the bundle wrapper encapsulating the job.
		 */
		public JobScheduleAction(BundleWrapper bundleWrapper)
		{
			this.bundleWrapper = bundleWrapper;
		}

		/**
		 * Execute this action.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			synchronized(bundleWrapper)
			{
				bundleWrapper.getBundle().setParameter(BundleParameter.JOB_PENDING, false);
				jobManager.jobUpdated(bundleWrapper);
			}
		}
	}
}
