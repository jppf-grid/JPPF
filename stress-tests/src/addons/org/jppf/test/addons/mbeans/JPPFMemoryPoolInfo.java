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

package org.jppf.test.addons.mbeans;

import java.io.Serializable;
import java.lang.management.*;
import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFMemoryPoolInfo implements Serializable
{
  /**
   * Mapping of memory info names to their memory usage information.
   */
  private final Map<String, JPPFMemoryUsage> memUsageMap = new HashMap<String, JPPFMemoryUsage>();
  /**
   * The memory type, either heap or non-heap.
   */
  private final MemoryType memoryType;
  /**
   * The name of this memory pool.
   */
  private final String name;

  /**
   * Fill this info from the specified memory pool.
   * @param name the name of this memory pool.
   * @param memoryType the type of this memory pool.
   */
  public JPPFMemoryPoolInfo(final String name, final MemoryType memoryType)
  {
    this.name = name;
    this.memoryType = memoryType;
  }

  /**
   * Fill this info from the specified memory pool.
   * @param bean an instance of {@link MemoryPoolMXBean}.
   */
  public JPPFMemoryPoolInfo(final MemoryPoolMXBean bean)
  {
    MemoryUsage mu = bean.getUsage();
    if (mu != null) memUsageMap.put("current", new JPPFMemoryUsage(mu));
    mu = bean.getPeakUsage();
    if (mu != null) memUsageMap.put("peak", new JPPFMemoryUsage(mu));
    mu = bean.getCollectionUsage();
    if (mu != null) memUsageMap.put("collection", new JPPFMemoryUsage(mu));
    this.memoryType = bean.getType();
    this.name = bean.getName();
  }

  /**
   * Get the mapping of memory pool names to their memeory usage information.
   * @return a mapping of <code>String</code>s to {@link JPPFMemoryUsage} instances.
   */
  public Map<String, JPPFMemoryUsage> getMemUsageMap()
  {
    return memUsageMap;
  }

  /**
   * Get the memory type, either heap or non-heap.
   * @return a {@link MemoryType} enum value.
   */
  public MemoryType getMemoryType()
  {
    return memoryType;
  }

  /**
   * Get the name of this memory pool.
   * @return the name as a string.
   */
  public String getName()
  {
    return name;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("Memory pool [");
    sb.append("name=").append(name).append(", type=").append(memoryType);
    sb.append("]");
    for (Map.Entry<String, JPPFMemoryUsage> entry: memUsageMap.entrySet())
    {
      sb.append("\n  ").append(entry.getKey()).append(" : ").append(entry.getValue());
    }
    return sb.toString();
  }
}
