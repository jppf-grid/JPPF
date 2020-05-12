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
package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class group tasks from the same client channel together.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class ServerTaskBundleClient {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerTaskBundleClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Count of instances of this class.
   */
  private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);
  /**
   * A unique id for this client bundle.
   */
  private final long id;
  /**
   * The job to execute.
   */
  private final TaskBundle job;
  /**
   * The shared data provider for this task bundle.
   */
  private final DataLocation dataProvider;
  /**
   * The tasks to be executed by the node.
   */
  private final List<ServerTask> taskList = new LinkedList<>();
  /**
   * The tasks to be sent back to the client.
   */
  private final List<ServerTask> tasksToSendList = new LinkedList<>();
  /**
   * The tasks to be executed by the node.
   */
  private final List<ServerTask> nullTasks = new LinkedList<>();
  /**
   * The count of pending tasks.
   */
  private final AtomicInteger pendingTasksCount = new AtomicInteger();
  /**
   * The list of listeners registered with this bundle.
   */
  private final List<CompletionListener> listenerList = new CopyOnWriteArrayList<>();
  /**
   * Bundle cancel indicator.
   */
  private boolean cancelled;
  /**
   * Bundle done indicator.
   */
  private boolean done;
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  private long jobReceivedTime;
  /**
   * The strategy to use to send the results back to the client.
   */
  final SendResultsStrategy strategy;
  /**
   * Id of the source bundle, if any. If no source bundle then it is set to -1.
   */
  final long sourceBundleId;
  /**
   * The positions of the tasks in this bundle.
   */
  private final int[] tasksPositions;
  /**
   * Whether ths client bundle is ended.
   */
  private final AtomicBoolean isEnded = new AtomicBoolean(false);

  /**
   * Initialize this task bundle and set its build number.
   * @param job the job to execute.
   * @param dataProvider the shared data provider for this task bundle.
   * @param taskList the tasks to execute.
   * @param forPeer whether the job comes from a peer driver.
   */
  public ServerTaskBundleClient(final TaskBundle job, final DataLocation dataProvider, final List<DataLocation> taskList, final boolean forPeer) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");
    id = INSTANCE_COUNT.incrementAndGet();
    this.job = job;
    this.dataProvider = dataProvider;
    this.sourceBundleId = -1L;
    if (!job.isHandshake() && !job.getParameter(BundleParameter.CLOSE_COMMAND, false)) {
      final int[] positions = job.getParameter(BundleParameter.TASK_POSITIONS);
      final int[] maxResubmits = job.getParameter(BundleParameter.TASK_MAX_RESUBMITS);
      final int slaMaxResubmits = job.getSLA().getMaxTaskResubmits();
      if (log.isTraceEnabled()) log.trace("id={}, nbTasks={}, nbPositions={} : {}", id, taskList.size(), (positions == null) ? -1 : positions.length, (positions == null) ? "null" : Arrays.toString(positions));
      for (int i = 0; i < taskList.size(); i++) {
        final DataLocation dataLocation = taskList.get(i);
        if ((positions == null) && !job.isHandshake()) throw new IllegalStateException("positions is null for " + this);
        if ((positions != null) &&  (i >= positions.length)) throw new IllegalStateException(i + " >= " + positions.length + " for " + this);
        final int pos = (positions == null)  || (i >= positions.length) ? -1 : positions[i];
        int maxResubmitCount = (maxResubmits == null) || (i > maxResubmits.length - 1) ? -1 : maxResubmits[i];
        if ((maxResubmitCount < 0) && (slaMaxResubmits >= 0)) maxResubmitCount = slaMaxResubmits;
        final ServerTask task = new ServerTask(this, dataLocation, pos, maxResubmitCount);
        if (dataLocation == null) {
          if (debugEnabled) log.debug("got null task at index {} for {}", i, job);
          nullTasks.add(task);
          task.resultReceived(task.getInitialTask());
        } else {
          this.taskList.add(task);
        }
      }
      this.pendingTasksCount.set(this.taskList.size() + nullTasks.size());
      if (forPeer) this.strategy = new SendResultsStrategy.SendAllResultsStrategy();
      else this.strategy = SendResultsStrategyManager.getStrategy(job.getSLA().getResultsStrategy());
    } else this.strategy = SendResultsStrategyManager.getStrategy(null);
    this.tasksPositions = computeTasksPositions();
  }

  /**
   * Initialize this task bundle and set its build number.
   * @param tasks the tasks to execute.
   * @param job the job to execute.
   * @param dataProvider the shared data provider for this task bundle.
   */
  public ServerTaskBundleClient(final Collection<ServerTask> tasks, final TaskBundle job, final DataLocation dataProvider) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");
    id = INSTANCE_COUNT.incrementAndGet();
    this.job = job;
    this.dataProvider = dataProvider;
    this.taskList.addAll(tasks);
    for (final ServerTask task: tasks) task.setBundle(this);
    this.pendingTasksCount.set(tasks.size());
    this.strategy = SendResultsStrategyManager.getStrategy(job.getSLA().getResultsStrategy());
    this.sourceBundleId = -1L;
    this.tasksPositions = computeTasksPositions();
  }

  /**
   * Initialize task bundle as copy as source bundle.
   * @param source the source bundle.
   * @param taskList the tasks to return.
   */
  private ServerTaskBundleClient(final ServerTaskBundleClient source, final List<ServerTask> taskList) {
    if (source == null) throw new IllegalArgumentException("source is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");
    id = INSTANCE_COUNT.incrementAndGet();
    final int size = taskList.size();
    this.job = source.getJob().copy();
    this.job.removeParameter(BundleParameter.JOB_TASK_GRAPH);
    this.job.setTaskCount(size);
    this.job.setInitialTaskCount(source.getJob().getInitialTaskCount());
    this.job.setCurrentTaskCount(size);
    this.dataProvider = source.getDataProvider();
    this.taskList.addAll(taskList);
    this.pendingTasksCount.set(0);
    this.done = source.isDone();
    this.cancelled = source.isCancelled();
    this.strategy = source.strategy;
    this.sourceBundleId = source.getId();
    this.tasksPositions = computeTasksPositions();
  }

  /**
   * @return the poisitions of the tasks in this bundle.
   */
  private int[] computeTasksPositions() {
    final int[] positions = new int[taskList.size()];
    int count = 0;
    for (final ServerTask task: taskList) positions[count++] = task.getPosition();
    return positions;
  }

  /**
   * Get the job this submission is for.
   * @return a {@link TaskBundle} instance.
   */
  public TaskBundle getJob() {
    return job;
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataLocation getDataProvider() {
    return dataProvider;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a <code>List</code> of arrays of bytes.
   */
  public List<ServerTask> getTaskList() {
    return taskList;
  }

  /**
   * Send back the null tasks immediately.
   */
  public void handleNullTasks() {
    if (!nullTasks.isEmpty()) {
      if (debugEnabled) log.debug("received {} null tasks", nullTasks.size());
      resultReceived(nullTasks);
      nullTasks.clear();
    }
  }

  /**
   * Called to notify that the contained task received result.
   * @param results the tasks for which results were received.
   */
  public synchronized void resultReceived(final Collection<ServerTask> results) {
    List<ServerTask> completedTasks = null;
    if (isCancelled()) return;
    if (debugEnabled) log.debug("received {} tasks for {}", results.size(), this);
    final List<ServerTask> tasks = new ArrayList<>(results.size());
    for (final ServerTask task: results) {
      if (task.getState() != TaskState.PENDING) {
        tasks.add(task);
        tasksToSendList.add(task);
        pendingTasksCount.decrementAndGet();
      }
    }
    done = pendingTasksCount.get() <= 0;
    final boolean shouldFire = done || strategy.sendResults(this, tasks);
    if (shouldFire) completedTasks = getAndClearCompletedTasks();
    if (debugEnabled) log.debug("processed {} tasks, completedTasks={}, done={}, tasksToSend={}, pendingTasksCount={}",
      tasks.size(), (completedTasks == null) ? 0 : completedTasks.size(), done, tasksToSendList.size(), pendingTasksCount.get());
    if (completedTasks != null) fireTasksCompleted(completedTasks);
  }

  /**
   * Called to notify that the task received exception during execution.
   * @param tasks the tasks for which an exception was received.
   * @param exception the exception.
   */
  public synchronized void resultReceived(final Collection<ServerTask> tasks, final Throwable exception) {
    List<ServerTask> completedTasks = null;
    if (isCancelled()) return;
    if (debugEnabled) log.debug("received exception [{}] for {}", ExceptionUtils.getMessage(exception), this);
    int count = 0;
    for (final ServerTask task: tasks) {
      if (task.getState() != TaskState.PENDING) {
        tasksToSendList.add(task);
        count++;
      }
      if (count > 0) pendingTasksCount.addAndGet(-count);
      task.resultReceived(exception);
    }
    done = pendingTasksCount.get() <= 0;
    final boolean shouldFire = done || strategy.sendResults(this, tasks);
    if (shouldFire) completedTasks = getAndClearCompletedTasks();
    if (debugEnabled) log.debug("processed {} tasks, completedTasks={}, done={}, tasksToSend={}", tasks.size(), (completedTasks == null ? 0 : completedTasks.size()), done, tasksToSendList.size());
    if (completedTasks != null) fireTasksCompleted(completedTasks);
  }

  /**
   * Get the job received time.
   * @return the time in milliseconds as a long value.
   */
  public long getJobReceivedTime() {
    return jobReceivedTime;
  }

  /**
   * Set the job received time.
   * @param jobReceivedTime the time in milliseconds as a long value.
   */
  public void setJobReceivedTime(final long jobReceivedTime) {
    this.jobReceivedTime = jobReceivedTime;
  }

  /**
   * Called when this task bundle is cancelled.
   */
  public void cancel() {
    List<ServerTask> completedTasks = null;
    synchronized(this) {
      if (!cancelled && !done) {
        if (debugEnabled) log.debug("cancelling client job " + this);
        this.cancelled = true;
        int count = 0;
        for (ServerTask task: taskList) {
          if (task.getState() == TaskState.PENDING) {
            task.cancel();
            tasksToSendList.add(task);
            count++;
            //pendingTasksCount.decrementAndGet();
          }
        }
        if (count > 0) pendingTasksCount.addAndGet(-count);
        this.done = true;
        completedTasks = getAndClearCompletedTasks();
      }
    }
    fireTasksCompleted(completedTasks);
  }

  /**
   * Make a copy of the tasks to send list, clear the list and return the copy.
   * @return a list of {@link ServerTask}s.
   */
  private synchronized List<ServerTask> getAndClearCompletedTasks() {
    final List<ServerTask> completedTasks = new ArrayList<>(tasksToSendList);
    tasksToSendList.clear();
    return completedTasks;
  }

  /**
   * Get the cancelled indicator.
   * @return <code>true</code> if job is cancelled, <code>false</code> otherwise.
   */
  public synchronized boolean isCancelled() {
    return cancelled;
  }

  /**
   * Get the <code>done</code> indicator.
   * @return <code>true</code> if this client job is done, <code>false</code> otherwise.
   */
  public synchronized boolean isDone() {
    return done;
  }

  /**
   * Extract <code>DataLocation</code> list from contained tasks.
   * @return the list of <code>DataLocation</code> instances.
   */
  public List<DataLocation> getDataLocationList() {
    final List<DataLocation> list = new ArrayList<>(taskList.size());
    for (final ServerTask task : taskList) list.add(task.getInitialTask());
    return list;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link JobSLA}.
   */
  public JobSLA getSLA() {
    return job.getSLA();
  }

  /**
   * Get the universal unique id for this bundle.
   * @return the uuid as a string.
   */
  public String getUuid() {
    return job.getUuid();
  }

  /**
   * Get the number of tasks in the bundle.
   * @return the number of tasks as an int.
   */
  public int getTaskCount() {
    return taskList.size();
  }

  /**
   * Get the number of tasks that remain to execute.
   * @return the number of tasks as an int.
   */
  public int getPendingTasksCount() {
    return pendingTasksCount.get();
  }

  /**
   * Notifies that tasks have been completed.
   * @param completedTasks the task whose results to send.
   */
  private void fireTasksCompleted(final List<ServerTask> completedTasks) {
    if (completedTasks != null) {
      final ServerTaskBundleClient bundle = new ServerTaskBundleClient(this, completedTasks);
      if (debugEnabled) log.debug("created bundle id=" + bundle.id + " for " + this);
      for (final CompletionListener listener : listenerList) listener.taskCompleted(bundle, completedTasks);
    }
  }

  /**
   * Notifies that bundle is completely executed.
   */
  public void bundleEnded() {
    if (debugEnabled) log.debug("bundle ended {}", this);
    if (isEnded.compareAndSet(false, true)) {
      for (final CompletionListener listener : listenerList) listener.bundleEnded(this);
    }
  }

  /**
   * Add a listener to the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void addCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    if (debugEnabled) log.debug("adding CompletionListener {} to {}", listener, this);
    listenerList.add(listener);
  }

  /**
   * Remove a listener from the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void removeCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    listenerList.remove(listener);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", pendingTasks=").append(pendingTasksCount);
    sb.append(", taskList.size()=").append(taskList.size());
    sb.append(", cancelled=").append(cancelled);
    sb.append(", done=").append(done);
    sb.append(", job=").append(job);
    sb.append("; strategy=").append(strategy == null ? "null" : strategy.getName());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the unique id for this client bundle.
   * @return the id as a long value.
   */
  public long getId() {
    return id;
  }

  /**
   * @return the id of the bundle from which this one was created, if any. If this bundle is the roigial one, then its id is returned.
   */
  public long getOriginalBundleId() {
    return (sourceBundleId < 0L) ? id : sourceBundleId;
  }

  /**
   * Listener providing a callback to invoke when a task's execution has completed.
   * @author Martin JANDA
   * @exclude
   */
  public interface CompletionListener {
    /**
     * Callback method invoked when the execution of the bundle has completed.
     * @param bundle the bundle that notifies completed result.
     * @param results the list of results of the bundle execution.
     */
    void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results);

    /**
     * Callback method invoked when execution of the bundle is done.
     * @param bundle the bundle that notifies that finished.
     */
    void bundleEnded(final ServerTaskBundleClient bundle);
  }

  /**
   * @return the positions of the tasks in this bundle.
   */
  public int[] getTasksPositions() {
    return tasksPositions;
  }
}
