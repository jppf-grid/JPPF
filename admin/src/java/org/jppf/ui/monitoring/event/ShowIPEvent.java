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

package org.jppf.ui.monitoring.event;

import java.util.EventObject;

import org.jppf.ui.monitoring.ShowIPHandler;
import org.jppf.ui.monitoring.data.BaseStatsHandler;

/**
 * Events sent to all listeners interested in changes to the "ShowIP" toggle.
 * @author Laurent Cohen
 * @since 5.0
 */
public class ShowIPEvent extends EventObject {
  /**
   * The state of the "ShowIP toggle" before the change.
   */
  private final boolean oldState;

  /**
   * Initialize this event.
   * @param source the source of this event.
   * @param oldState the old state of the toggle.
   */
  public ShowIPEvent(final ShowIPHandler source, final boolean oldState) {
    super(source);
    this.oldState = oldState;
  }

  /**
   * Get the state of the "ShowIP toggle" before the change.
   * @return {@code true} if the toggle was set to show IP adddresses, {@code false} otherwise.
   */
  public boolean isOldState() {
    return oldState;
  }

  /**
   * Get the source of this event.
   * @return the source as a {@link BaseStatsHandler} instance.
   */
  public ShowIPHandler getStatsHandler() {
    return (ShowIPHandler) getSource();
  }
}
