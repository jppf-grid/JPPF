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
public class ResetTaskCounterLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ResetTaskCounterLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = log.isDebugEnabled();

  /**
   * 
   */
  public ResetTaskCounterLink() {
    super(TopologyConstants.NODE_RESET_TASKS_ACTION, Model.of("Reset task counter"), "reset.gif");
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on reset task counter");
    final JPPFWebSession session = JPPFWebSession.get();
    final TableTreeData data = session.getTopologyData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      final CollectionMap<TopologyDriver, String> map = TopologyTreeData.getNodesMultimap(selectedNodes);
      for (final Map.Entry<TopologyDriver, Collection<String>> entry : map.entrySet()) {
        try {
          final NodeForwardingMBean forwarder = entry.getKey().getForwarder();
          if (forwarder == null) continue;
          final NodeSelector selector = new UuidSelector(entry.getValue());
          forwarder.resetTaskCounter(selector);
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
