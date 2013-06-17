/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.util.concurrent.Future;

import org.jppf.*;
import org.jppf.node.NodeExecutionInfo;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.*;
import org.slf4j.*;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public class NodeTaskWrapper implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeTaskWrapper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Timer managing the tasks timeout.
   */
  private final JPPFScheduleHandler timeoutHandler;
  /**
   * Indicator whether task was cancelled;
   */
  private boolean cancelled = false;
  /**
   * Indicator whether <code>onCancel</code> should be called when cancelled.
   */
  private boolean callOnCancel = false;
  /**
   * Indicator whether task timeout.
   */
  private boolean timeout = false;
  /**
   * Indicator that task was started.
   */
  private boolean started = false;
  /**
   * The task to execute within a try/catch block.
   */
  private final Task task;
  /**
   * The future created by the executor service.
   */
  private Future<?> future = null;
  /**
   * 
   */
  private JPPFNodeReconnectionNotification reconnectionNotification = null;
  /**
   * Holds the used cpu time for this task.
   */
  private NodeExecutionInfo executionInfo = null;
  /**
   * The elapsed time for this task's execution.
   */
  private long elapsedTime = 0L;
  /**
   * The class loader that was used to load the task class.
   */
  private final ClassLoader taskClassLoader;

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param task the task to execute within a try/catch block.
   * @param taskClassLoader the class loader that was used to load the task class.
   * @param timeoutHandler handles the timeout for this task.
   */
  public NodeTaskWrapper(final Task task, final ClassLoader taskClassLoader, final JPPFScheduleHandler timeoutHandler) {
    this.task = task;
    this.taskClassLoader = taskClassLoader;
    this.timeoutHandler = timeoutHandler;
  }

  /**
   * Set cancel indicator and cancel task when it implements <code>Future</code> interface.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  synchronized void cancel(final boolean callOnCancel) {
    this.cancelled = true;
    this.callOnCancel |= callOnCancel;
    if (task instanceof Future) {
      Future future = (Future) task;
      if (!future.isDone()) future.cancel(true);
    }
  }

  /**
   * Set timeout indicator and cancel task when it implements <code>Future</code> interface.
   */
  synchronized void timeout() {
    this.timeout |= !this.cancelled;
    if (!this.cancelled && !started) cancelTimeoutAction();
    if (task instanceof Future) {
      Future future = (Future) task;
      if (!future.isDone()) future.cancel(true);
    }
  }

  /**
   * Execute the task within a try/catch block.
   * @see Runnable#run()
   */
  @Override
  public void run()
  {
    if (traceEnabled) log.trace(toString());
    started = true;
    long id = Thread.currentThread().getId();
    long startTime = System.nanoTime();
    ClassLoader oldCl = null;
    try {
      oldCl = Thread.currentThread().getContextClassLoader();
      handleTimeout();
      Thread.currentThread().setContextClassLoader(taskClassLoader);
      executionInfo = CpuTimeCollector.computeExecutionInfo(id);
      if (!isCancelledOrTimedout()) task.run();
    } catch(JPPFNodeReconnectionNotification t) {
      reconnectionNotification = t;
    } catch(Throwable t) {
      if (t instanceof Exception) task.setException((Exception) t);
      else task.setException(new JPPFException(t));
    } finally {
      Thread.currentThread().setContextClassLoader(oldCl);
      try {
        elapsedTime = System.nanoTime() - startTime;
        if (executionInfo != null) executionInfo = CpuTimeCollector.computeExecutionInfo(id).subtract(executionInfo);
      } catch(JPPFNodeReconnectionNotification t) {
        if (reconnectionNotification == null) reconnectionNotification = t;
      } catch(Throwable ignore) {
      }
      try {
        silentTimeout();
        silentCancel();
      } catch(JPPFNodeReconnectionNotification t) {
        if (reconnectionNotification == null) reconnectionNotification = t;
      } catch (Throwable t) {
        if (t instanceof Exception) task.setException((Exception) t);
        else task.setException(new JPPFException(t));
      }
      if (task.getException() instanceof InterruptedException) task.setException(null);
      cancelTimeoutAction();
    }
  }

  /**
   * Get the task this wrapper executes within a try/catch block.
   * @return the task as a <code>JPPFTask</code> instance.
   */
  Task getTask() {
    return task;
  }

  /**
   * Silently call onTimeout() methods;
   * @return <code>true</code> when task timeout.
   */
  private boolean silentTimeout() {
    if (timeout) task.onTimeout();
    return timeout;
  }

  /**
   * Silently call onCancel() methods;
   * @return <code>true</code> when task was cancelled.
   */
  private boolean silentCancel() {
    if (cancelled && callOnCancel) task.onCancel();
    return cancelled;
  }

  /**
   * Determine whether this task was cancelled or timed out.
   * @return <code>true</code> if the task was cancelled or timed out, <code>false</code> otherwise.
   */
  private boolean isCancelledOrTimedout() {
    return cancelled || timeout;
  }

  /**
   * Handle the task expiration/timeout if any is specified.
   * @throws Exception if any error occurs.
   */
  private void handleTimeout() throws Exception {
    JPPFSchedule schedule = task.getTimeoutSchedule();
    if ((schedule != null) && ((schedule.getDuration() > 0L) || (schedule.getDate() != null))) {
      TimeoutTimerTask tt = new TimeoutTimerTask(this);
      timeoutHandler.scheduleAction(future, getTask().getTimeoutSchedule(), tt);
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append(", cancelled=").append(cancelled);
    sb.append(", callOnCancel=").append(callOnCancel);
    sb.append(", timeout=").append(timeout);
    sb.append(", started=").append(started);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the future created by the executor service.
   * @return an instance of {@link Future}.
   */
  public Future<?> getFuture()
  {
    return future;
  }

  /**
   * Set the future created by the executor service.
   * @param future an instance of {@link Future}.
   */
  public void setFuture(final Future<?> future)
  {
    this.future = future;
  }

  /**
   * Get the reconnection notification thrown by the atysk execution, if any.
   * @return a {@link JPPFNodeReconnectionNotification} or <code>null</code>.
   */
  JPPFNodeReconnectionNotification getReconnectionNotification()
  {
    return reconnectionNotification;
  }

  /**
   * Remove the specified future from the pending set and notify
   * all threads waiting for the end of the execution.
   */
  void cancelTimeoutAction()
  {
    if (future != null) timeoutHandler.cancelAction(future);
  }

  /**
   * Get trhe object that holds the used cpu time for this task.
   * @return a {@link NodeExecutionInfo} instance.
   */
  NodeExecutionInfo getExecutionInfo()
  {
    return executionInfo;
  }

  /**
   * Get the elapsed time for this task's execution.
   * @return the elapsed time in nanoseconds.
   */
  long getElapsedTime()
  {
    return elapsedTime;
  }
}
