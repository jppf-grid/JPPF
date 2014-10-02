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
package org.jppf.ui.monitoring.node.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.slf4j.*;

/**
 * This action stops a node.
 */
public class ServerStatisticsResetAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ServerStatisticsResetAction.class);

  /**
   * Initialize this action.
   */
  public ServerStatisticsResetAction() {
    setupIcon("/org/jppf/ui/resources/server_reset_stats.gif");
    setupNameAndTooltip("driver.reset.statistics");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * This method sets the enabled state to true if at list one driver is selected in the tree.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    this.selectedElements = selectedElements;
    for (Object o: selectedElements) {
      if (!(o instanceof AbstractTopologyComponent)) continue;
      AbstractTopologyComponent data = (AbstractTopologyComponent) o;
      if (!data.isNode()) {
        setEnabled(true);
        return;
      }
    }
    setEnabled(false);
  }

  /**
   * Perform the action.
   * @param event encapsulates the source of the event and additional information.
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    try {
      final List<JMXDriverConnectionWrapper> driverConnections = new ArrayList<>();
      for (Object o: selectedElements) {
        if (!(o instanceof AbstractTopologyComponent)) continue;
        AbstractTopologyComponent data = (AbstractTopologyComponent) o;
        if (data.isDriver()) driverConnections.add(((TopologyDriver) data).getJmx());
      }
      Runnable r = new Runnable() {
        @Override
        public void run() {
          for (JMXDriverConnectionWrapper jmx: driverConnections) {
            try {
              jmx.resetStatistics();
            } catch(Exception e) {
              log.error(e.getMessage(), e);
            }
          }
        }
      };
      runAction(r);
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
