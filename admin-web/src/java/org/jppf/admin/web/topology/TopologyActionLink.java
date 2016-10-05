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

import org.apache.wicket.model.IModel;
import org.jppf.admin.web.tabletree.AbstractActionLink;
import org.jppf.client.monitoring.topology.*;

/**
 *
 * @author Laurent Cohen
 */
public abstract class TopologyActionLink extends AbstractActionLink {
  /**
   *
   * @param id .
   * @param model .
   */
  public TopologyActionLink(final String id, final IModel<String> model) {
    super(id, model);

  }

  /**
   *
   * @param id .
   */
  public TopologyActionLink(final String id) {
    super(id);
  }

  /**
   * Extract the drivers, if any from the specified set of tree nodes.
   * @param treeNodes the tree nodes from which to get the drivers.
   * @return a list of {@link TopologyDriver} instances, possibly empty but never null.
   */
  protected List<TopologyDriver> extractDrivers(final List<DefaultMutableTreeNode> treeNodes) {
    List<TopologyDriver> drivers = new ArrayList<>();
    for (DefaultMutableTreeNode treeNode: treeNodes) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isDriver()) drivers.add((TopologyDriver) comp);
    }
    return drivers;
  }

  /**
   * Extract the nodes, if any from the specified set of tree nodes.
   * @param treeNodes the tree nodes from which to get the nodes.
   * @return a list of {@link TopologyNode} instances, possibly empty but never null.
   */
  protected List<TopologyNode> extractNodes(final List<DefaultMutableTreeNode> treeNodes) {
    List<TopologyNode> nodes = new ArrayList<>();
    for (DefaultMutableTreeNode treeNode: treeNodes) {
      AbstractTopologyComponent comp = (AbstractTopologyComponent) treeNode.getUserObject();
      if (comp.isNode()) nodes.add((TopologyNode) comp);
    }
    return nodes;
  }
}
