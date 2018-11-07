/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.client.async;

import org.jppf.nio.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.nio.client.ClientMessage;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
public class AsyncClientMessageReader extends NioMessageReader<AsyncClientContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this message reader.
   * @param server the nio server.
   */
  public AsyncClientMessageReader(final AsyncClientNioServer server) {
    super(server);
  }

  @Override
  protected MessageHandler<AsyncClientContext> createMessageHandler() {
    return AsyncClientMessageReader::handleMessage;
  }

  /**
   * Deserialize the specified message and route it to the specialized handling method.
   * @param context the context associated with the channel.
   * @param message the message to handle.
   * @throws Exception if any error occurs.
   */
  private static void handleMessage(final AsyncClientContext context, final NioMessage message) throws Exception {
    if (debugEnabled) log.debug("read message = {} from context = {}", message, context);
    final ClientMessage msg = (ClientMessage) message;
    final TaskBundle header = msg.getBundle();
    final AsyncClientMessageHandler handler = context.server.getMessageHandler();
    if (debugEnabled) log.debug("read bundle {} from client {}", header, context);
    if (header.isHandshake()) {
      if (context.isPeer()) handler.peerHandshakeResponseReceived(context, msg);
      else handler.handshakeReceived(context, msg);
    } else {
      handler.jobReceived(context, msg);
    }
  }
}
