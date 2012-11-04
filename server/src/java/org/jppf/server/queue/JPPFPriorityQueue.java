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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.execute.ExecutorStatus;
import org.jppf.job.JobListener;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.JobSLA;
import org.jppf.queue.*;
import org.jppf.server.*;
import org.jppf.server.job.*;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> implements JobManager
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
   * Counts the current number of connections with ACTIVE or EXECUTING status.
   */
  private final AtomicInteger nbWorkingConnections = new AtomicInteger(0);
  /**
   * The job manager.
   */
  private final JPPFJobManager jobManager;
  /**
   * Default callback for getting all available connections. returns an empty collection.
   */
  private static final Callable<List<AbstractNodeContext>> CALLABLE_ALL_CONNECTIONS_EMPTY = new Callable<List<AbstractNodeContext>>() {
    @Override
    public List<AbstractNodeContext> call() throws Exception {
      return Collections.emptyList();
    }
  };
  /**
   * A priority queue holding broadcast jobs that could not be sent due to no available connection.
   */
  private final PriorityBlockingQueue<ServerJobBroadcast> pendingBroadcasts = new PriorityBlockingQueue<ServerJobBroadcast>(16, new JobPriorityComparator());
  /**
   * Callback for getting all available connections. Used for processing broadcast jobs.
   */
  private Callable<List<AbstractNodeContext>> callableAllConnections = CALLABLE_ALL_CONNECTIONS_EMPTY;

  /**
   * Initialize this queue.
   * @param statsManager reference to statistics manager.
   * @param jobManager the job manager.
   */
  public JPPFPriorityQueue(final JPPFDriverStatsManager statsManager, final JPPFJobManager jobManager)
  {
    this.statsManager = statsManager;
    this.jobManager = jobManager;
  }

  /**
   * Set the callable source for all available connections.
   * @param callableAllConnections a {@link Callable} instance.
   */
  public void setCallableAllConnections(final Callable<List<AbstractNodeContext>> callableAllConnections) {
    if (callableAllConnections == null) this.callableAllConnections = CALLABLE_ALL_CONNECTIONS_EMPTY;
    else this.callableAllConnections = callableAllConnections;
  }

  @Override
  public void addBundle(final ServerTaskBundleClient bundleWrapper)
  {
    if (bundleWrapper == null) throw new IllegalArgumentException("bundleWrapper is null");

    JobSLA sla = bundleWrapper.getSLA();
    final String jobUuid = bundleWrapper.getUuid();
    lock.lock();
    try {
      if (sla.isBroadcastJob())
      {
        if (debugEnabled) log.debug("before processing broadcast job " + bundleWrapper.getJob());
        processBroadcastJob(bundleWrapper);
      } else {
        boolean queued;
        ServerJob serverJob = jobMap.get(jobUuid);
        if (serverJob == null) {
          queued = true;
          serverJob = new ServerJob(lock, jobManager, bundleWrapper.getJob(), bundleWrapper.getDataProvider());
          jobManager.jobQueued(serverJob);
          serverJob.setSubmissionStatus(SubmissionStatus.PENDING);
          serverJob.setQueueEntryTime(System.currentTimeMillis());
          serverJob.setJobReceivedTime(serverJob.getQueueEntryTime());
          serverJob.addOnDone(new RemoveBundleAction(this, serverJob));
          if (!sla.isBroadcastJob() || serverJob.getBroadcastUUID() != null) {
            if (debugEnabled) log.debug("adding bundle with " + bundleWrapper);
            scheduleManager.handleStartJobSchedule(serverJob);
            scheduleManager.handleExpirationJobSchedule(serverJob);
          }
          jobMap.put(jobUuid, serverJob);
        } else queued = false;
        if(serverJob.addBundle(bundleWrapper)) {
          if(!queued) removeFromListMap(sla.getPriority(), serverJob, priorityMap);
        } else return;

        if (!sla.isBroadcastJob() || serverJob.getBroadcastUUID() != null) {
          putInListMap(sla.getPriority(), serverJob, priorityMap);
          putInListMap(getSize(serverJob), serverJob, sizeMap);
        }
        updateLatestMaxSize();
        //if (queued) jobManager.jobQueued(serverJob);
        if (!queued)  JPPFDriver.getInstance().getStatsUpdater().tasksAdded(bundleWrapper.getTaskCount());
        fireQueueEvent(new QueueEvent(this, serverJob, false));
      }
      if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    } finally {
      lock.unlock();
    }
    statsManager.taskInQueue(bundleWrapper.getTaskCount());
  }

  /**
   * Handle requeue of the specified job.
   * @param job the job to requeue.
   */
  void requeue(final ServerJob job) {
    lock.lock();
    try {
      if (!jobMap.containsKey(job.getUuid())) throw new IllegalStateException("Job not managed");

      putInListMap(job.getSLA().getPriority(), job, priorityMap);
      putInListMap(getSize(job), job, sizeMap);
      fireQueueEvent(new QueueEvent(this, job, true));
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ServerTaskBundleNode nextBundle(final int nbTasks)
  {
    Iterator<ServerJob> it = iterator();
    return it.hasNext() ? nextBundle(it.next(), nbTasks) : null;
  }

  @Override
  public ServerTaskBundleNode nextBundle(final ServerJob bundleWrapper, final int nbTasks)
  {
    final ServerTaskBundleNode result;
    lock.lock();
    try {
      if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks, next bundle has " + bundleWrapper.getTaskCount() + " tasks");
      removeFromListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
      if (nbTasks >= bundleWrapper.getTaskCount())
      {
        bundleWrapper.setOnRequeue(new RequeueBundleAction(this, bundleWrapper));
        result = bundleWrapper.copy(bundleWrapper.getTaskCount());
        removeBundle(bundleWrapper, false);
      }
      else
      {
        if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
        result = bundleWrapper.copy(nbTasks);
        putInListMap(getSize(bundleWrapper), bundleWrapper, sizeMap);
        List<ServerJob> bundleList = priorityMap.get(bundleWrapper.getSLA().getPriority());
        bundleList.remove(bundleWrapper);
        bundleList.add(bundleWrapper);
      }
      updateLatestMaxSize();
      if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    } finally {
      lock.unlock();
    }
    statsManager.taskOutOfQueue(result.getTaskCount(), System.currentTimeMillis() - bundleWrapper.getQueueEntryTime());
    return result;
  }

  /**
   * Remove the specified bundle from the queue.
   * @param bundleWrapper the bundle to remove.
   * @return the removed bundle.
   */
  @Override
  public ServerJob removeBundle(final ServerJob bundleWrapper)
  {
    return removeBundle(bundleWrapper, true);
  }

  /**
   * Remove the specified bundle from the queue.
   * @param bundleWrapper the bundle to remove.
   * @param removeFromJobMap flag whether bundle should be removed from job map.
   * @return the removed bundle.
   */
  public ServerJob removeBundle(final ServerJob bundleWrapper, final boolean removeFromJobMap)
  {
    if (bundleWrapper == null) throw new IllegalArgumentException("bundleWrapper is null");

    lock.lock();
    try {
      if (removeFromJobMap) {
        jobMap.remove(bundleWrapper.getUuid());
        scheduleManager.clearSchedules(bundleWrapper.getUuid());
        jobManager.jobEnded(bundleWrapper);
      }

      if (debugEnabled) log.debug("removing bundle from queue, jobId= " + bundleWrapper.getName());
      removeFromListMap(bundleWrapper.getSLA().getPriority(), bundleWrapper, priorityMap);

      for (ServerTaskBundleClient bundle : bundleWrapper.getCompletionBundles()) {
        addBundle(bundle);
      }
    } finally {
      lock.unlock();
    }
    return bundleWrapper;
  }

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid the uuid of the job to re-prioritize.
   * @param newPriority the new priority of the job.
   */
  @Override
  public void updatePriority(final String jobUuid, final int newPriority)
  {
    lock.lock();
    try {
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
    } finally {
      lock.unlock();
    }
  }

  /**
   * Cancel the job with the specified UUID
   * @param jobId the uuid of the job to cancel.
   * @return whether cancellation was successful.
   */
  @Override
  public boolean cancelJob(final String jobId)
  {
    lock.lock();
    try {
      ServerJob job = jobMap.get(jobId);
      return job != null && job.cancel(false);
    } finally {
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
      scheduleManager.close();
    } finally {
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
    try {
      return jobMap.get(jobId);
    } finally {
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
    try {
      return Collections.unmodifiableSet(jobMap.keySet());
    } finally {
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
   * Process the specified broadcast job.
   * This consists in creating one job per node, each containing the same tasks,
   * and with an execution policy that enforces its execution ont he designated node only.
   * @param bundleWrapper the broadcast job to process.
   */
  void processBroadcastJob(final ServerTaskBundleClient bundleWrapper)
  {
    final String jobUuid = bundleWrapper.getUuid();
    ServerJob serverJob = jobMap.get(jobUuid);
    if (serverJob == null) {
      ServerJobBroadcast broadcastJob = new ServerJobBroadcast(lock, jobManager, bundleWrapper.getJob(), bundleWrapper.getDataProvider());
      broadcastJob.setSubmissionStatus(SubmissionStatus.PENDING);
      broadcastJob.setQueueEntryTime(System.currentTimeMillis());
      broadcastJob.setJobReceivedTime(broadcastJob.getQueueEntryTime());
      broadcastJob.addOnDone(new RemoveBundleAction(this, broadcastJob));

      jobMap.put(jobUuid, broadcastJob);
      broadcastJob.addBundle(bundleWrapper);
      jobManager.jobQueued(broadcastJob);
      pendingBroadcasts.offer(broadcastJob);
      processPendingBroadcasts();
    } else
      serverJob.addBundle(bundleWrapper);
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
      for (Map.Entry<String, ServerJob> entry : jobMap.entrySet())
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
    if (nbWorkingConnections.get() <= 0) return;
    List<AbstractNodeContext> connections;
    try {
      connections = callableAllConnections.call();
    } catch (Throwable e) {
      connections = Collections.emptyList();
    }
    if (connections.isEmpty()) return;
    ServerJobBroadcast clientJob;
    while ((clientJob = pendingBroadcasts.poll()) != null)
    {
      if (debugEnabled) log.debug("queuing job " + clientJob.getJob());
      processPendingBroadcast(connections, clientJob);
    }
  }

  /**
   * Dispatch broadcast job to all available ACTIVE and EXECUTING connections.
   * @param connections the list of all available connections.
   * @param broadcastJob the job to dispatch to connections.
   */
  private void processPendingBroadcast(final List<AbstractNodeContext> connections, final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");

    JobSLA sla = broadcastJob.getSLA();
    List<ServerJobBroadcast> jobList = new ArrayList<ServerJobBroadcast>(connections.size());

    Set<String> uuidSet = new HashSet<String>();
    for (AbstractNodeContext connection : connections)
    {
      ExecutorStatus status = connection.getExecutionStatus();
      if (status == ExecutorStatus.ACTIVE || status == ExecutorStatus.EXECUTING)
      {
        String uuid = connection.getUuid();
        if (uuid != null && uuid.length() > 0 && uuidSet.add(uuid))
        {
          ServerJobBroadcast newBundle = broadcastJob.createBroadcastJob(uuid);
          JPPFManagementInfo info = connection.getManagementInfo();
          ExecutionPolicy policy = sla.getExecutionPolicy();
          if ((policy != null) && !policy.accepts(info.getSystemInfo())) continue;
          ExecutionPolicy broadcastPolicy = new Equal("jppf.uuid", true, uuid);
          if (policy != null) broadcastPolicy = broadcastPolicy.and(policy);
          newBundle.setSLA(((JPPFJobSLA) sla).copy());
          newBundle.setMetadata(broadcastJob.getMetadata());
          newBundle.getSLA().setExecutionPolicy(broadcastPolicy);
          newBundle.setName(broadcastJob.getName() + " [node: " + info.toString() + ']');
          newBundle.setUuid(new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString());
          jobList.add(newBundle);
          if (debugEnabled) log.debug("Execution policy for job uuid=" + newBundle.getUuid() + " :\n" + newBundle.getBroadcastUUID());
        }
      }
    }
    if (jobList.isEmpty()) broadcastJob.taskCompleted(null, null);
    else {
      lock.lock();
      try {
        fireQueueEvent(new QueueEvent(this, broadcastJob, false));
        for (ServerJobBroadcast job : jobList) addBroadcastJob(job);
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Add an broadcast job to the queue, and notify all listeners about it.
   * @param broadcastJob the job with assigned broadcast id to add to the queue.
   */
  private void addBroadcastJob(final ServerJobBroadcast broadcastJob)
  {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");

    final String jobUuid = broadcastJob.getUuid();
    broadcastJob.setSubmissionStatus(SubmissionStatus.PENDING);
    broadcastJob.setQueueEntryTime(System.currentTimeMillis());
    broadcastJob.setJobReceivedTime(broadcastJob.getQueueEntryTime());
    broadcastJob.addOnDone(new RemoveBundleAction(this, broadcastJob));

    putInListMap(broadcastJob.getSLA().getPriority(), broadcastJob, priorityMap);
    if (debugEnabled) log.debug("adding bundle with " + broadcastJob);
    scheduleManager.handleStartJobSchedule(broadcastJob);
    scheduleManager.handleExpirationJobSchedule(broadcastJob);
    jobMap.put(jobUuid, broadcastJob);
    updateLatestMaxSize();
    jobManager.jobQueued(broadcastJob);
    fireQueueEvent(new QueueEvent(this, broadcastJob, false));

    if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap) + " - " + formatSizeMapInfo("sizeMap", sizeMap));
    statsManager.taskInQueue(broadcastJob.getTaskCount());
  }

  /**
   * Get the bundle size to use for bundle size tuning.
   * @param bundleWrapper the bundle to get the size from.
   * @return the bundle size as an int.
   */
  @Override
  protected int getSize(final ServerJob bundleWrapper)
  {
    return bundleWrapper.getTaskCount();
  }
}
