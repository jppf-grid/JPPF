/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import org.jppf.node.protocol.*;
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
  private TreeMap<JPPFPriority, List<ServerJob>> priorityMap = new TreeMap<JPPFPriority, List<ServerJob>>();
  /**
   * Contains the ids of all queued jobs.
   */
  private Map<String, Map<String, ServerJob>> jobMap = new HashMap<String, Map<String, ServerJob>>();
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
  @Override
  public void addBundle(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    if (debugEnabled) log.debug("processing bundle " + bundle);
    JobSLA sla = bundle.getSLA();
    if (sla.isBroadcastJob() && (bundle.getParameter(BundleParameter.NODE_BROADCAST_UUID) == null))
    {
      if (debugEnabled) log.debug("before processing broadcast job " + bundle);
      processBroadcastJob(bundleWrapper);
      return;
    }
    try
    {
      lock.lock();
      ServerJob other = null;
      String jobUuid = bundle.getUuid();
      Map<String, ServerJob> map = jobMap.get(jobUuid);
      if (map != null) other = map.get(bundle.getUuidPath().toString());
      if (other != null)
      {
        JPPFTaskBundle otherBundle = (JPPFTaskBundle) other.getJob();
        if (otherBundle.getCompletionListener() == bundle.getCompletionListener())
        {
          ((BundleWrapper) other).merge(bundleWrapper, false);
          if (debugEnabled) log.debug("re-submitting bundle " + bundle);
          bundle.setParameter(BundleParameter.REAL_TASK_COUNT, bundle.getTaskCount());
          fireQueueEvent(new QueueEvent(this, other, true));
        }
      }
      else
      {
        bundle.setQueueEntryTime(System.currentTimeMillis());
        putInListMap(new JPPFPriority(sla.getPriority()), bundleWrapper, priorityMap);
        putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
        Boolean requeued = (Boolean) bundle.removeParameter(BundleParameter.JOB_REQUEUE);
        if (requeued == null) requeued = false;
        if (debugEnabled) log.debug("adding bundle " + bundle);
        if (!requeued)
        {
          handleStartJobSchedule(bundleWrapper);
          handleExpirationJobSchedule(bundleWrapper);
        }
        if (map == null)
        {
          map = new HashMap<String, ServerJob>();
          jobMap.put(jobUuid, map);
        }
        map.put(bundle.getUuidPath().asString(), bundleWrapper);
        fireQueueEvent(new QueueEvent(this, bundleWrapper, requeued));
      }
      updateLatestMaxSize();
    }
    finally
    {
      lock.unlock();
    }
    if (debugEnabled) log.debug("finished processing bundle " + bundle);
    statsManager.taskInQueue(bundle.getTaskCount());
  }

  /**
   * Get the next object in the queue.
   * @param nbTasks the maximum number of tasks to get out of the bundle.
   * @return the most recent object that was added to the queue.
   * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(int)
   */
  @Override
  public ServerJob nextBundle(final int nbTasks)
  {
    Iterator<ServerJob> it = iterator();
    return it.hasNext() ? nextBundle(it.next(),  nbTasks) : null;
  }

  /**
   * Get the next object in the queue.
   * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
   * @param nbTasks the maximum number of tasks to get out of the bundle.
   * @return the most recent object that was added to the queue.
   * @see org.jppf.server.queue.AbstractJPPFQueue#nextBundle(org.jppf.server.protocol.BundleWrapper, int)
   */
  @Override
  public ServerJob nextBundle(final ServerJob bundleWrapper, final int nbTasks)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    ServerJob result = null;
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
        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, 0);
      }
      else
      {
        if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle " + bundle);
        result = ((BundleWrapper) bundleWrapper).copy(nbTasks);
        int newSize = bundle.getTaskCount();
        List<ServerJob> list = sizeMap.get(newSize);
        if (list == null)
        {
          list = new ArrayList<ServerJob>();
          //sizeMap.put(newSize, list);
          sizeMap.put(size, list);
        }
        list.add(bundleWrapper);
        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, bundle.getTaskCount());
        List<ServerJob> bundleList = priorityMap.get(new JPPFPriority(bundle.getSLA().getPriority()));
        bundleList.remove(bundleWrapper);
        bundleList.add(bundleWrapper);
      }
      updateLatestMaxSize();
      jobManager.jobUpdated(bundleWrapper);
      //result.getBundle().setExecutionStartTime(System.currentTimeMillis());
    }
    finally
    {
      lock.unlock();
    }
    JPPFTaskBundle resultJob = (JPPFTaskBundle) result.getJob();
    if (debugEnabled) log.debug("next job is " + resultJob);
    statsManager.taskOutOfQueue(resultJob.getTaskCount(), System.currentTimeMillis() - resultJob.getQueueEntryTime());
    return result;
  }

  /**
   * Determine whether the queue is empty or not.
   * @return true if the queue is empty, false otherwise.
   * @see org.jppf.server.queue.JPPFQueue#isEmpty()
   */
  @Override
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
  @Override
  public int getMaxBundleSize()
  {
    lock.lock();
    try
    {
      return latestMaxSize;
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Update the value of the max bundle size.
   */
  private void updateLatestMaxSize()
  {
    latestMaxSize = sizeMap.isEmpty() ? latestMaxSize : sizeMap.lastKey();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServerJob removeBundle(final ServerJob bundleWrapper)
  {
    lock.lock();
    try
    {
      JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
      if (debugEnabled) log.debug("removing bundle from queue: " + bundle);
      removeFromListMap(new JPPFPriority(bundle.getSLA().getPriority()), bundleWrapper, priorityMap);
      ServerJob result = null;
      String uuid = bundle.getUuid();
      Map<String, ServerJob> map = jobMap.get(uuid);
      if (map != null)
      {
        result = map.remove(bundle.getUuidPath().asString());
        if (map.isEmpty()) jobMap.remove(uuid);
      }
      return result;
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
  @Override
  public Iterator<ServerJob> iterator()
  {
    return new BundleIterator(priorityMap, lock);
  }

  /**
   * Process the start schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  private void handleStartJobSchedule(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    JPPFSchedule schedule = bundle.getSLA().getJobSchedule();
    if (schedule != null)
    {
      bundle.setParameter(BundleParameter.JOB_PENDING, true);
      String jobId = bundle.getName();
      String uuid = bundle.getUuid();
      if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + jobId);
      try
      {
        long dt = (Long) bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME);
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
  private void handleExpirationJobSchedule(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    bundle.setParameter(BundleParameter.JOB_EXPIRED, false);
    JPPFSchedule schedule = bundle.getSLA().getJobExpirationSchedule();
    if (schedule != null)
    {
      String jobId = (String) bundle.getName();
      String uuid = bundle.getUuid();
      if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + jobId);
      long dt = (Long) bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME);
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
  public void clearSchedules(final String jobUuid)
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
  private void processBroadcastJob(final ServerJob bundleWrapper)
  {
    JPPFDistributedJob bundle = bundleWrapper.getJob();
    Map<String, JPPFManagementInfo> uuidMap = JPPFDriver.getInstance().getNodeHandler().getUuidMap();
    if (uuidMap.isEmpty())
    {
      ((JPPFTaskBundle) bundle).fireTaskCompleted(bundleWrapper);
      return;
    }
    BroadcastJobCompletionListener completionListener = new BroadcastJobCompletionListener(bundleWrapper, uuidMap.keySet());
    JobSLA sla = bundle.getSLA();
    ExecutionPolicy policy = sla.getExecutionPolicy();
    List<ServerJob> jobList = new ArrayList<ServerJob>(uuidMap.size());
    for (Map.Entry<String, JPPFManagementInfo> entry: uuidMap.entrySet())
    {
      BundleWrapper job = ((BundleWrapper) bundleWrapper).copy();
      JPPFTaskBundle newBundle = (JPPFTaskBundle) job.getJob();
      String uuid = entry.getKey();
      JPPFManagementInfo info = entry.getValue();
      newBundle.setParameter(BundleParameter.NODE_BROADCAST_UUID, uuid);
      if ((policy != null) && !policy.accepts(info.getSystemInfo())) continue;
      ExecutionPolicy broadcastPolicy = new Equal("jppf.uuid", true, uuid);
      if (policy != null) broadcastPolicy = broadcastPolicy.and(policy);
      newBundle.setSLA(((JPPFJobSLA) sla).copy());
      newBundle.getSLA().setExecutionPolicy(broadcastPolicy);
      newBundle.setCompletionListener(completionListener);
      newBundle.setName(bundle.getName() + " [node: " + info.toString() + ']');
      newBundle.setUuid(new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
      if (debugEnabled) log.debug("Execution policy for job uuid=" + newBundle.getUuid() + " :\n" + broadcastPolicy);
      jobList.add(job);
    }
    for (ServerJob job: jobList) addBundle(job);
  }

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid the uuid of the job to re-prioritize.
   * @param newPriority the new priority of the job.
   */
  public void updatePriority(final String jobUuid, final int newPriority)
  {
    lock.lock();
    try
    {
      Map<String, ServerJob> map = jobMap.get(jobUuid);
      if (map == null) return;
      for (Map.Entry<String, ServerJob> entry: map.entrySet())
      {
        ServerJob job = entry.getValue();
        if (job == null) return;
        int oldPriority = job.getJob().getSLA().getPriority();
        if (oldPriority != newPriority)
        {
          job.getJob().getSLA().setPriority(newPriority);
          removeFromListMap(new JPPFPriority(oldPriority), job, priorityMap);
          putInListMap(new JPPFPriority(newPriority), job, priorityMap);
          jobManager.jobUpdated(job);
        }
      }
    }
    finally
    {
      lock.unlock();
    }
  }
}
