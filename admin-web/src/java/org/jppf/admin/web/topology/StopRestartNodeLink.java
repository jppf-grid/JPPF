/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.LoggingUtils;
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
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The type of stop/restart action.
   */
  private ActionType actionType;

  /**
   *
   */
  public static enum ActionType {
    /**
     * Stop.
     */
    STOP(TopologyTree.NODE_STOP_ACTION, "Stop node", "traffic_light_red.gif", false, false),
    /**
     * Restart.
     */
    RESTART(TopologyTree.NODE_RESTART_ACTION, "Restart node", "traffic_light_red_green.gif", true, false),
    /**
     * Deferred stop.
     */
    STOP_DEFERRED(TopologyTree.NODE_STOP_DEFERRED_ACTION, "Stop node (deferred)", "traffic_light_red_yellow.gif", false, true),
    /**
     * Deferred restart.
     */
    RESTART_DEFERRED(TopologyTree.NODE_RESTART_DEFERRED_ACTION, "Restart node (deferred)", "traffic_light_red_green_yellow.gif", true, true);

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
    JPPFWebSession session = getSession(target);
    final TableTreeData data = session.getTopologyData();
    List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      boolean interruptIfRunning = !actionType.isDeferred();
      boolean restart = actionType.isRestart();
      CollectionMap<TopologyDriver, String> map = TopologyTreeData.getNodesMultimap(selectedNodes);
      for (Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
        try {
          JPPFNodeForwardingMBean forwarder = entry.getKey().getForwarder();
          if (forwarder == null) continue;
          if (debugEnabled) log.debug(String.format("invoking %s with interrupt=%b for the nodes: %s", (restart ? "restart()" : "shutdown()"), interruptIfRunning, entry.getValue()));
          NodeSelector selector = new UuidSelector(entry.getValue());
          if (restart) forwarder.restart(selector, interruptIfRunning);
          else forwarder.shutdown(selector, interruptIfRunning);
        } catch (Exception e) {
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
