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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.execute.ExecutorStatus;
import org.jppf.job.*;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.JPPFDriverStatsManager;
import org.jppf.server.job.JobManager;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.slf4j.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue implements JobManager, JobNotificationEmitter
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
   * Comparator for job priority.
   */
  private static final Comparator<Integer> PRIORITY_COMPARATOR = new Comparator<Integer>() {
    @Override
    public int compare(final Integer o1, final Integer o2) {
      if (o1 == null) return (o2 == null) ? 0 : 1;
      else if (o2 == null) return -1;
      return o2.compareTo(o1);
    }
  };
  /**
   * A map of task bundles, ordered by descending priority.
   */
  private final TreeMap<Integer, List<ServerJob>> priorityMap = new TreeMap<Integer, List<ServerJob>>(PRIORITY_COMPARATOR);
  /**
   * Contains the ids of all queued jobs.
   */
  final Map<String, ServerJob> jobMap = new HashMap<String, ServerJob>();
  /**
   * The driver stats manager
   */
  private final JPPFDriverStatsManager statsManager;
  /**
   * The list of registered job listeners.
   */
  private final List<JobListener> jobListeners = new ArrayList<JobListener>();
  /**
   * Manages jobs start and expiration scheduling.
   */
  private final ScheduleManager scheduleManager = new ScheduleManager();
  /**
   * Manages operations on broadcast jons.
   */
  private final BroadcastJobManager broadcastJobManager;
  /**
   * Counts the current number of connections with ACTIVE or EXECUTING status.
   */
  private final AtomicInteger nbWorkingConnections = new AtomicInteger(0);

  /**
   * Initialize this queue.
   * @param statsManager reference to statistics manager.
   */
  public JPPFPriorityQueue(final JPPFDriverStatsManager statsManager)
  {
    this.statsManager = statsManager;
    broadcastJobManager = new BroadcastJobManager(this);
  }

  /**
   * Set the callable source for all available connections.
   * @param callableAllConnections a {@link Callable} instance.
   */
  public void setCallableAllConnections(final Callable<List<AbstractNodeContext>> callableAllConnections) {
    broadcastJobManager.setCallableAllConnections(callableAllConnections);
  }

  @Override
  public void addBundle(final ServerJob bundleWrapper)
  {
    JobSLA sla = bundleWrapper.getSLA();
    final String jobUuid = bundleWrapper.getUuid();
    if (sla.isBroadcastJob() && (bundleWrapper.getBroadcastUUID() == null))
    {
      if (debugEnabled) log.debug("before processing broadcast job " + bundleWrapper.getJob());
      broadcastJobManager.processBroadcastJob(bundleWrapper);
    } else {
      lock.lock();
      try
      {
        ServerJob other = jobMap.get(jobUuid);
        if (other != null) throw new IllegalStateException("Job " + jobUuid + " already enqueued");
        bundleWrapper.addOnDone(new Runnable()
        {
          @Override
          public void run()
          {
            lock.lock();
            try
            {
              jobMap.remove(jobUuid);
              removeBundle(bundleWrapper);
            }
            finally
            {
              lock.unlock();
            }
          }
        });
        bundleWrapper.setSubmissionStatus(SubmissionStatus.PENDING);
        bundleWrapper.setQueueEntryTime(System.currentTimeMillis());
        bundleWrapper.setJobReceivedTime(bundleWrapper.getQueueEntryTime());

        if(!sla.isBroadcastJob() || bundleWrapper.getBroadcastUUID() != null) {
          putInListMap(sla.getPriority(), bundleWrapper, priorityMap);
          putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
          if (debugEnabled) log.debug("adding bundle with " + bundleWrapper);
          scheduleManager.handleStartJobSchedule(bundleWrapper);
          scheduleManager.handleExpirationJobSchedule(bundleWrapper);
        }
        jobMap.put(jobUuid, bundleWrapper);
        updateLatestMaxSize();
        fireQueueEvent(new QueueEvent(this, bundleWrapper, false));
      }
      finally
      {
        lock.unlock();
      }
    }
    if (debugEnabled)
    {
      log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    }
    statsManager.taskInQueue(bundleWrapper.getTaskCount());
  }

  /**
   * Handle requeuing of the specified job.
   * @param job the job to requeue.
   */
  protected void requeue(final ServerJob job) {
    lock.lock();
    try {
      if(!jobMap.containsKey(job.getUuid())) throw new IllegalStateException("Job not managed");

      putInListMap(job.getSLA().getPriority(), job, priorityMap);
      putInListMap(getSize(job), job, sizeMap);
      fireQueueEvent(new QueueEvent(this, job, true));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ServerTaskBundle nextBundle(final int nbTasks)
  {
    Iterator<ServerJob> it = iterator();
    return it.hasNext() ? nextBundle(it.next(),  nbTasks) : null;
  }

  @Override
  public ServerTaskBundle nextBundle(final ServerJob bundleWrapper, final int nbTasks)
  {
    final ServerTaskBundle result;
    lock.lock();
    try
    {
      if (debugEnabled)
      {
        log.debug("requesting bundle with " + nbTasks + " tasks, next bundle has " + bundleWrapper.getTaskCount() + " tasks");
      }
      int size = getSize(bundleWrapper);
      removeFromListMap(size, bundleWrapper, sizeMap);
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
//        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, 0);
      }
      else
      {
        if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
        result =  bundleWrapper.copy(nbTasks);
        int newSize = bundleWrapper.getTaskCount();
        List<ServerJob> list = sizeMap.get(newSize);
        if (list == null)
        {
          list = new ArrayList<ServerJob>();
          //sizeMap.put(newSize, list);
          sizeMap.put(size, list);
        }
        list.add(bundleWrapper);
//        bundle.setParameter(BundleParameter.REAL_TASK_COUNT, bundleWrapper.getTaskCount());
        List<ServerJob> bundleList = priorityMap.get(bundleWrapper.getSLA().getPriority());
        bundleList.remove(bundleWrapper);
        bundleList.add(bundleWrapper);
      }
      updateLatestMaxSize();
    }
    finally
    {
      lock.unlock();
    }
    if (debugEnabled)
    {
      log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    }
    statsManager.taskOutOfQueue(result.getTaskCount(), System.currentTimeMillis() - bundleWrapper.getQueueEntryTime());
    return result;
  }

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

  @Override
  public ServerJob removeBundle(final ServerJob bundleWrapper)
  {
    lock.lock();
    try
    {
      if (debugEnabled) log.debug("removing bundle from queue, jobId= " + bundleWrapper.getName());
      removeFromListMap(bundleWrapper.getSLA().getPriority(), bundleWrapper, priorityMap);
//      jobMap.remove(bundleWrapper.getUuid());
      return null;
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public Iterator<ServerJob> iterator()
  {
    return new BundleIterator(priorityMap, lock);
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
      ServerJob job = jobMap.get(jobUuid);
      if (job == null) return;
      int oldPriority = job.getJob().getSLA().getPriority();
      if (oldPriority != newPriority)
      {
        job.getJob().getSLA().setPriority(newPriority);
        removeFromListMap(oldPriority, job, priorityMap);
        putInListMap(newPriority, job, priorityMap);
        job.fireJobUpdated();
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
      ServerJob job = jobMap.get(jobId);
      if(job == null)
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
    try
    {
      scheduleManager.close();
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Get the job for the jobId.
   * @param jobId the uuid of the job.
   * @return a <code>ServerJob</code> instance.
   */
  public ServerJob getJob(final String jobId) {
    lock.lock();
    try
    {
      return jobMap.get(jobId);
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return a set of ids as strings.
   */
  @Override
  public Set<String> getAllJobIds() {
    lock.lock();
    try
    {
      return Collections.unmodifiableSet(jobMap.keySet());
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public void addJobListener(final JobListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (jobListeners) {
      jobListeners.add(listener);
    }
  }

  @Override
  public void removeJobListener(final JobListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (jobListeners) {
      jobListeners.remove(listener);
    }
  }

  @Override
  public ServerJob getBundleForJob(final String jobUuid) {
    return getJob(jobUuid);
  }

  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  @Override
  public void fireJobEvent(final JobNotification event)
  {
    if(event == null) throw new IllegalArgumentException("event is null");

    synchronized(jobListeners)
    {
      switch (event.getEventType())
      {
        case JOB_QUEUED:
          for (JobListener listener: jobListeners) listener.jobQueued(event);
          break;
        case JOB_ENDED:
          for (JobListener listener: jobListeners) listener.jobEnded(event);
          break;

        case JOB_UPDATED:
          for (JobListener listener: jobListeners) listener.jobUpdated(event);
          break;

        case JOB_DISPATCHED:
          for (JobListener listener: jobListeners) listener.jobDispatched(event);
          break;

        case JOB_RETURNED:
          for (JobListener listener: jobListeners) listener.jobReturned(event);
          break;

        default:
          throw new IllegalStateException("Unsupported event type: " + event.getEventType());
      }
    }
  }

  /**
   * Determine whether there is at east one connection, idle or not.
   * @return <code>true</code> if there is at least one connection, <code>false</code> otherwise.
   */
  public synchronized boolean hasWorkingConnection()
  {
    return nbWorkingConnections.get() > 0;
  }

  /**
   * Update count of working connections base on status change.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  public void updateWorkingConnections(final ExecutorStatus oldStatus, final ExecutorStatus newStatus) {
    boolean bNew = (newStatus == ExecutorStatus.ACTIVE) || (newStatus == ExecutorStatus.EXECUTING);
    boolean bOld = (oldStatus == ExecutorStatus.ACTIVE) || (oldStatus == ExecutorStatus.EXECUTING);
    if (bNew && !bOld) nbWorkingConnections.incrementAndGet();
    else if (!bNew && bOld) nbWorkingConnections.decrementAndGet();
  }

  /**
   * Cancels queued broadcast jobs for connection.
   * @param connectionUUID The connection UUID that failed or was disconnected.
   */
  public void cancelBroadcastJobs(final String connectionUUID)
  {
    broadcastJobManager.cancelBroadcastJobs(connectionUUID);
  }

  /**
   * Process the jobs in the pending broadcast queue.
   * This method is normally called from <code>TaskQueueChecker.dispatch()</code>.
   */
  public void processPendingBroadcasts()
  {
    broadcastJobManager.processPendingBroadcasts();
  }
}
