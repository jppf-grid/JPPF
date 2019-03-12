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

package org.jppf.server.nio.heartbeat;

import org.jppf.comm.recovery.HeartbeatMessage;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
class HeartbeatMessageReader extends NioMessageReader<HeartbeatContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatMessageReader.class);
  /**
   * 
   * @param server .
   */
  public HeartbeatMessageReader(final StatelessNioServer<HeartbeatContext> server) {
    super(server);
  }

  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  @Override
  protected MessageHandler<HeartbeatContext> createMessageHandler() {
    return HeartbeatMessageReader::handleMessage;
  }

  /**
   * Deserialize the specified message and route it to the specialized handling method.
   * @param context the context associated with the channel.
   * @param message the message to handle.
   * @throws Exception if any error occurs.
   */
  private static void handleMessage(final HeartbeatContext context, final NioMessage message) throws Exception {
    if (debugEnabled) log.debug("read message = {} from context = {}", message, context);
    final HeartbeatMessage heartbeatMessage = context.deserializeData(message);
    if (debugEnabled) log.debug("read message {} from {}", message, context);
    if (context.getUuid() == null) {
      final String uuid = heartbeatMessage.getUuid();
      context.setUuid(uuid);
    }
    context.server.getMessageHandler().responseReceived(heartbeatMessage);
  }
}
