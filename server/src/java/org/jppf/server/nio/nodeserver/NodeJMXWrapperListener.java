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

package org.jppf.server.nio.nodeserver;

import org.jppf.management.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeJMXWrapperListener implements JMXConnectionWrapperListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeJMXWrapperListener.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node context.
   */
  private final BaseNodeContext context;
  /**
   * 
   */
  private final NodeConnectionCompletionListener listener;

  /**
   * Initialize this listener.
   * @param context the node context.
   * @param listener the listener to conneciton completion event.
   */
  public NodeJMXWrapperListener(final BaseNodeContext context, final NodeConnectionCompletionListener listener) {
    this.context = context;
    this.listener = listener;
  }

  @Override
  public void onConnected(final JMXConnectionWrapperEvent event) {
    if (debugEnabled) log.debug("JMX connection established from {}, for {}", this, context);
    if (context.getJmxConnection() != null) context.getJmxConnection().removeJMXConnectionWrapperListener(this);
    else if (context.getPeerJmxConnection() != null) context.getPeerJmxConnection().removeJMXConnectionWrapperListener(this);
    listener.nodeConnected(context);
  }

  @Override
  public void onConnectionTimeout(final JMXConnectionWrapperEvent event) {
    if (debugEnabled) log.debug("received jmxWrapperTimeout() for {}, exception: {}", context, ExceptionUtils.getStackTrace(event.getJMXConnectionWrapper().getLastConnectionException()));
    if (context.getJmxConnection() != null) context.getJmxConnection().removeJMXConnectionWrapperListener(this);
    else if (context.getPeerJmxConnection() != null) context.getPeerJmxConnection().removeJMXConnectionWrapperListener(this);
    context.setJmxConnection(null);
    context.setPeerJmxConnection(null);
    listener.nodeConnected(context);
  }
}
