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

package org.jppf.client.balancer.execution;

import org.jppf.JPPFException;
import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.node.protocol.Task;

import java.util.List;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 * @author Martin JANDA
 */
class NodeTaskWrapper extends AbstractNodeTaskWrapper
{
  /**
   * The execution manager.
   */
  private final LocalExecutionManager executionManager;

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param executionManager reference to the execution manager.
   * @param task             the task to execute within a try/catch block.
   * @param uuidPath         the key to the JPPFContainer for the task's classloader.
   * @param number           the internal number identifying the task for the thread pool.
   */
  public NodeTaskWrapper(final LocalExecutionManager executionManager, final Task task, final List<String> uuidPath, final long number)
  {
    super(task, uuidPath, number);
    this.executionManager = executionManager;
  }

  /**
   * Get the number identifying the task.
   * @return long value identifying the task.
   */
  public long getNumber()
  {
    return number;
  }

  /**
   * Execute the task within a try/catch block.
   * @see Runnable#run()
   */
  @Override
  public void run()
  {
    long cpuTime = 0L;
    long elapsedTime = 0L;
    JPPFNodeReconnectionNotification reconnectionNotification = null;
    try
    {
      long id = Thread.currentThread().getId();
      executionManager.processTaskTimeout(this);
      long startTime = System.nanoTime();
      long startCpuTime = 0; //executionManager.getCpuTime(id);
      task.run();
      try
      {
        // convert cpu time from nanoseconds to milliseconds
        cpuTime = 0; //(executionManager.getCpuTime(id) - startCpuTime) / 1000000L;
        elapsedTime = (System.nanoTime() - startTime) / 1000000L;
      }
      catch (Throwable ignore)
      {
      }
    }
    catch (JPPFNodeReconnectionNotification t)
    {
      reconnectionNotification = t;
    }
    catch (Throwable t)
    {
      if (t instanceof Exception)
      {
        task.setException((Exception) t);
      }
      else
      {
        task.setException(new JPPFException(t));
      }
    }
    finally
    {
      if (reconnectionNotification == null)
      {
        try
        {
          executionManager.taskEnded(this, cpuTime, elapsedTime);
        }
        catch (JPPFNodeReconnectionNotification t)
        {
          reconnectionNotification = t;
        }
      }
      if (reconnectionNotification != null)
      {
        executionManager.setReconnectionNotification(reconnectionNotification);
      }
    }
  }
}
