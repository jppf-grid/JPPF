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

package org.jppf.test.addons.mbeans;

import java.io.Serializable;
import java.lang.management.*;
import java.util.*;

/**
 * Instances of this class hold diagnostics information on the whole JVM.
 * @author Laurent Cohen
 */
public class DiagnosticsInfo implements Serializable
{
  /**
   * Mapping of memory pool names to their memeory usage information.
   */
  private final Map<String, JPPFMemoryPoolInfo> memPoolMap = new HashMap<String, JPPFMemoryPoolInfo>();

  /**
   * Create this object and initialize its data.
   */
  public DiagnosticsInfo()
  {
    collectMemoryUsage();
  }

  /**
   * Get the mapping of memory pool names to their memeory usage information.
   * @return a mapping of <code>String</code>s to {@link JPPFMemoryPoolInfo} instances.
   */
  public Map<String, JPPFMemoryPoolInfo> getMemUsageMap()
  {
    return memPoolMap;
  }

  /**
   * Collect the memory usage information.
   */
  void collectMemoryUsage()
  {
    /*
    List<MemoryPoolMXBean> list = ManagementFactory.getMemoryPoolMXBeans();
    if ((list != null) && !list.isEmpty()) for (MemoryPoolMXBean bean: list) memPoolMap.put(bean.getName(), new JPPFMemoryPoolInfo(bean));
    */
    MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
    JPPFMemoryPoolInfo info = new JPPFMemoryPoolInfo("global", null);
    info.getMemUsageMap().put("heap", new JPPFMemoryUsage(bean.getHeapMemoryUsage()));
    info.getMemUsageMap().put("non-heap", new JPPFMemoryUsage(bean.getNonHeapMemoryUsage()));
    memPoolMap.put("global", info);
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, JPPFMemoryPoolInfo> entry: memPoolMap.entrySet())
    {
      sb.append(entry.getValue()).append('\n');
    }
    return sb.toString();
  }
}
