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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.protocol.results.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class group tasks from the same client channel together.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class ServerTaskBundleClient
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ServerTaskBundleClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
  private final List<ServerTask> taskList = new LinkedList<ServerTask>();
  /**
   * The tasks to be sent back to the client.
   */
  private final List<ServerTask> tasksToSendList = new LinkedList<ServerTask>();
  /**
   * The count of pending tasks.
   */
  private final AtomicInteger pendingTasksCount = new AtomicInteger();
  /**
   * The list of listeners registered with this bundle.
   */
  private final List<CompletionListener> listenerList = new ArrayList<CompletionListener>();
  /**
   * Array constructed each time a listener is added or removed.
   * This is a performance optimization in the scenario where listners are invoked more often than they are added or removed.
   */
  private CompletionListener[] listenerArray = null;
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
    for (int index = 0; index < taskList.size(); index++) {
      DataLocation dataLocation = taskList.get(index);
      this.taskList.add(new ServerTask(this, index, dataLocation));
    }
    this.pendingTasksCount.set(this.taskList.size());
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

    job = source.getJob().copy(taskList.size());
    job.initialTaskCount = source.getJob().getInitialTaskCount();
    job.currentTaskCount = job.taskCount;
    if ((job.getTaskCount() == 0) && (job.getName().indexOf("handshake") < 0))
    {
      boolean breakpoint = true;
    }
    //job.currentTaskCount = job.taskCount = taskList.size();
    this.dataProvider = source.getDataProvider();
    this.taskList.addAll(taskList);
    this.pendingTasksCount.set(0);
    this.done = source.isDone();
    this.cancelled = source.isCancelled();
    this.strategy = source.strategy;
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
    if (index < 0 || index >= taskList.size()) throw new IllegalArgumentException("index should in range 0.." + (taskList.size()-1));

    boolean fire;
    ServerTask task = taskList.get(index);
    if (task.getState() == TaskState.RESULT) {
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
   * Called to notify that the contained task received result.
   * @param results the tasks for which results were received.
   */
  public synchronized void resultReceived(final Collection<Pair<Integer, DataLocation>> results) {
    if (isCancelled()) return;
    if (debugEnabled) log.debug("*** received " + results.size() + " tasks for " + this);
    List<ServerTask> tasks = new ArrayList<ServerTask>(results.size());
    for (Pair<Integer, DataLocation> result: results) {
      ServerTask task = taskList.get(result.first());
      //if (task.getState() != ServerTask.State.RESULT) {
      if (task.getState() == TaskState.PENDING) {
        tasksToSendList.add(task);
        pendingTasksCount.decrementAndGet();
      }
      task.resultReceived(result.second());
    }
    done = pendingTasksCount.get() <= 0;
    boolean fire = strategy.sendResults(this, tasks);
    if (done || fire) fireTasksCompleted();
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
   * Called to notify that the task received exception during execution.
   * @param tasks the tasks for which an exception was received.
   * @param exception the exception.
   */
  public synchronized void resultReceived(final Collection<ServerTask> tasks, final Throwable exception)
  {
    if (isCancelled()) return;
    for (ServerTask task: tasks)
    {
      if (task.getState() == TaskState.PENDING) {
        tasksToSendList.add(task);
        pendingTasksCount.decrementAndGet();
      }
      task.resultReceived(exception);
    }
    done = pendingTasksCount.get() <= 0;
    boolean fire = strategy.sendResults(this, tasks);
    if (done || fire) fireTasksCompleted();
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
    if (!cancelled && !done)
    {
      if (debugEnabled) log.debug("cancelling client job " + this);
      this.cancelled = true;
      for (ServerTask task: taskList)
      {
        if (task.getState() == TaskState.PENDING) {
          task.cancel();
          tasksToSendList.add(task);
          pendingTasksCount.decrementAndGet();
        }
      }
      this.done = true;
      //fireTasksCompleted(taskList);
      fireTasksCompleted();
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
   * Get the <code>done</code> indicator.
   * @return <code>true</code> if this client job is done, <code>false</code> otherwise.
   */
  public synchronized boolean isDone()
  {
    return done;
  }

  /**
   * Extract <code>DataLocation</code> list from contained tasks.
   * @return the list of <code>DataLocation</code> instances.
   */
  public List<DataLocation> getDataLocationList()
  {
    List<DataLocation> list = new ArrayList<DataLocation>(taskList.size());
    for (ServerTask task : taskList) list.add(task.getDataLocation());
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
   * Notifies
   * @param results completed tasks.
   */
  protected void fireTasksCompleted(final List<ServerTask> results) {
    if (results == null) throw new IllegalArgumentException("results is null");

    CompletionListener[] listeners;
    synchronized (listenerList) {
      listeners = listenerArray;
    }
    ServerTaskBundleClient bundle = new ServerTaskBundleClient(this, results);
    for (CompletionListener listener : listeners) listener.taskCompleted(bundle, results);
  }

  /**
   * Notifies that tasks have been completed.
   */
  protected void fireTasksCompleted() {
    List<ServerTask> completedTasks = new ArrayList<ServerTask>(tasksToSendList);
    tasksToSendList.clear();

    CompletionListener[] listeners;
    synchronized (listenerList) {
      listeners = listenerArray;
    }
    ServerTaskBundleClient bundle = new ServerTaskBundleClient(this, completedTasks);
    for (CompletionListener listener : listeners) listener.taskCompleted(bundle, completedTasks);
  }

  /**
   * Notifies that bundle is completely executed.
   */
  public void bundleEnded() {
    if (dataProvider == null) throw new IllegalArgumentException("dataProvider is null");

    CompletionListener[] listeners;
    synchronized (listenerList) {
      listeners = listenerArray;
    }
    for (CompletionListener listener : listeners) listener.bundleEnded(this);
  }

  /**
   * Add a listener to the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void addCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList) {
      listenerList.add(listener);
      listenerArray = listenerList.toArray(new CompletionListener[listenerList.size()]);
    }
  }

  /**
   * Remove a listener from the list of bundle listeners.
   * @param listener a {@link CompletionListener} instance.
   */
  public void removeCompletionListener(final CompletionListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList) {
      listenerList.remove(listener);
      listenerArray = listenerList.toArray(new CompletionListener[listenerList.size()]);
    }
  }

  @Override
  public String toString()
  {
    //return ReflectionUtils.dumpObject(this, "pendingTasksCount", "cancelled", "job", "taskList", "dataProvider");
    return ReflectionUtils.dumpObject(this, "pendingTasksCount", "cancelled", "done", "job");
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
