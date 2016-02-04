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

import java.util.EventListener;

/**
 * Listener interface for JPPF statistics.
 * @author Laurent Cohen
 */
public interface JPPFStatisticsListener extends EventListener {
  /**
   * Called when a new snapshot is created.
   * @param event the event encapsulating the {@link JPPFSnapshot} that was created and the {@link JPPFStatistics} it belongs to.
   */
  void snapshotAdded(JPPFStatisticsEvent event);

  /**
   * Called when a snapshot is removed.
   * @param event the event encapsulating a {@link JPPFSnapshot} that was removed and the {@link JPPFStatistics} it belonged to.
   */
  void snapshotRemoved(JPPFStatisticsEvent event);

  /**
   * Called when a snapshot is updated.
   * @param event the event encapsulating a {@link JPPFSnapshot} that was updated and the {@link JPPFStatistics} it belongs to.
   */
  void snapshotUpdated(JPPFStatisticsEvent event);
}
