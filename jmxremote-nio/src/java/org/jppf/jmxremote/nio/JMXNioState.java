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

package org.jppf.jmxremote.nio;

import org.jppf.nio.*;

/**
 * Abstract superclass for the states of a JMX NIO channel.
 * @author Laurent Cohen
 */
abstract class JMXNioState extends NioState<JMXTransition> {
  /**
   * The server which handles the channels states and transitions.
   */
  final JMXNioServer jmxServer;
  /**
   * The transition manager.
   */
  final StateTransitionManager<JMXState, JMXTransition> transitionManager;

  /**
   * Initialize with the specified NIO server.
   * @param jmxServer the server which handles the channels states and transitions.
   */
  JMXNioState(final JMXNioServer jmxServer) {
    this.jmxServer = jmxServer;
    this.transitionManager = jmxServer.getTransitionManager();
  }
}
