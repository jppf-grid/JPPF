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
import org.apache.wicket.model.*;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.AbstractComponent;

/**
 *
 * @author Laurent Cohen
 */
public class SelectDriversLink extends AbstractActionLink {
  /**
   *
   */
  public SelectDriversLink() {
    super("topology.select_drivers", Model.of("Select drivers"));
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    JPPFWebSession session = getSession(target);
    DefaultMutableTreeNode root = (DefaultMutableTreeNode) session.getTopologyModel().getRoot();
    JPPFTableTree tableTree = session.getTopologyTableTree();
    SelectionHandler handler = session.getTopologySelectionHandler();
    handler.clear();
    for (int i=0; i<root.getChildCount(); i++) {
      DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) root.getChildAt(i);
      handler.select(((AbstractComponent<?>) dmtn.getUserObject()).getUuid());
    }
    target.add(tableTree);
  }
}
