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

package org.jppf.ui.monitoring.data;

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.options.*;
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
  static boolean debugEnabled = log.isDebugEnabled();
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
   * Initialize this status bar handler.
   * @param statusBarOption the option holding the status bar UI component.
   */
  public StatusBarHandler(final OptionElement statusBarOption) {
    this.statusBarOption = statusBarOption;
    TopologyManager manager = StatsHandler.getInstance().getTopologyManager();
    updateStatusBar("/StatusNbServers", manager.getDriverCount());
    updateStatusBar("/StatusNbNodes", manager.getNodeCount());
    manager.addTopologyListener(this);
  }

  /**
   * Update the number of active servers or nodes in the status bar.
   * @param name the name of the field to update.
   * @param n the number of servers to add or subtract.
   */
  void updateStatusBar(final String name, final int n) {
    try {
      AtomicInteger nb = "/StatusNbServers".equals(name) ? nbServers : nbNodes;
      int newNb = nb.addAndGet(n);
      if (debugEnabled) log.debug("updating '" + name + "' with value = " + n + ", result = " + newNb);
      FormattedNumberOption option = (FormattedNumberOption) statusBarOption.findFirstWithName(name);
      if (option != null) option.setValue(Double.valueOf(newNb));
    } catch(Throwable t) {
      log.error(t.getMessage(), t);
    }
  }

  /**
   * Refresh the number of active servers and nodes in the status bar.
   */
  public void refreshStatusBar() {
    FormattedNumberOption option = (FormattedNumberOption) statusBarOption.findFirstWithName("/StatusNbServers");
    if (option != null) option.setValue(Double.valueOf(nbServers.get()));
    option = (FormattedNumberOption) statusBarOption.findFirstWithName("/StatusNbNodes");
    if (option != null) option.setValue(Double.valueOf(nbNodes.get()));
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    updateStatusBar("/StatusNbServers", 1);
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    updateStatusBar("/StatusNbServers", -1);
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    if (event.getNodeData() != null) updateStatusBar("/StatusNbNodes", 1);
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    if (event.getNodeData() != null) updateStatusBar("/StatusNbNodes", -1);
  }
}
