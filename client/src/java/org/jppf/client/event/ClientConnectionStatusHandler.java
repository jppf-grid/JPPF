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

package org.jppf.client.event;

import org.jppf.client.JPPFClientConnectionStatus;

/**
 * Interface implemented by all classes that desire to handle a connection status
 * and notify others about status changes.
 * @author Laurent Cohen
 * @exclude
 */
public interface ClientConnectionStatusHandler
{
  /**
   * Get the status of this connection.
   * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
   */
  JPPFClientConnectionStatus getStatus();

  /**
   * Set the status of this connection.
   * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
   */
  void setStatus(JPPFClientConnectionStatus status);

  /**
   * Add a connection status listener to this connection's list of listeners.
   * @param listener the listener to add to the list.
   */
  void addClientConnectionStatusListener(ClientConnectionStatusListener listener);

  /**
   * Remove a connection status listener from this connection's list of listeners.
   * @param listener the listener to remove from the list.
   */
  void removeClientConnectionStatusListener(ClientConnectionStatusListener listener);
}
