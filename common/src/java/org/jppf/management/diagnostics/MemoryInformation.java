/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.management.diagnostics;

import java.io.Serializable;
import java.lang.management.*;


/**
 * Instances of this class hold memory usage information.
 * @author Laurent Cohen
 */
public class MemoryInformation implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The heap memory usage.
   */
  private MemoryUsageInformation heapMemoryUsage = null;
  /**
   * The non-heap memory usage.
   */
  private MemoryUsageInformation nonHeapMemoryUsage = null;

  /**
   * Create this object and initialize its data.
   */
  public MemoryInformation()
  {
    collectMemoryUsage();
  }

  /**
   * Collect the memory usage information.
   */
  private void collectMemoryUsage()
  {
    MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
    this.heapMemoryUsage = new MemoryUsageInformation(bean.getHeapMemoryUsage());
    this.nonHeapMemoryUsage = new MemoryUsageInformation(bean.getNonHeapMemoryUsage());
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("heap    : ").append(heapMemoryUsage).append('\n');
    sb.append("non-heap: ").append(nonHeapMemoryUsage).append('\n');
    return sb.toString();
  }

  /**
   * Get the heap memory usage.
   * @return a <code>JPPFMemoryUsage</code> instance.
   */
  public MemoryUsageInformation getHeapMemoryUsage()
  {
    return heapMemoryUsage;
  }

  /**
   * Get the non-heap memory usage.
   * @return a <code>JPPFMemoryUsage</code> instance.
   */
  public MemoryUsageInformation getNonHeapMemoryUsage()
  {
    return nonHeapMemoryUsage;
  }
}
