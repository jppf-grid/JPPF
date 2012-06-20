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
package org.jppf.server.node;

import org.jppf.*;
import org.jppf.node.*;
import org.jppf.node.protocol.Task;

import java.util.concurrent.Future;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
class NodeTaskWrapper extends AbstractNodeTaskWrapper
{
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
   * Initialize this task wrapper with a specified JPPF task.
   * @param executionManager reference to the execution manager.
   * @param task the task to execute within a try/catch block.
   * @param number the internal number identifying the task for the thread pool.
   * @param classLoader the class loader used as context class loader.
   */
  public NodeTaskWrapper(final NodeExecutionManagerImpl executionManager, final Task task, final long number, final ClassLoader classLoader)
  {
    super(task, number);
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

    if (task instanceof Future)
    {
      Future future = (Future) task;
      if (!future.isDone()) future.cancel(true);
    }
  }

  /**
   * Set timeout indicator and cancel task when it implements <code>Future</code> interface.
   */
  public synchronized void timeout() {
    this.timeout |= !this.cancelled;

    if (task instanceof Future)
    {
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
    JPPFNodeReconnectionNotification rn = null;
    ThreadManager.UsedClassLoader usedClassLoader = null;
    ThreadManager threadManager = executionManager.getThreadManager();
    NodeExecutionInfo info = null;
    long elapsedTime = 0L;
    try
    {
      usedClassLoader = threadManager.useClassLoader(classLoader);
      long id = Thread.currentThread().getId();
      long startTime = System.nanoTime();
      info = threadManager.computeExecutionInfo(id);
      task.run();
      try
      {
        // convert cpu time from nanoseconds to milliseconds
        if (info != null) info = threadManager.computeExecutionInfo(id).subtract(info);
        elapsedTime = (System.nanoTime() - startTime) / 1000000L;
      }
      catch(Throwable ignore)
      {
      }
    }
    catch(JPPFNodeReconnectionNotification t)
    {
      rn = t;
    }
    catch(Throwable t)
    {
      if (t instanceof Exception) task.setException((Exception) t);
      else task.setException(new JPPFException(t));
    }
    finally
    {
      boolean remove = false;
      try
      {
        remove |= silentTimeout();
        remove |= silentCancel();
      }
      catch (Throwable t)
      {
        if (t instanceof Exception) task.setException((Exception) t);
        else task.setException(new JPPFException(t));
      }
      if (task.getException() instanceof InterruptedException) task.setException(null);

      if (usedClassLoader != null) usedClassLoader.dispose();

      if (remove) executionManager.removeFuture(number);

      if (rn == null)
      {
        try
        {
          executionManager.taskEnded(task, number, info, elapsedTime);
        }
        catch(JPPFNodeReconnectionNotification t)
        {
          rn = t;
        }
      }
      if (rn != null) executionManager.setReconnectionNotification(rn);
    }
  }

  /**
   * Silently call onTimeout() methods;
   * @return <code>true</code> when task timeout.
   */
  protected synchronized boolean silentTimeout() {
    if (timeout) task.onTimeout();
    return timeout;
  }

  /**
   * Silently call onCancel() methods;
   * @return <code>true</code> when task was cancelled.
   */
  protected synchronized boolean silentCancel() {
    if (cancelled && callOnCancel) task.onCancel();
    return cancelled;
  }

  /**
   * Get the context class loader for this task.
   * @return a {@link ClassLoader} instance.
   */
  public ClassLoader getClassLoader()
  {
    return classLoader;
  }
}
