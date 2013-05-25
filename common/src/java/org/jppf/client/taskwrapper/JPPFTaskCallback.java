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

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class are intended to be delegates for the {@link JPPFTask#onCancel()} and {@link JPPFTask#onTimeout()} methods
 * for tasks that do not directly extend {@link JPPFTask}.
 * @author Laurent Cohen
 */
public abstract class JPPFTaskCallback implements Runnable, Serializable
{
  /**
   * The task this callback is associated with.
   */
  private JPPFTask task = null;

  /**
   * Get the task this callback is associated with.
   * @return a <code>JPPFTask</code> instance.
   */
  public final JPPFTask getTask()
  {
    return task;
  }

  /**
   * Set the task this callback is associated with.
   * @param task a <code>JPPFTask</code> instance.
   */
  final void setTask(final JPPFTask task)
  {
    this.task = task;
  }
}
