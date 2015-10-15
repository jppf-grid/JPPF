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
package org.jppf.ui.monitoring.node.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.*;
import org.jppf.utils.collections.CollectionMap;
import org.slf4j.*;

/**
 * This action stops a node.
 */
public class ToggleNodeActiveAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ToggleNodeActiveAction.class);

  /**
   * Initialize this action.
   */
  public ToggleNodeActiveAction() {
    setupIcon("/org/jppf/ui/resources/toggle_active.gif");
    setupNameAndTooltip("toggle.active");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    setEnabled(dataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event not used.
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        CollectionMap<TopologyDriver, String> map = getDriverMap();
        for (Map.Entry<TopologyDriver, Collection<String>> entry : map.entrySet()) {
          try {
            JMXDriverConnectionWrapper driverJmx = entry.getKey().getJmx();
            if (driverJmx == null) continue;
            NodeSelector selector = new UuidSelector(entry.getValue());
            driverJmx.toggleActiveState(selector);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    runAction(r);
  }
}
