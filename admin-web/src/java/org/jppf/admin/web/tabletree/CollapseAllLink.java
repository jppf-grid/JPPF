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
import org.jppf.ui.treetable.TreeViewType;

/**
 * Collapse all the nodes in a tree.
 * @author Laurent Cohen
 */
public class CollapseAllLink extends AbstractViewTypeLink {
  /**
   * @param id the id of this link.
   * @param viewType the type of view this button is part of.
   */
  public CollapseAllLink(final String id, final TreeViewType viewType) {
    super(id, Model.of("Collapse all"), viewType, false);
    imageName = "collapse.gif";
  }

  @Override
  public void onClick(final AjaxRequestTarget target, final TableTreeData data) {
    final DefaultMutableTreeNode root = (DefaultMutableTreeNode) data.getModel().getRoot();
    if (target.getPage() instanceof TableTreeHolder) {
      final JPPFTableTree tableTree = ((TableTreeHolder) target.getPage()).getTableTree();
      for (int i=0; i<root.getChildCount(); i++) tableTree.collapse((DefaultMutableTreeNode) root.getChildAt(i));
      target.add(tableTree);
    }
  }
}
