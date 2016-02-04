/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.List;

import org.jppf.io.DataLocation;
import org.jppf.job.JobReturnReason;
import org.jppf.server.protocol.ServerTaskBundleNode;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeDispatchTimeoutAction implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeDispatchTimeoutAction.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The server handling the node to which the bundle was sent.
   */
  private final NodeNioServer server;
  /**
   * The bundle sent to the node.
   */
  private final ServerTaskBundleNode nodeBundle;
  /**
   * Context for the dispatch node.
   */
  private final AbstractNodeContext context;

  /**
   * Initialize this action with the specified server and nodBundle.
   * @param server the server handling the node to which the bundle was sent.
   * @param nodeBundle the bundle sent to the node.
   * @param context the context for the dispatch node, may be null  for an offline node.
   */
  public NodeDispatchTimeoutAction(final NodeNioServer server, final ServerTaskBundleNode nodeBundle, final AbstractNodeContext context) {
    if (server == null) throw new IllegalArgumentException("server cannot be null");
    if (nodeBundle == null) throw new IllegalArgumentException("node bundle cannot be null");
    this.server = server;
    this.nodeBundle = nodeBundle;
    this.context = context;
  }

  @Override
  public void run() {
    if (!nodeBundle.getJob().isHandshake()) {
      if (debugEnabled) log.debug("node dispatch expiring : {}", nodeBundle);
      nodeBundle.expire();
      String jobUuid = nodeBundle.getJob().getUuid();
      if (context == null) {
        server.getOfflineNodeHandler().removeNodeBundle(jobUuid, nodeBundle.getId());
        nodeBundle.setJobReturnReason(JobReturnReason.DISPATCH_TIMEOUT);
        nodeBundle.taskCompleted(null);
        nodeBundle.resultsReceived((List<DataLocation>) null);
      } else {
        try {
          context.cancelJob(jobUuid, false);
        } catch (Exception e) {
          if (debugEnabled) log.debug("error cancelling job {} : {}", context, ExceptionUtils.getStackTrace(e));
          else log.warn("error cancelling job {} : {}", context, ExceptionUtils.getMessage(e));
        }
      }
    }
  }
}
