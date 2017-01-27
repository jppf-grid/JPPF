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
package org.jppf.ui.monitoring.event;

import java.util.EventObject;

import org.jppf.ui.monitoring.data.BaseStatsHandler;

/**
 * Event sent when the stats data has changed.
 * @author Laurent Cohen
 */
public class StatsHandlerEvent extends EventObject {
  /**
   * Enumeration of the types of events.
   */
  public enum Type {
    /**
     * Update with a new data snapshot.
     */
    UPDATE,
    /**
     * The whole dataset shall be reset.
     */
    RESET
  }

  /**
   * The type of this event.
   */
  private Type type = Type.UPDATE;

  /**
   * Initialize this event with a specified source <code>StatsHandler</code>.
   * @param source the stats formatter whose data has changed.
   * @param type the type of this event.
   */
  public StatsHandlerEvent(final BaseStatsHandler source, final Type type) {
    super(source);
    this.type = type;
  }

  /**
   * Get the type of this event.
   * @return the type as a typesafe <code>Type</code> enumerated value.
   */
  public Type getType() {
    return type;
  }
}
