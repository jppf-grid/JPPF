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

package org.jppf.node;

import java.io.Serializable;

/**
 * Instances of this class hold statistics about the execution of a tasks bundle.
 * @author Laurent Cohen
 */
public class NodeExecutionInfo implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Total cpu time used by the tasks.
   */
  public long cpuTime;
  /**
   * Total user time used by the tasks.
   */
  public long userTime;

  /**
   * Default no-arg constructor.
   */
  public NodeExecutionInfo()
  {
    this(0L, 0L);
  }

  /**
   * Default no-args constructor.
   * @param cpuTime total cpu time used by the tasks.
   * @param userTime total user time used by the tasks.
   */
  public NodeExecutionInfo(final long cpuTime, final long userTime)
  {
    this.cpuTime = cpuTime;
    this.userTime = userTime;
  }

  /**
   * Add the times of another instance to this one.
   * @param other the other execution info object from which to add the values.
   * @return this execution info.
   */
  public NodeExecutionInfo add(final NodeExecutionInfo other)
  {
    cpuTime += other.cpuTime;
    userTime += other.userTime;
    return this;
  }

  /**
   * Subtract the times of another instance from this one.
   * @param other the other execution info object from which to subtract the values.
   * @return this execution info.
   */
  public NodeExecutionInfo subtract(final NodeExecutionInfo other)
  {
    cpuTime -= other.cpuTime;
    userTime -= other.userTime;
    return this;
  }
}
