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

package org.jppf.client.taskwrapper;

import java.io.Serializable;

import org.jppf.node.protocol.Task;

/**
 * Instances of this class are intended to be delegates for the {@link Task#onCancel()} and {@link Task#onTimeout()} methods
 * for tasks that do not implement {@link Task}.
 * @param <T> the ytpe of result returned by the task.
 * @author Laurent Cohen
 */
public abstract class JPPFTaskCallback<T> implements Runnable, Serializable
{
  /**
   * The task this callback is associated with.
   */
  private Task<T> task = null;

  /**
   * Get the task this callback is associated with.
   * @return a <code>Task</code> instance.
   */
  public final Task<T> getTask()
  {
    return task;
  }

  /**
   * Set the task this callback is associated with.
   * @param task a <code>Task</code> instance.
   */
  final void setTask(final Task<T> task)
  {
    this.task = task;
  }
}
