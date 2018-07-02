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

package org.jppf.server.nio.heartbeat;

import java.nio.channels.SelectionKey;

import org.jppf.nio.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Nio-based server handling echanges of heartbeat messages between servers and nodes.
 * @author Laurent Cohen
 */
public class HeartbeatNioServer extends NioServer<HeartbeatState, HeartbeatTransition> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The message handler for this server.
   */
  private final HeartbeatMessageHandler messageHandler;

  /**
   * Initialize this server with a specified identifier and name.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public HeartbeatNioServer(final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
    messageHandler = new HeartbeatMessageHandler(this);
  }

  @Override
  protected NioServerFactory<HeartbeatState, HeartbeatTransition> createFactory() {
    return new HeartBeatServerFactory(this);
  }

  @Override
  public NioContext<HeartbeatState> createNioContext(final Object... params) {
    return new HeartbeatContext(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel) {
    try {
      if (debugEnabled) log.debug("accepting {}", channel);
      if (!channel.getContext().isPeer()) transitionManager.transitionChannel(channel, HeartbeatTransition.TO_SEND_INITIAL_MESSAGE);
      messageHandler.addChannel((HeartbeatContext) channel.getContext());
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeConnection(channel);
    }
  }

  /**
   * @return the message handler for this server.
   */
  public HeartbeatMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * Close the specified channel.
   * @param channel the channel to close.
   */
  void closeConnection(final ChannelWrapper<?> channel) {
    try {
      messageHandler.removeChannel((HeartbeatContext) channel.getContext());
      final SelectionKey key = (SelectionKey) channel.getChannel();
      key.cancel();
      key.channel().close();
    } catch (final Exception e) {
      log.error("error closing channel {}: {}", channel, ExceptionUtils.getStackTrace(e));
    }
  }
}
