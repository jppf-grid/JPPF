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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

import org.jppf.execute.ExecutorStatus;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.*;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
class BroadcastJobManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BroadcastJobManager.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
  private final PriorityBlockingQueue<ServerJob> pendingBroadcasts = new PriorityBlockingQueue<ServerJob>(16, new JobPriorityComparator());
  /**
   * The JPPF job queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * The queue lock.
   */
  private final Lock lock;
  /**
   * Contains the ids of all queued jobs.
   */
  private final Map<String, ServerJob> jobMap;
  /**
   * Callback for getting all available connections. Used for processing broadcast jobs.
   */
  private Callable<List<AbstractNodeContext>> callableAllConnections = CALLABLE_ALL_CONNECTIONS_EMPTY;

  /**
   * Initialize this manager witht he specified queue.
   * @param queue the JPPF job queue.
   */
  BroadcastJobManager(final JPPFPriorityQueue queue)
  {
    this.queue = queue;
    this.lock = queue.getLock();
    this.jobMap = queue.jobMap;
  }

  /**
   * Set the callable source for all available connections.
   * @param callableAllConnections a {@link Callable} instance.
   */
  void setCallableAllConnections(final Callable<List<AbstractNodeContext>> callableAllConnections) {
    if(callableAllConnections == null) this.callableAllConnections = CALLABLE_ALL_CONNECTIONS_EMPTY;
    else this.callableAllConnections = callableAllConnections;
  }

  /**
   * Process the specified broadcast job.
   * This consists in creating one job per node, each containing the same tasks,
   * and with an execution policy that enforces its execution ont he designated node only.
   * @param bundleWrapper the broadcast job to process.
   */
  void processBroadcastJob(final ServerJob bundleWrapper)
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
              queue.removeBundle(bundleWrapper);
            } finally {
              lock.unlock();
            }
          }
        });
        bundleWrapper.setSubmissionStatus(SubmissionStatus.PENDING);
        bundleWrapper.setQueueEntryTime(System.currentTimeMillis());
        bundleWrapper.setJobReceivedTime(bundleWrapper.getQueueEntryTime());

        jobMap.put(jobUuid, bundleWrapper);
        queue.fireQueueEvent(new QueueEvent(queue, bundleWrapper, false));
        for (ServerJob job : jobList) queue.addBundle(job);
      } finally {
        lock.unlock();
      }
    }
  }

  /**
   * Cancels queued broadcast jobs for connection.
   * @param connectionUUID The connection UUID that failed or was disconnected.
   */
  void cancelBroadcastJobs(final String connectionUUID)
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
    }
    finally
    {
      lock.unlock();
    }
    for (String jobID : jobIDs) queue.cancelJob(jobID);
  }

  /**
   * Process the jobs in the pending broadcast queue.
   * This method is normally called from <code>TaskQueueChecker.dispatch()</code>.
   */
  void processPendingBroadcasts()
  {
    if (!queue.hasWorkingConnection() || pendingBroadcasts.isEmpty()) return;
    ServerJob clientJob;
    while ((clientJob = pendingBroadcasts.poll()) != null)
    {
      if (debugEnabled) log.debug("queuing job " + clientJob.getJob());
      queue.addBundle(clientJob);
    }
  }
}
