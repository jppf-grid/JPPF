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

package org.jppf.server.nio.classloader.client;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.nio.*;
import org.slf4j.*;

/**
 * Writes all messages in the channel's context queue, if any, or keeps writing the current message until the socket send buffer is full.
 * @author Laurent Cohen
 */
public class AsyncClientClassMessageWriter extends NioMessageWriter<AsyncClientClassContext> {
  
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientClassMessageWriter.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this message writer.
   * @param server the nio server.
   */
  public AsyncClientClassMessageWriter(final StatelessNioServer<AsyncClientClassContext> server) {
    super(server);
  }

  @Override
  protected void postWrite(final AsyncClientClassContext context, final NioMessage data) throws Exception {
    if (debugEnabled) {
      final ClassLoaderNioMessage msg = (ClassLoaderNioMessage) data;
      final JPPFResourceWrapper resource = msg.getResource();
      log.debug("fully sent message {} for resource {} from {}", data, resource, context);
    }
  }
}
