/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.ui.monitoring.node;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.*;
import org.slf4j.*;

/**
 * Listens to JPPF client connection status changes for rendering purposes.
 */
class ConnectionStatusListener implements ClientConnectionStatusListener
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ConnectionStatusListener.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The name of the connection.
   */
  String driverUuid = null;
  /**
   * The node data panel.
   */
  NodeDataPanel panel = null;

  /**
   * Initialize this listener with the specified connection name.
   * @param panel the node data panel.
   * @param driverUuid the name of the connection.
   */
  public ConnectionStatusListener(final NodeDataPanel panel, final String driverUuid)
  {
    this.driverUuid = driverUuid;
    this.panel = panel;
  }

  /**
   * Invoked when thew connection status has changed.
   * @param event the connection status event.
   * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event)
  {
    if ((event.getClientConnectionStatusHandler().getStatus() == JPPFClientConnectionStatus.ACTIVE) ||
        (event.getClientConnectionStatusHandler().getStatus() == JPPFClientConnectionStatus.EXECUTING))
    {
      ClientConnectionStatusHandler ccsh =  event.getClientConnectionStatusHandler();
      if (debugEnabled) log.debug("Received connection status changed event for " + ccsh + " : " + ccsh.getStatus());
      DefaultMutableTreeNode driverNode = panel.getManager().findDriver(driverUuid);
      if (driverNode != null) panel.getModel().changeNode(driverNode);
    }
  }
}
