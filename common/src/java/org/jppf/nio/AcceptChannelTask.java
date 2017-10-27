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

package org.jppf.nio;

import java.nio.channels.*;

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.io.IO;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This task performs the processing of a newly accepted channel.
 */
public class AcceptChannelTask implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AcceptChannelTask.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The related nio server.
   */
  private final NioServer<?, ?> server;
  /**
   * The newly accepted socket channel.
   */
  private final SocketChannel channel;
  /**
   * Determines whether ssl is enabled for the channel
   */
  private final boolean ssl;
  /**
   * The server socketc channel that accepted the connection.
   */
  private final ServerSocketChannel serverSocketChannel; 

  /**
   * Initialize this task with the specified selection key.
   * @param serverSocketChannel the server socketc channel that accepted the connection.
   * @param server the related nio server.
   * @param channel the newly accepted socket channel.
   * @param ssl determines whether ssl is enabled for the channel.
   */
  public AcceptChannelTask(final NioServer<?, ?> server, final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final boolean ssl) {
    this.server = server;
    this.channel = channel;
    this.ssl = ssl;
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  public void run() {
    try {
      if (debugEnabled) log.debug("accepting channel {}, ssl={}", channel, ssl);
      channel.socket().setSendBufferSize(IO.SOCKET_BUFFER_SIZE);
      channel.socket().setReceiveBufferSize(IO.SOCKET_BUFFER_SIZE);
      channel.socket().setTcpNoDelay(IO.SOCKET_TCP_NODELAY);
      channel.socket().setKeepAlive(IO.SOCKET_KEEPALIVE);
      intercept();
      if (channel.isBlocking()) channel.configureBlocking(false);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      StreamUtils.close(channel, log);
      return;
    }
    server.accept(serverSocketChannel, channel, null, ssl, false);
  }

  /**
   * Invoke the interceptors for the channel.
   * @throws Exception if any error occurs.
   */
  private void intercept() throws Exception {
    if (InterceptorHandler.hasInterceptor()) {
      channel.configureBlocking(true);
      if (!InterceptorHandler.invokeOnAccept(channel)) throw new JPPFException("connection denied by interceptor: " + channel);
    }
  }
}