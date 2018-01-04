/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.EventObject;

/**
 * A statistics event.
 * @author Laurent Cohen
 */
public class JPPFStatisticsEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The snapshot that was created, removed or updated.
   */
  private final JPPFSnapshot snapshot;

  /**
   * Initialize this event with the specified source statistics and snapshot.
   * @param statistics the statistics object to whom the snapshot belongs.
   * @param snapshot the snapshot for which the event occurred.
   */
  public JPPFStatisticsEvent(final JPPFStatistics statistics, final JPPFSnapshot snapshot) {
    super(statistics);
    this.snapshot = snapshot;
  }

  /**
   * Get the statistics source of this event.
   * @return a {@link JPPFStatistics} instance.
   */
  public JPPFStatistics getStatistics() {
    return (JPPFStatistics) getSource();
  }

  /**
   * Get the snapshot that was created, removed or updated.
   * @return a {@link JPPFSnapshot} instance.
   */
  public JPPFSnapshot getSnapshot() {
    return snapshot;
  }
}
