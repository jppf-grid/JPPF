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

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * Writes all messages in the channel's context queue, if any, or keeps writing the current message until the socket send buffer is full.
 * @author Laurent Cohen
 */
public class AsyncNodeClassMessageWriter extends NioMessageWriter<AsyncNodeClassContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeClassMessageWriter.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this message writer.
   * @param server the nio server.
   */
  public AsyncNodeClassMessageWriter(final AsyncNodeClassNioServer server) {
    super(server);
  }

  @Override
  protected void postWrite(final AsyncNodeClassContext context, final NioMessage data) throws Exception {
    final ClassLoaderNioMessage msg = (ClassLoaderNioMessage) data;
    final JPPFResourceWrapper resource = msg.getResource();
    if (debugEnabled) log.debug("fully sent message {} for resource = {} from context {}", data, resource, context);
    handleResponseSent(context, resource);
  }

  /**
   * 
   * @param context the node channel.
   * @param resource the repsosnse that was sent.
   * @throws Exception if any error occurs.
   */
  static void handleResponseSent(final AsyncNodeClassContext context, final JPPFResourceWrapper resource) throws Exception {
    if ((resource != null) && (resource.getState() == JPPFResourceWrapper.State.NODE_RESPONSE)) {
      NioHelper.getGlobalexecutor().execute(() -> {
        try {
          context.getServer().getMessageHandler().responseSent(context, resource);
        } catch (final Exception e) {
          context.handleException(e);
        }
      });
    }
  }
}
