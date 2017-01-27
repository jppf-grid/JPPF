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

package org.jppf.server;

import org.jppf.management.JPPFSystemInformation;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.stats.*;

/**
 * This statistics listener updates the server's {@link JPPFSystemInformation} whenever the server statistics change.
 * @author Laurent Cohen
 */
class StatsSystemInformationUpdater implements JPPFStatisticsListener {
  /**
   * 
   */
  private final TypedProperties statsProperties;

  /**
   * Initialize this listener with the specified system information.
   * @param info he system information to keep up to date with statistics events.
   */
  StatsSystemInformationUpdater(final JPPFSystemInformation info) {
    this.statsProperties = info.getStats();
  }

  @Override
  public void snapshotAdded(final JPPFStatisticsEvent event) {
    update(event.getSnapshot());
  }

  @Override
  public void snapshotRemoved(final JPPFStatisticsEvent event) {
    statsProperties.remove(event.getSnapshot().getLabel());
  }

  @Override
  public void snapshotUpdated(final JPPFStatisticsEvent event) {
    update(event.getSnapshot());
  }

  /**
   * Update the system info with the vazlues in the specified statistics snapshot.
   * @param snapshot the snapshot to update from.
   */
  private void update(final JPPFSnapshot snapshot) {
    JPPFStatisticsHelper.toProperties(statsProperties, snapshot);
  }
}
