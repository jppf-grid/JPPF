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

package org.jppf.execute;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFReconnectionNotification;
import org.jppf.execute.ThreadManager.UsedClassLoader;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public abstract class AbstractExecutionManager implements ExecutionManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractExecutionManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Timer managing the tasks timeout.
   */
  protected final JPPFScheduleHandler timeoutHandler = new JPPFScheduleHandler("Task Timeout Timer");
  /**
   * The bundle whose tasks are currently being executed.
   */
  protected TaskBundle bundle;
  /**
   * The list of tasks to execute.
   */
  protected List<Task<?>> taskList;
  /**
   * The uuid path of the current bundle.
   */
  protected List<String> uuidList;
  /**
   * Holds the tasks submitted to the executor.
   */
  protected List<NodeTaskWrapper> taskWrapperList;
  /**
   * Dispatches tasks notifications to registered listeners.
   */
  protected final TaskExecutionDispatcher taskNotificationDispatcher;
  /**
   * Determines whether the number of threads or their priority has changed.
   */
  protected final AtomicBoolean configChanged = new AtomicBoolean(true);
  /**
   * Set if the node must reconnect to the driver.
   */
  protected AtomicReference<JPPFReconnectionNotification> reconnectionNotification = new AtomicReference<>(null);
  /**
   * The thread manager that is used for execution.
   */
  protected final ThreadManager threadManager;
  /**
   * Determines whether the current job has been cancelled.
   */
  protected AtomicBoolean jobCancelled = new AtomicBoolean(false);
  /**
   * The class loader used to load the tasks and the classes they need from the client.
   */
  protected UsedClassLoader usedClassLoader;
  /**
   * The data provider for the current job.
   */
  protected DataProvider dataProvider;
  /**
   * The total accumulated elapsed time of the tasks in the current bundle.
   */
  protected final AtomicLong accumulatedElapsed = new AtomicLong(0L);

  /**
   * Initialize this execution manager with the specified node.
   * @param config the configuration to get the thread manager properties from.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   */
  public AbstractExecutionManager(final TypedProperties config, final JPPFProperty<Integer> nbThreadsProperty) {
    taskNotificationDispatcher = new TaskExecutionDispatcher(getClass().getClassLoader());
    threadManager = ThreadManager.newInstance(config, nbThreadsProperty);
  }

  @Override
  public void execute(final TaskBundle bundle, final List<Task<?>> taskList) throws Exception {
    if ((taskList == null) || taskList.isEmpty()) return;
    if (debugEnabled) log.debug("executing {} tasks of bundle {}", taskList.size(), bundle);
    try {
      setup(bundle, taskList);
      if (!isJobCancelled()) {
        int count = 0;
        final ExecutorCompletionService<NodeTaskWrapper> ecs = new ExecutorCompletionService<>(getExecutor());
        if (debugEnabled) log.debug("submitting {} tasks of bundle {}", taskList.size(), bundle);
        synchronized(taskWrapperList) {
          for (final Task<?> task : taskList) {
            if (!(task instanceof JPPFExceptionResult)) {
              if (task instanceof AbstractTask) ((AbstractTask<?>) task).setExecutionDispatcher(taskNotificationDispatcher);
              final NodeTaskWrapper taskWrapper = new NodeTaskWrapper(task, usedClassLoader.getClassLoader(), timeoutHandler);
              taskWrapperList.add(taskWrapper);
              ecs.submit(taskWrapper, taskWrapper);
              count++;
            }
          }
        }
        if (debugEnabled) log.debug("getting execution results for {} tasks of bundle {}", taskList.size(), bundle);
        for (int i=0; i<count; i++) {
          try {
            final Future<NodeTaskWrapper> future = ecs.take();
            if (!future.isCancelled()) {
              final NodeTaskWrapper taskWrapper = future.get();
              taskEnded(taskWrapper);
            }
          } catch (final Exception e) {
            log.debug("Exception when executing task", e);
          }
        }
      }
    } finally {
      cleanup();
    }
  }

  @Override
  public void cancelAllTasks(final boolean callOnCancel, final boolean requeue) {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel=" + callOnCancel + ", requeue=" + requeue);
    if (requeue && (bundle != null)) {
      synchronized(bundle) {
        bundle.setRequeue(true);
        bundle.getSLA().setSuspended(true);
      }
    }
    if (taskWrapperList != null) {
      synchronized(taskWrapperList) {
        for (NodeTaskWrapper ntw: taskWrapperList) cancelTask(ntw, callOnCancel);
      }
    }
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param taskWrapper the index of the task to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  private void cancelTask(final NodeTaskWrapper taskWrapper, final boolean callOnCancel) {
    if (debugEnabled) log.debug("cancelling task = " + taskWrapper);
    final Future<?> future = taskWrapper.getFuture();
    if (!future.isDone()) {
      if (debugEnabled) log.debug("calling future.cancel(true) for task = " + taskWrapper);
      taskWrapper.cancel(callOnCancel);
      future.cancel(taskWrapper.getTask().isInterruptible());
      taskWrapper.cancelTimeoutAction();
      taskEnded(taskWrapper);
    }
  }

  @Override
  public void shutdown() {
    if (debugEnabled) log.debug("closing {}", this);
    getExecutor().shutdownNow();
    timeoutHandler.clear(true);
    taskNotificationDispatcher.close();
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  protected abstract void setup(final TaskBundle bundle, final List<Task<?>> taskList);

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   */
  protected abstract void cleanup();

  /**
   * Notification sent by a node task wrapper when a task is complete.
   * @param taskWrapper the task that just ended.
   * @exclude
   */
  protected void taskEnded(final NodeTaskWrapper taskWrapper) {
    final long elapsedTime = taskWrapper.getElapsedTime();
    accumulatedElapsed.addAndGet(elapsedTime);
    final ExecutionInfo info = taskWrapper.getExecutionInfo();
    final long cpuTime = (info == null) ? 0L : (info.cpuTime / 1000000L);
    final Task<?> task = taskWrapper.getTask();
    taskNotificationDispatcher.fireTaskEnded(task, getCurrentJobId(), getCurrentJobName(), cpuTime, elapsedTime/1000000L, task.getThrowable() != null);
  }

  @Override
  public String getCurrentJobId() {
    return (bundle != null) ? bundle.getUuid() : null;
  }

  /**
   * Get the job name.
   * @return the name as a string.
   */
  public String getCurrentJobName() {
    return (bundle != null) ? bundle.getName() : null;
  }

  @Override
  public ExecutorService getExecutor() {
    return threadManager.getExecutorService();
  }

  @Override
  public boolean checkConfigChanged() {
    return configChanged.compareAndSet(true, false);
  }

  @Override
  public void triggerConfigChanged() {
    configChanged.compareAndSet(false, true);
  }

  @Override
  public void setThreadPoolSize(final int size) {
    if (size <= 0) {
      log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
      return;
    }
    final int oldSize = getThreadPoolSize();
    threadManager.setPoolSize(size);
    final int newSize = getThreadPoolSize();
    if (oldSize != newSize) {
      log.info("Node thread pool size changed from " + oldSize + " to " + size);
      JPPFConfiguration.set(JPPFProperties.PROCESSING_THREADS, size);
      triggerConfigChanged();
    }
  }

  @Override
  public int getThreadPoolSize() {
    return threadManager.getPoolSize();
  }

  @Override
  public int getThreadsPriority() {
    return threadManager.getPriority();
  }

  @Override
  public void updateThreadsPriority(final int newPriority) {
    threadManager.setPriority(newPriority);
  }

  @Override
  public ThreadManager getThreadManager() {
    return threadManager;
  }

  @Override
  public boolean isJobCancelled() {
    return jobCancelled.get();
  }

  @Override
  public void setJobCancelled(final boolean jobCancelled) {
    this.jobCancelled.set(jobCancelled);
  }

  @Override
  public TaskBundle getBundle() {
    return bundle;
  }

  @Override
  public void setBundle(final TaskBundle bundle) {
    this.bundle = bundle;
  }

  @Override
  public TaskExecutionDispatcher getTaskNotificationDispatcher() {
    return taskNotificationDispatcher;
  }
}
