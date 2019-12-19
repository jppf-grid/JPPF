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

import org.jppf.nio.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.nio.AbstractTaskBundleMessage;
import org.jppf.server.protocol.ServerTaskBundleNode;
import org.slf4j.*;

/**
 * Writes all messages in the channel's context queue, if any, or keeps writing the current message until the socket send buffer is full.
 * @author Laurent Cohen
 */
public class AsyncNodeMessageWriter extends NioMessageWriter<AsyncNodeContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeMessageWriter.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this message writer.
   * @param server the nio server.
   */
  public AsyncNodeMessageWriter(final StatelessNioServer<AsyncNodeContext> server) {
    super(server);
  }

  @Override
  protected void preWrite(final AsyncNodeContext context, final NioMessage data) throws Exception {
    final TaskBundle header = ((AbstractTaskBundleMessage) data).getBundle();
    if (debugEnabled) log.debug("before sending message {} for job [uuid={}, name={}, handshake={}] from context {}", data, header.getUuid(), header.getName(), header.isHandshake(), context);
    if (!header.isHandshake()) {
      final ServerTaskBundleNode nodeBundle = context.getJobEntry(header.getUuid(), header.getBundleId());
      if (nodeBundle == null) log.warn("null nodeBundle for header = {}, context = {}, data = {}", header, context, data);
      context.getServer().getMessageHandler().beforeSendingBundle(context, nodeBundle);
    }
  }

  @Override
  protected void postWrite(final AsyncNodeContext context, final NioMessage data) throws Exception {
    final AbstractTaskBundleMessage msg = (AbstractTaskBundleMessage) data;
    final TaskBundle header = msg.getBundle();
    if (debugEnabled) log.debug("fully sent message {} for job [uuid={}, name={}, handshake={}] from context {}", data, header.getUuid(), header.getName(), header.isHandshake(), context);
    if (!header.isHandshake()) {
      final ServerTaskBundleNode nodeBundle = context.getJobEntry(header.getUuid(), header.getBundleId());
      context.getServer().getMessageHandler().bundleSent(context, nodeBundle);
    }
  }
}
