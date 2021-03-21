/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.nio.acceptor;

import java.nio.channels.*;

import org.jppf.JPPFException;
import org.jppf.nio.*;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
class AcceptorMessageReader {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AcceptorMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  static void read(final AcceptorContext context) throws Exception {
    if (context.isSsl()) {
      synchronized(context.getSocketChannel()) {
        doRead(context);
      }
    } else doRead(context);
  }

  /**
   * Read from the channel until no more data is available (i.e. socket receive buffer is empty).
   * @param context the JMX context that reads the data.
   * @throws Exception if any error occurs.
   */
  private static void doRead(final AcceptorContext context) throws Exception {
    if (log.isTraceEnabled()) log.trace("about to read from channel {}", context);
    while (true) {
      if (context.readMessage()) {
        final int id = context.getId();
        if (debugEnabled) log.debug("read identifier '{}' for {}", JPPFIdentifiers.asString(id), context);
        final int port = context.getServerSocketChannel().socket().getLocalPort();
        final NioHelper nioHelper = NioHelper.getNioHelper(port);
        final NioServer server = nioHelper.getServer(id);
        if (server == null) {
          final String name = JPPFIdentifiers.asString(id);
          if ((name == null) || "UNKNOWN".equalsIgnoreCase(name))
            throw new JPPFException("unknown JPPF identifier: " + id + " (0x" + Integer.toHexString(id).toUpperCase() + ")");
          else throw new JPPFException("no server is started for JPPF identifier [" + id + ", 0x" + Integer.toHexString(id).toUpperCase() + ", " + name + "]");
        }
        if (debugEnabled) log.debug("cancelling key for {}", context);
        final SocketChannel socketChannel = context.getSocketChannel();
        final SelectionKey key = socketChannel.keyFor(context.server.getSelector());
        context.setClosed(true);
        key.cancel();
        if (debugEnabled) log.debug("transfering channel to new server {}", server);
        server.accept(context.getServerSocketChannel(), socketChannel, context.getSSLHandler(), context.isSsl(), false);
        if (debugEnabled) log.debug("channel accepted: {}", socketChannel);
        context.setSSLHandler(null);
        break;
      } else if (context.readByteCount <= 0L) break;
    }
  }
}
