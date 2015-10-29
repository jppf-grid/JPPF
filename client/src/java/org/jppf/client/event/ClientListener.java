/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.EventListener;

/**
 * Listener interface for receiving notifications when  a new connection to a server
 * is established and when an existing connections fails and is removed from the client. 
 * @author Laurent Cohen
 */
public interface ClientListener extends EventListener
{
  /**
   * Notify this listener that a new driver connection was created.
   * @param event the event to notify this listener of.
   */
  void newConnection(ClientEvent event);

  /**
   * Notify this listener that a driver connection has failed.
   * @param event the event to notify this listener of.
   */
  void connectionFailed(ClientEvent event);
}
