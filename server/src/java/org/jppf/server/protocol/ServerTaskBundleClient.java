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
package org.jppf.server.protocol;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.*;
import org.jppf.utils.ExceptionUtils;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Count of instances of this class.
   */
  private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);
  /**
   * A unique id for this client bundle.
   */
  private final long id = INSTANCE_COUNT.incrementAndGet();
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
  private boolean cancelled = false;
  /**
   * Bundle done indicator.
   */
  private boolean done = false;
  /**
   * Time at which the job is received on the server side. In milliseconds since January 1, 1970 UTC.
   */
  private long jobReceivedTime = 0L;
  /**
   * The strategy to use to send the results back tot he client.
   */
  //private SendResultsStrategy strategy = new SendAllResultsStrategy();
  final SendResultsStrategy strategy;

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param dataProvider the shared data provider for this task bundle.
   */
  public ServerTaskBundleClient(final TaskBundle job, final DataLocation dataProvider) {
    this(job, dataProvider, Collections.<DataLocation>emptyList());
  }

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param dataProvider the shared data provider for this task bundle.
   * @param taskList the tasks to execute.
   */
  public ServerTaskBundleClient(final TaskBundle job, final DataLocation dataProvider, final List<DataLocation> taskList) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");
    this.job = job;
    int[] positions = job.getParameter(BundleParameter.TASK_POSITIONS);
    int[] maxResubmits = job.getParameter(BundleParameter.TASK_MAX_RESUBMITS);
    this.dataProvider = dataProvider;
    int slaMaxResubmits = job.getSLA().getMaxTaskResubmits();
    for (int index = 0; index < taskList.size(); index++) {
      DataLocation dataLocation = taskList.get(index);
      int pos = (positions == null) || (index > positions.length - 1) ? -1 : positions[index];
      int maxResubmitCount = (maxResubmits == null) || (index > maxResubmits.length - 1) ? -1 : maxResubmits[index];
      if ((maxResubmitCount < 0) && (slaMaxResubmits >= 0)) maxResubmitCount = slaMaxResubmits;
      ServerTask task = new ServerTask(this, dataLocation, pos, maxResubmitCount);
      if (dataLocation == null) {
        if (debugEnabled) log.debug("job '" + job.getName() + "' has null task at index " + index);
        nullTasks.add(task);
        task.resultReceived(task.getInitialTask());
      } else {
        this.taskList.add(task);
      }
    }
    this.pendingTasksCount.set(this.taskList.size() + nullTasks.size());
    this.strategy = SendResultsStrategyManager.getStrategy(job.getSLA().getResultsStrategy());
  }

  /**
   * Initialize task bundle as copy as source bundle.
   * @param source the source bundle.
   * @param taskList the tasks to return.
   */
  private ServerTaskBundleClient(final ServerTaskBundleClient source, final List<ServerTask> taskList) {
    if (source == null) throw new IllegalArgumentException("source is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");
    int size = taskList.size();
    //job = source.getJob().copy(size);
    this.job = source.getJob().copy();
    this.job.setTaskCount(size);
    this.job.setInitialTaskCount(source.getJob().getInitialTaskCount());
    this.job.setCurrentTaskCount(size);
    this.dataProvider = source.getDataProvider();
    this.taskList.addAll(taskList);
    this.pendingTasksCount.set(0);
    this.done = source.isDone();
    this.cancelled = source.isCancelled();
    this.strategy = source.strategy;
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
  public List<ServerTask> getTaskList()
  {
    return taskList;
  }

  /**
   * Send back the null tasks immediately.
   */
  public void handleNullTasks() {
    if (!nullTasks.isEmpty()) {
      resultReceived(nullTasks);
      nullTasks.clear();
    }
  }

  /**
   * Called to notify that the contained task received result.
   * @param results the tasks for which results were received.
   */
  public void resultReceived(final Collection<ServerTask> results) {
    List<ServerTask> completedTasks = null;
    synchronized (this) {
      if (isCancelled()) return;
      if (debugEnabled) log.debug("received " + results.size() + " tasks for " + this);
      List<ServerTask> tasks = new ArrayList<>(results.size());
      for (ServerTask task: results) {
        if (task.getState() != TaskState.PENDING) {
          tasks.add(task);
          tasksToSendList.add(task);
          pendingTasksCount.decrementAndGet();
        }
      }
      done = pendingTasksCount.get() <= 0;
      boolean shouldFire = done || strategy.sendResults(this, tasks);
      if (shouldFire) completedTasks = getAndClearCompletedTasks();
    }
    if (completedTasks != null) fireTasksCompleted(completedTasks);
  }

  /**
   * Called to notify that the task received exception during execution.
   * @param tasks the tasks for which an exception was received.
   * @param exception the exception.
   */
  public synchronized void resultReceived(final Collection<ServerTask> tasks, final Throwable exception) {
    List<ServerTask> completedTasks = null;
    synchronized (this) {
      if (isCancelled()) return;
      if (debugEnabled) log.debug("received exception [" + ExceptionUtils.getMessage(exception) + "] for " + this);
      for (ServerTask task: tasks) {
        if (task.getState() != TaskState.PENDING) {
          tasksToSendList.add(task);
          pendingTasksCount.decrementAndGet();
        }
        task.resultReceived(exception);
      }
      done = pendingTasksCount.get() <= 0;
      boolean shouldFire = done || strategy.sendResults(this, tasks);
      if (shouldFire) completedTasks = getAndClearCompletedTasks();
    }
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
        for (ServerTask task: taskList) {
          if (task.getState() == TaskState.PENDING) {
            task.cancel();
            tasksToSendList.add(task);
            pendingTasksCount.decrementAndGet();
          }
        }
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
    List<ServerTask> completedTasks = new ArrayList<>(tasksToSendList);
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
    List<DataLocation> list = new ArrayList<>(taskList.size());
    for (ServerTask task : taskList) list.add(task.getInitialTask());
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
    ServerTaskBundleClient bundle = new ServerTaskBundleClient(this, completedTasks);
    if (debugEnabled) log.debug("created bundle id=" + bundle.id + " for " + this);
    for (CompletionListener listener : listenerList) listener.taskCompleted(bundle, completedTasks);
  }

  /**
   * Notifies that bundle is completely executed.
   */
  public void bundleEnded() {
    for (CompletionListener listener : listenerList) listener.bundleEnded(this);
  }

  /**
   * Add a listener to the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void addCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
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
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("id=").append(id);
    sb.append(", pendingTasks=").append(pendingTasksCount);
    sb.append(", cancelled=").append(cancelled);
    sb.append(", done=").append(done);
    sb.append(", job=").append(job);
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
}
