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

package org.jppf.server.nio.nodeserver.async;

import static org.jppf.node.protocol.BundleParameter.NODE_OFFLINE_OPEN_REQUEST;

import java.util.concurrent.*;

import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Reads from the channel until no more data is available. Each fully read message is handed off
 * to a global thread pool for deserialization and processing.
 * @author Laurent Cohen
 */
public class AsyncNodeMessageReader extends NioMessageReader<AsyncNodeContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeMessageReader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * 
   */
  private final ExecutorService executor;

  /**
   * Initialize this message reader.
   * @param server the nio server.
   */
  public AsyncNodeMessageReader(final AsyncNodeNioServer server) {
    super(server);
    final int n = server.getConfiguration().getInt("jppf.node.reader.max.threads", Runtime.getRuntime().availableProcessors());
    this.executor = (n > 0) ? Executors.newFixedThreadPool(n, new JPPFThreadFactory("NodeReader")) : GlobalExecutor.getGlobalexecutor();
  }

  @Override
  protected MessageHandler<AsyncNodeContext> createMessageHandler() {
    return AsyncNodeMessageReader::handleMessage;
  }

  /**
   * Deserialize the specified message and route it to the specialized handling method.
   * @param context the context associated with the channel.
   * @param message the message to handle.
   * @throws Exception if any error occurs.
   */
  private static void handleMessage(final AsyncNodeContext context, final NioMessage message) throws Exception {
    if (debugEnabled) log.debug("read message = {} from context = {}", message, context);
    final AbstractTaskBundleMessage msg = (AbstractTaskBundleMessage) message;
    final TaskBundle header = msg.getBundle();
    final AsyncNodeMessageHandler handler = context.getServer().getMessageHandler();
    if (debugEnabled) log.debug("read bundle {} from node {}", header, context);
    if (header.isHandshake() || header.getParameter(NODE_OFFLINE_OPEN_REQUEST, false)) {
      handler.handshakeReceived(context, msg);
    } else if (header.isNotification()) {
      handler.notificationReceived(context, (NotificationBundle) header);
    } else {
      handler.resultsReceived(context, msg);
    }
  }

  @Override
  protected ExecutorService getExecutor() {
    return executor;
  }
}
