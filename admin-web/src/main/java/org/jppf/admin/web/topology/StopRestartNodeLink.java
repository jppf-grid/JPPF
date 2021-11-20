/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.admin.web.topology;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.TableTreeData;
import org.jppf.admin.web.utils.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.utils.collections.CollectionMap;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class StopRestartNodeLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(StopRestartNodeLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The type of stop/restart action.
   */
  private ActionType actionType;

  /**
   *  The type of stop/restart and immediate/differed node action.
   */
  public static enum ActionType {
    /**
     * Stop.
     */
    STOP(TopologyConstants.NODE_STOP_ACTION, "Stop node", "traffic_light_red.gif", false, false),
    /**
     * Restart.
     */
    RESTART(TopologyConstants.NODE_RESTART_ACTION, "Restart node", "traffic_light_red_green.gif", true, false),
    /**
     * Deferred stop.
     */
    STOP_DEFERRED(TopologyConstants.NODE_STOP_DEFERRED_ACTION, "Stop node (deferred)", "traffic_light_red_yellow.gif", false, true),
    /**
     * Deferred restart.
     */
    RESTART_DEFERRED(TopologyConstants.NODE_RESTART_DEFERRED_ACTION, "Restart node (deferred)", "traffic_light_red_green_yellow.gif", true, true),
    /**
     * Reconnect.
     */
    RECONNECT(TopologyConstants.NODE_RECONNECT_ACTION, "Reconnect node", "reconnect.png", false, false),
    /**
     * Reconnect.
     */
    RECONNECT_DEFERRED(TopologyConstants.NODE_RECONNECT_DEFERRED_ACTION, "Reconnect node (deferred)", "reconnect-deferred.png", false, true)
    ;

    /**
     * The action id.
     */
    private final String id;
    /**
     * The action model string.
     */
    private final String model;
    /**
     * The name of the associated icon.
     */
    private final String imageName;
    /**
     * Whether the action is a restart action.
     */
    private final boolean restart;
    /**
     * Whether the action is deferred.
     */
    private final boolean deferred;

    /**
     *
     * @param id the action id.
     * @param model the action model string.
     * @param imageName the name of the associated icon.
     * @param restart whether the action is a restart action.
     * @param deferred whether the action is deferred.
     */
    private ActionType(final String id, final String model, final String imageName, final boolean restart, final boolean deferred) {
      this.id = id;
      this.model = model;
      this.imageName = imageName;
      this.restart = restart;
      this.deferred = deferred;
    }

    /**
     * @return the action id.
     */
    public String getId() {
      return id;
    }

    /**
     * @return the action model string.
     */
    public String getModel() {
      return model;
    }

    /**
     * @return the name of the associated icon.
     */
    public String getImageName() {
      return imageName;
    }

    /**
     * @return whether the action is a restart action.
     */
    public boolean isRestart() {
      return restart;
    }

    /**
     * @return deferred whether the action is deferred.
     */
    public boolean isDeferred() {
      return deferred;
    }
  }

  /**
   * @param actionType the type of stop/restart action.
   */
  public StopRestartNodeLink(final ActionType actionType) {
    super(actionType.getId(), Model.of(actionType.getModel()), actionType.getImageName());
    this.actionType = actionType;
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on {}", actionType);
    final JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getTopologyData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      final boolean interruptIfRunning = !actionType.isDeferred();
      final boolean restart = actionType.isRestart();
      final CollectionMap<TopologyDriver, String> map = TopologyTreeData.getNodesMultimap(selectedNodes);
      for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
        try {
          final NodeForwardingMBean forwarder = entry.getKey().getForwarder();
          if (forwarder == null) continue;
          if (debugEnabled) log.debug("invoking {} with interrupt={} for the nodes: {}", (restart ? "restart()" : "shutdown()"), interruptIfRunning, entry.getValue());
          final NodeSelector selector = new UuidSelector(entry.getValue());
          switch(actionType) {
            case RESTART:
            case RESTART_DEFERRED:
              forwarder.restart(selector, interruptIfRunning);
              break;
            case STOP:
            case STOP_DEFERRED:
              forwarder.shutdown(selector, interruptIfRunning);
              break;
            case RECONNECT:
            case RECONNECT_DEFERRED:
              forwarder.reconnect(selector, interruptIfRunning);
              break;
          }
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  /**
   *
   */
  public static class Action extends AbstractManagerRoleAction {
    @Override
    public void setEnabled(final List<DefaultMutableTreeNode> selected) {
      enabled = isNodeSelected(selected);
    }
  }
}
