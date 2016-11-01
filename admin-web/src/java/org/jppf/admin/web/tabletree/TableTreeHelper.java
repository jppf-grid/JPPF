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

package org.jppf.admin.web.tabletree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.admin.web.health.HealthNodeRenderer;
import org.jppf.admin.web.jobs.JobNodeRenderer;
import org.jppf.admin.web.topology.TopologyNodeRenderer;
import org.jppf.ui.treetable.TreeViewType;

/**
 * 
 * @author Laurent Cohen
 */
public class TableTreeHelper {
  /**
   * Recursively expand all non-leaf nodes in the specified table tree.
   * @param tableTree the table tree that renders the nodes.
   * @param node the root node to expand.
   */
  public static void expand(final JPPFTableTree tableTree, final DefaultMutableTreeNode node) {
    if (node.isLeaf()) return;
    if (!node.isRoot()) tableTree.expand(node);
    for (int i=0; i<node.getChildCount(); i++) expand(tableTree, (DefaultMutableTreeNode) node.getChildAt(i));
  }

  /**
   * Create a new tree node renderter for the specified type of view.
   * @param type the view type.
   * @return an instance of an implementation of {@link TreeNodeRenderer}.
   */
  public static TreeNodeRenderer newTreeNodeRenderer(final TreeViewType type) {
    switch(type) {
      case TOPOLOGY: return new TopologyNodeRenderer();
      case JOBS: return new JobNodeRenderer();
      case HEALTH: return new HealthNodeRenderer();
    }
    throw new IllegalArgumentException(String.format("Unsupported tree view type: %s", type));
  }
}
