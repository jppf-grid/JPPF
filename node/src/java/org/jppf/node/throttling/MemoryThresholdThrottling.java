/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node.throttling;

import org.jppf.node.Node;
import org.jppf.utils.SystemUtils;
import org.slf4j.*;

/**
 * A throttling mechanism that causes a node to not accept jobs whenever heap usage reaches a configured percentage of the maximum heap size. It can be configured with the following properties:<br>
 * <ul>
 * <li>it can be activated by setting the node configuration property {@code jppf.node.throttling.memory.threshold.active = true} ({@code false} by default)</li>
 * <li>the threshold can be set as a percentage of the maximum heap size with the node configuration property {@code jppf.node.throttling.memory.threshold = 87.5} (90% by default)</li>
 * <li>it can also call {@code System.gc()}, in an attempt to mitigate the high heap usage, after {@link #acceptsNewJobs(Node)} returns {@code false} a configured consecutive number of times.
 * The corresponding configuration property is set as {@code jppf.node.throttling.memory.threshold.maxNbTimesFalse = 2} (3 by default)</li>
 * </ul>
 * <p>Example configuration:<br>
 * <pre class="jppf_pre">
 * <span style="color: green"># activate the throttling plugin</span>
 * jppf.node.throttling.memory.threshold.active = true
 * <span style="color: green"># % of maximum heap size that causes the node to refuse jobs</span>
 * jppf.node.throttling.memory.threshold = 87.5
 * <span style="color: green"># number of time the check returns false before invoking System.gc()</span>
 * jppf.node.throttling.memory.threshold.maxNbTimesFalse = 5
 * </pre>
 * @author Laurent Cohen
 * @since 6.1
 */
public class MemoryThresholdThrottling implements JPPFNodeThrottling {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(MemoryThresholdThrottling.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Used for logging purposes, the first time {@link #acceptsNewJobs(Node)} is called.
   */
  private boolean firstTime = true;
  /**
   * Number of times in a row that {@link #acceptsNewJobs(Node)} returned {@code false}.
   */
  private int nbTimesFalse; 

  /**
   * Default constructor.
   */
  public MemoryThresholdThrottling() {
  }

  @Override
  public boolean acceptsNewJobs(final Node node) {
    final boolean active = node.getConfiguration().getBoolean("jppf.node.throttling.memory.threshold.active", false);
    if (firstTime) {
      firstTime = false;
      if (debugEnabled) log.debug("MemoryThresholdThrottling is {}", active ?"active" : "inactive");
    }
    if (!active) return true;
    final double threshold = node.getConfiguration().getDouble("jppf.node.throttling.memory.threshold", 90d);
    final double pct = SystemUtils.heapUsagePct();
    if (traceEnabled) log.trace(String.format("current heap usage is %.2f %%", pct));
    final boolean result = pct < threshold;
    if (!result) {
      if (debugEnabled) log.debug(String.format("detected that current heap usage (%.2f %%) > threshold (%.2f %%)", pct, threshold));
      nbTimesFalse++;
      final int maxNbTimesFalse = node.getConfiguration().getInt("jppf.node.throttling.memory.threshold.maxNbTimesFalse", 3);
      if (nbTimesFalse >= maxNbTimesFalse) {
        System.gc();
        nbTimesFalse = 0;
      }
    } else nbTimesFalse = 0;
    
    return result;
  }
}
