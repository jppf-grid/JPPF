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
public class CancelPendingActionLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(CancelPendingActionLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   *
   */
  public CancelPendingActionLink() {
    super(TopologyConstants.CANCEL_PENDING_ACTION, Model.of("Cancel pending action"), "cancel_deferred_action.gif");
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on System info");
    JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getTopologyData();
    List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      CollectionMap<TopologyDriver, String> map = TopologyTreeData.getNodesMultimap(selectedNodes);
      for (Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
        try {
          JPPFNodeForwardingMBean forwarder = entry.getKey().getForwarder();
          if (forwarder == null) continue;
          NodeSelector selector = new UuidSelector(entry.getValue());
          if (debugEnabled) log.debug("invoking cancelPendingAction() for the nodes: " + entry.getValue());
          forwarder.forwardInvoke(selector, JPPFNodeAdminMBean.MBEAN_NAME, "cancelPendingAction");
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
