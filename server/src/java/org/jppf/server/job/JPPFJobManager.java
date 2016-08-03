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

package org.jppf.server.job;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.execute.ExecutorChannel;
import org.jppf.job.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
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
  private static Logger log = LoggerFactory.getLogger(JPPFJobManager.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Mapping of jobs to the nodes they are executing on.
   */
  private final CollectionMap<String, ChannelJobPair> jobMap = new ArrayListHashMap<>();
  /**
   * Processes the event queue asynchronously.
   */
  private final ExecutorService executor;
  /**
   * The list of registered job life manager listeners.
   */
  private final List<JobManagerListener> jobManagerListeners = new CopyOnWriteArrayList<>();
  /**
   * The list of registered job dispatch listeners.
   */
  private final List<JobTasksListener> taskReturnListeners = new CopyOnWriteArrayList<>();
  /**
   * Count of notifications in the executor's quueue.
   */
  private final AtomicInteger notifCount = new AtomicInteger(0);
  /**
   * Peak count of notifications in the executor's quueue.
   */
  private final AtomicInteger notifMax = new AtomicInteger(0);

  /**
   * Default constructor.
   */
  public JPPFJobManager() {
    //executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobManager"));
    executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new JPPFThreadFactory("JobManager")) {
      @Override
      protected void afterExecute(final Runnable r, final Throwable t) {
        if (JPPFDriver.JPPF_DEBUG) notifCount.decrementAndGet();
      }
    };
  }

  /**
   * Get all the nodes to which a all or part of a job is dispatched.
   * @param jobUuid the id of the job.
   * @return a list of <code>SelectableChannel</code> instances.
   */
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

  @Override
  public void jobDispatched(final AbstractServerJob serverJob, final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle) {
    TaskBundle bundle = nodeBundle.getJob();
    String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      jobMap.putValue(jobUuid, new ChannelJobPair(channel, serverJob));
    }
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : added node " + channel);
    submitEvent(JobEventType.JOB_DISPATCHED, bundle, channel);
    fireJobTasksEvent(channel, nodeBundle, true);
  }

  @Override
  public synchronized void jobReturned(final AbstractServerJob serverJob, final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle) {
    TaskBundle bundle = nodeBundle.getJob();
    String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      if (!jobMap.removeValue(jobUuid, new ChannelJobPair(channel, serverJob))) {
        log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + bundle.getName());
      } else if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : removed node " + channel);
    }
    //if (debugEnabled) log.debug("call stack:\n{}", ExceptionUtils.getCallStack());
    submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
    fireJobTasksEvent(channel, nodeBundle, false);
  }

  /**
   * Called when a job is added to the server queue.
   * @param serverJob the queued job.
   */
  public void jobQueued(final ServerJob serverJob) {
    TaskBundle bundle = serverJob.getJob();
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' queued");
    submitEvent(JobEventType.JOB_QUEUED, serverJob, null);
    JPPFStatistics stats = JPPFDriver.getInstance().getStatistics();
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
    if (serverJob.getJob().isHandshake()) return; // skip notifications for handshake bundles

    TaskBundle bundle = serverJob.getJob();
    long time = System.currentTimeMillis() - serverJob.getJobReceivedTime();
    String jobUuid = bundle.getUuid();
    synchronized(jobMap) {
      jobMap.removeValues(jobUuid);
    }
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' ended");
    //if (debugEnabled) log.debug("call stack:\n{}", ExceptionUtils.getCallStack());
    submitEvent(JobEventType.JOB_ENDED, serverJob, null);
    JPPFStatistics stats = JPPFDriver.getInstance().getStatistics();
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
    //jobUpdated(source);
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param bundle the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final TaskBundle bundle, final ExecutorChannel<?> channel) {
    executor.submit(new JobEventTask(this, eventType, bundle, null, channel));
    if (JPPFDriver.JPPF_DEBUG) incNotifCount();
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param job the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final ServerJob job, final ExecutorChannel<?> channel) {
    executor.submit(new JobEventTask(this, eventType, null, job, channel));
    if (JPPFDriver.JPPF_DEBUG) incNotifCount();
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
    return JPPFDriver.getInstance().getUuid();
  }

  /**
   * Add a listener to the list of dispatch listeners.
   * @param listener the listener to add to the list.
   * @deprecated use {@link #addJobTasksListener(JobTasksListener)} instead.
   */
  @Override
  public void addTaskReturnListener(final TaskReturnListener listener) {
    if (listener != null) taskReturnListeners.add(new DelegatingJobTasksListener(listener));
  }

  /**
   * Remove a listener from the list of dispatch listeners.
   * @param listener the listener to remove from the list.
   * @deprecated use {@link #removeJobTasksListener(JobTasksListener)} instead.
   */
  @Override
  public void removeTaskReturnListener(final TaskReturnListener listener) {
    if (listener != null) taskReturnListeners.remove(new DelegatingJobTasksListener(listener));
  }

  /**
   * Add a listener to the list of dispatch listeners.
   * @param listener the listener to add to the list.
   */
  @Override
  public void addJobTasksListener(final JobTasksListener listener) {
    taskReturnListeners.add(listener);
  }

  /**
   * Remove a listener from the list of dispatch listeners.
   * @param listener the listener to remove from the list.
   */
  @Override
  public void removeJobTasksListener(final JobTasksListener listener) {
    taskReturnListeners.remove(listener);
  }

  /**
   * Fire a job dispatch event.
   * @param channel the node to which the job is dispatched.
   * @param nodeBundle the task bundle returned from the node.
   * @param isDispatch whether this is a dispatch event notification.
   */
  private void fireJobTasksEvent(final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle, final boolean isDispatch) {
    if (!taskReturnListeners.isEmpty()) {
      JobTasksEvent event = createJobTasksEvent(channel, nodeBundle);
      for (JobTasksListener listener: taskReturnListeners) {
        if (isDispatch) listener.tasksDispatched(event);
        else listener.tasksReturned(event);
      }
    }
  }

  /**
   * Fire a job dispatch event.
   * @param channel the node to which the job is dispatched.
   * @param nodeBundle the task bundle returned from the node.
   * @return an instance of {@link TaskReturnEvent}.
   */
  private JobTasksEvent createJobTasksEvent(final ExecutorChannel<?> channel, final ServerTaskBundleNode nodeBundle) {
    List<ServerTask> tasks = nodeBundle.getTaskList();
    List<ServerTaskInformation> taskInfos = new ArrayList<>(tasks.size());
    for (ServerTask task: tasks) taskInfos.add(new ServerTaskInformation(
        task.getJobPosition(), task.getThrowable(), task.getExpirationCount(), task.getMaxResubmits(), task.getTaskResubmitCount()));
    TaskBundle job = nodeBundle.getJob();
    return new JobTasksEvent(job.getUuid(), job.getName(), taskInfos, nodeBundle.getJobReturnReason(), channel.getManagementInfo());
  }

  /**
   * Load all the dispatch listeners defined in the classpath with SPI.
   */
  @SuppressWarnings("deprecation")
  public void loadTaskReturnListeners() {
    List<TaskReturnListener> list = new ServiceFinder().findProviders(TaskReturnListener.class);
    for (TaskReturnListener listener: list) addJobTasksListener(new DelegatingJobTasksListener(listener));
    List<JobTasksListener> list2 = new ServiceFinder().findProviders(JobTasksListener.class);
    for (JobTasksListener listener: list2) addJobTasksListener(listener);
  }

  /**
   * Get the count of notifications in the executor's quueue.
   * @return the ocunt as an int.
   */
  public int getNotifCount() {
    return notifCount.get();
  }

  /**
   * Peak count of notifications in the executor's quueue.
   * @return the ocunt as an int.
   */
  public int getNotifMax() {
    return notifMax.get();
  }

  /**
   * Increment the current count of notifications and update the peak value.
   */
  private void incNotifCount() {
    int n = notifCount.incrementAndGet();
    if (n > notifMax.get()) notifMax.set(n);
  }

  /**
   * Delegates {@link JobTasksEvent} notifications to an old, deprecated {@link TaskReturnListener}.
   */
  @SuppressWarnings("deprecation")
  private static class DelegatingJobTasksListener implements JobTasksListener {
    /**
     * The listener to delegate to.
     */
    private final TaskReturnListener delegate;

    /**
     * Initialize with a listener to delegate to.
     * @param delegate the listener to delegate to.
     */
    DelegatingJobTasksListener(final TaskReturnListener delegate) {
      if (delegate == null) throw new IllegalArgumentException("delegate can't be null");
      this.delegate = delegate;
    }

    @Override
    public void tasksDispatched(final JobTasksEvent event) {
    }

    @Override
    public void tasksReturned(final JobTasksEvent event) {
      delegate.tasksReturned(event);
    }

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof DelegatingJobTasksListener)) return false;
      return delegate.equals(((DelegatingJobTasksListener) obj).delegate);
    }
  }
}
