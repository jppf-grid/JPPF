/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorStatus;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.*;
import org.jppf.queue.QueueEvent;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class BroadcastManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFPriorityQueue.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * A priority queue holding broadcast jobs that could not be sent due to no available connection.
   */
  private final ConcurrentHashMap<String, ServerJobBroadcast> pendingBroadcasts = new ConcurrentHashMap<>();
  /**
   * 
   */
  private final JPPFPriorityQueue queue;
  /**
   * 
   */
  private final Lock lock;
  /**
   * Counts the current number of connections with ACTIVE or EXECUTING status.
   */
  private final AtomicInteger nbWorkingConnections = new AtomicInteger(0);
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
   * Callback for getting all available connections. Used for processing broadcast jobs.
   */
  private Callable<List<AbstractNodeContext>> callableAllConnections = CALLABLE_ALL_CONNECTIONS_EMPTY;
  /**
   * 
   */
  private final Map<String, ServerJob> jobMap; 

  /**
   * 
   * @param queue the job queue.
   */
  BroadcastManager(final JPPFPriorityQueue queue) {
    this.queue = queue;
    this.lock = queue.getLock();
    this.jobMap = queue.getJobMap();
  }

  /**
   * Set the callable source for all available connections.
   * @param callableAllConnections a {@link Callable} instance.
   */
  void setCallableAllConnections(final Callable<List<AbstractNodeContext>> callableAllConnections) {
    if (callableAllConnections == null) this.callableAllConnections = CALLABLE_ALL_CONNECTIONS_EMPTY;
    else this.callableAllConnections = callableAllConnections;
  }

  /**
   * Update count of working connections base on status change.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  void updateWorkingConnections(final ExecutorStatus oldStatus, final ExecutorStatus newStatus) {
    final boolean bNew = (newStatus == ExecutorStatus.ACTIVE) || (newStatus == ExecutorStatus.EXECUTING);
    final boolean bOld = (oldStatus == ExecutorStatus.ACTIVE) || (oldStatus == ExecutorStatus.EXECUTING);
    if (bNew && !bOld) nbWorkingConnections.incrementAndGet();
    else if (!bNew && bOld) nbWorkingConnections.decrementAndGet();
  }

  /**
   * Process the specified broadcast job.
   * This consists in creating one job per node, each containing the same tasks,
   * and with an execution policy that enforces its execution ont he designated node only.
   * @param clientBundle the broadcast job to process.
   */
  void processBroadcastJob(final ServerTaskBundleClient clientBundle) {
    final String jobUuid = clientBundle.getUuid();
    final ServerJob serverJob = jobMap.get(jobUuid);
    if (serverJob == null) {
      final ServerJobBroadcast broadcastJob = new ServerJobBroadcast(lock, queue.jobManager, clientBundle.getJob(), clientBundle.getDataProvider());
      broadcastJob.setSubmissionStatus(SubmissionStatus.PENDING);
      broadcastJob.setQueueEntryTime(System.currentTimeMillis());
      broadcastJob.setJobReceivedTime(broadcastJob.getQueueEntryTime());
      broadcastJob.addOnDone(new RemoveBundleAction(queue, broadcastJob));
      jobMap.put(jobUuid, broadcastJob);
      broadcastJob.addBundle(clientBundle);
      queue.scheduleManager.handleStartJobSchedule(broadcastJob);
      queue.scheduleManager.handleExpirationJobSchedule(broadcastJob);
      queue.jobManager.jobQueued(broadcastJob);
      pendingBroadcasts.put(jobUuid, broadcastJob);
      //processPendingBroadcasts();
    } else serverJob.addBundle(clientBundle);
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
      for (Map.Entry<String, ServerJob> entry : jobMap.entrySet()) {
        if (connectionUUID.equals(entry.getValue().getBroadcastUUID())) jobIDs.add(entry.getKey());
      }
    } finally {
      lock.unlock();
    }
    for (String jobID : jobIDs) queue.cancelJob(jobID);
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
    } catch (@SuppressWarnings("unused") final Throwable e) {
      connections = Collections.emptyList();
    }
    if (connections.isEmpty()) return;
    for (final Map.Entry<String, ServerJobBroadcast> entry: pendingBroadcasts.entrySet()) {
      final ServerJobBroadcast broadcastJob = entry.getValue();
      if (debugEnabled) log.debug("queuing job " + broadcastJob.getJob());
      processPendingBroadcast(connections, broadcastJob);
    }
  }

  /**
   * Dispatch broadcast job to all available ACTIVE and EXECUTING connections.
   * @param connections the list of all available connections.
   * @param broadcastJob the job to dispatch to connections.
   */
  private void processPendingBroadcast(final List<AbstractNodeContext> connections, final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    if (pendingBroadcasts.remove(broadcastJob.getUuid()) == null) return;
    final JobSLA sla = broadcastJob.getSLA();
    final List<ServerJobBroadcast> jobList = new ArrayList<>(connections.size());
    final Set<String> uuidSet = new HashSet<>();
    for (final AbstractNodeContext connection : connections) {
      final ExecutorStatus status = connection.getExecutionStatus();
      if (status == ExecutorStatus.ACTIVE || status == ExecutorStatus.EXECUTING) {
        final String uuid = connection.getUuid();
        if (uuid != null && uuid.length() > 0 && uuidSet.add(uuid)) {
          final JPPFManagementInfo info = connection.getManagementInfo();
          final ExecutionPolicy policy = sla.getExecutionPolicy();
          TaskQueueChecker.preparePolicy(policy, broadcastJob, JPPFDriver.getInstance().getStatistics(), 0);
          if ((policy != null) && !policy.evaluate(info.getSystemInfo())) {
            if (debugEnabled) log.debug("node uuid={} refused for broadcast {}", uuid, broadcastJob);
            continue;
          }
          ExecutionPolicy broadcastPolicy = new Equal("jppf.uuid", true, uuid);
          if (policy != null) broadcastPolicy = broadcastPolicy.and(policy);
          final ServerJobBroadcast newBundle = broadcastJob.createBroadcastJob(uuid);
          newBundle.setSLA(sla.copy());
          newBundle.setMetadata(broadcastJob.getMetadata());
          newBundle.getSLA().setExecutionPolicy(broadcastPolicy);
          newBundle.setName(broadcastJob.getName() + " [node: " + info.toString() + ']');
          newBundle.setUuid(JPPFUuid.normalUUID());
          jobList.add(newBundle);
          if (debugEnabled) log.debug("node uuid={} accepted for broadcast {}", uuid, broadcastJob);
        }
      }
    }
    if (jobList.isEmpty()) broadcastJob.jobEnded();
    else {
      lock.lock();
      try {
        queue.fireBundleAdded(new QueueEvent<>(queue, broadcastJob, false));
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
  private void addBroadcastJob(final ServerJobBroadcast broadcastJob) {
    if (broadcastJob == null) throw new IllegalArgumentException("broadcastJob is null");
    final String jobUuid = broadcastJob.getUuid();
    broadcastJob.setSubmissionStatus(SubmissionStatus.PENDING);
    broadcastJob.setQueueEntryTime(System.currentTimeMillis());
    broadcastJob.setJobReceivedTime(broadcastJob.getQueueEntryTime());
    broadcastJob.addOnDone(new RemoveBundleAction(queue, broadcastJob));
    queue.getPriorityMap().putValue(broadcastJob.getSLA().getPriority(), broadcastJob);
    if (debugEnabled) log.debug("adding bundle with " + broadcastJob);
    queue.scheduleManager.handleStartJobSchedule(broadcastJob);
    queue.scheduleManager.handleExpirationJobSchedule(broadcastJob);
    jobMap.put(jobUuid, broadcastJob);
    queue.updateLatestMaxSize();
    queue.jobManager.jobQueued(broadcastJob);
    queue.fireBundleAdded(new QueueEvent<>(queue, broadcastJob, false));
    JPPFDriver.getInstance().getStatistics().addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, broadcastJob.getTaskCount());
  }
}
