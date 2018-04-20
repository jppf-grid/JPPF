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

package org.jppf.nio.acceptor;

import java.nio.channels.*;

import org.jppf.JPPFException;
import org.jppf.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class IdentifyingPeerState extends AcceptorServerState {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IdentifyingPeerState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public IdentifyingPeerState(final AcceptorNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  @Override
  public AcceptorTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    final AcceptorContext context = (AcceptorContext) channel.getContext();
    if (log.isTraceEnabled()) log.trace("about to read from channel {}", channel);
    while (true) {
      if (context.readMessage(channel)) {
        if (!(channel instanceof SelectionKeyWrapper)) return null;
        final int id = context.getId();
        if (debugEnabled) log.debug("read identifier '{}' for {}", JPPFIdentifiers.asString(id), channel);
        final NioServer<?, ?> server = NioHelper.getServer(id);
        if (server == null) throw new JPPFException("unknown JPPF identifier: " + id);
        if (debugEnabled) log.debug("cancelling key for {}", channel);
        final SelectionKey key = (SelectionKey) channel.getChannel();
        final SocketChannel socketChannel = (SocketChannel) key.channel();
        key.cancel();
        if (debugEnabled) log.debug("transfering channel to new server {}", server);
        server.accept(context.getServerSocketChannel(), socketChannel, context.getSSLHandler(), context.isSsl(), false);
        if (debugEnabled) log.debug("channel accepted: {}", socketChannel);
        context.setSSLHandler(null);
        return null;
      } else if (context.byteCount <= 0L) break;
    }
    return AcceptorTransition.TO_IDENTIFYING_PEER;
  }
}
