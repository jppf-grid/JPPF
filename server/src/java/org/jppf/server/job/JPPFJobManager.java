/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.server.job;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.execute.ExecutorChannel;
import org.jppf.job.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.JPPFThreadFactory;
import org.jppf.utils.collections.*;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Instances of this class manage and monitor the jobs throughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager implements ServerJobChangeListener, JobNotificationEmitter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFJobManager.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of jobs to the nodes they are executing on.
   */
  private final CollectionMap<String, ChannelJobPair> jobMap = new ArrayListHashMap<>();
  /**
   * Mapping of job ids to the corresponding <code>JPPFTaskBundle</code>.
   */
  //private final Map<String, ServerJob> bundleMap = new HashMap<>();
  /**
   * Processes the event queue asynchronously.
   */
  private final ExecutorService executor;
  /**
   * The list of registered listeners.
   */
  private final List<JobManagerListener> eventListeners = new CopyOnWriteArrayList<>();
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * Reference to the driver queue.
   */
  private final JPPFPriorityQueue queue = (JPPFPriorityQueue) driver.getQueue();

  /**
   * Default constructor.
   */
  public JPPFJobManager() {
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobManager"));
  }

  /**
   * Get all the nodes to which a all or part of a job is dispatched.
   * @param jobUuid the id of the job.
   * @return a list of <code>SelectableChannel</code> instances.
   */
  @SuppressWarnings("unchecked")
  public List<ChannelJobPair> getNodesForJob(final String jobUuid) {
    if (jobUuid == null) return Collections.emptyList();
    synchronized(jobMap) {
      List<ChannelJobPair> list = (List<ChannelJobPair>) jobMap.getValues(jobUuid);
      return list == null ? Collections.<ChannelJobPair>emptyList() : Collections.unmodifiableList(list);
    }
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return an array of ids as strings.
   */
  public String[] getAllJobIds() {
    synchronized(jobMap) {
      Set<String> keys = jobMap.keySet();
      if (debugEnabled) log.debug("keys = {}", keys);
      return keys.toArray(new String[keys.size()]);
    }
  }

  /**
   * Get the queued bundle wrapper for the specified job.
   * @param jobUuid the id of the job to look for.
   * @return a <code>ServerJob</code> instance, or null if the job is not queued anymore.
   */
  public ServerJob getBundleForJob(final String jobUuid) {
    return ((JPPFPriorityQueue) driver.getQueue()).getBundleForJob(jobUuid);
  }

  @Override
  public void jobDispatched(final AbstractServerJob serverJob, final ExecutorChannel channel, final ServerTaskBundleNode bundleNode) {
    TaskBundle bundle = bundleNode.getJob();
    String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      jobMap.putValue(jobUuid, new ChannelJobPair(channel, serverJob));
    }
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : added node " + channel);
    submitEvent(JobEventType.JOB_DISPATCHED, bundle, channel);
  }

  @Override
  public synchronized void jobReturned(final AbstractServerJob serverJob, final ExecutorChannel channel, final ServerTaskBundleNode bundleNode) {
    TaskBundle bundle = bundleNode.getJob();
    String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      if (!jobMap.removeValue(jobUuid, new ChannelJobPair(channel, serverJob))) {
        log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + bundle.getName());
      } else if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : removed node " + channel);
    }
    submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
  }

  /**
   * Called when a job is added to the server queue.
   * @param serverJob the queued job.
   */
  public void jobQueued(final ServerJob serverJob) {
    TaskBundle bundle = serverJob.getJob();
    String jobUuid = bundle.getUuid();
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' queued");
    submitEvent(JobEventType.JOB_QUEUED, serverJob, null);
    driver.getStatistics().addValue(JPPFStatisticsHelper.JOB_TOTAL, 1);
    driver.getStatistics().addValue(JPPFStatisticsHelper.JOB_COUNT, 1);
    driver.getStatistics().addValue(JPPFStatisticsHelper.JOB_TASKS, bundle.getTaskCount());
  }

  /**
   * Called when a job is complete and returned to the client.
   * @param serverJob the completed job.
   */
  public void jobEnded(final ServerJob serverJob) {
    if (serverJob == null) throw new IllegalArgumentException("bundleWrapper is null");
    if (serverJob.getJob().isHandshake()) return; // skip notifications for handshake bundles

    TaskBundle bundle = serverJob.getJob();
    long time = System.currentTimeMillis() - serverJob.getJobReceivedTime();
    String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      jobMap.removeValues(jobUuid);
    }
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' ended");
    submitEvent(JobEventType.JOB_ENDED, serverJob, null);
    JPPFStatistics stats = driver.getStatistics();
    stats.addValue(JPPFStatisticsHelper.JOB_COUNT, -1);
    stats.addValue(JPPFStatisticsHelper.JOB_TIME, time);
  }

  @Override
  public void jobUpdated(final AbstractServerJob job) {
    if (debugEnabled) log.debug("jobId '" + job.getName() + "' updated");
    submitEvent(JobEventType.JOB_UPDATED, (ServerJob) job, null);
  }

  @Override
  public void jobStatusChanged(final AbstractServerJob source, final SubmissionStatus oldValue, final SubmissionStatus newValue) {
    jobUpdated(source);
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param bundle the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final TaskBundle bundle, final ExecutorChannel channel) {
    executor.submit(new JobEventTask(this, eventType, bundle, channel));
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param job the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final ServerJob job, final ExecutorChannel channel) {
    executor.submit(new JobEventTask(this, eventType, job, channel));
  }

  /**
   * Close this job manager and release its resources.
   */
  public synchronized void close() {
    executor.shutdownNow();
    jobMap.clear();
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addJobListener(final JobManagerListener listener) {
    eventListeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeJobListener(final JobManagerListener listener) {
    eventListeners.remove(listener);
  }

  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  @Override
  public void fireJobEvent(final JobNotification event) {
    if (event == null) throw new IllegalArgumentException("event is null");
    switch (event.getEventType()) {
      case JOB_QUEUED:
        for (JobManagerListener listener: eventListeners) listener.jobQueued(event);
        break;

      case JOB_ENDED:
        for (JobManagerListener listener: eventListeners) listener.jobEnded(event);
        break;

      case JOB_UPDATED:
        for (JobManagerListener listener: eventListeners) listener.jobUpdated(event);
        break;

      case JOB_DISPATCHED:
        for (JobManagerListener listener: eventListeners) listener.jobDispatched(event);
        break;

      case JOB_RETURNED:
        for (JobManagerListener listener: eventListeners) listener.jobReturned(event);
        break;

      default:
        throw new IllegalStateException("Unsupported event type: " + event.getEventType());
    }
  }
}
