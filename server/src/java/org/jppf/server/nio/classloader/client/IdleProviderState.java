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

package org.jppf.server.nio.classloader.client;

import static org.jppf.server.nio.classloader.client.ClientClassTransition.*;

import java.net.ConnectException;

import org.jppf.nio.ChannelWrapper;

/**
 * This class represents an idle state for a class loader provider.
 * @author Laurent Cohen
 */
public class IdleProviderState extends ClientClassServerState {
  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public IdleProviderState(final ClientClassNioServer server) {
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
  public ClientClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    ClientClassContext context = (ClientClassContext) channel.getContext();
    if (channel.isReadable() && !channel.isLocal())
    {
      ((ClientClassNioServer) server).removeProviderConnection(context.getUuid(), channel);
      throw new ConnectException("provider " + channel + " has been disconnected");
    }
    if (context.hasPendingRequest()) return TO_SENDING_PROVIDER_REQUEST;
    return context.isPeer() ? TO_IDLE_PEER_PROVIDER : TO_IDLE_PROVIDER;
  }
}
