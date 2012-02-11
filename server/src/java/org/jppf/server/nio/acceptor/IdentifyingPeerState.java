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

package org.jppf.server.nio.acceptor;

import java.nio.channels.*;

import org.jppf.JPPFException;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.*;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * This class performs performs the work of reading a task bundle execution response from a node.
 * @author Laurent Cohen
 */
class IdentifyingPeerState extends AcceptorServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IdentifyingPeerState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the driver.
   */
  private static JPPFDriver driver = JPPFDriver.getInstance();

  /**
   * Initialize this state.
   * @param server the server that handles this state.
   */
  public IdentifyingPeerState(final AcceptorNioServer server)
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
  public AcceptorTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    AcceptorContext context = (AcceptorContext) channel.getContext();
    if (context.readMessage(channel))
    {
      if (!(channel instanceof SelectionKeyWrapper)) return null;
      int id = context.getId();
      if (debugEnabled) log.debug("read identifier '" + JPPFIdentifiers.asString(id) + "' for " + channel);
      NioServer newServer = null;
      switch(id)
      {
        case JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL:
          newServer = driver.getClientClassServer();
          break;
        case JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL:
          newServer = driver.getNodeClassServer();
          break;
        case JPPFIdentifiers.CLIENT_JOB_DATA_CHANNEL:
          newServer = driver.getClientNioServer();
          break;
        case JPPFIdentifiers.NODE_JOB_DATA_CHANNEL:
          newServer = driver.getNodeNioServer();
          break;
        default:
          throw new JPPFException("unknown JPPF identifier: " + id);
      }
      if (debugEnabled) log.debug("cancelling key for " + channel);
      SelectionKey key = (SelectionKey) channel.getChannel();
      SocketChannel socketChannel = (SocketChannel) key.channel();
      key.cancel();
      if (debugEnabled) log.debug("registering channel with new server");
      newServer.getLock().lock();
      try
      {
        newServer.getSelector().wakeup();
        ChannelWrapper<?> newChannel = newServer.accept(socketChannel, context.getSSLEngine());
        newChannel.getContext().setSSLEngine(context.getSSLEngine());
        if (debugEnabled) log.debug("channel registered: " + newChannel);
      }
      finally
      {
        newServer.getLock().unlock();
      }
      context.setSSLEngine(null);
      return null;
    }
    return AcceptorTransition.TO_IDENTIFYING_PEER;
  }
}
