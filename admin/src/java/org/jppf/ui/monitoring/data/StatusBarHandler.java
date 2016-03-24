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

import javax.swing.SwingUtilities;

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
   * The option holding the count of drivers.
   */
  private final FormattedNumberOption serverField;
  /**
   * The option holding the count of nodes.
   */
  private final FormattedNumberOption nodeField;
  /**
   * 
   */
  private final TopologyManager manager;

  /**
   * Initialize this status bar handler.
   * @param statusBarOption the option holding the status bar UI component.
   */
  public StatusBarHandler(final OptionElement statusBarOption) {
    this.serverField = (FormattedNumberOption) statusBarOption.findFirstWithName("/StatusNbServers");
    this.nodeField = (FormattedNumberOption) statusBarOption.findFirstWithName("/StatusNbNodes");
    manager = StatsHandler.getInstance().getTopologyManager();
    updateStatusBar();
    manager.addTopologyListener(this);
  }

  /**
   * Update the number of active servers and nodes in the status bar.
   */
  void updateStatusBar() {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        if (debugEnabled) log.debug("updating status bar with nbDrivers={}, nbNodes={}", manager.getDriverCount(), manager.getNodeCount());
        synchronized(StatusBarHandler.this) {
          serverField.setValue(manager.getDriverCount());
          nodeField.setValue(manager.getNodeCount());
        }
      }
    };
    SwingUtilities.invokeLater(r);
  }

  @Override
  public void driverAdded(final TopologyEvent event) {
    updateStatusBar();
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
    updateStatusBar();
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
    updateStatusBar();
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
    updateStatusBar();
  }
}
