/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.job.management;

import java.util.*;

import javax.management.*;

import org.jppf.job.*;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Implementation of the job management bean.
 * @author Laurent Cohen
 * @exclude
 */
public class DriverJobManagement extends NotificationBroadcasterSupport implements DriverJobManagementMBean {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(DriverJobManagement.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;

  /**
   * Initialize this MBean.
   * @param driver reference to the JPPF driver.
   */
  public DriverJobManagement(final JPPFDriver driver) {
    this.driver = driver;
    driver.getJobManager().addJobManagerListener(new JobEventNotifier());
  }

  @Override
  public void cancelJob(final String jobUuid) throws Exception {
    cancelJob(getServerJob(jobUuid));
  }

  /**
   * Cancel the job with the specified id.
   * @param job the job to cancel.
   * @throws Exception if any error occurs.
   */
  private void cancelJob(final ServerJob job) throws Exception {
    if (job != null) {
      if (debugEnabled) log.debug("Request to cancel job '{}'", job.getJob().getName());
      job.cancel(driver, false);
      final JPPFStatistics stats = driver.getStatistics();
      stats.addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, -job.getTaskCount());
    } else if (debugEnabled) log.debug("job is null");
  }

  @Override
  public void suspendJob(final String jobUuid, final Boolean requeue) throws Exception {
    suspendJob(getServerJob(jobUuid), requeue);
  }

  /**
   * Suspend the job with the specified id.
   * @param job the job to suspend.
   * @param requeue {@code true} if the sub-jobs running on each node should be canceled and requeued, {@code false} if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   */
  private static void suspendJob(final ServerJob job, final Boolean requeue) throws Exception {
    if (job == null) {
      if (debugEnabled) log.debug("job is null");
      return;
    }
    if (debugEnabled) log.debug("Request to suspend job '" + job.getJob().getName() + '\'');
    job.setSuspended(true, requeue);
  }

  @Override
  public void resumeJob(final String jobUuid) throws Exception {
    resumeJob(getServerJob(jobUuid));
  }

  /**
   * Resume the specified job.
   * @param job the job to resume.
   * @throws Exception if any error occurs.
   */
  private void resumeJob(final ServerJob job) throws Exception {
    if (job == null) {
      if (debugEnabled) log.debug("job is null");
      return;
    }
    if (debugEnabled) log.debug("Request to resume job '" + job.getJob().getName() + '\'');
    job.setSuspended(false, false);
    if (driver.isAsyncNode()) driver.getAsyncNodeNioServer().getTaskQueueChecker().wakeUp();
    else driver.getNodeNioServer().getTaskQueueChecker().wakeUp();
  }

  @Override
  public void updateMaxNodes(final String jobUuid, final Integer maxNodes) throws Exception {
    updateMaxNodes(getServerJob(jobUuid), maxNodes);
  }

  /**
   * Update the maximum number of nodes a job can run on.
   * @param job the job to update.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   */
  private static void updateMaxNodes(final ServerJob job, final Integer maxNodes) throws Exception {
    if (job == null) {
      if (debugEnabled) log.debug("job is null");
      return;
    }
    if (debugEnabled) log.debug("Request to update maxNodes to " + maxNodes + " for jobId = '" + job.getJob().getName() + '\'');
    job.setMaxNodes(maxNodes);
  }

  @Override
  public String[] getAllJobUuids() throws Exception {
    final Set<String> ids = driver.getQueue().getAllJobIds();
    return ids.toArray(new String[ids.size()]);
  }

  /**
   * Get an object describing the job with the specified uuid.
   * @param jobUuid the id of the job to get information about.
   * @return an instance of <code>JobInformation</code>.
   * @throws Exception if any error occurs.
   */
  @Override
  public JobInformation getJobInformation(final String jobUuid) throws Exception {
    final ServerJob job = getServerJob(jobUuid);
    if (job == null) return null;
    final JobInformation jobInfo = new JobInformation(jobUuid, job.getName(), job.getTaskCount(), job.getInitialTaskCount(), job.getSLA().getPriority(), job.isSuspended(), job.isPending());
    jobInfo.setMaxNodes(job.getSLA().getMaxNodes());
    return jobInfo;
  }

  @Override
  public NodeJobInformation[] getNodeInformation(final String jobUuid) throws Exception {
    return getNodeInformation(getServerJob(jobUuid));
  }

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @param job the job for which to find node information.
   * @return array of <code>NodeManagementInfo</code> instances.
   * @throws Exception if any error occurs.
   */
  private static NodeJobInformation[] getNodeInformation(final ServerJob job) throws Exception {
    if (job == null) return NodeJobInformation.EMPTY_ARRAY;
    if (!(job instanceof ServerJobBroadcast)) return job.getNodeJobInformation();
    final ServerJobBroadcast broadcast = (ServerJobBroadcast) job;
    final List<ServerJobBroadcast> dispatches = broadcast.getDispatchedBroadcasts();
    final List<NodeJobInformation> result = new ArrayList<>(dispatches.size());
    for (final ServerJobBroadcast childJob: dispatches) {
      final NodeJobInformation[] nji = childJob.getNodeJobInformation();
      if (nji != null) {
        for (final NodeJobInformation info: nji) result.add(info);
      }
    }
    return result.toArray(new NodeJobInformation[result.size()]);
  }

  @Override
  public void updatePriority(final String jobUuid, final Integer newPriority) {
    if (debugEnabled) log.debug("Updating priority of jobId = '" + jobUuid + "' to: " + newPriority);
    driver.getQueue().updatePriority(jobUuid, newPriority);
  }

  /**
   * A job manager listener that sends a notification through the mbean for each job manager event.
   */
  private class JobEventNotifier implements JobManagerListener {
    /**
     * Called when a new job is put in the job queue.
     * @param event encapsulates the information about the event.
     */
    @Override
    public void jobQueued(final JobNotification event) {
      sendNotification(event);
    }

    /**
     * Called when a job is complete and has been sent back to the client.
     * @param event encapsulates the information about the event.
     */
    @Override
    public void jobEnded(final JobNotification event) {
      sendNotification(event);
    }

    /**
     * Called when the current number of tasks in a job was updated.
     * @param event encapsulates the information about the event.
     */
    @Override
    public void jobUpdated(final JobNotification event) {
      sendNotification(event);
    }

    /**
     * Called when all or part of a job is is sent to a node for execution.
     * @param event encapsulates the information about the event.
     */
    @Override
    public void jobDispatched(final JobNotification event) {
      sendNotification(event);
    }

    /**
     * Called when all or part of a job has returned from irs execution on a node.
     * @param event encapsulates the information about the event.
     */
    @Override
    public void jobReturned(final JobNotification event) {
      sendNotification(event);
    }
  }

  @Override
  public void sendNotification(final Notification notification) {
    if (debugEnabled && (notification instanceof JobNotification)) {
      final JobNotification event = (JobNotification) notification;
      if (event.getEventType() != JobEventType.JOB_UPDATED)
        log.debug("sending event {} for job {}, node={}", event.getEventType(), event.getJobInformation(), event.getNodeInfo());
    }
    super.sendNotification(notification);
  }

  @Override
  public void cancelJobs(final JobSelector selector) throws Exception {
    final List<ServerJob> jobs = selectJobs(selector);
    if (debugEnabled) log.debug("request to cancel {} jobs, job selector = {}", jobs.size(), selector);
    for (final ServerJob job: jobs) cancelJob(job);
  }

  @Override
  public void suspendJobs(final JobSelector selector, final Boolean requeue) throws Exception {
    for (final ServerJob job: selectJobs(selector)) suspendJob(job, requeue);
  }

  @Override
  public void resumeJobs(final JobSelector selector) throws Exception {
    for (final ServerJob job: selectJobs(selector)) resumeJob(job);
  }

  @Override
  public void updateMaxNodes(final JobSelector selector, final Integer maxNodes) throws Exception {
    for (final ServerJob job: selectJobs(selector)) updateMaxNodes(job, maxNodes);
  }

  @Override
  public JobInformation[] getJobInformation(final JobSelector selector) throws Exception {
    final Set<JobInformation> result = new HashSet<>();
    final List<ServerJob> jobs = selectJobs(selector);
    for (final ServerJob job: jobs) {
      if (!JPPFJobManager.isBroadcastDispatch(job)) {
        final JobInformation info = getJobInformation(job.getUuid());
        result.add(info);
      }
    }
    return result.toArray(new JobInformation[result.size()]);
  }

  @Override
  public Map<String, NodeJobInformation[]> getNodeInformation(final JobSelector selector) throws Exception {
    final Map<String, NodeJobInformation[]> result = new HashMap<>();
    final List<ServerJob> jobs = selectJobs(selector);
    for (final ServerJob job: jobs) {
      if (!JPPFJobManager.isBroadcastDispatch(job)) {
        final NodeJobInformation[] info = getNodeInformation(job);
        if (info != null) result.put(job.getUuid(), info);
      }
    }
    return result;
  }

  @Override
  public void updatePriority(final JobSelector selector, final Integer newPriority) {
    for (final ServerJob job: selectJobs(selector)) updatePriority(job.getUuid(), newPriority);
  }

  /**
   * Select the uuids of the jobs specified by a given job selector.
   * @param selector determines for which jobs to return the uuid.
   * @return a set of uuids, possibly empty.
   */
  private List<ServerJob> selectJobs(final JobSelector selector) {
    final JPPFPriorityQueue queue = driver.getQueue();
    final List<ServerJob> allJobs = queue.getAllJobs();
    final List<ServerJob> list = new ArrayList<>(allJobs.size());
    for (final ServerJob job: allJobs) {
      if (selector.accepts(job.getJob()) && !JPPFJobManager.isBroadcastDispatch(job)) list.add(job);
    }
    return list;
  }

  /**
   * Get the queued bundle wrapper for the specified job.
   * @param jobUuid the id of the job to look for.
   * @return a <code>ServerJob</code> instance, or null if the job is not queued anymore.
   */
  private ServerJob getServerJob(final String jobUuid) {
    return driver.getQueue().getBundleForJob(jobUuid);
  }

  @Override
  public void updateJobs(final JobSelector selector, final JobSLA sla, final JobMetadata metadata) {
    if ((sla == null) && (metadata == null)) return;
    final JPPFPriorityQueue queue = driver.getQueue();
    final List<ServerJob> jobs = queue.selectJobs(selector);
    if (debugEnabled) log.debug("updating sla and metadata for " + jobs.size() + " jobs");
    if (jobs.isEmpty()) return;
    int newPriority = 0;
    if (sla != null) newPriority = sla.getPriority();
    for (final ServerJob job: jobs) {
      if (debugEnabled) log.debug("updating sla and metadata for job " + job.getName());
      if (sla != null) {
        final int oldPriority = job.getSLA().getPriority();
        if (oldPriority != newPriority) queue.updatePriority(job.getUuid(), newPriority);
      }
      job.update(driver, sla, metadata);
    }
    if (driver.isAsyncNode()) driver.getAsyncNodeNioServer().getTaskQueueChecker().wakeUp();
    else driver.getNodeNioServer().getTaskQueueChecker().wakeUp();
  }

  @Override
  public void addNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    if (debugEnabled) log.debug("adding notification listener={} with filter={} and handback={}", listener, filter, handback);
    super.addNotificationListener(listener, filter, handback);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("removing notification listener=" + listener);
    super.removeNotificationListener(listener);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("removing notification listener={} with filter={} and handback={}", listener, filter, handback);
    super.removeNotificationListener(listener, filter, handback);
  }
}
