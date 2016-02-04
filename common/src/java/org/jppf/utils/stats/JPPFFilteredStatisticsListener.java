/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.utils.stats;

/**
 * A statistics listener with an optionally associated {@link JPPFStatistics.Filter snapshot filter}.
 * @author Laurent Cohen
 */
public abstract class JPPFFilteredStatisticsListener implements JPPFStatisticsListener {
  /**
   * Get an optional filter to associate with this listener. The listener will only be notified for snapshots that are accepted by the filter.
   * The default implementation returns {@code null} (no filter) and is intended to be overriden as needed in subclasses. 
   * <p>Additionally, this method is only called once for each listener, so you don't have to keep a reference to the created filter.
   * @return a {@link JPPFStatistics.Filter} instance.
   */
  public JPPFStatistics.Filter getFilter() {
    return null;
  }
}
