/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.node.provisioning;

import org.jppf.management.JPPFNodeAdminMBean;
import org.jppf.process.LauncherListenerProtocolHandler;
import org.jppf.server.node.JPPFNode;

/**
 *
 * @author Laurent Cohen
 * @since 5.0
 */
public class SlaveNodeProtocolHandler implements LauncherListenerProtocolHandler {
  /**
   * Request a restart with {@code interruptIfRunning = true}.
   */
  public final static int RESTART_INTERRUPT = 1;
  /**
   * Request a restart with {@code interruptIfRunning = false}.
   */
  public final static int RESTART_NO_INTERRUPT = 2;
  /**
   * Request a shutdown with {@code interruptIfRunning = true}.
   */
  public final static int SHUTDOWN_INTERRUPT = 3;
  /**
   * Request a shutdown with {@code interruptIfRunning = false}.
   */
  public final static int SHUTDOWN_NO_INTERRUPT = 4;
  /**
   * The node to send requests to.
   */
  private final JPPFNode node;

  /**
   * Initialize this rpotocol handler witht he specified node.
   * @param node the node to send requests to.
   */
  public SlaveNodeProtocolHandler(final JPPFNode node) {
    this.node = node;
  }

  @Override
  public void performAction(final int actionCode) {
    if (node == null) return;
    JPPFNodeAdminMBean mbean = node.getNodeAdmin();
    if (mbean == null) return;
    try {
      switch(actionCode) {
        case RESTART_INTERRUPT:
          mbean.restart(true);
          break;
        case RESTART_NO_INTERRUPT:
          mbean.restart(false);
          break;
        case SHUTDOWN_INTERRUPT:
          mbean.shutdown(true);
          break;
        case SHUTDOWN_NO_INTERRUPT:
          mbean.shutdown(false);
          break;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
