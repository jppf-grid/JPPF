/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.client;

import java.io.Serializable;
import java.util.*;

import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * Instances of this class hold and manage the results of a job.
 * @author Laurent Cohen
 */
public class JobResults implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobResults.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A map containing the tasks that have been successfully executed,
   * ordered by ascending position in the submitted list of tasks.
   */
  private final SortedMap<Integer, Task<?>> resultMap = new TreeMap<>();

  /**
   * Get the current number of received results.
   * @return the number of results as an int.
   */
  public synchronized int size()
  {
    return resultMap.size();
  }

  /**
   * Determine whether this job received a result for the task at the specified position.
   * @param position the task position to check.
   * @return <code>true</code> if a result was received, <code>false</code> otherwise.
   */
  public synchronized boolean hasResult(final int position)
  {
    return resultMap.containsKey(position);
  }

  /**
   * Get the result for the task at the specified position.
   * @param position the position of the task to get.
   * @return a <code>JPPFTask</code> instance, or null if no result was received for a task at this position.
   * @deprecated use {@link #getResultTask(int)} instead.
   */
  public JPPFTask getResult(final int position)
  {
    return (JPPFTask) resultMap.get(position);
  }

  /**
   * Get the result for the task at the specified position.
   * @param position the position of the task to get.
   * @return a <code>Task</code> instance, or null if no result was received for a task at this position.
   */
  public Task<?> getResultTask(final int position)
  {
    return resultMap.get(position);
  }

  /**
   * Add the specified results to this job.
   * @param tasks the list of tasks for which results were received.
   * @deprecated use {@link #addResults(List)} instead.
   */
  @Deprecated
  public synchronized void putResults(final List<JPPFTask> tasks)
  {
    for (JPPFTask task : tasks)
    {
      int pos = task.getPosition();
      if (debugEnabled) log.debug("adding result at positon {}", pos);
      if (hasResult(pos)) log.warn("position {} (out of {}) already has a result", pos, tasks.size());
      resultMap.put(pos, task);
    }
  }

  /**
   * Add the specified results to this job.
   * @param tasks the list of tasks for which results were received.
   */
  public synchronized void addResults(final List<Task<?>> tasks)
  {
    for (Task<?> task : tasks)
    {
      int pos = task.getPosition();
      if (debugEnabled) log.debug("adding result at positon {}", pos);
      if (hasResult(pos)) log.warn("position {} (out of {}) already has a result", pos, tasks.size());
      resultMap.put(pos, task);
    }
  }

  /**
   * Get all the tasks received as results for this job.
   * @return a collection of {@link JPPFTask} instances.
   * @deprecated use {@link #getAllResults()} instead.
   */
  @Deprecated
  public synchronized Collection<JPPFTask> getAll()
  {
    List<JPPFTask> list = new ArrayList<>(resultMap.size());
    for (Task<?> task: resultMap.values()) list.add((JPPFTask) task);
    return Collections.unmodifiableCollection(list);
  }

  /**
   * Get all the tasks received as results for this job.
   * @return a collection of {@link JPPFTask} instances.
   */
  public synchronized Collection<Task<?>> getAllResults()
  {
    return Collections.unmodifiableCollection(resultMap.values());
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("size=").append(size());
    sb.append(", positions=").append(resultMap.keySet());
    sb.append(']');
    return sb.toString();
  }
}
