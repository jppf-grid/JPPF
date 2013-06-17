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

package org.jppf.client.balancer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.server.protocol.TaskState;


/**
 * This map counts the number of each enum value that is put as a map value.
 * @author Laurent Cohen
 * @exclude
 */
public class TaskStateMap extends TreeMap<Integer, TaskState>
{
  /**
   * Maps for each state the number of tasks in this state.
   */
  private final Map<TaskState, AtomicInteger> stateCounts = new EnumMap<TaskState, AtomicInteger>(TaskState.class);

  /**
   * Initialize this map.
   */
  public TaskStateMap()
  {
    super();
    for (TaskState state: TaskState.values()) stateCounts.put(state, new AtomicInteger(0));
  }

  @Override
  public TaskState put(final Integer key, final TaskState state)
  {
    if (state != null) stateCounts.get(state).incrementAndGet();
    return super.put(key, state);
  }

  @Override
  public TaskState remove(final Object key)
  {
    TaskState state = super.remove(key);
    if (state != null) stateCounts.get(state).decrementAndGet();
    return state;
  }

  /**
   * Get the number of tasks in the specified state.
   * @param state the state for which to get a count.
   * @return the number of tasks that have this state.
   */
  public int getStateCount(final TaskState state)
  {
    if (state == null) return 0;
    return stateCounts.get(state).get();
  }

  @Override
  public void clear()
  {
    super.clear();
    for (TaskState state: TaskState.values()) stateCounts.get(state).set(0);
  }
}
