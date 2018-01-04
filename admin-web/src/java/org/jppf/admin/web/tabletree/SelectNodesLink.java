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

package org.jppf.admin.web.tabletree;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.ui.treetable.TreeViewType;

/**
 * Select all nodes within a topology tree.
 * @author Laurent Cohen
 */
public class SelectNodesLink extends AbstractViewTypeLink {
  /**
   * @param id the id of this link.
   * @param viewType the type of view this button is part of.
   */
  public SelectNodesLink(final String id, final TreeViewType viewType) {
    super(id, Model.of("Select nodes"), viewType, true);
    imageName = "select_nodes.gif";
  }

  @Override
  protected void onClick(final AjaxRequestTarget target, final TableTreeData data) {
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) data.getModel().getRoot();
    final SelectionHandler handler = data.getSelectionHandler();
    handler.clearSelection();
    for (int i=0; i<root.getChildCount(); i++) {
      final DefaultMutableTreeNode dmtnDriver = (DefaultMutableTreeNode) root.getChildAt(i);
      for (int j=0; j<dmtnDriver.getChildCount(); j++) {
        final DefaultMutableTreeNode dmtnNode = (DefaultMutableTreeNode) dmtnDriver.getChildAt(j);
        final AbstractTopologyComponent node = (AbstractTopologyComponent) dmtnNode.getUserObject();
        if (node.isNode()) handler.select(node.getUuid());
      }
    }
  }
}
