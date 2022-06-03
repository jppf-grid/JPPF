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

package org.jppf.admin.web.jobs;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.tabletree.*;
import org.jppf.client.monitoring.jobs.AbstractJobComponent;
import org.jppf.ui.treetable.TreeViewType;

/**
 * Select all jobs within the jobs tree view.
 * @author Laurent Cohen
 */
public class SelectJobsLink extends AbstractViewTypeLink {
  /**
   * @param id the id of this link.
   */
  public SelectJobsLink(final String id) {
    super(id, Model.of("Select jobs"), TreeViewType.JOBS, true);
    imageName = "select_jobs.gif";
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
        final AbstractJobComponent job = (AbstractJobComponent) dmtnNode.getUserObject();
        handler.select(job.getUuid());
      }
    }
  }
}
