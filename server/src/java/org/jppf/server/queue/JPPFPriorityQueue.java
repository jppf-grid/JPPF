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
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.execute.ExecutorStatus;
import org.jppf.job.JobListener;
import org.jppf.job.JobNotification;
import org.jppf.job.JobNotificationEmitter;
import org.jppf.server.*;
import org.jppf.server.job.JobManager;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.*;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.utils.JPPFUuid;
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
   * An of task bundles, ordered by descending priority.
   */
  private final TreeMap<Integer, List<ServerJob>> priorityMap = new TreeMap<Integer, List<ServerJob>>(PRIORITY_COMPARATOR);
  /**
   * Contains the ids of all queued jobs.
   */
  private final Map<String, ServerJob> jobMap = new HashMap<String, ServerJob>();
  /**
   * The driver stats manager
   */
  private final JPPFDriverStatsManager statsManager;
  /**
   * The list of registered job listeners.
   */
  private final List<JobListener> jobListeners = new ArrayList<JobListener>();
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
  private final PriorityBlockingQueue<ServerJob> pendingBroadcasts = new PriorityBlockingQueue<ServerJob>(16, new JobPriorityComparator());
  /**
   * Counts the current number of connections with ACTIVE or EXECUTING status.
   */
  private final AtomicInteger nbWorkingConnections = new AtomicInteger(0);
  /**
   * Callback for getting all available connections. Used for processing broadcast jobs.
   */
  private Callable<List<AbstractNodeContext>> callableAllConnections = new Callable<List<AbstractNodeContext>>() {
    @Override
    public List<AbstractNodeContext> call() throws Exception {
      return Collections.emptyList();
    }
  };

  /**
   * Initialize this queue.
   * @param statsManager reference to statistics manager.
   */
  public JPPFPriorityQueue(final JPPFDriverStatsManager statsManager)
  {
    this.statsManager = statsManager;
  }

  /**
   * Set the callable source for all available connections.
   * @param callableAllConnections a {@link Callable} instance.
   */
  public void setCallableAllConnections(final Callable<List<AbstractNodeContext>> callableAllConnections) {
    if(callableAllConnections == null)
      this.callableAllConnections = new Callable<List<AbstractNodeContext>>() {
        @Override
        public List<AbstractNodeContext> call() throws Exception {
          return Collections.emptyList();
        }
      };
    else
      this.callableAllConnections = callableAllConnections;
  }

  @Override
  public void addBundle(final ServerJob bundleWrapper)
  {
    JobSLA sla = bundleWrapper.getSLA();
    final String jobUuid = bundleWrapper.getUuid();
    if (sla.isBroadcastJob() && (bundleWrapper.getBroadcastUUID() == null))
    {
      if (debugEnabled) log.debug("before processing broadcast job " + bundleWrapper.getJob());
      processBroadcastJob(bundleWrapper);
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
          handleStartJobSchedule(bundleWrapper);
          handleExpirationJobSchedule(bundleWrapper);
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
      //latestMaxSize = sizeMap.isEmpty() ? latestMaxSize : sizeMap.lastKey();
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
   * Process the start schedule specified in the job SLA.
   * @param bundleWrapper the job to process.
   */
  private void handleStartJobSchedule(final ServerJob bundleWrapper)
  {
    JPPFSchedule schedule = bundleWrapper.getSLA().getJobSchedule();
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
      catch(ParseException e)
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
  private void handleExpirationJobSchedule(final ServerJob bundleWrapper)
  {
    JPPFSchedule schedule = bundleWrapper.getSLA().getJobExpirationSchedule();
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
      catch(ParseException e)
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
  private void processBroadcastJob(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = bundleWrapper.getJob();
    List<AbstractNodeContext> connections;
    try {
      connections = callableAllConnections.call();
    } catch (Throwable e) {
      connections = Collections.emptyList();
    }
    if (connections.isEmpty())
    {
//      bundleWrapper.taskCompleted(null, null);
      pendingBroadcasts.offer(bundleWrapper);
      return;
    }
    JobSLA sla = bundle.getSLA();
    List<ServerJob> jobList = new ArrayList<ServerJob>(connections.size());

    Set<String> uuidSet = new HashSet<String>();
    for (AbstractNodeContext connection : connections)
    {
      ExecutorStatus status = connection.getExecutionStatus();
      if(status == ExecutorStatus.ACTIVE || status == ExecutorStatus.EXECUTING)
      {
        String uuid = connection.getUuid();
        if (uuid != null && uuid.length() > 0 && uuidSet.add(uuid))
        {
          ServerJob newBundle = bundleWrapper.createBroadcastJob(uuid);
          JPPFManagementInfo info = connection.getManagementInfo();
          ExecutionPolicy policy = sla.getExecutionPolicy();
          if ((policy != null) && !policy.accepts(info.getSystemInfo())) continue;
          ExecutionPolicy broadcastPolicy = new Equal("jppf.uuid", true, uuid);
          if (policy != null) broadcastPolicy = broadcastPolicy.and(policy);
          newBundle.setSLA(((JPPFJobSLA) sla).copy());
          newBundle.setMetadata(bundle.getMetadata());
          newBundle.getSLA().setExecutionPolicy(broadcastPolicy);
          newBundle.setName(bundle.getName() + " [node: " + info.toString() + ']');
          newBundle.setUuid(new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
          jobList.add(newBundle);
          if (debugEnabled) log.debug("Execution policy for job uuid=" + newBundle.getUuid() + " :\n" + broadcastPolicy);
        }
      }
    }
    if (jobList.isEmpty()) bundleWrapper.taskCompleted(null, null);
    else {
      final String jobUuid = bundleWrapper.getUuid();
      lock.lock();
      try {
        ServerJob other = jobMap.get(jobUuid);
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
        fireQueueEvent(new QueueEvent(this, bundleWrapper, false));
        for (ServerJob job : jobList) addBundle(job);
      } finally {
        lock.unlock();
      }
    }
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
      jobScheduleHandler.clear(true);
      jobExpirationHandler.clear(true);
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Cancels queued broadcast jobs for connection.
   * @param connectionUUID The connection UUID that failed or was disconnected.
   */
  public void cancelBroadcastJobs(final String connectionUUID)
  {
    if(connectionUUID == null || connectionUUID.isEmpty()) return;

    Set<String> jobIDs = Collections.emptySet();
    lock.lock();
    try
    {
      if (jobMap.isEmpty()) return;

      jobIDs = new HashSet<String>();
      for (Map.Entry<String, ServerJob> entry : jobMap.entrySet())
      {
        if (connectionUUID.equals(entry.getValue().getBroadcastUUID())) jobIDs.add(entry.getKey());
      }
    } finally
    {
      lock.unlock();
    }
    for (String jobID : jobIDs) {
      cancelJob(jobID);
    }
  }

  /**
   * Process the jobs in the pending broadcast queue.
   * This method is normally called from <code>TaskQueueChecker.dispatch()</code>.
   */
  public void processPendingBroadcasts() {
    if (!hasWorkingConnection()) return;
    ServerJob clientJob;
    while ((clientJob = pendingBroadcasts.poll()) != null)
    {
      if (debugEnabled) log.debug("queuing job " + clientJob.getJob());
      addBundle(clientJob);
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
}
