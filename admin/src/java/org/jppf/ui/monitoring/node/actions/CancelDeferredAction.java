/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.CollectionMap;
import org.slf4j.*;

/**
 * This action stops a node.
 */
public class CancelDeferredAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CancelDeferredAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this action.
   */
  public CancelDeferredAction() {
    setupNameAndTooltip("cancel.deferred.action");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects selected in the tre table.
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
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final CollectionMap<TopologyDriver, String> map = getDriverMap();
        for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
          try {
            final JPPFNodeForwardingMBean forwarder = entry.getKey().getForwarder();
            if (forwarder == null) continue;
            final NodeSelector selector = new UuidSelector(entry.getValue());
            if (debugEnabled) log.debug("invoking cancelPendingAction() for the nodes: " + entry.getValue());
            forwarder.forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "cancelPendingAction");
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    runAction(r);
  }
}
