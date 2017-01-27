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

package org.jppf.server.nio.classloader.client;

import static org.jppf.server.nio.classloader.client.ClientClassTransition.*;
import static org.jppf.utils.StringUtils.build;

import java.net.ConnectException;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.ResourceRequest;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class represents the state of sending a request to a provider.
 * @author Laurent Cohen
 */
class SendingProviderRequestState extends ClientClassServerState {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SendingProviderRequestState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public SendingProviderRequestState(final ClientClassNioServer server) {
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
    if (channel.isReadable() && !channel.isLocal()) throw new ConnectException(build("provider ", channel, " has been disconnected"));
    ResourceRequest request = context.getCurrentRequest();
    if (request == null) {
      request = context.pollPendingRequest();
      if (request != null) {
        context.setMessage(null);
        JPPFResourceWrapper resource = request.getResource();
        if (resource.isSingleResource()) {
          String uuid = resource.getUuidPath().getFirst();
          byte[] content = server.getClassCache().getCacheContent(uuid, resource.getName());
          if (content != null) {
            if (debugEnabled) log.debug("resource [uuid={}, res={}] found in the cache, request will not be sent to the client", uuid, resource.getName());
            resource.setDefinition(content);
            resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
            context.sendNodeResponse(request, resource);
            context.setResource(null);
            return context.hasPendingRequest() ? TO_SENDING_PROVIDER_REQUEST : TO_IDLE_PROVIDER;
          }
        }
        context.setResource(resource);
        if (debugEnabled) log.debug(build("provider ", channel, " serving new request [", context.getResource().getName(), "] from node: ", request.getChannel()));
        context.serializeResource();
        context.setCurrentRequest(request);
      }
    }
    if (context.writeMessage(channel)) {
      if (debugEnabled) log.debug(build("request sent to provider ", channel, " from node ", request, ", resource: ", context.getResource().getName()));
      context.setMessage(null);
      return TO_WAITING_PROVIDER_RESPONSE;
    }
    return TO_SENDING_PROVIDER_REQUEST;
  }
}
