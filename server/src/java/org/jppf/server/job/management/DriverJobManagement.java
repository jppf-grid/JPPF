/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import org.jppf.server.protocol.ServerJob;
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
  private static Logger log = LoggerFactory.getLogger(DriverJobManagement.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this MBean.
   */
  public DriverJobManagement() {
    driver.getJobManager().addJobManagerListener(new JobEventNotifier());
  }

  /**
   * Cancel the job with the specified id.
   * @param jobUuid the id of the job to cancel.
   * @throws Exception if any error occurs.
   */
  @Override
  public void cancelJob(final String jobUuid) throws Exception {
    ServerJob serverJob = getServerJob(jobUuid);
    if (serverJob != null) {
      if (debugEnabled) log.debug("Request to cancel job '{}'", serverJob.getJob().getName());
      serverJob.cancel(false);
      //driver.getNodeNioServer().getNodeReservationHandler().onJobCancelled(serverJob);
      JPPFStatistics stats = driver.getStatistics();
      stats.addValue(JPPFStatisticsHelper.TASK_QUEUE_COUNT, -serverJob.getTaskCount());
    } else if (debugEnabled) log.debug("Could not find job with uuid = '" + jobUuid + '\'');
  }

  /**
   * Suspend the job with the specified id.
   * @param jobUuid the id of the job to suspend.
   * @param requeue true if the sub-jobs running on each node should be canceled and requeued, false if they should be left to execute until completion.
   * @throws Exception if any error occurs.
   */
  @Override
  public void suspendJob(final String jobUuid, final Boolean requeue) throws Exception {
    ServerJob bundleWrapper = getServerJob(jobUuid);
    if (bundleWrapper == null) {
      if (debugEnabled) log.debug("Could not find job with uuid = '" + jobUuid + '\'');
      return;
    }
    if (debugEnabled) log.debug("Request to suspend jobId = '" + bundleWrapper.getJob().getName() + '\'');
    bundleWrapper.setSuspended(true, Boolean.TRUE.equals(requeue));
  }

  /**
   * Resume the job with the specified id.
   * @param jobUuid the id of the job to resume.
   * @throws Exception if any error occurs.
   */
  @Override
  public void resumeJob(final String jobUuid) throws Exception {
    ServerJob bundleWrapper = getServerJob(jobUuid);
    if (bundleWrapper == null) {
      if (debugEnabled) log.debug("Could not find job with uuid = '" + jobUuid + '\'');
      return;
    }
    if (debugEnabled) log.debug("Request to resume jobId = '" + bundleWrapper.getJob().getName() + '\'');
    bundleWrapper.setSuspended(false, false);
    driver.getNodeNioServer().getTaskQueueChecker().wakeUp();
  }

  /**
   * Update the maximum number of nodes a node can run on.
   * @param jobUuid the id of the job to update.
   * @param maxNodes the new maximum number of nodes for the job.
   * @throws Exception if any error occurs.
   */
  @Override
  public void updateMaxNodes(final String jobUuid, final Integer maxNodes) throws Exception {
    ServerJob serverJob = getServerJob(jobUuid);
    if (serverJob == null) {
      if (debugEnabled) log.debug("Could not find job with uuid = '" + jobUuid + '\'');
      return;
    }
    if (debugEnabled) log.debug("Request to update maxNodes to " + maxNodes + " for jobId = '" + serverJob.getJob().getName() + '\'');
    serverJob.setMaxNodes(maxNodes);
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return a set of ids as strings.
   * @throws Exception if any error occurs.
   * @deprecated use {@link #getAllJobUuids()} instead.
   */
  @Override
  public String[] getAllJobIds() throws Exception {
    return getAllJobUuids();
  }

  @Override
  public String[] getAllJobUuids() throws Exception {
    Set<String> ids = JPPFDriver.getInstance().getQueue().getAllJobIds();
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
    ServerJob job = getServerJob(jobUuid);
    if (job == null) return null;
    JobInformation jobInfo = new JobInformation(jobUuid, job.getName(), job.getTaskCount(), job.getInitialTaskCount(), job.getSLA().getPriority(), job.isSuspended(), job.isPending());
    jobInfo.setMaxNodes(job.getSLA().getMaxNodes());
    return jobInfo;
  }

  /**
   * Get a list of objects describing the nodes to which the whole or part of a job was dispatched.
   * @param jobUuid the id of the job for which to find node information.
   * @return array of <code>NodeManagementInfo</code> instances.
   * @throws Exception if any error occurs.
   */
  @Override
  public NodeJobInformation[] getNodeInformation(final String jobUuid) throws Exception {
    ServerJob bundleWrapper = getServerJob(jobUuid);
    if (bundleWrapper == null) return NodeJobInformation.EMPTY_ARRAY;
    return bundleWrapper.getNodeJobInformation();
  }

  @Override
  public void updatePriority(final String jobUuid, final Integer newPriority) {
    if (debugEnabled) log.debug("Updating priority of jobId = '" + jobUuid + "' to: " + newPriority);
    JPPFDriver.getInstance().getQueue().updatePriority(jobUuid, newPriority);
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
      JobNotification event = (JobNotification) notification;
      if (debugEnabled) log.debug(String.format("sending event %s for job %s, node=%s", event.getEventType(), event.getJobInformation(), event.getNodeInfo()));
    }
    super.sendNotification(notification);
  }

  @Override
  public void cancelJobs(final JobSelector selector) throws Exception {
    Set<String> uuids = selectJobUuids(selector);
    if (debugEnabled) log.debug("request to cancel jobs with these uuids: {}, job selector = {}", uuids, selector);
    for (String uuid: uuids) cancelJob(uuid);
  }

  @Override
  public void suspendJobs(final JobSelector selector, final Boolean requeue) throws Exception {
    for (String uuid: selectJobUuids(selector)) suspendJob(uuid, requeue);
  }

  @Override
  public void resumeJobs(final JobSelector selector) throws Exception {
    for (String uuid: selectJobUuids(selector)) resumeJob(uuid);
  }

  @Override
  public void updateMaxNodes(final JobSelector selector, final Integer maxNodes) throws Exception {
    for (String uuid: selectJobUuids(selector)) updateMaxNodes(uuid, maxNodes);
  }

  @Override
  public JobInformation[] getJobInformation(final JobSelector selector) throws Exception {
    Set<String> uuids = selectJobUuids(selector);
    Set<JobInformation> result = new HashSet<>();
    for (String uuid: uuids) {
      JobInformation info = getJobInformation(uuid);
      if (info != null) result.add(info);
    }
    return result.toArray(new JobInformation[result.size()]);
  }

  @Override
  public Map<String, NodeJobInformation[]> getNodeInformation(final JobSelector selector) throws Exception {
    Set<String> uuids = selectJobUuids(selector);
    Map<String, NodeJobInformation[]> result = new HashMap<>();
    for (String uuid: uuids) {
      NodeJobInformation[] info = getNodeInformation(uuid);
      if (info != null) result.put(uuid, info);
    }
    return result;
  }

  @Override
  public void updatePriority(final JobSelector selector, final Integer newPriority) {
    for (String uuid: selectJobUuids(selector)) updatePriority(uuid, newPriority);
  }

  /**
   * Select the uuids of the jobs specified by a given job selector.
   * @param selector determines for which jobs to return the uuid.
   * @return a set of uuids, possibly empty.
   */
  private Set<String> selectJobUuids(final JobSelector selector) {
    JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    if ((selector == null) || (selector instanceof AllJobsSelector)) return queue.getAllJobIds();
    if (selector instanceof JobUuidSelector) {
      Set<String> allUuids = queue.getAllJobIds();
      allUuids.retainAll(((JobUuidSelector) selector).getUuids());
      return allUuids;
    }
    Set<String> list = new HashSet<>();
    List<ServerJob> allJobs = queue.getAllJobs();
    for (ServerJob job: allJobs) {
      if (selector.accepts(job.getJob())) list.add(job.getUuid());
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
    JPPFPriorityQueue queue = JPPFDriver.getInstance().getQueue();
    List<ServerJob> jobs = queue.selectJobs(selector);
    if (debugEnabled) log.debug("updating sla and metadata for " + jobs.size() + " jobs");
    if (jobs.isEmpty()) return;
    int newPriority = 0;
    if (sla != null) newPriority = sla.getPriority();
    for (ServerJob job: jobs) {
      if (debugEnabled) log.debug("updating sla and metadata for job " + job.getName());
      if (sla != null) {
        int oldPriority = job.getSLA().getPriority();
        if (oldPriority != newPriority) queue.updatePriority(job.getUuid(), newPriority);
      }
      job.update(sla, metadata);
    }
    driver.getNodeNioServer().getTaskQueueChecker().wakeUp();
  }
}
