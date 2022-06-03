/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.example.loadbalancer.common;

import org.jppf.node.policy.CustomPolicy;
import org.jppf.utils.PropertiesCollection;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * A custom execution policy that checks a node has a specified minimum
 * amount of memory for each processing thread in the node.
 * @author Laurent Cohen
 */
public class MyCustomPolicy extends CustomPolicy {
  /**
   * Minimum available size per node processing thread, in bytes.
   */
  private long minimumSizePerThread = 0L;

  /**
   * Initialize this policy with the specified parameter.
   * @param minimumSizePerThreadStr the minimum available heap size per node processing thread.
   */
  public MyCustomPolicy(final String minimumSizePerThreadStr) {
    super(minimumSizePerThreadStr);
    this.minimumSizePerThread = Long.valueOf(minimumSizePerThreadStr);
  }

  /**
   * Determines whether this policy accepts the specified node.
   * @param info system information for the node on which the tasks will run if accepted.
   * @return true if the node is accepted, false otherwise.
   */
  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    // get the number of processing threads in the node
    final long nbThreads = info.getProperties("jppf").get(JPPFProperties.PROCESSING_THREADS);
    // get the node's max heap size
    long maxHeap = info.getProperties("runtime").getLong("maxMemory");
    // we assume that 10 MB is taken by JPPF code and data
    maxHeap -= 10 * 1024 * 1024;
    // return true only if there is at least minimumSizePerThread of memory available for each thread
    return maxHeap / nbThreads >= minimumSizePerThread;
  }
}
