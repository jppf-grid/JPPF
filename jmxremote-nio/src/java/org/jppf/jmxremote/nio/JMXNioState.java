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
   * Initialize with the specified NIO server.
   * @param jmxServer the server which handles the channels states and transitions.
   */
  JMXNioState(final JMXNioServer jmxServer) {
    this.jmxServer = jmxServer;
  }

  /**
   * Set the spceied state to the channel and prepare it for selection.
   * @param channel the channel to transition.
   * @param state the transition to set.
   * @param updateOps the value to AND-wise update the interest ops with.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   * @return {@code null}.
   * @throws Exception if any error occurs.
   */
  JMXTransition transitionChannel(final ChannelWrapper<?> channel, final JMXState state, final int updateOps, final boolean add) throws Exception {
    JMXContext context = (JMXContext) channel.getContext();
    context.setState(state);
    jmxServer.getTransitionManager().updateInterestOps(channel.getSocketChannel(), updateOps, add);
    return null;
  }
}
