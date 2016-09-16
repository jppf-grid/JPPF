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

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.tabletree.TreeNodeRenderer;
import org.jppf.client.monitoring.topology.*;

/**
 *
 * @author Laurent Cohen
 */
public class TopologyNodeRenderer implements TreeNodeRenderer {
  @Override
  public String getText(final DefaultMutableTreeNode treeNode) {
    AbstractTopologyComponent topologyComp = (AbstractTopologyComponent) treeNode.getUserObject();
    return topologyComp.getDisplayName();
  }

  @Override
  public String getIconPath(final DefaultMutableTreeNode treeNode) {
    AbstractTopologyComponent topologyComp = (AbstractTopologyComponent) treeNode.getUserObject();
    String path = "driver.gif";
    if (topologyComp.isNode()) {
      TopologyNode topologyNode = (TopologyNode) topologyComp;
      path = topologyNode.getManagementInfo().isMasterNode() ? "node-master.png" : "node-slave.png";
    }
    return "images/tree/" + path;
  }
}
