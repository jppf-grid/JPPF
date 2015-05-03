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

package org.jppf.node;

import org.jppf.management.JPPFNodeAdminMBean;
import org.jppf.process.*;
import org.jppf.server.node.JPPFNode;
import org.slf4j.*;

/**
 * The protocol handler which executes commands sent to a slave node by its master node.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class ShutdownRestartNodeProtocolHandler implements LauncherListenerProtocolHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ShutdownRestartNodeProtocolHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node to send requests to.
   */
  private final JPPFNode node;

  /**
   * Initialize this rpotocol handler witht he specified node.
   * @param node the node to send requests to.
   */
  public ShutdownRestartNodeProtocolHandler(final JPPFNode node) {
    this.node = node;
  }

  @Override
  public void performAction(final int actionCode) {
    if (node == null) return;
    JPPFNodeAdminMBean mbean = node.getNodeAdmin();
    if (mbean == null) return;
    try {
      if (debugEnabled) log.debug("processing {} command code", ProcessCommands.getCommandName(actionCode));
      switch(actionCode) {
        case ProcessCommands.RESTART_INTERRUPT:
          mbean.restart(true);
          break;
        case ProcessCommands.RESTART_NO_INTERRUPT:
          mbean.restart(false);
          break;
        case ProcessCommands.SHUTDOWN_INTERRUPT:
          mbean.shutdown(true);
          break;
        case ProcessCommands.SHUTDOWN_NO_INTERRUPT:
          mbean.shutdown(false);
          break;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
