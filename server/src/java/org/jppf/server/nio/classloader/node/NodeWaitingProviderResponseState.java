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

package org.jppf.server.nio.classloader.node;

import static org.jppf.server.nio.classloader.ClassTransition.*;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.jppf.classloader.*;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.ResourceRequest;
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
    Map<JPPFResourceWrapper, ResourceRequest> pendingResponses = context.getPendingResponses();
    if (pendingResponses.isEmpty()) return sendResponse(context);
    JPPFResourceWrapper res = context.getResource();
    Queue<JPPFResourceWrapper> toRemove = new LinkedBlockingQueue<JPPFResourceWrapper>();
    CompositeResourceWrapper composite = null;
    if (res instanceof CompositeResourceWrapper) composite = (CompositeResourceWrapper) res;
    for (Map.Entry<JPPFResourceWrapper, ResourceRequest> entry: pendingResponses.entrySet())
    {
      JPPFResourceWrapper resource = entry.getValue().getResource();
      if (resource.getState() == JPPFResourceWrapper.State.NODE_RESPONSE)
      {
        if (debugEnabled) log.debug("got response for resource " + resource);
        toRemove.add(resource);
        if (composite != null)
        {
          composite.getResources().remove(resource);
          composite.getResources().add(resource);
        }
        else context.setResource(resource);
      }
    }
    while (!toRemove.isEmpty()) pendingResponses.remove(toRemove.poll());
    if (pendingResponses.isEmpty())
    {
      if (debugEnabled) log.debug("sending final response " + context.getResource());
      return sendResponse(context);
    }
    return TO_IDLE_NODE;
  }

  /**
   * Serialize the resource and send it back to the node.
   * @param context the context which serializes the resource.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if any error occurs.
   */
  protected ClassTransition sendResponse(final ClassContext context) throws Exception
  {
    context.serializeResource();
    return TO_SENDING_NODE_RESPONSE;
  }
}
