/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio.classloader;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.List;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.server.nio.ChannelWrapper;
import org.slf4j.*;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
class NodeWaitingProviderResponseState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeWaitingProviderResponseState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the JPPFNIOServer this state relates to.
   */
  public NodeWaitingProviderResponseState(final ClassNioServer server)
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
    JPPFResourceWrapper resource = context.getResource();
    if (JPPFResourceWrapper.State.NODE_RESPONSE.equals(resource.getState())) return TO_SENDING_NODE_RESPONSE;
    String name = resource.getName();
    //uuidPath.decPosition();
    String uuid = resource.getUuidPath().getCurrentElement();
    ChannelWrapper<?> provider = findProviderConnection(uuid);
    if (provider != null)
    {
      synchronized(provider)
      {
        if (debugEnabled) log.debug("request resource [" + name + "] from client: " + provider + " for node: " + channel);
        ClassContext providerContext = (ClassContext) provider.getContext();
        providerContext.addRequest(channel);
        return TO_IDLE_NODE;
      }
    }
    if (debugEnabled) log.debug("no available provider: setting null response for node " + channel);
    resource.setDefinition(null);
    resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
    context.serializeResource(channel);
    return TO_SENDING_NODE_RESPONSE;
  }

  /**
   * Find a provider connection for the specified provider uuid.
   * @param uuid the uuid for which to find a conenction.
   * @return a <code>SelectableChannel</code> instance.
   * @throws Exception if an error occurs while searching for a connection.
   */
  private ChannelWrapper<?> findProviderConnection(final String uuid) throws Exception
  {
    ChannelWrapper<?> result = null;
    List<ChannelWrapper<?>> connections = server.getProviderConnections(uuid);
    if (connections == null) return null;
    int minRequests = Integer.MAX_VALUE;
    for (ChannelWrapper<?> channel: connections)
    {
      ClassContext ctx = (ClassContext) channel.getContext();
      int size = ctx.getNbPendingRequests();
      if (size < minRequests)
      {
        minRequests = size;
        result = channel;
      }
    }
    return result;
  }
}
