/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.execute.async;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFReconnectionNotification;
import org.jppf.execute.*;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public abstract class AbstractAsyncExecutionManager implements AsyncExecutionManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractAsyncExecutionManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Timer managing the tasks timeout.
   */
  protected final JPPFScheduleHandler timeoutHandler = new JPPFScheduleHandler("Task Timeout Timer");
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
   * Mapping of jobUuid + bunldeId to the corresponding {@code JobProcessingEntry} objects.
   */
  protected final Map<String, JobProcessingEntry> jobEntries = new HashMap<>();
  /**
   * Mapping of job uuids to the ids of the bundles currently processed for this job.
   */
  protected final CollectionMap<String, Long> jobBundleIds = new ArrayListHashMap<>();
  /**
   * List of listeners to this execution manager.
   */
  protected final List<ExecutionManagerListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Map of the bundles being read from the server and not yet submitted to this execution manager.
   */
  protected final Map<String, JobPendingEntry> pendingEntries = new HashMap<>();
  /**
   * Mapping of job uuids to the ids of the pending bundles for each job.
   */
  protected final CollectionMap<String, Long> pendingBundleIds = new ArrayListHashMap<>();

  /**
   * Initialize this execution manager with the specified node.
   * @param config the configuration to get the thread manager properties from.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   */
  public AbstractAsyncExecutionManager(final TypedProperties config, final JPPFProperty<Integer> nbThreadsProperty) {
    taskNotificationDispatcher = new TaskExecutionDispatcher(getClass().getClassLoader());
    threadManager = ThreadManager.newInstance(config, nbThreadsProperty);
  }

  @Override
  public void execute(final TaskBundle bundle, final List<Task<?>> taskList) throws Exception {
    if ((taskList == null) || taskList.isEmpty()) return;
    if (debugEnabled) log.debug("executing {} tasks of bundle {}", taskList.size(), bundle);
    final JobProcessingEntry jobEntry = setup(bundle, taskList);
    jobEntry.executionManager = this;
    final String bundleKey = bundle.getUuid() + bundle.getBundleId();
    synchronized(jobEntries) {
      final JobPendingEntry pendingEntry = pendingEntries.remove(bundleKey);
      if (pendingEntry != null) {
        pendingBundleIds.removeValue(bundle.getUuid(), bundle.getBundleId());
        jobEntry.jobCancelled.set(pendingEntry.jobCancelled.get());
      }
      jobEntries.put(bundleKey, jobEntry);
      jobBundleIds.putValue(bundle.getUuid(), bundle.getBundleId());
    }
    if (!jobEntry.jobCancelled.get()) {
      synchronized(jobEntry.taskWrapperList) {
        if (debugEnabled) log.debug("wrapping up to {} executable tasks of bundle {}", taskList.size(), bundle);
        for (final Task<?> task : taskList) {
          if (!(task instanceof JPPFExceptionResult)) {
            if (task instanceof AbstractTask) ((AbstractTask<?>) task).setExecutionDispatcher(taskNotificationDispatcher);
            final NodeTaskWrapper taskWrapper = new NodeTaskWrapper(jobEntry, task, jobEntry.getClassLoader(), timeoutHandler);
            jobEntry.taskWrapperList.add(taskWrapper);
            jobEntry.submittedCount++;
          }
        }
        if (!jobEntry.taskWrapperList.isEmpty()) {
          if (debugEnabled) log.debug("submitting {} executable tasks of bundle {}", jobEntry.taskWrapperList.size(), bundle);
          for (final NodeTaskWrapper taskWrapper: jobEntry.taskWrapperList) getExecutor().submit(taskWrapper, taskWrapper);
          if (debugEnabled) log.debug("submited {} tasks", jobEntry.taskWrapperList.size());
        } else {
          if (debugEnabled) log.debug("there are no tasks to execute in bundle {}, ending job", bundle);
          jobEnded(jobEntry);
        }
      }
    } else {
      if (debugEnabled) log.debug("bundle was cancelled before its execution started, ending job {}", bundle);
      jobEnded(jobEntry);
    }
  }

  @Override
  public void cancelAllTasks(final boolean callOnCancel, final boolean requeue) {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel={}, requeue={}", callOnCancel, requeue);
    synchronized(jobEntries) {
      for (final JobProcessingEntry jobEntry: jobEntries.values()) {
        if (requeue) {
          synchronized(jobEntry.bundle) {
            jobEntry.bundle.setRequeue(true);
            jobEntry.bundle.getSLA().setSuspended(true);
          }
        }
        if (jobEntry.taskWrapperList != null) {
          synchronized(jobEntry.taskWrapperList) {
            for (final NodeTaskWrapper ntw: jobEntry.taskWrapperList) cancelTask(ntw, callOnCancel);
          }
        }
      }
    }
  }

  @Override
  public void cancelJob(final String jobUuid, final boolean callOnCancel, final boolean requeue) {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel={}, requeue={}, jobUuid={}", callOnCancel, requeue, jobUuid);
    synchronized(jobEntries) {
      final Collection<Long> pendingIds = pendingBundleIds.getValues(jobUuid);
      if (pendingIds != null) {
        final List<Long> pendingIdList = new ArrayList<>(pendingIds);
        for (final long bundleId: pendingIdList) {
          final JobPendingEntry pendingEntry = pendingEntries.get(jobUuid + bundleId);
          if (pendingEntry != null) {
            if (debugEnabled) log.debug("setting cancelled status on pending entry with jobUuid={}, bundleId={}", jobUuid, bundleId);
            pendingEntry.jobCancelled.set(true);
          }
        }
      }
      final Collection<Long> bundleIds = jobBundleIds.getValues(jobUuid);
      if (debugEnabled) log.debug("cancelling {} bundles for jobUuid={}", (bundleIds == null) ? 0: bundleIds.size(), jobUuid);
      if (bundleIds == null) return;
      final List<Long> bundleIdList = new ArrayList<>(bundleIds);
      for (final long bundleId: bundleIdList) {
        final JobProcessingEntry jobEntry = jobEntries.get(jobUuid + bundleId);
        if (jobEntry == null) continue;
        jobEntry.jobCancelled.set(true);
        if (debugEnabled) log.debug("cancelling {}", jobEntry.bundle);
        if (requeue) {
          synchronized(jobEntry.bundle) {
            jobEntry.bundle.setRequeue(true);
            jobEntry.bundle.getSLA().setSuspended(true);
          }
        }
        if (jobEntry.taskWrapperList != null) {
          synchronized(jobEntry.taskWrapperList) {
            for (final NodeTaskWrapper taskWrapper: jobEntry.taskWrapperList) cancelTask(taskWrapper, callOnCancel);
          }
        }
      }
    }
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param taskWrapper the index of the task to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  private static void cancelTask(final NodeTaskWrapper taskWrapper, final boolean callOnCancel) {
    if (debugEnabled) log.debug("cancelling task = {}", taskWrapper);
    final Future<?> future = taskWrapper.getFuture();
    if (!future.isDone()) {
      if (debugEnabled) log.debug("calling future.cancel(true) for task = {}", taskWrapper);
      taskWrapper.cancel(callOnCancel);
      future.cancel(taskWrapper.getTask().isInterruptible());
      taskWrapper.cancelTimeoutAction();
      if (!taskWrapper.hasStarted()) taskWrapper.taskEnded();
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
   * @return an instance of {@link JobProcessingEntry}.
   */
  protected abstract JobProcessingEntry setup(final TaskBundle bundle, final List<Task<?>> taskList);

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   * @param jobEntry encapsulates information about the job.
   */
  protected abstract void cleanup(JobProcessingEntry jobEntry);

  @Override
  public void taskEnded(final NodeTaskWrapper taskWrapper) {
    try {
      if (traceEnabled) log.trace("task ended: {}", taskWrapper);
      final long elapsedTime = taskWrapper.getElapsedTime();
      final TaskBundle bundle;
      final int n, submittedCount;
      final JobProcessingEntry jobEntry = taskWrapper.getJobEntry();
      synchronized(jobEntry) {
        bundle = jobEntry.bundle;
        jobEntry.accumulatedElapsed.addAndGet(elapsedTime);
        n = jobEntry.resultCount.incrementAndGet();
        submittedCount = jobEntry.submittedCount;
      }
      final ExecutionInfo info = taskWrapper.getExecutionInfo();
      final long cpuTime = (info == null) ? 0L : (info.cpuTime / 1_000_000L);
      final Task<?> task = taskWrapper.getTask();
      if (traceEnabled) log.trace("sending task ended notification for {}, bundle={}", taskWrapper, bundle);
      taskNotificationDispatcher.fireTaskEnded(task, bundle.getUuid(), bundle.getName(), cpuTime, elapsedTime / 1_000_000L, task.getThrowable() != null);
      if (traceEnabled) log.trace("resultCount={} for {}", n, taskWrapper);
      if (n >= submittedCount) jobEnded(jobEntry);
    } catch (final RuntimeException e) {
      log.error("error in taskEnded() for {}", taskWrapper, e);
    }
  }

  /**
   * 
   * @param jobEntry the job to process.
   */
  private void jobEnded(final JobProcessingEntry jobEntry) {
    final TaskBundle bundle = jobEntry.bundle;
    final List<Task<?>> taskList = jobEntry.taskList;
    if (debugEnabled) log.debug("processing completion of {} tasks of job {}", taskList.size(), bundle);
    synchronized(jobEntries) {
      jobEntries.remove(bundle.getUuid() + bundle.getBundleId());
      jobBundleIds.removeValue(bundle.getUuid(), bundle.getBundleId());
    }
    cleanup(jobEntry);
    fireJobFinished(bundle, taskList, jobEntry.t);
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
  public TaskExecutionDispatcher getTaskNotificationDispatcher() {
    return taskNotificationDispatcher;
  }

  @Override
  public List<TaskBundle> getBundles(final String jobUuid) {
    final List<TaskBundle> result = new ArrayList<>();
    synchronized(jobEntries) {
      final Collection<Long> bundleIds = jobBundleIds.getValues(jobUuid);
      if (bundleIds != null) {
        for (final Long id: bundleIds) {
          final JobProcessingEntry jobEntry = jobEntries.get(jobUuid + id);
          if (jobEntry != null) result.add(jobEntry.bundle);
        }
      }
    }
    return result;
  }

  @Override
  public void addExecutionManagerListener(final ExecutionManagerListener listener) {
    if (listener != null) listeners.add(listener);
  }

  @Override
  public void removeExecutionManagerListener(final ExecutionManagerListener listener) {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Called when the execution of a task bundle has finished.
   * @param bundle the TaskBundle which holds information on the job.
   * @param tasks the tasks that were executed.
   * @param t a {@link Throwable} that prevented or interrupted the job processing.
   */
  protected void fireJobFinished(final TaskBundle bundle, final List<Task<?>> tasks, final Throwable t) {
    if (debugEnabled) log.debug("sending notification to listeners for completion of {} tasks of job {}", tasks.size(), bundle);
    for (final ExecutionManagerListener listener: listeners) {
      if (listener != null) listener.bundleExecuted(bundle, tasks, t);
    }
  }

  @Override
  public void addPendignJobEntry(final TaskBundle bundle) {
    if (debugEnabled) log.debug("adding pending entry for {}", bundle);
    synchronized(jobEntries) {
      final JobPendingEntry entry = new JobPendingEntry();
      entry.bundle = bundle;
      pendingEntries.put(bundle.getUuid() + bundle.getBundleId(), entry);
      pendingBundleIds.putValue(bundle.getUuid(), bundle.getBundleId());
    }
  }
}
