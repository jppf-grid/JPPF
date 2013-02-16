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
import org.jppf.node.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.slf4j.*;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
class NodeTaskWrapper implements Runnable
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
   * The execution manager.
   */
  private final NodeExecutionManagerImpl executionManager;
  /**
   * The class loader instance.
   */
  private final ClassLoader classLoader;
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
   * 
   */
  Future<?> future = null;

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param executionManager reference to the execution manager.
   * @param task the task to execute within a try/catch block.
   * @param classLoader the class loader used as context class loader.
   */
  public NodeTaskWrapper(final NodeExecutionManagerImpl executionManager, final Task task, final ClassLoader classLoader) {
    this.task = task;
    this.executionManager = executionManager;
    this.classLoader = classLoader;
  }

  /**
   * Set cancel indicator and cancel task when it implements <code>Future</code> interface.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  public synchronized void cancel(final boolean callOnCancel) {
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
  public synchronized void timeout() {
    this.timeout |= !this.cancelled;
    if (!this.cancelled && !started) executionManager.cancelTimeoutAction(this);
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
    JPPFNodeReconnectionNotification rn = null;
    ThreadManager.UsedClassLoader usedClassLoader = null;
    ThreadManager threadManager = executionManager.getThreadManager();
    NodeExecutionInfo info = null;
    long elapsedTime = 0L;
    long id = Thread.currentThread().getId();
    long startTime = System.nanoTime();
    try {
      usedClassLoader = threadManager.useClassLoader(classLoader);
      handleTimeout();
      info = threadManager.computeExecutionInfo(id);
      if (!isCancelledOrTimedout()) task.run();
    } catch(JPPFNodeReconnectionNotification t) {
      rn = t;
    } catch(Throwable t) {
      if (t instanceof Exception) task.setException((Exception) t);
      else task.setException(new JPPFException(t));
    } finally {
      try {
        elapsedTime = System.nanoTime() - startTime;
        if (info != null) info = threadManager.computeExecutionInfo(id).subtract(info);
      } catch(Throwable ignore) {
      }
      try {
        silentTimeout();
        silentCancel();
      } catch (Throwable t) {
        if (t instanceof Exception) task.setException((Exception) t);
        else task.setException(new JPPFException(t));
      }
      if (task.getException() instanceof InterruptedException) task.setException(null);
      if (usedClassLoader != null) usedClassLoader.dispose();
      executionManager.cancelTimeoutAction(this);
      if (rn == null) {
        try {
          executionManager.taskEnded(task, info, elapsedTime);
        } catch(JPPFNodeReconnectionNotification t) {
          rn = t;
        }
      }
      if (rn != null) executionManager.setReconnectionNotification(rn);
    }
  }

  /**
   * Get the task this wrapper executes within a try/catch block.
   * @return the task as a <code>JPPFTask</code> instance.
   */
  public Task getTask() {
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
  void handleTimeout() throws Exception {
    JPPFSchedule schedule = task.getTimeoutSchedule();
    if ((schedule != null) && ((schedule.getDuration() > 0L) || (schedule.getDate() != null))) executionManager.processTaskTimeout(this);
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
}
