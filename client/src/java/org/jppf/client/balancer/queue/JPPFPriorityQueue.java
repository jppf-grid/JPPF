/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import static org.jppf.utils.CollectionUtils.*;

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

import org.jppf.client.JPPFJob;
import org.jppf.client.balancer.*;
import org.jppf.client.submission.SubmissionStatus;
import org.jppf.execute.ExecutorStatus;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.JobSLA;
import org.jppf.queue.*;
import org.jppf.scheduling.*;
import org.jppf.server.protocol.JPPFJobSLA;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue<ClientJob, ClientJob, ClientTaskBundle>
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFPriorityQueue.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The job submission manager
   */
  private final SubmissionManagerClient submissionManager;
  /**
   * Handles the schedule of each job that has one.
   */
  private final JPPFScheduleHandler jobScheduleHandler = new JPPFScheduleHandler("Job Schedule Handler");
  /**
   * Handles the expiration schedule of each job that has one.
   */
  private final JPPFScheduleHandler jobExpirationHandler = new JPPFScheduleHandler("Job Expiration Handler");
  /**
   * A priority queue holding broadcast jobs that could not be sent due to no available connection.
   */
  private final PriorityBlockingQueue<ClientJob> pendingBroadcasts = new PriorityBlockingQueue<ClientJob>(16, new JobPriorityComparator());

  /**
   * Initialize this queue.
   * @param submissionManager reference to submission manager.
   */
  public JPPFPriorityQueue(final SubmissionManagerClient submissionManager)
  {
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
    final String jobUuid = bundleWrapper.getUuid();
    if (sla.isBroadcastJob() && (bundleWrapper.getBroadcastUUID() == null))
    {
      if (debugEnabled) log.debug("before processing broadcast job " + bundleWrapper.getJob());
      processBroadcastJob(bundleWrapper);
    } else {
      lock.lock();
      try {
        ClientJob other = jobMap.get(jobUuid);
        if (other != null) throw new IllegalStateException("Job " + jobUuid + " already enqueued");
        bundleWrapper.addOnDone(new Runnable()
        {
          @Override
          public void run()
          {
            lock.lock();
            try {
              jobMap.remove(jobUuid);
              removeBundle(bundleWrapper);
            } finally {
              lock.unlock();
            }
          }
        });
        bundleWrapper.setSubmissionStatus(SubmissionStatus.PENDING);
        bundleWrapper.setQueueEntryTime(System.currentTimeMillis());
        bundleWrapper.setJobReceivedTime(bundleWrapper.getQueueEntryTime());

        if (!sla.isBroadcastJob() || bundleWrapper.getBroadcastUUID() != null) {
          priorityMap.putValue(sla.getPriority(), bundleWrapper);
          sizeMap.putValue(getSize(bundleWrapper), bundleWrapper);
          if (debugEnabled) log.debug("adding bundle with " + bundleWrapper);
          handleStartJobSchedule(bundleWrapper);
          handleExpirationJobSchedule(bundleWrapper);
        }
        jobMap.put(jobUuid, bundleWrapper);
        updateLatestMaxSize();
        fireQueueEvent(new QueueEvent<ClientJob, ClientJob, ClientTaskBundle>(this, bundleWrapper, false));
        if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Handle requeue of the specified job.
   * @param job the job to requeue.
   */
  protected void requeue(final ClientJob job) {
    lock.lock();
    try {
      if (!jobMap.containsKey(job.getUuid())) throw new IllegalStateException("Job not managed");

      priorityMap.putValue(job.getSLA().getPriority(), job);
      sizeMap.putValue(getSize(job), job);
      fireQueueEvent(new QueueEvent<ClientJob, ClientJob, ClientTaskBundle>(this, job, true));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ClientTaskBundle nextBundle(final int nbTasks)
  {
    Iterator<ClientJob> it = iterator();
    return it.hasNext() ? nextBundle(it.next(), nbTasks) : null;
  }

  @Override
  public ClientTaskBundle nextBundle(final ClientJob bundleWrapper, final int nbTasks)
  {
    final ClientTaskBundle result;
    lock.lock();
    try {
      if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks, next bundle has " + bundleWrapper.getTaskCount() + " tasks");
      int size = getSize(bundleWrapper);
      sizeMap.removeValue(size, bundleWrapper);
      if (nbTasks >= bundleWrapper.getTaskCount())
      {
        bundleWrapper.setOnRequeue(new Runnable()
        {
          @Override
          public void run()
          {
            requeue(bundleWrapper);
          }
        });
        result = bundleWrapper.copy(bundleWrapper.getTaskCount());
        removeBundle(bundleWrapper);
      }
      else
      {
        if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
        result = bundleWrapper.copy(nbTasks);
        int newSize = bundleWrapper.getTaskCount();
        sizeMap.putValue(size, bundleWrapper);
        // to ensure that other jobs with same priority are also processed without waiting
        priorityMap.moveToEndOfList(bundleWrapper.getSLA().getPriority(), bundleWrapper);
      }
      updateLatestMaxSize();
      if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    } finally {
      lock.unlock();
    }
    return result;
  }

  @Override
  public boolean isEmpty()
  {
    lock.lock();
    try {
      return priorityMap.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get the bundle size to use for bundle size tuning.
   * @param bundleWrapper the bundle to get the size from.
   * @return the bundle size as an int.
   */
  protected int getSize(final ClientJob bundleWrapper)
  {
    //return bundle.getTaskCount();
    return bundleWrapper.getJob().getTasks().size();
  }

  @Override
  public ClientJob removeBundle(final ClientJob bundleWrapper)
  {
    lock.lock();
    try {
      if (debugEnabled) log.debug("removing bundle from queue, jobId=" + bundleWrapper.getName());
      priorityMap.removeValue(bundleWrapper.getSLA().getPriority(), bundleWrapper);
      return null;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Process the start schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  private void handleStartJobSchedule(final ClientJob bundleWrapper)
  {
    JPPFSchedule schedule = bundleWrapper.getClientSLA().getJobSchedule();
    if (schedule != null)
    {
      bundleWrapper.setPending(true);
      String jobId = bundleWrapper.getName();
      final String uuid = bundleWrapper.getUuid();
      if (debugEnabled) log.debug("found start " + schedule + " for jobId = " + jobId);
      try
      {
        long dt = bundleWrapper.getJobReceivedTime();
        jobScheduleHandler.scheduleAction(uuid, schedule, new JobScheduleAction(bundleWrapper), dt);
        bundleWrapper.addOnDone(new Runnable()
        {
          @Override
          public void run()
          {
            jobScheduleHandler.cancelAction(uuid);
          }
        });
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
    JPPFSchedule schedule = bundleWrapper.getClientSLA().getJobExpirationSchedule();
    if (schedule != null)
    {
      String jobId = bundleWrapper.getName();
      final String uuid = bundleWrapper.getUuid();
      if (debugEnabled) log.debug("found expiration " + schedule + " for jobId = " + jobId);
      long dt = bundleWrapper.getJobReceivedTime();
      try
      {
        jobExpirationHandler.scheduleAction(uuid, schedule, new JobExpirationAction(bundleWrapper), dt);
        bundleWrapper.addOnDone(new Runnable()
        {
          @Override
          public void run()
          {
            jobExpirationHandler.cancelAction(uuid);
          }
        });
      }
      catch (ParseException e)
      {
        log.error("Unparsable expiration date for job id " + jobId + " : date = " + schedule.getDate() +
                ", date format = " + (schedule.getFormat() == null ? "null" : schedule.getFormat()), e);
      }
    }
  }

  /**
   * Process the specified broadcast job.
   * This consists in creating one job per node, each containing the same tasks,
   * and with an execution policy that enforces its execution ont he designated node only.
   * @param bundleWrapper the broadcast job to process.
   */
  private void processBroadcastJob(final ClientJob bundleWrapper)
  {
    JPPFJob bundle = bundleWrapper.getJob();
    List<ChannelWrapper> connections = submissionManager.getAllConnections();
    if (connections.isEmpty())
    {
      //bundleWrapper.taskCompleted(null, null);
      pendingBroadcasts.offer(bundleWrapper);
      return;
    }
    JobSLA sla = bundle.getSLA();
    List<ClientJob> jobList = new ArrayList<ClientJob>(connections.size());

    Set<String> uuidSet = new HashSet<String>();
    for (ChannelWrapper connection : connections)
    {
      ExecutorStatus status = connection.getExecutionStatus();
      if (status == ExecutorStatus.ACTIVE || status == ExecutorStatus.EXECUTING)
      {
        String uuid = connection.getUuid();
        if (uuid != null && uuid.length() > 0 && uuidSet.add(uuid))
        {
          ClientJob newBundle = bundleWrapper.createBroadcastJob(uuid);
          JPPFManagementInfo info = connection.getManagementInfo();
          newBundle.setSLA(((JPPFJobSLA) sla).copy());
          newBundle.setMetadata(bundle.getMetadata());
          newBundle.setName(bundle.getName() + " [driver: " + info.toString() + ']');
          newBundle.setUuid(new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
          jobList.add(newBundle);
        }
      }
    }
    if (jobList.isEmpty()) bundleWrapper.taskCompleted(null, null);
    else {
      final String jobUuid = bundleWrapper.getUuid();
      lock.lock();
      try {
        ClientJob other = jobMap.get(jobUuid);
        if (other != null) throw new IllegalStateException("Job " + jobUuid + " already enqueued");

        bundleWrapper.addOnDone(new Runnable() {
          @Override
          public void run() {
            lock.lock();
            try {
              jobMap.remove(jobUuid);
              removeBundle(bundleWrapper);
            } finally {
              lock.unlock();
            }
          }
        });
        bundleWrapper.setSubmissionStatus(SubmissionStatus.PENDING);
        bundleWrapper.setQueueEntryTime(System.currentTimeMillis());
        bundleWrapper.setJobReceivedTime(bundleWrapper.getQueueEntryTime());

        jobMap.put(jobUuid, bundleWrapper);
        fireQueueEvent(new QueueEvent<ClientJob, ClientJob, ClientTaskBundle>(this, bundleWrapper, false));
        for (ClientJob job : jobList) addBundle(job);
      } finally {
        lock.unlock();
      }
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
        priorityMap.removeValue(oldPriority, job);
        priorityMap.putValue(newPriority, job);
      }
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Cancel the job with the specified UUID
   * @param jobId the uuid of the job to cancel.
   * @return whether cancellation was successful.
   */
  public boolean cancelJob(final String jobId)
  {
    lock.lock();
    try
    {
      ClientJob job = jobMap.get(jobId);
      if (job == null)
        return false;
      else
        return job.cancel(false);
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Close this queue and all resources it uses.
   */
  public void close()
  {
    lock.lock();
    try {
      jobScheduleHandler.clear(true);
      jobExpirationHandler.clear(true);
      pendingBroadcasts.clear();
      jobMap.clear();
      priorityMap.clear();
      sizeMap.clear();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Cancels queued broadcast jobs for connection.
   * @param connectionUUID The connection UUID that failed or was disconnected.
   */
  public void cancelBroadcastJobs(final String connectionUUID)
  {
    if (connectionUUID == null || connectionUUID.isEmpty()) return;

    Set<String> jobIDs = Collections.emptySet();
    lock.lock();
    try {
      if (jobMap.isEmpty()) return;

      jobIDs = new HashSet<String>();
      for (Map.Entry<String, ClientJob> entry : jobMap.entrySet())
      {
        if (connectionUUID.equals(entry.getValue().getBroadcastUUID())) jobIDs.add(entry.getKey());
      }
    } finally {
      lock.unlock();
    }
    for (String jobID : jobIDs) cancelJob(jobID);
  }

  /**
   * Process the jobs in the pending broadcast queue.
   * This method is normally called from <code>TaskQueueChecker.dispatch()</code>.
   */
  public void processPendingBroadcasts() {
    if (!submissionManager.hasWorkingConnection()) return;
    ClientJob clientJob;
    while ((clientJob = pendingBroadcasts.poll()) != null)
    {
      if (debugEnabled) log.debug("queuing job " + clientJob.getJob());
      addBundle(clientJob);
    }
  }
}
