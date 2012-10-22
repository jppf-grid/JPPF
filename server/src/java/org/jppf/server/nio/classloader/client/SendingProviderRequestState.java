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

package org.jppf.server.nio.classloader.client;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.net.ConnectException;

import org.jppf.server.nio.*;
import org.jppf.server.nio.classloader.*;
import org.slf4j.*;

/**
 * This class represents the state of sending a request to a provider.
 * @author Laurent Cohen
 */
class SendingProviderRequestState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SendingProviderRequestState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public SendingProviderRequestState(final ClassNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   * @see org.jppf.server.nio.NioState#performTransition(java.nio.channels.SelectionKey)
   */
  @Override
  public ClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    ClassContext context = (ClassContext) channel.getContext();
    if (channel.isReadable() && !channel.isLocal())
    {
      throw new ConnectException("provider " + channel + " has been disconnected");
    }
    ResourceRequest request = context.getCurrentRequest();
    if (request == null)
    {
      request = context.pollPendingRequest();
      if (request != null)
      {
        context.setMessage(null);
        context.setResource(request.getResource());
        if (debugEnabled) log.debug("provider " + channel + " serving new resource request [" + context.getResource().getName() + "] from node: " + request);
        context.serializeResource();
        context.setCurrentRequest(request);
      }
    }
    if (request == null)
    {
      if (debugEnabled) log.debug("provider: " + channel + " has no request to process, returning to idle mode");
      context.setMessage(null);
      //return context.isPeer() ? TO_IDLE_PEER_PROVIDER : TO_IDLE_PROVIDER;
      return TO_IDLE_PROVIDER;
    }
    if (context.writeMessage(channel))
    {
      if (debugEnabled) log.debug("request sent to the provider " + channel + " from node " + request +
        ", resource: " + context.getResource().getName());
      context.setMessage(new BaseNioMessage(context.getSSLHandler() != null));
      return TO_WAITING_PROVIDER_RESPONSE;
    }
    return TO_SENDING_PROVIDER_REQUEST;
  }
}
