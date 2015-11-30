/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.server.nio.nodeserver;

import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
class NodeJMXWrapperListener implements JMXWrapperListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeJMXWrapperListener.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The node context.
   */
  private final AbstractNodeContext context;
  /**
   * The node server.
   */
  private final NodeNioServer server;

  /**
   * Initialize this listener.
   * @param context the node context.
   */
  NodeJMXWrapperListener(final AbstractNodeContext context) {
    this.context = context;
    this.server = JPPFDriver.getInstance().getNodeNioServer();
  }

  @Override
  public void jmxWrapperConnected(final JMXWrapperEvent event) {
    server.nodeConnected(context);
  }

  @Override
  public void jmxWrapperTimeout(final JMXWrapperEvent event) {
    if (debugEnabled) log.debug("received jmxWrapperTimeout() for {}", context);
    context.jmxConnection = null;
    context.peerJmxConnection = null;
    server.nodeConnected(context);
  }
}
