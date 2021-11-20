/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.execute.ExecutorChannel;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.NodeReservationHandler;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.stats.*;
import org.slf4j.*;

/**
 * Instances of this class manage and monitor the jobs throughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager implements ServerJobChangeListener, JobNotificationEmitter, JobTasksListenerManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFJobManager.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of jobs to the nodes they are executing on.
   */
  private final CollectionMap<String, ChannelJobPair> jobMap = new ArrayListHashMap<>();
  /**
   * The list of registered job life manager listeners.
   */
  private final List<JobManagerListener> jobManagerListeners = new CopyOnWriteArrayList<>();
  /**
   * The list of registered job dispatch listeners.
   */
  private final List<JobTasksListener> taskReturnListeners = new CopyOnWriteArrayList<>();
  /**
   * Reference to the JPPF driver.
   */
  private final JPPFDriver driver;
  /**
   * Queue of job notifications waiting to be sent.
   */
  private final QueueHandler<JobNotification> eventQueue;

  /**
   * Default constructor.
   * @param driver reference to the JPPF driver.
   */
  public JPPFJobManager(final JPPFDriver driver) {
    this.driver = driver;
    final TypedProperties config = driver.getConfiguration();
    int size = config.get(JPPFProperties.JMX_NOTIF_QUEUE_SIZE);
    if (size <= 0) size = Integer.MAX_VALUE;
    eventQueue = QueueHandler.<JobNotification>builder()
      .named("JobNotifications")
      .withCapacity(size)
      .handlingElementsAs(this::fireJobEvent)
      .usingSingleDequuerThread()
      .build();
  }

  /**
   * Get all the nodes to which a all or part of a job is dispatched.
   * @param jobUuid the id of the job.
   * @return a list of {@link ChannelJobPair} instances.
   */
  public List<ChannelJobPair> getNodesForJob(final String jobUuid) {
    if (jobUuid == null) return Collections.emptyList();
    synchronized(jobMap) {
      final List<ChannelJobPair> list = (List<ChannelJobPair>) jobMap.getValues(jobUuid);
      return list == null ? Collections.<ChannelJobPair>emptyList() : Collections.unmodifiableList(list);
    }
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return an array of ids as strings.
   */
  public String[] getAllJobIds() {
    synchronized(jobMap) {
      final Set<String> keys = jobMap.keySet();
      if (debugEnabled) log.debug("keys = {}", keys);
      return keys.toArray(new String[keys.size()]);
    }
  }

  @Override
  public void jobDispatched(final AbstractServerJob serverJob, final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle) {
    final TaskBundle bundle = nodeBundle.getJob();
    final String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      jobMap.putValue(jobUuid, new ChannelJobPair(channel, serverJob));
    }
    if (debugEnabled) log.debug("job '{}' dispatched to node {}", bundle.getName(), channel);
    if (!isBroadcastDispatch(serverJob)) {
      submitEvent(JobEventType.JOB_DISPATCHED, bundle, channel);
      fireJobTasksEvent(channel, nodeBundle, true);
    } else {
      final ServerJobBroadcast broadcast = (ServerJobBroadcast) serverJob;
      submitEvent(JobEventType.JOB_DISPATCHED, broadcast.getParentJob(), channel);
    }
    final JPPFStatistics stats = driver.getStatistics();
    stats.addValue(JPPFStatisticsHelper.JOB_DISPATCH_TOTAL, 1);
    stats.addValue(JPPFStatisticsHelper.JOB_DISPATCH_COUNT, 1);
    stats.addValue(JPPFStatisticsHelper.JOB_DISPATCH_TASKS, nodeBundle.getTaskCount());
  }

  @Override
  public synchronized void jobReturned(final AbstractServerJob serverJob, final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle) {
    final TaskBundle bundle = nodeBundle.getJob();
    final String jobUuid = bundle.getUuid();
    if (debugEnabled) log.debug("job '{}' returned from node {}", bundle.getName(), channel);
    synchronized(jobMap) {
      jobMap.removeValue(jobUuid, new ChannelJobPair(channel, serverJob));
    }
    if (!isBroadcastDispatch(serverJob)) {
      submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
      fireJobTasksEvent(channel, nodeBundle, false);
    } else {
      final ServerJobBroadcast broadcast = (ServerJobBroadcast) serverJob;
      submitEvent(JobEventType.JOB_RETURNED, broadcast.getParentJob(), channel);
    }
    final JPPFStatistics stats = driver.getStatistics();
    stats.addValue(JPPFStatisticsHelper.JOB_DISPATCH_COUNT, -1);
    stats.addValue(JPPFStatisticsHelper.JOB_DISPATCH_TIME, System.currentTimeMillis() - nodeBundle.getDispatchStartTime());
  }

  /**
   * Called when a job is added to the server queue.
   * @param serverJob the queued job.
   */
  public void jobQueued(final ServerJob serverJob) {
    final TaskBundle bundle = serverJob.getJob();
    if (debugEnabled) log.debug("jobId '{}' queued", bundle.getName());
    if (!isBroadcastDispatch(serverJob)) {
      submitEvent(JobEventType.JOB_QUEUED, serverJob, null);
    }
    final JPPFStatistics stats = driver.getStatistics();
    stats.addValue(JPPFStatisticsHelper.JOB_TOTAL, 1);
    stats.addValue(JPPFStatisticsHelper.JOB_COUNT, 1);
    stats.addValue(JPPFStatisticsHelper.JOB_TASKS, bundle.getTaskCount());
  }

  /**
   * Called when a job is complete and returned to the client.
   * @param serverJob the completed job.
   */
  public void jobEnded(final ServerJob serverJob) {
    if (serverJob == null) throw new IllegalArgumentException("bundleWrapper is null");
    final TaskBundle bundle = serverJob.getJob();
    if (bundle.isHandshake()) return; // skip notifications for handshake bundles
    final long time = System.currentTimeMillis() - serverJob.getJobReceivedTime();
    final String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      jobMap.removeValues(jobUuid);
    }
    if (debugEnabled) log.debug("jobId '{}' ended", bundle.getName());
    if (serverJob.getSLA().getDesiredNodeConfiguration() != null) {
      final NodeReservationHandler handler = driver.getAsyncNodeNioServer().getNodeReservationHandler();
      handler.removeJobReservations(serverJob.getUuid());
    }
    if (!isBroadcastDispatch(serverJob)) submitEvent(JobEventType.JOB_ENDED, serverJob, null);
    final JPPFStatistics stats = driver.getStatistics();
    stats.addValue(JPPFStatisticsHelper.JOB_COUNT, -1);
    stats.addValue(JPPFStatisticsHelper.JOB_TIME, time);
    stats.addValue(JPPFStatisticsHelper.DISPATCH_PER_JOB_COUNT, ((AbstractServerJobBase) serverJob).getTotalDispatches());
  }

  @Override
  public void jobUpdated(final AbstractServerJob job, final boolean headerUpdated) {
    //if (debugEnabled) log.debug("jobId '{}' updated", job.getName());
    if (headerUpdated) driver.getQueue().getPersistenceHandler().updateJobHeader((ServerJob) job);
    submitEvent(JobEventType.JOB_UPDATED, (ServerJob) job, null);
  }

  @Override
  public void jobStatusChanged(final AbstractServerJob source, final SubmissionStatus oldValue, final SubmissionStatus newValue) {
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param bundle the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final TaskBundle bundle, final ExecutorChannel<?> channel) {
    try {
      eventQueue.put(newJobNotification(this, eventType, bundle, null, channel));
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param job the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final ServerJob job, final ExecutorChannel<?> channel) {
    try {
      eventQueue.put(newJobNotification(this, eventType, null, job, channel));
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Create a job notification with the specified parameters.
   * @param jobManager the job manager that submits the events.
   * @param eventType the type of event to generate.
   * @param bundle the task bundle data.
   * @param job the job data.
   * @param channel the id of the job source of the event.
   * @return a {@link JobNotification} instance.
   */
  private static JobNotification newJobNotification(final JobNotificationEmitter jobManager, final JobEventType eventType, final TaskBundle bundle, final ServerJob job, final ExecutorChannel<?> channel) {
    final JobSLA sla;
    final JobInformation jobInfo;
    if (job != null) {
      sla = job.getSLA();
      jobInfo = new JobInformation(job.getUuid(), job.getName(), job.getTaskCount(), job.getInitialTaskCount(), sla.getPriority(), job.isSuspended(), job.isPending());
    } else {
      sla = bundle.getSLA();
      jobInfo = new JobInformation(bundle.getUuid(), bundle.getName(), bundle.getCurrentTaskCount(), bundle.getInitialTaskCount(), sla.getPriority(), sla.isSuspended(),
        bundle.getParameter(BundleParameter.JOB_PENDING, false));
    }
    jobInfo.setMaxNodes(sla.getMaxNodes());
    final JPPFManagementInfo nodeInfo = (channel == null) ? null : channel.getManagementInfo();
    return new JobNotification(jobManager.getEmitterUuid(), eventType, jobInfo, nodeInfo, System.currentTimeMillis());
  }

  /**
   * Close this job manager and release its resources.
   */
  public synchronized void close() {
    eventQueue.close();
    jobMap.clear();
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addJobManagerListener(final JobManagerListener listener) {
    jobManagerListeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeJobManagerListener(final JobManagerListener listener) {
    jobManagerListeners.remove(listener);
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
        for (JobManagerListener listener: jobManagerListeners) listener.jobQueued(event);
        break;

      case JOB_ENDED:
        for (JobManagerListener listener: jobManagerListeners) listener.jobEnded(event);
        break;

      case JOB_UPDATED:
        for (JobManagerListener listener: jobManagerListeners) listener.jobUpdated(event);
        break;

      case JOB_DISPATCHED:
        for (JobManagerListener listener: jobManagerListeners) listener.jobDispatched(event);
        break;

      case JOB_RETURNED:
        for (JobManagerListener listener: jobManagerListeners) listener.jobReturned(event);
        break;

      default:
        throw new IllegalStateException("Unsupported event type: " + event.getEventType());
    }
  }

  @Override
  public String getEmitterUuid() {
    return driver.getUuid();
  }

  /**
   * Add a listener to the list of dispatch listeners.
   * @param listener the listener to add to the list.
   */
  @Override
  public void addJobTasksListener(final JobTasksListener listener) {
    if (debugEnabled) log.debug("adding JobTasksListener {}", listener);
    taskReturnListeners.add(listener);
  }

  /**
   * Remove a listener from the list of dispatch listeners.
   * @param listener the listener to remove from the list.
   */
  @Override
  public void removeJobTasksListener(final JobTasksListener listener) {
    if (debugEnabled) log.debug("removing JobTasksListener {}", listener);
    taskReturnListeners.remove(listener);
  }

  /**
   * Called when final tasks results have been received and are about to be sent back to the client.
   * @param channel the node to which the job is dispatched.
   * @param job the job for which results are received.
   * @param tasks the job's tasks for which there are results.
   */
  public synchronized void jobResultsReceived(final ExecutorChannel<?> channel, final ServerJob job, final Collection<ServerTask> tasks) {
    driver.getQueue().getPersistenceHandler().storeResults(job, tasks);
    if (!taskReturnListeners.isEmpty()) {
      if (debugEnabled) log.debug("results received with channel={}, job={}, nb Tasks={}", channel, job, tasks.size());
      final JobTasksEvent event = createJobTasksEvent(channel, job, tasks);
      for (final JobTasksListener listener: taskReturnListeners) listener.resultsReceived(event);
    }
  }

  /**
   * Fire a job dispatch event.
   * @param channel the node to which the job is dispatched.
   * @param nodeBundle the task bundle returned from the node.
   * @param isDispatch whether this is a dispatch event notification.
   */
  private void fireJobTasksEvent(final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle, final boolean isDispatch) {
    if (!taskReturnListeners.isEmpty()) {
      final JobTasksEvent event = createJobTasksEvent(channel, nodeBundle);
      if (isDispatch) {
        for (JobTasksListener listener: taskReturnListeners) listener.tasksDispatched(event);
      } else {
        for (JobTasksListener listener: taskReturnListeners) listener.tasksReturned(event);
      }
    }
  }

  /**
   * Fire a job dispatch event.
   * @param channel the node to which the job is dispatched.
   * @param nodeBundle the task bundle returned from the node.
   * @return an instance of {@link TaskReturnEvent}.
   */
  private static JobTasksEvent createJobTasksEvent(final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle) {
    final List<ServerTask> tasks = nodeBundle.getTaskList();
    final List<ServerTaskInformation> taskInfos = new ArrayList<>(tasks.size());
    for (final ServerTask task: tasks) taskInfos.add(new ServerTaskInformation(
      task.getPosition(), task.getThrowable(), task.getExpirationCount(), task.getMaxResubmits(), task.getTaskResubmitCount(), task.getResult()));
    final TaskBundle job = nodeBundle.getJob();
    return new JobTasksEvent(job.getUuid(), job.getName(), job.getSLA(), job.getMetadata(), taskInfos, nodeBundle.getJobReturnReason(), channel.getManagementInfo());
  }

  /**
   * Fire a job dispatch event.
   * @param channel the node to which the job is dispatched.
   * @param job the job for which results are received.
   * @param tasks the job's tasks for which there are results.
   * @return an instance of {@link TaskReturnEvent}.
   */
  private static JobTasksEvent createJobTasksEvent(final ExecutorChannel<?> channel, final ServerJob job, final Collection<ServerTask> tasks) {
    final List<ServerTaskInformation> taskInfos = new ArrayList<>(tasks.size());
    for (final ServerTask task: tasks) taskInfos.add(new ServerTaskInformation(
      task.getPosition(), task.getThrowable(), task.getExpirationCount(), task.getMaxResubmits(), task.getTaskResubmitCount(), task.getResult()));
    return new JobTasksEvent(job.getUuid(), job.getName(), job.getSLA(), job.getMetadata(), taskInfos, JobReturnReason.RESULTS_RECEIVED, channel.getManagementInfo());
  }

  /**
   * Load all the dispatch listeners defined in the classpath with SPI.
   */
  public void loadTaskReturnListeners() {
    if (debugEnabled) log.debug("loading task return listeners");
    final List<JobTasksListener> list2 = new ServiceFinder().findProviders(JobTasksListener.class);
    for (final JobTasksListener listener: list2) addJobTasksListener(listener);
  }

  /**
   * Get the count of notifications in the executor's quueue.
   * @return the ocunt as an int.
   */
  public int getNotifCount() {
    return eventQueue.size();
  }

  /**
   * Peak count of notifications in the executor's quueue.
   * @return the ocunt as an int.
   */
  public int getNotifMax() {
    return eventQueue.getPeakSize();
  }

  /**
   * Determine whether the specified job is the dispatch of a broadcast job to a node.
   * @param job the job to evaluate.
   * @return {@code true} if the job is the dispatch of a broadcast job, {@code false} otherwise.
   */
  public static boolean isBroadcastDispatch(final AbstractServerJob job) {
    if (!(job instanceof ServerJobBroadcast)) return false;
    return job.getBroadcastUUID() != null;
  }
}
