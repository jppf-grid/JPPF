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

package org.jppf.server.nio.client;

import static org.jppf.server.nio.client.ClientTransition.*;

import java.net.ConnectException;

import org.jppf.nio.ChannelWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class represents an idle state for a client connection (job data channel).
 * @author Laurent Cohen
 */
class IdleState extends ClientServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IdleState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public IdleState(final ClientNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public ClientTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    if (debugEnabled) log.debug("exec() for " + channel);
    if (channel.isReadable()) {
      throw new ConnectException("client " + channel + " has been disconnected");
    }
    final ClientContext context = (ClientContext) channel.getContext();
    return context.isPeer() ? TO_IDLE_PEER : TO_IDLE;
  }
}
