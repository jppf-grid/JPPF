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

package org.jppf.server.nio.classloader.node;

import org.jppf.classloader.*;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
public class AsyncNodeClassMessageReader extends NioMessageReader<AsyncNodeClassContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeClassMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this message reader.
   * @param server the nio server.
   */
  public AsyncNodeClassMessageReader(final AsyncNodeClassNioServer server) {
    super(server);
  }

  @Override
  protected MessageHandler<AsyncNodeClassContext> createMessageHandler() {
    return this::handleMessage;
  }

  /**
   * Deserialize the specified message and route it to the specialized handling method.
   * @param context the context associated with the channel.
   * @param message the message to handle.
   * @throws Exception if any error occurs.
   */
  private void handleMessage(final AsyncNodeClassContext context, final NioMessage message) throws Exception {
    if (debugEnabled) log.debug("read message = {} from context = {}", message, context);
    final ClassLoaderNioMessage msg = (ClassLoaderNioMessage) message;
    final JPPFResourceWrapper resource = context.deserializeResource(msg);
    final AsyncNodeClassMessageHandler handler = context.getServer().getMessageHandler();
    if (debugEnabled) log.debug("read resource {} from node {}", resource, context);
    switch(resource.getState()) {
      case NODE_INITIATION:
        handler.handshakeRequest(context, resource);
        break;

      case PROVIDER_REQUEST:
      case NODE_REQUEST:
        handler.nodeRequest(context, resource);
        break;

      case CLOSE_CHANNEL:
        handler.closeChannelRequest(context, resource);
        break;
    }
  }
}
