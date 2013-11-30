/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import static org.jppf.utils.StringUtils.build;

import java.net.ConnectException;

import org.jppf.nio.ChannelWrapper;
import org.jppf.server.nio.classloader.*;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * This class represents the state of sending a response to a node.
 * @author Laurent Cohen
 */
class SendingNodeResponseState extends ClassServerState
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SendingNodeResponseState.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this state with a specified NioServer.
   * @param server the NioServer this state relates to.
   */
  public SendingNodeResponseState(final ClassNioServer server)
  {
    super(server);
  }

  /**
   * Execute the action associated with this channel state.
   * @param channel the selection key corresponding to the channel and selector for this state.
   * @return a state transition as an <code>NioTransition</code> instance.
   * @throws Exception if an error occurs while transitioning to another state.
   */
  @Override
  public ClassTransition performTransition(final ChannelWrapper<?> channel) throws Exception
  {
    if (channel.isReadable() && !channel.isLocal())
    {
      throw new ConnectException(build("node ", channel, " has been disconnected"));
    }
    ClassContext context = (ClassContext) channel.getContext();
    if (context.writeMessage(channel))
    {
      long elapsed = (System.nanoTime() - context.getRequestStartTime()) / 1_000_000L;
      driver.getStatistics().addValues(JPPFStatisticsHelper.NODE_CLASS_REQUESTS_TIME, elapsed, context.getResource().getResources().length);
      if (debugEnabled) log.debug(build("node: ", channel, ", response [", context.getResource(), "] sent to the node"));
      context.setMessage(null);
      return TO_WAITING_NODE_REQUEST;
    }
    return TO_SENDING_NODE_RESPONSE;
  }
}
