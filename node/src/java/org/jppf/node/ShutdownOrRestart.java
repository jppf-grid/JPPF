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

package org.jppf.node;

import java.security.*;

import org.jppf.server.node.JPPFNode;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Task used to terminate the JVM.
 * @exclude
 */
public class ShutdownOrRestart implements Runnable {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeRunner.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * {@code true} if the node is to be restarted, {@code false} to only shut it down.
   */
  private final boolean restart;
  /**
   * {@code true} to exit the JVM after shutdown, {@code false} otherwise.
   */
  private final boolean exit;
  /**
   * The node to shutdon and/or restart.
   */
  private final NodeInternal node;

  /**
   * Initialize this task.
   * @param restart {@code true} if the node is to be restarted, {@code false} to only shut it down.
   * @param exit {@code true} to exit the JVM after shutdown, {@code false} otherwise.
   * @param node this node.
   */
  public ShutdownOrRestart(final boolean restart, final boolean exit, final NodeInternal node) {
    this.restart = restart;
    this.exit = exit;
    this.node = node;
  }

  @Override
  public void run() {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
      @Override
      public Object run() {
        if (debugEnabled) log.debug("stopping the node");
        node.stopNode();
        // close the JMX server connection to avoid request being sent again by the client.
        if (debugEnabled) log.debug("stopping the JMX server");
        try {
          ((JPPFNode) node).stopJmxServer();
          Thread.sleep(500L);
        } catch(@SuppressWarnings("unused") final Exception ignore) {
        }
        if (exit) {
          final int exitCode = restart ? 2 : 0;
          log.info("exiting the node with exit code {}", exitCode);
          System.exit(exitCode);
        }
        return null;
      }
    });
  }
}