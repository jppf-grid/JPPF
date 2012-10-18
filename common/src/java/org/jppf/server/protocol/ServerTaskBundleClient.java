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
package org.jppf.server.protocol;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.JobSLA;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Instances of this class group tasks from the same client channel together.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class ServerTaskBundleClient
{
  /**
   * The job to execute.
   */
  private final JPPFTaskBundle job;
  /**
   * The shared data provider for this task bundle.
   */
  private final DataLocation dataProvider;
  /**
   * The tasks to be executed by the node.
   */
  private final List<ServerTask> taskList;
  /**
   * The count of pending tasks.
   */
  private final AtomicInteger pendingTasksCount = new AtomicInteger();
  /**
   * The list of listeners registered with this bundle.
   */
  private final List<CompletionListener> listenerList = new ArrayList<CompletionListener>();
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
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param dataProvider the shared data provider for this task bundle.
   */
  public ServerTaskBundleClient(final JPPFTaskBundle job, final DataLocation dataProvider) {
    this(job, dataProvider, Collections.<DataLocation>emptyList());
  }

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param dataProvider the shared data provider for this task bundle.
   * @param taskList the tasks to execute.
   */
  public ServerTaskBundleClient(final JPPFTaskBundle job, final DataLocation dataProvider, final List<DataLocation> taskList) {
    if (job == null) throw new IllegalArgumentException("job is null");
    if (taskList == null) throw new IllegalArgumentException("taskList is null");

    this.job = job;
    this.dataProvider = dataProvider;
    this.taskList = new ArrayList<ServerTask>(taskList.size());
    for (int index = 0; index < taskList.size(); index++) {
      DataLocation dataLocation = taskList.get(index);
      this.taskList.add(new ServerTask(this, index, dataLocation));
    }

    if (dataProvider != null) this.pendingTasksCount.set(this.taskList.size());
  }

  /**
   * Initialize task bundle as copy as source bundle.
   * @param job   the job to execute.
   * @param source the source bundle to copy.
   */
  protected ServerTaskBundleClient(final JPPFTaskBundle job, final ServerTaskBundleClient source) {
    if (job == null) throw new IllegalArgumentException("job is null");

    this.job = job;
    this.dataProvider = source.getDataProvider();
    List<ServerTask> taskList = source.getTaskList();
    this.taskList = new ArrayList<ServerTask>(taskList.size());
    for (ServerTask task : taskList) {
      this.taskList.add(new ServerTask(this, task.getPosition(), task.getDataLocation()));
    }

    this.pendingTasksCount.set(this.taskList.size());
  }

  /**
   * Get the job this submission is for.
   * @return a {@link JPPFTaskBundle} instance.
   */
  public JPPFTaskBundle getJob()
  {
    return job;
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataLocation getDataProvider()
  {
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
   * Called to notify that the contained task received result.
   * @param index the task position for received result.
   * @param result the result.
   */
  public void resultReceived(final int index, final DataLocation result)
  {
    if (index < 0 || index >= taskList.size()) throw new IllegalArgumentException("index should in range 0.." + taskList.size());

    boolean fire;
    ServerTask task = taskList.get(index);
    if (task.getState() == ServerTask.State.RESULT)
    {
      fire = false;
    } else {
      fire = pendingTasksCount.decrementAndGet() == 0;
    }
    task.resultReceived(result);
    if (fire && !done) {
      done = true;
      fireTasksCompleted(taskList);
    }
  }

  /**
   * Called to notify that the task received exception during execution.
   * @param index the task position for received exception.
   * @param exception the exception.
   */
  public void resultReceived(final int index, final Throwable exception)
  {
    if (index < 0 || index >= taskList.size()) throw new IllegalArgumentException("index should in range 0.." + taskList.size());
    taskList.get(index).resultReceived(exception);
  }

  /**
   * Get the job received time.
   * @return the time in milliseconds as a long value.
   */
  public long getJobReceivedTime()
  {
    return jobReceivedTime;
  }

  /**
   * Set the job received time.
   * @param jobReceivedTime the time in milliseconds as a long value.
   */
  public void setJobReceivedTime(final long jobReceivedTime)
  {
    this.jobReceivedTime = jobReceivedTime;
  }

  /**
   * Called when this task bundle is cancelled.
   */
  public synchronized void cancel()
  {
    if (!this.cancelled && !this.done)
    {
      this.cancelled = true;
      for (ServerTask task : taskList) {
        task.resultReceived(task.getDataLocation());
      }
      this.done = true;
      fireTasksCompleted(taskList);
    }
  }

  /**
   * Get the cancelled indicator.
   * @return <code>true</code> if job is cancelled, <code>false</code> otherwise.
   */
  public synchronized boolean isCancelled()
  {
    return cancelled;
  }

  /**
   * Extract <code>DataLocation</code> list from contained tasks.
   * @return the list of <code>DataLocation</code> instances.
   */
  public List<DataLocation> getDataLocationList()
  {
    List<DataLocation> list = new ArrayList<DataLocation>(taskList.size());
    for (ServerTask task : taskList) {
      list.add(task.getDataLocation());
    }
    return list;
  }

  /**
   * Get the service level agreement between the job and the server.
   * @return an instance of {@link JobSLA}.
   */
  public JobSLA getSLA()
  {
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
  public int getPendingTasksCount()
  {
    return pendingTasksCount.get();
  }

  /**
   * Make a copy of this bundle.
   * @param job   the job to copy.
   * @return a new <code>ServerTaskBundleClient</code> instance.
   */
  public ServerTaskBundleClient copy(final JPPFTaskBundle job)
  {
    return new ServerTaskBundleClient(job, this);
  }

  /**
   * Notifies
   * @param results completed tasks.
   */
  protected void fireTasksCompleted(final List<ServerTask> results) {
    if (results == null) throw new IllegalArgumentException("results is null");

    CompletionListener[] listeners;
    synchronized (listenerList) {
      listeners = listenerList.toArray(new CompletionListener[listenerList.size()]);
    }
    for (CompletionListener listener : listeners) {
      listener.taskCompleted(this, results);
    }
  }

  /**
   * Notifies that bundle is completely executed.
   */
  public void bundleEnded() {
    if (dataProvider == null) throw new IllegalArgumentException("dataProvider is null");

    CompletionListener[] listeners;
    synchronized (listenerList) {
      listeners = listenerList.toArray(new CompletionListener[listenerList.size()]);
    }
    for (CompletionListener listener : listeners) {
      listener.bundleEnded(this);
    }
  }

  /**
   * Add a listener to the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void addCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList) {
      listenerList.add(listener);
    }
  }

  /**
   * Remove a listener from the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void removeCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList) {
      listenerList.add(listener);
    }
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("ServerTaskBundleClient");
    sb.append("{dataProvider=").append(dataProvider);
    sb.append(", job=").append(job);
    sb.append(", pendingTasksCount=").append(pendingTasksCount.get());
    sb.append(", taskList=").append(taskList);
    sb.append(", cancelled=").append(isCancelled());
    sb.append('}');
    return sb.toString();
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
