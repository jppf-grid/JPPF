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

package org.jppf.server.event;

import java.util.EventListener;

/**
 * Interface for classes that wish to be notified of node connection events.
 * @author Laurent Cohen
 */
public interface NodeConnectionListener extends EventListener
{
  /**
   * Called when a node is connected to the server.
   * @param event encapsulates information about the connected node.
   */
  void nodeConnected(NodeConnectionEvent event);

  /**
   * Called when a node is disconnected from the server.
   * @param event encapsulates information about the connected node.
   */
  void nodeDisconnected(NodeConnectionEvent event);
}
