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

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.classloader.node.NodeClassNioServer;
import org.slf4j.*;

/**
 * This class represents the state of waiting for the response from a provider.
 * @author Laurent Cohen
 */
class WaitingProviderResponseState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WaitingProviderResponseState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The class cache.
   */
  private static ClassCache classCache = driver.getInitializer().getClassCache();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public WaitingProviderResponseState(final ClassNioServer server)
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
    if (context.readMessage(channel))
    {
      if (debugEnabled) log.debug("read response from provider: " + channel + " complete, sending to node " + context.getCurrentRequest() + 
        ", resource: " + context.getResource().getName());
      JPPFResourceWrapper resource = context.deserializeResource();
      // putting the definition in cache
      if ((resource.getDefinition() != null) && (resource.getCallable() == null))
        classCache.setCacheContent(context.getUuid(), resource.getName(), resource.getDefinition());
      // fowarding it to channel that requested
      ChannelWrapper<?> destinationChannel = context.getCurrentRequest();
      ClassContext destinationContext = (ClassContext) destinationChannel.getContext();
      while (!ClassState.SENDING_NODE_RESPONSE.equals(destinationChannel.getContext().getState()) ||
        (destinationChannel.getKeyOps() != 0)) Thread.sleep(0L, 100000);
      context.setCurrentRequest(null);
      resource.setState(JPPFResourceWrapper.State.NODE_RESPONSE);
      destinationContext.setResource(resource);
      destinationContext.serializeResource();
      NodeClassNioServer nodeClassServer = (NodeClassNioServer) driver.getNodeClassServer();
      nodeClassServer.getTransitionManager().transitionChannel(destinationChannel, TO_SENDING_NODE_RESPONSE);
      //context.setMessage(null);
      return TO_SENDING_PROVIDER_REQUEST;
    }
    return TO_WAITING_PROVIDER_RESPONSE;
  }
}
