/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import static org.jppf.utils.collections.CollectionUtils.formatSizeMapInfo;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.client.balancer.*;
import org.jppf.execute.ExecutorStatus;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.*;
import org.jppf.queue.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A JPPF queue whose elements are ordered by decreasing priority.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class JPPFPriorityQueue extends AbstractJPPFQueue<ClientJob, ClientJob, ClientTaskBundle> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFPriorityQueue.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The job manager
   */
  private final JobManagerClient jobManager;
  /**
   * Handles the schedule of each job that has one.
   */
  private final ScheduleManager scheduleManager = new ScheduleManager();
  /**
   * A priority queue holding broadcast jobs that could not be sent due to no available connection.
   */
  private final ConcurrentHashMap<String, ClientJob> pendingBroadcasts = new ConcurrentHashMap<>();

  /**
   * Initialize this queue.
   * @param jobManager reference to job manager.
   */
  public JPPFPriorityQueue(final JobManagerClient jobManager) {
    this.jobManager = jobManager;
  }

  /**
   * Add an object to the queue, and notify all listeners about it.
   * @param clientJob the object to add to the queue.
   */
  @Override
  public void addBundle(final ClientJob clientJob) {
    JobSLA sla = clientJob.getSLA();
    final String jobUuid = clientJob.getUuid();
    if (sla.isBroadcastJob() && (clientJob.getBroadcastUUID() == null)) {
      if (debugEnabled) log.debug("before processing broadcast job " + clientJob.getJob());
      processBroadcastJob(clientJob, jobManager.getWorkingRemoteConnections());
    } else {
      lock.lock();
      try {
        prepareClientJob(clientJob);
        if (!sla.isBroadcastJob() || clientJob.getBroadcastUUID() != null) {
          priorityMap.putValue(sla.getPriority(), clientJob);
          incrementSizeCount(getSize(clientJob));
          if (debugEnabled) log.debug("adding bundle with " + clientJob);
          scheduleManager.handleStartJobSchedule(clientJob);
          scheduleManager.handleExpirationJobSchedule(clientJob);
        }
        jobMap.put(jobUuid, clientJob);
        updateLatestMaxSize();
        fireBundleAdded(new QueueEvent<>(this, clientJob, false));
        if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap));
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
      incrementSizeCount(getSize(job));
      fireBundleAdded(new QueueEvent<>(this, job, true));
      job.jobRequeued();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public ClientTaskBundle nextBundle(final ClientJob bundleWrapper, final int nbTasks) {
    final ClientTaskBundle result;
    lock.lock();
    try {
      if (debugEnabled) log.debug("requesting bundle with " + nbTasks + " tasks, next bundle has " + bundleWrapper.getTaskCount() + " tasks");
      int size = getSize(bundleWrapper);
      decrementSizeCount(size);
      if (nbTasks >= bundleWrapper.getTaskCount()) {
        bundleWrapper.setOnRequeue(new Runnable() {
          @Override
          public void run() {
            requeue(bundleWrapper);
          }
        });
        result = bundleWrapper.copy(bundleWrapper.getTaskCount());
        removeBundle(bundleWrapper);
      } else {
        if (debugEnabled) log.debug("removing " + nbTasks + " tasks from bundle");
        result = bundleWrapper.copy(nbTasks);
        int newSize = bundleWrapper.getTaskCount();
        incrementSizeCount(size);
        // to ensure that other jobs with same priority are also processed without waiting
        priorityMap.moveToEndOfList(bundleWrapper.getSLA().getPriority(), bundleWrapper);
      }
      updateLatestMaxSize();
      if (debugEnabled) log.debug("Maps size information: " + formatSizeMapInfo("priorityMap", priorityMap));
    } finally {
      lock.unlock();
    }
    return result;
  }

  @Override
  public boolean isEmpty() {
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
  protected int getSize(final ClientJob bundleWrapper) {
    //return bundle.getTaskCount();
    return bundleWrapper.getJob().getJobTasks().size();
  }

  @Override
  public ClientJob removeBundle(final ClientJob job) {
    lock.lock();
    try {
      if (debugEnabled) log.debug("removing bundle from queue, jobId=" + job.getName());
      priorityMap.removeValue(job.getSLA().getPriority(), job);
      fireBundleRemoved(new QueueEvent<>(this, job, false));
      return job;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Process the specified broadcast job.
   * This consists in creating one job per node, each containing the same tasks,
   * and with an execution policy that enforces its execution ont he designated node only.
   * @param clientJob the broadcast job to process.
   */
  /**
   * Process the specified broadcast job.
   * This consists in creating one job per node, each containing the same tasks and with an execution policy that enforces its execution ont he designated node only.
   * @param clientJob the broadcast job to process.
   * @param workingRemoteConnections the connections to which the job may be broadcasted.
   */
  private void processBroadcastJob(final ClientJob clientJob, final List<ChannelWrapper> workingRemoteConnections) {
    scheduleManager.handleStartJobSchedule(clientJob);
    scheduleManager.handleExpirationJobSchedule(clientJob);
    JPPFJob bundle = clientJob.getJob();
    List<ChannelWrapper> connections = jobManager.getAllConnections();
    for (Iterator<ChannelWrapper> it=connections.iterator(); it.hasNext();) {
      ChannelWrapper ch = it.next();
      ExecutorStatus status = ch.getExecutionStatus();
      if (ch.isLocal() || !((status == ExecutorStatus.ACTIVE) || (status == ExecutorStatus.EXECUTING))) it.remove();
    }
    if (log.isTraceEnabled()) log.trace(String.format("%d connection(s) for broadcast job '%s' : %s", connections.size(), bundle.getName(), connections));
    if (connections.isEmpty()) {
      pendingBroadcasts.putIfAbsent(bundle.getUuid(), clientJob);
      return;
    }
    pendingBroadcasts.remove(bundle.getUuid());
    JobSLA sla = bundle.getSLA();
    List<ClientJob> jobList = new ArrayList<>(connections.size());
    Set<String> uuidSet = new HashSet<>();
    for (ChannelWrapper connection : connections) {
      String uuid = connection.getUuid();
      if ((uuid != null) && (uuid.length() > 0) && uuidSet.add(uuid)) {
        ClientJob newBundle = clientJob.createBroadcastJob(uuid);
        JPPFManagementInfo info = connection.getManagementInfo();
        newBundle.setClientSLA(((JPPFJobClientSLA) bundle.getClientSLA()).copy());
        newBundle.setSLA(((JPPFJobSLA) sla).copy());
        newBundle.setMetadata(bundle.getMetadata());
        newBundle.setName(bundle.getName() + " [driver: " + info.toString() + ']');
        newBundle.setUuid(JPPFUuid.normalUUID());
        jobList.add(newBundle);
      }
    }
    if (jobList.isEmpty()) clientJob.taskCompleted(null, null);
    else {
      final String jobUuid = clientJob.getUuid();
      lock.lock();
      try {
        prepareClientJob(clientJob);
        jobMap.put(jobUuid, clientJob);
        fireBundleAdded(new QueueEvent<>(this, clientJob, false));
        for (ClientJob job : jobList) addBundle(job);
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Setup a {@link ClientJob} before adding it to the queue.
   * @param clientJob the job to prepare.
   */
  private void prepareClientJob(final ClientJob clientJob) {
    ClientJob other = jobMap.get(clientJob.getUuid());
    if (other != null) throw new IllegalStateException("Job " + clientJob.getUuid() + " already enqueued");
    clientJob.addOnDone(new Runnable() {
      @Override
      public void run() {
        lock.lock();
        try {
          jobMap.remove(clientJob.getUuid());
          removeBundle(clientJob);
        } finally {
          lock.unlock();
        }
      }
    });
    clientJob.setJobStatus(JobStatus.PENDING);
    clientJob.setQueueEntryTime(System.currentTimeMillis());
    clientJob.setJobReceivedTime(clientJob.getQueueEntryTime());
  }

  /**
   * Update the priority of the job with the specified uuid.
   * @param jobUuid     the uuid of the job to re-prioritize.
   * @param newPriority the new priority of the job.
   */
  public void updatePriority(final String jobUuid, final int newPriority) {
    lock.lock();
    try {
      ClientJob job = jobMap.get(jobUuid);
      if (job == null) return;
      int oldPriority = job.getJob().getSLA().getPriority();
      if (oldPriority != newPriority) {
        job.getJob().getSLA().setPriority(newPriority);
        priorityMap.removeValue(oldPriority, job);
        priorityMap.putValue(newPriority, job);
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
  public boolean cancelJob(final String jobId) {
    if (debugEnabled) log.debug("requesting cancel of jobId=" + jobId);
    lock.lock();
    try {
      ClientJob job = jobMap.get(jobId);
      return job == null ? false : job.cancel(false);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Close this queue and all resources it uses.
   */
  public void close() {
    lock.lock();
    try {
      scheduleManager.close();
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
  public void cancelBroadcastJobs(final String connectionUUID) {
    if (connectionUUID == null || connectionUUID.isEmpty()) return;
    Set<String> jobIDs = Collections.emptySet();
    lock.lock();
    try {
      if (jobMap.isEmpty()) return;

      jobIDs = new HashSet<>();
      for (Map.Entry<String, ClientJob> entry : jobMap.entrySet()) {
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
    if (!jobManager.hasWorkingConnection() || pendingBroadcasts.isEmpty()) return;
    for (Map.Entry<String, ClientJob> entry: pendingBroadcasts.entrySet()) {
      ClientJob clientJob = entry.getValue();
      if (log.isTraceEnabled()) log.trace("queuing broadcast job " + clientJob.getJob());
      processBroadcastJob(clientJob, jobManager.getWorkingRemoteConnections());
    }
  }

  /**
   * Get all the {@link JPPFJob}s currently in the queue.
   * <p>This method should be used with caution, as its cost is in O(n), with n being the number of jobs in the queue.
   * @return a list of {@link JPPFJob} instances ordered by their priority.
   * @since 4.1
   */
  public List<JPPFJob> getJPPFJobs() {
    lock.lock();
    try {
      int size = priorityMap.size();
      if (size <= 0) return Collections.EMPTY_LIST;
      List<JPPFJob> list = new ArrayList<>(size);
      for (ClientJob clientJob: priorityMap) list.add(clientJob.getJob());
      return list;
    } finally {
      lock.unlock();
    }
  }
}
