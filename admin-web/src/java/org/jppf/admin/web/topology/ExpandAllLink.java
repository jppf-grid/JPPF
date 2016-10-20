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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.*;

/**
 *
 * @author Laurent Cohen
 */
public class ExpandAllLink extends AbstractActionLink {
  /**
   *
   */
  public ExpandAllLink() {
    super(TopologyConstants.EXPAND_ALL_ACTION, Model.of("Expand all"));
    imageName = "expand.gif";
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    JPPFWebSession session = getSession(target);
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) session.getTopologyData().getModel().getRoot();
    if (target.getPage() instanceof TableTreeHolder) {
      JPPFTableTree tableTree = ((TableTreeHolder) target.getPage()).getTableTree();
      TableTreeHelper.expand(tableTree, root);
      target.add(tableTree);
    }
  }
}
