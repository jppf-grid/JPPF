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
package org.jppf.client.event;

import java.util.*;

import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;

/**
 * Event object used to notify interested listeners that a list of task results
 * have been received from the server.
 * @author Laurent Cohen
 */
public class TaskResultEvent extends EventObject
{
  /**
   * An eventual throwable that was raised while receiving the results.
   */
  private final Throwable throwable;

  /**
   * Initialize this event with a specified list of tasks.
   * @param taskList the list of tasks whose results have been received from the server.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public TaskResultEvent(final List<Task<?>> taskList, final Throwable throwable)
  {
    super(taskList);
    this.throwable = throwable;
  }

  /**
   * Get the list of tasks whose results have been received from the server.
   * To properly order the results, developers should use {@link Task#getPosition()} for each task.
   * @return a list of <code>JPPFTask</code> instances.
   * @deprecated as of v4.0, use {@link #getTasks()} instead.
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public List<JPPFTask> getTaskList()
  {
    return (List<JPPFTask>) getSource();
  }

  /**
   * Get the list of tasks whose results have been received from the server.
   * To properly order the results, developers should use {@link Task#getPosition()} for each task.
   * @return a list of <code>Task</code> instances.
   */
  @SuppressWarnings("unchecked")
  public List<Task<?>> getTasks()
  {
    return (List<Task<?>>) getSource();
  }

  /**
   * Get the throwable eventually raised while receiving the results.
   * @return a <code>Throwable</code> instance, or null if no exception or error was raised.
   */
  public Throwable getThrowable()
  {
    return throwable;
  }
}
