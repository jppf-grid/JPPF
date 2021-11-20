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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.execute.async.JobProcessingEntry;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public class NodeTaskWrapper implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeTaskWrapper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Timer managing the tasks timeout.
   */
  private final JPPFScheduleHandler timeoutHandler;
  /**
   * Indicator whether task was cancelled;
   */
  private boolean cancelled;
  /**
   * Indicator whether <code>onCancel</code> should be called when cancelled.
   */
  private boolean callOnCancel;
  /**
   * Indicator whether task timeout.
   */
  private boolean timeout;
  /**
   * Indicator that task was started.
   */
  private boolean started;
  /**
   * The task to execute within a try/catch block.
   */
  @SuppressWarnings("rawtypes")
  private final Task task;
  /**
   * The future created by the executor service.
   */
  private Future<?> future;
  /**
   * Holds the used cpu time for this task.
   */
  private ExecutionInfo executionInfo;
  /**
   * The elapsed time for this task's execution.
   */
  private long elapsedTime = 0L;
  /**
   * The class loader that was used to load the task class.
   */
  private final ClassLoader taskClassLoader;
  /**
   * Encapsulates information about the job the task is a part of.
   */
  private final JobProcessingEntry jobEntry;
  /**
   * Whether this task has ended.
   */
  private final AtomicBoolean ended = new AtomicBoolean(false);

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param task the task to execute within a try/catch block.
   * @param taskClassLoader the class loader that was used to load the task class.
   * @param timeoutHandler handles the timeout for this task.
   */
  public NodeTaskWrapper(final Task<?> task, final ClassLoader taskClassLoader, final JPPFScheduleHandler timeoutHandler) {
    this(null, task, taskClassLoader, timeoutHandler);
  }

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param jobEntry encapsulates information about the job the task is a part of.
   * @param task the task to execute within a try/catch block.
   * @param taskClassLoader the class loader that was used to load the task class.
   * @param timeoutHandler handles the timeout for this task.
   */
  public NodeTaskWrapper(final JobProcessingEntry jobEntry, final Task<?> task, final ClassLoader taskClassLoader, final JPPFScheduleHandler timeoutHandler) {
    this.task = task;
    this.taskClassLoader = taskClassLoader;
    this.timeoutHandler = timeoutHandler;
    this.jobEntry = jobEntry;
    TaskThreadLocals.setRequestUuid(task.getJob().getUuid());
    if (traceEnabled) {
      log.trace("initialized {}", this);
      try {
        log.trace("in constructor: contextClassLoader={}, taskClassLoader={}", Thread.currentThread().getContextClassLoader(), taskClassLoader);
      } catch(final Throwable e) {
        log.error("error in constructor, contextClassLoader={}, taskClassLoader={}", Thread.currentThread().getContextClassLoader(), taskClassLoader, e);
      }
    }
  }

  /**
   * Set cancel indicator and cancel task when it implements <code>Future</code> interface.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  public synchronized void cancel(final boolean callOnCancel) {
    this.cancelled = true;
    this.callOnCancel |= callOnCancel;
    if (task instanceof Future) {
      final Future<?> future = (Future<?>) task;
      if (!future.isDone()) future.cancel(true);
    } else if (task instanceof CancellationHandler) {
      try {
        ((CancellationHandler) task).doCancelAction();
      } catch (final Throwable t) {
        if (task.getThrowable() == null) task.setThrowable(t);
        if (traceEnabled) log.trace("throwable raised in cancel()", t);
      }
    }
  }

  /**
   * Set timeout indicator and cancel task when it implements {@code Future} interface.
   */
  synchronized void timeout() {
    this.timeout |= !this.cancelled;
    if (!this.cancelled && !started) cancelTimeoutAction();
    if (task instanceof Future) {
      final Future<?> future = (Future<?>) task;
      if (!future.isDone()) future.cancel(true);
    } else if (task instanceof TimeoutHandler) {
      try {
        ((TimeoutHandler) task).doTimeoutAction();
      } catch (final Throwable t) {
        if (task.getThrowable() == null) task.setThrowable(t);
        if (traceEnabled) log.trace("throwable raised in doCancelAction()", t);
      }
    }
  }

  /**
   * Execute the task within a try/catch block.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void run() {
    if (traceEnabled) log.trace("running {}", this);
    started = true;
    final long id = Thread.currentThread().getId();
    final long startTime = System.nanoTime();
    ClassLoader oldCl = null;
    try {
      oldCl = Thread.currentThread().getContextClassLoader();
      handleTimeout();
      Thread.currentThread().setContextClassLoader(taskClassLoader);
      TaskThreadLocals.setRequestUuid(task.getJob().getUuid());
      executionInfo = CpuTimeCollector.computeExecutionInfo(id);
      if (!isCancelledOrTimedout()) {
        if (traceEnabled) log.trace("starting task id={}", task.getId());
        task.run();
      }
    } catch(final Throwable t) {
      task.setThrowable(t);
      if (t instanceof UnsatisfiedLinkError) task.setResult(ExceptionUtils.getStackTrace(t));
      if (traceEnabled) log.trace(t.getMessage(), t);
    } finally {
      Thread.currentThread().setContextClassLoader(oldCl);
      try {
        elapsedTime = System.nanoTime() - startTime;
        if (executionInfo != null) executionInfo = CpuTimeCollector.computeExecutionInfo(id).subtract(executionInfo);
      } catch(final Throwable e) {
        if (traceEnabled) log.trace("error in finally of {}", this, e);
      }
      try {
        silentTimeout();
        silentCancel();
      } catch (final Throwable t) {
        task.setThrowable(t);
      }
      if (task.getThrowable() instanceof InterruptedException) task.setThrowable(null);
      cancelTimeoutAction();
      if (jobEntry != null) taskEnded();
    }
  }

  /**
   * Get the task this wrapper executes within a try/catch block.
   * @return the task as a {@code Task} instance.
   */
  public Task<?> getTask() {
    return task;
  }

  /**
   * Silently call onTimeout() methods;
   * @return {@code true} when task timeout.
   */
  private boolean silentTimeout() {
    if (timeout) task.onTimeout();
    return timeout;
  }

  /**
   * Silently call onCancel() methods;
   * @return {@code true} when task was cancelled.
   */
  private boolean silentCancel() {
    if (cancelled && callOnCancel) task.onCancel();
    return cancelled;
  }

  /**
   * Determine whether this task was cancelled or timed out.
   * @return {@code true} if the task was cancelled or timed out, {@code false} otherwise.
   */
  private boolean isCancelledOrTimedout() {
    return cancelled || timeout;
  }

  /**
   * Handle the task expiration/timeout if any is specified.
   * @throws Exception if any error occurs.
   */
  private void handleTimeout() throws Exception {
    final JPPFSchedule schedule = task.getTimeoutSchedule();
    if ((schedule != null) && ((schedule.getDuration() > 0L) || (schedule.getDate() != null))) {
      final TimeoutTimerTask tt = new TimeoutTimerTask(this);
      timeoutHandler.scheduleAction(future, getTask().getTimeoutSchedule(), tt);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("task=").append(task);
    if (task.getTaskObject() != task) {
      sb.append(", taskObject=").append(task.getTaskObject());
      if (task.getTaskObject() != null) sb.append(", taskObject class=").append(task.getTaskObject().getClass());
    }
    sb.append(", cancelled=").append(cancelled);
    sb.append(", callOnCancel=").append(callOnCancel);
    sb.append(", timeout=").append(timeout);
    sb.append(", started=").append(started);
    if (jobEntry != null) sb.append(", jobEntry=").append(jobEntry);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the future created by the executor service.
   * @return an instance of {@link Future}.
   */
  public Future<?> getFuture() {
    return future;
  }

  /**
   * Set the future created by the executor service.
   * @param future an instance of {@link Future}.
   */
  public void setFuture(final Future<?> future) {
    this.future = future;
  }

  /**
   * Remove the specified future from the pending set and notify
   * all threads waiting for the end of the execution.
   */
  public void cancelTimeoutAction() {
    if (future != null) timeoutHandler.cancelAction(future);
  }

  /**
   * Get trhe object that holds the used cpu time for this task.
   * @return a {@link ExecutionInfo} instance.
   */
  public ExecutionInfo getExecutionInfo() {
    return executionInfo;
  }

  /**
   * Get the elapsed time for this task's execution.
   * @return the elapsed time in nanoseconds.
   */
  public long getElapsedTime() {
    return elapsedTime;
  }

  /**
   * @return the object which encapsulates information about the job the task is a part of.
   */
  public JobProcessingEntry getJobEntry() {
    return jobEntry;
  }

  /**
   * called when a task terminates.
   */
  public void taskEnded() {
    if (ended.compareAndSet(false, true)) jobEntry.executionManager.taskEnded(this);
  }

  /**
   * @return whether the task has started.
   */
  public boolean hasStarted() {
    return started;
  }
}
