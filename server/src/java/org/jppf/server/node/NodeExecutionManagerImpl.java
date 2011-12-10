/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.node;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.node.NodeExecutionManager;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.*;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 */
public class NodeExecutionManagerImpl extends ThreadManager implements NodeExecutionManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeExecutionManagerImpl.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node that uses this execution manager.
   */
  private JPPFNode node = null;
  /**
   * Timer managing the tasks timeout.
   */
  protected JPPFScheduleHandler timeoutHandler = new JPPFScheduleHandler("Task Timeout Timer");
  /**
   * Mapping of tasks numbers to their id.
   */
  protected Map<String, List<Long>> idMap = new Hashtable<String, List<Long>>();
  /**
   * Mapping of internal number to the corresponding tasks.
   */
  protected Map<Long, Task> taskMap = new Hashtable<Long, Task>();
  /**
   * The bundle whose tasks are currently being executed.
   */
  protected JPPFTaskBundle bundle = null;
  /**
   * The list of tasks to execute.
   */
  protected List<? extends Task> taskList = null;
  /**
   * The uuid path of the current bundle.
   */
  protected List<String> uuidList = null;
  /**
   * Holds a the set of futures generated by submitting each task.
   */
  protected Map<Long, Future<?>> futureMap = new Hashtable<Long, Future<?>>();
  /**
   * Counter for the number of tasks whose execution is over,
   * including tasks that completed normally, were cancelled or timed out.
   */
  protected AtomicLong taskCount = new AtomicLong(0L);
  /**
   * List of listeners to task execution events.
   */
  private final List<TaskExecutionListener> taskExecutionListeners = new ArrayList<TaskExecutionListener>();
  /**
   * Temporary array of listeners used for faster access.
   */
  private TaskExecutionListener[] listenersArray = new TaskExecutionListener[0];
  /**
   * Determines whether the number of threads or their priority has changed.
   */
  protected AtomicBoolean configChanged = new AtomicBoolean(true);
  /**
   * Set if the node must reconnect to the driver.
   */
  protected JPPFNodeReconnectionNotification reconnectionNotification = null;

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   */
  public NodeExecutionManagerImpl(final JPPFNode node)
  {
    super();
    this.node = node;
  }

  /**
   * Execute the specified tasks of the specified tasks bundle.
   * @param bundle the bundle to which the tasks are associated.
   * @param taskList the list of tasks to execute.
   * @throws Exception if the execution failed.
   */
  public void execute(final JPPFTaskBundle bundle, final List<? extends Task> taskList) throws Exception
  {
    if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
    NodeExecutionInfo info;
    if (isCpuTimeEnabled())
    {
      info = computeExecutionInfo();
    } else {
      info = null;
    }
    setup(bundle, taskList);
    for (Task task : taskList) performTask(task);
    waitForResults();
    cleanup();
    if (info != null)
    {
      NodeExecutionInfo info2 = computeExecutionInfo();
      info2.cpuTime -= info.cpuTime;
      info2.userTime -= info.userTime;
      if (debugEnabled) log.debug("total cpu time used: " + info2.cpuTime + " ms, user time: " + info2.userTime);
    }
  }

  /**
   * Execute a single task.
   * @param task the task to execute.
   * @return a number identifying the task that was submitted.
   * @throws Exception if the execution failed.
   */
  public synchronized long performTask(final Task task) throws Exception
  {
    String id = task.getId();
    long number = incTaskCount();
    taskMap.put(number, task);
    Future<?> f = getThreadPool().submit(new NodeTaskWrapper(node, task, uuidList, number));
    if (!f.isDone()) futureMap.put(number, f);
    if (id != null)
    {
      List<Long> pairList = idMap.get(id);
      if (pairList == null)
      {
        pairList = new ArrayList<Long>();
        idMap.put(id, pairList);
      }
      pairList.add(number);
    }
    JPPFSchedule schedule = task.getTimeoutSchedule();
    if ((schedule != null) && ((schedule.getDuration() > 0L) || (schedule.getDate() != null)))
    {
      processTaskExpirationDate(task, number);
    }
    return number;
  }

  /**
   * Cancel all executing or pending tasks.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   */
  public synchronized void cancelAllTasks(final boolean callOnCancel, final boolean requeue)
  {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel=" + callOnCancel + ", requeue=" + requeue);
    if (requeue)
    {
      bundle.setParameter(BundleParameter.JOB_REQUEUE, true);
      bundle.getSLA().setSuspended(true);
    }
    List<Long> list = new ArrayList<Long>(futureMap.keySet());
    for (Long n: list) cancelTask(n, callOnCancel);
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param id the id of the tasks to cancel.
   * @deprecated the task cancel feature is inherently unsafe, as it depends on the task
   * having a unique id among all the tasks running in the grid, which cannot be guaranteed.
   * This feature has been removed from the management APIs, with no replacement.
   * Tasks can still be cancelled, but only as part of job cancel.
   */
  public synchronized void cancelTask(final String id)
  {
    if (debugEnabled) log.debug("cancelling tasks with id = " + id);
    List<Long> numberList = idMap.remove(id);
    if (numberList == null) return;
    if (debugEnabled) log.debug("number of tasks to cancel: " + numberList.size());
    for (Long number: numberList) cancelTask(number, true);
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param number the index of the task to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  private synchronized void cancelTask(final Long number, final boolean callOnCancel)
  {
    if (debugEnabled) log.debug("cancelling task number = " + number);
    Future<?> future = futureMap.get(number);
    if (!future.isDone())
    {
      future.cancel(true);
      Task task = taskMap.remove(number);
      if (task != null && callOnCancel) task.onCancel();
      removeFuture(number);
    }
  }

  /**
   * Restart the execution of the tasks with the specified id.<br>
   * The task(s) will be restarted even if their execution has already completed.
   * @param id the id of the task or tasks to restart.
   * @deprecated the task restart feature is inherently unsafe, as it depends on the task
   * having a unique id among all the tasks running in the grid, which cannot be guaranteed.
   * This feature has been removed from the management APIs, with no replacement. 
   */
  public synchronized void restartTask(final String id)
  {
    if (debugEnabled) log.debug("restarting tasks with id = " + id);
    List<Long> numberList = idMap.remove(id);
    if (numberList == null) return;
    if (debugEnabled) log.debug("number of tasks to restart: " + numberList.size());
    for (Long number: numberList)
    {
      Future<?> future = futureMap.get(number);
      if (!future.isDone())
      {
        future.cancel(true);
        Task task = taskMap.remove(number);
        removeFuture(number);
        if (task != null)
        {
          task.onRestart();
          try
          {
            performTask(task);
          }
          catch(Exception e)
          {
            log.error(e.getMessage(), e);
          }
        }
      }
    }
  }

  /**
   * Notify the timer that a task must be aborted if its timeout period expired.
   * @param task the JPPF task for which to set the timeout.
   * @param number a number identifying the task submitted to the thread pool.
   * @throws Exception if any error occurs.
   */
  private void processTaskExpirationDate(final Task task, final long number) throws Exception
  {
    JPPFSchedule schedule = task.getTimeoutSchedule();
    if ((schedule != null) && schedule.hasDate())
    {
      TimeoutTimerTask tt = new TimeoutTimerTask(this, number, task);
      timeoutHandler.scheduleAction(getFutureFromNumber(number), task.getTimeoutSchedule(), tt);
    }
  }

  /**
   * Notify the timer that a task must be aborted if its timeout period expired.
   * @param task the JPPF task for which to set the timeout.
   * @param number a number identifying the task submitted to the thread pool.
   * @throws Exception if any error occurs.
   */
  public void processTaskTimeout(final Task task, final long number) throws Exception
  {
    if (task.getTimeoutSchedule() != null)
    {
      TimeoutTimerTask tt = new TimeoutTimerTask(this, number, task);
      timeoutHandler.scheduleAction(getFutureFromNumber(number), task.getTimeoutSchedule(), tt);
    }
  }

  /**
   * Shutdown this execution manager.
   */
  public void shutdown()
  {
    getThreadPool().shutdownNow();
    timeoutHandler.clear(true);
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  public void setup(final JPPFTaskBundle bundle, final List<? extends Task> taskList)
  {
    this.bundle = bundle;
    this.taskList = taskList;
    this.uuidList = bundle.getUuidPath().getList();
    taskCount = new AtomicLong(0L);
    node.getLifeCycleEventHandler().fireJobStarting();
  }

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   */
  public void cleanup()
  {
    node.getLifeCycleEventHandler().fireJobEnding();
    this.bundle = null;
    this.taskList = null;
    this.uuidList = null;
    futureMap.clear();
    taskMap.clear();
    timeoutHandler.clear();
    idMap.clear();
  }

  /**
   * Wait until all tasks are complete.
   * @throws Exception if the execution failed.
   */
  public synchronized void waitForResults() throws Exception
  {
    while (!futureMap.isEmpty() && (getReconnectionNotification() == null))
    {
      goToSleep();
    }
    if (getReconnectionNotification() != null)
    {
      cancelAllTasks(true, false);
      throw reconnectionNotification;
    }
  }

  /**
   * Remove the specified future from the pending set and notify
   * all threads waiting for the end of the execution.
   * @param number task identifier for the future of the task to remove.
   */
  public synchronized void removeFuture(final long number)
  {
    Future<?> future = futureMap.remove(number);
    if (future != null) timeoutHandler.cancelAction(future);
    wakeUp();
  }

  /**
   * Increment the current task count and return the new value.
   * @return the new values a long.
   */
  private long incTaskCount()
  {
    return taskCount.incrementAndGet();
  }

  /**
   * Notification sent by a node task wrapper when a task is complete.
   * @param taskNumber the number identifying the task.
   * @param cpuTime the cpu time taken by the task.
   * @param elapsedTime the wall clock time taken by the task
   * @param hasError determines whether the task had an exception.
   */
  public void taskEnded(final long taskNumber, final long cpuTime, final long elapsedTime, final boolean hasError)
  {
    TaskExecutionEvent event = new TaskExecutionEvent(taskMap.get(taskNumber), getCurrentJobId(), cpuTime, elapsedTime, hasError);
    removeFuture(taskNumber);
    TaskExecutionListener[] tmp;
    synchronized(taskExecutionListeners)
    {
      tmp = listenersArray;
    }
    for (TaskExecutionListener listener : tmp) listener.taskExecuted(event);
  }

  /**
   * Get the future corresponding to the specified task number.
   * @param number the number identifying the task.
   * @return a <code>Future</code> instance.
   */
  public synchronized Future<?> getFutureFromNumber(final long number)
  {
    return futureMap.get(number);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFDistributedJob getCurrentJob()
  {
    return bundle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Task> getTasks()
  {
    return taskList == null ? null : Collections.unmodifiableList(taskList);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getCurrentJobId()
  {
    return (bundle != null) ? bundle.getUuid() : null;
  }

  /**
   * Add a task execution listener to the list of task execution listeners.
   * @param listener the listener to add.
   */
  public void addTaskExecutionListener(final TaskExecutionListener listener)
  {
    synchronized(taskExecutionListeners)
    {
      taskExecutionListeners.add(listener);
      listenersArray = taskExecutionListeners.toArray(new TaskExecutionListener[taskExecutionListeners.size()]);
    }
  }

  /**
   * Remove a task execution listener from the list of task execution listeners.
   * @param listener the listener to remove.
   */
  public void removeTaskExecutionListener(final TaskExecutionListener listener)
  {
    synchronized(taskExecutionListeners)
    {
      taskExecutionListeners.remove(listener);
      listenersArray = taskExecutionListeners.toArray(new TaskExecutionListener[taskExecutionListeners.size()]);
    }
  }

  /**
   * Get the executor used by this execution manager.
   * @return an <code>ExecutorService</code> instance.
   */
  public ExecutorService getExecutor()
  {
    return getThreadPool();
  }

  /**
   * Determines whether the configuration has changed and resets the flag if it has.
   * @return true if the config was changed, false otherwise.
   */
  public boolean checkConfigChanged()
  {
    return configChanged.compareAndSet(true, false);
  }

  /**
   * Get the notification that the node must reconnect to the driver.
   * @return a {@link JPPFNodeReconnectionNotification} instance.
   */
  synchronized JPPFNodeReconnectionNotification getReconnectionNotification()
  {
    return reconnectionNotification;
  }

  /**
   * Set the notification that the node must reconnect to the driver.
   * @param reconnectionNotification a {@link JPPFNodeReconnectionNotification} instance.
   */
  public synchronized void setReconnectionNotification(final JPPFNodeReconnectionNotification reconnectionNotification)
  {
    if (this.reconnectionNotification == null) return;
    this.reconnectionNotification = reconnectionNotification;
  }
}
