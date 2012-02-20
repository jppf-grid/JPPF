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

import org.jppf.node.protocol.Task;

import java.util.List;

/**
 * Wrapper around a JPPF task used to catch exceptions caused by the task execution.
 * @author Domingos Creado
 * @author Laurent Cohen
 */
public abstract class AbstractNodeTaskWrapper implements Runnable
{
  /**
   * The task to execute within a try/catch block.
   */
  protected final Task task;
  /**
   * The key to the JPPFContainer for the task's classloader.
   */
  protected final List<String> uuidPath;
  /**
   * The number identifying the task.
   */
  protected final long number;

  /**
   * Initialize this task wrapper with a specified JPPF task.
   * @param task     the task to execute within a try/catch block.
   * @param uuidPath the key to the JPPFContainer for the task's classloader.
   * @param number   the internal number identifying the task for the thread pool.
   */
  public AbstractNodeTaskWrapper(final Task task, final List<String> uuidPath, final long number)
  {
    this.task = task;
    this.uuidPath = uuidPath;
    this.number = number;
  }

  /**
   * Get the task this wrapper executes within a try/catch block.
   * @return the task as a <code>JPPFTask</code> instance.
   */
  public Task getTask()
  {
    return task;
  }
}
