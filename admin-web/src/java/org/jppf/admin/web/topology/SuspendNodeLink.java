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

package org.jppf.admin.web.topology;

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.utils.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.CollectionMap;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class SuspendNodeLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SuspendNodeLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * 
   */
  public SuspendNodeLink() {
    super(TopologyConstants.NODE_SUSPEND_ACTION, Model.of("Suspend node"), "toggle_active.gif");
    setEnabled(false);
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on suspend node");
    final JPPFWebSession session = JPPFWebSession.get();
    final TopologyTreeData data = session.getTopologyData();
    final List<DefaultMutableTreeNode> selectedNodes = data.getSelectedTreeNodes();
    if (!selectedNodes.isEmpty()) {
      final CollectionMap<TopologyDriver, String> map = TopologyTreeData.getNodesMultimap(selectedNodes);
      for (final Map.Entry<TopologyDriver, Collection<String>> entry : map.entrySet()) {
        try {
          final JMXDriverConnectionWrapper driverJmx = entry.getKey().getJmx();
          if (driverJmx == null) continue;
          final NodeSelector selector = new UuidSelector(entry.getValue());
          driverJmx.toggleActiveState(selector);
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
