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

package org.jppf.server.nio.classloader.node;

import static org.jppf.server.nio.classloader.node.NodeClassTransition.TO_IDLE_NODE;
import static org.jppf.utils.StringUtils.build;

import java.util.*;

import org.jppf.classloader.*;
import org.jppf.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.*;
import org.slf4j.*;

/**
 * This class represents the state of waiting for a request from a node.
 * @author Laurent Cohen
 */
class NodeWaitingProviderResponseState extends NodeClassServerState {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeWaitingProviderResponseState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the JPPFNIOServer this state relates to.
   */
  public NodeWaitingProviderResponseState(final NodeClassNioServer server) {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  @Override
  public NodeClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception {
    NodeClassContext context = (NodeClassContext) channel.getContext();
    if (!context.hasPendingResponse()) return sendResponse(context);
    Map<JPPFResourceWrapper, ResourceRequest>  pendingResponses = context.getPendingResponses();
    JPPFResourceWrapper res = context.getResource();
    Queue<JPPFResourceWrapper> toRemove = new LinkedList<>();
    CompositeResourceWrapper composite = null;
    if (res instanceof CompositeResourceWrapper) composite = (CompositeResourceWrapper) res;
    for (Map.Entry<JPPFResourceWrapper, ResourceRequest> entry: pendingResponses.entrySet())
    {
      JPPFResourceWrapper resource = entry.getValue().getResource();
      switch(resource.getState())
      {
        case NODE_RESPONSE:
        case NODE_RESPONSE_ERROR:
          if (debugEnabled) log.debug(build("node[id=", channel.getId(), "] got response for resource ", resource));
          toRemove.add(resource);
          if (composite != null) composite.addOrReplaceResource(resource);
          else context.setResource(resource);
          break;
      }
    }
    context.removePendingResponses(toRemove);
    return context.hasPendingResponse() ? TO_IDLE_NODE : sendResponse(context);
  }
}
