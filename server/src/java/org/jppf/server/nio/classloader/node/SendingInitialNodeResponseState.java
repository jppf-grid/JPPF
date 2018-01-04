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

package org.jppf.server.nio.classloader.node;

import static org.jppf.server.nio.classloader.node.NodeClassTransition.*;

import java.net.ConnectException;

import org.jppf.nio.ChannelWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * State of sending the initial response to a newly created node channel.
 * @author Laurent Cohen
 */
class SendingInitialNodeResponseState extends NodeClassServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingInitialNodeResponseState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public SendingInitialNodeResponseState(final NodeClassNioServer server) {
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
  public NodeClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    if (channel.isReadable() && !channel.isLocal()) {
      throw new ConnectException("node " + channel + " has been disconnected");
    }
    final NodeClassContext context = (NodeClassContext) channel.getContext();
    if (context.writeMessage(channel)) {
      if (debugEnabled) log.debug("sent node init response for uuid = {}", context.getUuid());
      if (debugEnabled) log.debug("sent uuid=" + context.getResource().getProviderUuid() + " to node " + channel);
      context.setMessage(null);
      return TO_WAITING_NODE_REQUEST;
    }
    return TO_SENDING_INITIAL_NODE_RESPONSE;
  }
}
