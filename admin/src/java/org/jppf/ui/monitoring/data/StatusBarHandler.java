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

package org.jppf.ui.monitoring.data;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.options.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class handles updates ot the status bar.
 * @author Laurent Cohen
 */
public class StatusBarHandler extends TopologyListenerAdapter {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(StatusBarHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Identifies the field counting the drivers.
   */
  private static final int DRIVERS = 1;
  /**
   * Identifies the field counting the nodes.
   */
  private static final int NODES = 2;
  /**
   * Number of active servers.
   */
  private AtomicInteger nbServers = new AtomicInteger(0);
  /**
   * Number of active nodes.
   */
  private AtomicInteger nbNodes = new AtomicInteger(0);
  /**
   * The option holding the status bar UI component.
   */
  private final OptionElement statusBarOption;
  /**
   * The option holding the count of drivers.
   */
  private final FormattedNumberOption serverField;
  /**
   * The option holding the count of nodes.
   */
  private final FormattedNumberOption nodeField;

  /**
   * Initialize this status bar handler.
   * @param statusBarOption the option holding the status bar UI component.
   */
  public StatusBarHandler(final OptionElement statusBarOption) {
    this.statusBarOption = statusBarOption;
    this.serverField = (FormattedNumberOption) statusBarOption.findFirstWithName("/StatusNbServers");
    this.nodeField = (FormattedNumberOption) statusBarOption.findFirstWithName("/StatusNbNodes");
    TopologyManager manager = StatsHandler.getInstance().getTopologyManager();
    updateStatusBar(DRIVERS, manager.getDriverCount());
    updateStatusBar(NODES, manager.getNodeCount());
    manager.addTopologyListener(this);
  }

  /**
   * Update the number of active servers or nodes in the status bar.
   * @param fieldId the id of the field to update.
   * @param n the number of servers to add or subtract.
   */
  void updateStatusBar(final int fieldId, final int n) {
    try {
      FormattedNumberOption field = (fieldId == DRIVERS) ? serverField : nodeField;
      AtomicInteger nb = (fieldId == DRIVERS) ? nbServers : nbNodes;
      int newNb = nb.addAndGet(n);
      if (debugEnabled) log.debug("updating '" + field.getName() + "' with value = " + n + ", result = " + newNb);
      field.setValue(Double.valueOf(newNb));
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Refresh the number of active servers and nodes in the status bar.
   */
  public void refreshStatusBar() {
    serverField.setValue(Double.valueOf(nbServers.get()));
    nodeField.setValue(Double.valueOf(nbNodes.get()));
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    updateStatusBar(DRIVERS, 1);
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    updateStatusBar(DRIVERS, -1);
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    if (event.getNodeOrPeer().isNode()) updateStatusBar(NODES, 1);
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    if (event.getNodeOrPeer().isNode()) updateStatusBar(NODES, -1);
  }
}
