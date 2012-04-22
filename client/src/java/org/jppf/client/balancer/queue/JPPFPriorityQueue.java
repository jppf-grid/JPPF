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

package org.jppf.client.balancer.queue;

import org.jppf.client.balancer.ChannelWrapper;
import org.jppf.client.balancer.ClientJob;
import org.jppf.client.balancer.ClientTaskBundle;
import org.jppf.client.balancer.SubmissionManagerClient;
import org.jppf.client.balancer.job.JPPFJobManager;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.Equal;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.node.protocol.JobSLA;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.server.protocol.JPPFJobSLA;
import org.jppf.utils.JPPFUuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.*;

import static org.jppf.utils.CollectionUtils.*;

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
  private TreeMap<JPPFPriority, List<ClientJob>> priorityMap = new TreeMap<JPPFPriority, List<ClientJob>>();
  /**
   * Contains the ids of all queued jobs.
   */
  private Map<String, ClientJob> jobMap = new HashMap<String, ClientJob>();
//  /**
//   * The driver stats manager.
//   */
//  protected JPPFDriverStatsManager statsManager = null;
  /**
   * The job manager.
   */
  protected JPPFJobManager jobManager = null;
  /**
   * The job submission manager
   */
  private final SubmissionManagerClient submissionManager;
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
   * @param jobManager        reference to job manager.
   * @param submissionManager reference to submission manager.
   */
  public JPPFPriorityQueue(final JPPFJobManager jobManager, final SubmissionManagerClient submissionManager)
  {
//    this.statsManager = JPPFDriver.getInstance().getStatsManager();
    this.jobManager = jobManager;
    this.submissionManager = submissionManager;
  }

  /**
   * Add an object to the queue, and notify all listeners about it.
   * @param bundleWrapper the object to add to the queue.
   */
  @Override
  public void addBundle(final ClientJob bundleWrapper)
  {
    JobSLA sla = bundleWrapper.getSLA();
    String jobUuid = bundleWrapper.getUuid();
    if (sla.isBroadcastJob() && (bundleWrapper.getBroadcastUUID() == null))
    {
      if (debugEnabled)
      {
        log.debug("before processing broadcast job with id=" + bundleWrapper.getName() + ", uuid=" + jobUuid + ", task count=" + bundleWrapper.getTaskCount());
      }
      processBroadcastJob(bundleWrapper);
      return;
    }
    try
    {
      lock.lock();
      ClientJob other = jobMap.get(jobUuid);
      if (other != null)
      {
        other.merge(bundleWrapper, false);
        if (debugEnabled) log.debug("re-submitting bundle with " + bundleWrapper);
//        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, bundleWrapper.getTaskCount());
        fireQueueEvent(new QueueEvent(this, other, true));
      }
      else
      {
        bundleWrapper.setSubmissionStatus(SubmissionStatus.PENDING);
        bundleWrapper.setQueueEntryTime(System.currentTimeMillis());
        putInListMap(new JPPFPriority(sla.getPriority()), bundleWrapper, priorityMap);
        putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
        boolean requeued = bundleWrapper.isRequeued(); // Boolean.TRUE.equals(bundle.removeParameter(BundleParameter.JOB_REQUEUE));
        if (debugEnabled) log.debug("adding bundle with " + bundleWrapper);
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
    if (debugEnabled)
    {
      log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    }
//    statsManager.taskInQueue(bundle.getTaskCount());
  }

  @Override
  protected void fireQueueEvent(final QueueEvent event)
  {
    super.fireQueueEvent(event);
    if (!event.isRequeued())
    {
      jobManager.jobQueued(event.getBundleWrapper());
    }
    else
    {
      jobManager.jobUpdated(event.getBundleWrapper());
    }
  }

  /**
   * Get the next object in the queue.
   * @param nbTasks the maximum number of tasks to get out of the bundle.
   * @return the most recent object that was added to the queue.
   */
  @Override
  public ClientTaskBundle nextBundle(final int nbTasks)
  {
    Iterator<ClientJob> it = iterator();
    return it.hasNext() ? nextBundle(it.next(), nbTasks) : null;
  }

  /**
   * Get the next object in the queue.
   * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
   * @param nbTasks       the maximum number of tasks to get out of the bundle.
   * @return the most recent object that was added to the queue.
   */
  @Override
  public ClientTaskBundle nextBundle(final ClientJob bundleWrapper, final int nbTasks)
  {
    final ClientTaskBundle result;
    try
    {
      lock.lock();
      if (debugEnabled)
      {
        log.debug("requesting bundle with " + nbTasks + " tasks, next bundle has " + bundleWrapper.getTaskCount() + " tasks");
      }
      int size = getSize(bundleWrapper);
      removeFromListMap(size, bundleWrapper, sizeMap);
      if (nbTasks >= bundleWrapper.getTaskCount())
      {
        result = bundleWrapper.copy(bundleWrapper.getTaskCount());
        removeBundle(bundleWrapper);
//        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, 0);
      }
      else
      {
        if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
        result = bundleWrapper.copy(nbTasks);
        int newSize = bundleWrapper.getTaskCount();
        List<ClientJob> list = sizeMap.get(newSize);
        if (list == null)
        {
          list = new ArrayList<ClientJob>();
          //sizeMap.put(newSize, list);
          sizeMap.put(size, list);
        }
        list.add(bundleWrapper);
//        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, bundleWrapper.getTaskCount());
        List<ClientJob> bundleList = priorityMap.get(new JPPFPriority(bundleWrapper.getSLA().getPriority()));
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
    if (debugEnabled)
    {
      log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " +
              formatSizeMapInfo("sizeMap", sizeMap));
    }
//    ClientTaskBundle resultJob = (ClientTaskBundle) result.getJob();
//    statsManager.taskOutOfQueue(resultJob.getTaskCount(), System.currentTimeMillis() - resultJob.getQueueEntryTime());
    result.setOnRequeue(new Runnable() {
      @Override
      public void run()
      {
        ClientJob clientJob = new ClientJob(result.getJob(), result.getTasksL());
        clientJob.setRequeued(true);
        addBundle(clientJob);
      }
    });
    return result;
  }

  /**
   * Determine whether the queue is empty or not.
   * @return true if the queue is empty, false otherwise.
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
   */
  @Override
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
  @Override
  public ClientJob removeBundle(final ClientJob bundleWrapper)
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("removing bundle from queue, jobId=" + bundleWrapper.getName());
      removeFromListMap(new JPPFPriority(bundleWrapper.getSLA().getPriority()), bundleWrapper, priorityMap);
      return jobMap.remove(bundleWrapper.getUuid());
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Get an iterator on the task bundles in this queue.
   * @return an iterator.
   * @see Iterable#iterator()
   */
  @Override
  public Iterator<ClientJob> iterator()
  {
    return new BundleIterator(priorityMap, lock);
  }

  /**
   * Process the start schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  private void handleStartJobSchedule(final ClientJob bundleWrapper)
  {
    JPPFSchedule schedule = bundleWrapper.getSLA().getJobSchedule();
    if (schedule != null)
    {
      bundleWrapper.setPending(true);
      String jobId = bundleWrapper.getName();
      String uuid = bundleWrapper.getUuid();
      if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + jobId);
      try
      {
        long dt = bundleWrapper.getJobReceivedTime();
        jobScheduleHandler.scheduleAction(uuid, schedule, new JobScheduleAction(bundleWrapper, jobManager), dt);
      }
      catch (ParseException e)
      {
        bundleWrapper.setPending(false);
        log.error("Unparseable start date for job id " + jobId + " : date = " + schedule.getDate() +
                ", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
      }
    }
    else
    {
      bundleWrapper.setPending(false);
    }
  }

  /**
   * Process the expiration schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  private void handleExpirationJobSchedule(final ClientJob bundleWrapper)
  {
    bundleWrapper.setJobExpired(false);
    JPPFSchedule schedule = bundleWrapper.getSLA().getJobExpirationSchedule();
    if (schedule != null)
    {
      String jobId = bundleWrapper.getName();
      String uuid = bundleWrapper.getUuid();
      if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + jobId);
      long dt = bundleWrapper.getJobReceivedTime();
      try
      {
        jobExpirationHandler.scheduleAction(uuid, schedule, new JobExpirationAction(bundleWrapper), dt);
      }
      catch (ParseException e)
      {
        bundleWrapper.setJobExpired(false);
        log.error("Unparsable expiration date for job id " + jobId + " : date = " + schedule.getDate() +
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
  private void processBroadcastJob(final ClientJob bundleWrapper)
  {
    JPPFDistributedJob bundle = bundleWrapper.getJob();
    List<ChannelWrapper> connections = submissionManager.getAllConnections();
//    Map<String, JPPFManagementInfo> uuidMap = JPPFDriver.getInstance().getNodeHandler().getUuidMap();
    if (connections.isEmpty())
    {
      bundleWrapper.fireTaskCompleted();
      return;
    }
    BroadcastJobCompletionListener completionListener = new BroadcastJobCompletionListener(bundleWrapper, connections);
    JobSLA sla = bundle.getSLA();
    ExecutionPolicy policy = sla.getExecutionPolicy();
    List<ClientJob> jobList = new ArrayList<ClientJob>(connections.size());

    Set<String> uuidSet = new HashSet<String>();
    for (ChannelWrapper connection : connections)
    {
      String uuid = connection.getUuid();
      if (uuidSet.add(uuid))
      {
        ClientJob job = bundleWrapper.copy();
        ClientJob newBundle = job;

//        System.out.println(" sending to connection '" + uuid + "' - policy: " + policy);
        JPPFManagementInfo info = connection.getManagementInfo();
        newBundle.setBroadcastUUID(uuid);
        if ((policy != null) && !policy.accepts(connection.getSystemInfo()))
        {
//          System.out.println("Policy not accepted");
          continue;
        }
        ExecutionPolicy broadcastPolicy = new Equal("jppf.uuid", true, uuid);
        newBundle.setSLA(((JPPFJobSLA) sla).copy());
//        newBundle.setLocalExecutionPolicy(broadcastPolicy);
        job.setCompletionListener(completionListener);
        newBundle.setName(bundle.getName() + " [node: " + info.toString() + ']');
        newBundle.setUuid(new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
        if (debugEnabled) log.debug("Execution policy for job uuid=" + newBundle.getUuid() + " :\n" + broadcastPolicy);
        jobList.add(job);
      }
    }
    if (jobList.isEmpty())
    {
      bundleWrapper.fireTaskCompleted();
    }
    else
    {
      for (ClientJob job : jobList) addBundle(job);
    }
  }

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid     the uuid of the job to re-prioritize.
   * @param newPriority the new priority of the job.
   */
  public void updatePriority(final String jobUuid, final int newPriority)
  {
    lock.lock();
    try
    {
      ClientJob job = jobMap.get(jobUuid);
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
    finally
    {
      lock.unlock();
    }
  }
}
