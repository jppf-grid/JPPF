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
package org.jppf.ui.monitoring.diagnostics;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.*;

import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;
import org.jppf.utils.collections.CollectionMap;
import org.slf4j.*;

/**
 * This action restarts a node.
 */
public class GCAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(GCAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this action.
   */
  public GCAction() {
    setupIcon("/org/jppf/ui/resources/gc.gif");
    setupNameAndTooltip("health.gc");
  }

  @Override
  public void updateState(final List<Object> selectedElements) {
    this.selectedElements = selectedElements;
    dataArray = selectedElements.isEmpty() ? EMPTY_TOPOLOGY_DATA_ARRAY : new TopologyData[selectedElements.size()];
    for (int i=0; i<selectedElements.size(); i++) dataArray[i] = (TopologyData) selectedElements.get(i);
    setEnabled(dataArray.length > 0);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    runAction(new RunnableAction());
  }

  /**
   * 
   */
  private class RunnableAction implements Runnable {
    @Override
    public void run() {
      // do the gc() in the drivers
      for (TopologyData data: dataArray) {
        if (data.isDriver() && (data.getDiagnostics() != null)) {
          try {
            data.getDiagnostics().gc();
          } catch (IOException e) {
            data.initializeProxies();
            log.error(e.getMessage(), e);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
      // do the gc() in the nodes grouped by server attachment
      CollectionMap<TopologyData, String> map = getDriverMap();
      for (Map.Entry<TopologyData, Collection<String>> entry: map.entrySet()) {
        try {
          JPPFNodeForwardingMBean forwarder = entry.getKey().getNodeForwarder();
          if (forwarder == null) continue;
          NodeSelector selector = new NodeSelector.UuidSelector(entry.getValue());
          forwarder.gc(selector);
        } catch(IOException e) {
          entry.getKey().initializeProxies();
          log.error(e.getMessage(), e);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }
}
