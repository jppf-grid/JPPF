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

package org.jppf.client.event;

import java.util.EventObject;

import org.jppf.client.JPPFClientConnectionStatus;

/**
 * Event sent to notify of a status change for a client connection.
 * @author Laurent Cohen
 */
public class ClientConnectionStatusEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The connection status before the change.
   */
  private JPPFClientConnectionStatus oldStatus = null;

  /**
   * Initialize this event with a client connection as source.
   * @param source the event source.
   * @param oldStatus the connection status before the change.
   */
  public ClientConnectionStatusEvent(final ClientConnectionStatusHandler source, final JPPFClientConnectionStatus oldStatus) {
    super(source);
    this.oldStatus = oldStatus;
  }

  /**
   * Get the source of this event.
   * @return the event source as a <code>ClientConnectionStatusHandler</code> instance.
   */
  public ClientConnectionStatusHandler getClientConnectionStatusHandler() {
    return (ClientConnectionStatusHandler) getSource();
  }

  /**
   * Get the connection status before the change.
   * @return a {@link JPPFClientConnectionStatus} enum value.
   */
  public JPPFClientConnectionStatus getOldStatus() {
    return oldStatus;
  }
}
