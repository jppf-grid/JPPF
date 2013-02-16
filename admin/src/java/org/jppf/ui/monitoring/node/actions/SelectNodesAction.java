/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import javax.swing.tree.*;

import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.treetable.*;

/**
 * Action performed to select all drivers in the topology view.
 * @author Laurent Cohen
 */
public class SelectNodesAction extends AbstractSelectionAction
{
  /**
   * Initialize this action with the specified tree table panel.
   * @param panel the tree table panel to which this action applies.
   */
  public SelectNodesAction(final NodeDataPanel panel)
  {
    super(panel);
    setupIcon("/org/jppf/ui/resources/select_nodes.gif");
    setupNameAndTooltip("select.nodes");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void actionPerformed(final ActionEvent e)
  {
    synchronized(panel)
    {
      JPPFTreeTable treeTable = panel.getTreeTable();
      TreeTableModelAdapter model = (TreeTableModelAdapter) treeTable.getModel();
      List<TreePath> selectionPath = new ArrayList<TreePath>();
      for (DefaultMutableTreeNode driverNode: getDriverNodes())
      {
        for (int i=0; i<driverNode.getChildCount(); i++)
        {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode) driverNode.getChildAt(i);
          TopologyData data = (TopologyData) node.getUserObject();
          if ((data == null) || !(TopologyDataType.NODE == data.getType())) continue;
          selectionPath.add(treeTable.getPathForNode(node));
        }
      }
      model.setSelectedPaths(selectionPath.toArray(new TreePath[selectionPath.size()]));
    }
  }
}
