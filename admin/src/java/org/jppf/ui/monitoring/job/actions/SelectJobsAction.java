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
package org.jppf.ui.monitoring.job.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.tree.*;

import org.jppf.ui.treetable.*;

/**
 * This action stops a job.
 */
public class SelectJobsAction extends AbstractJobAction {
  /**
   * The panel wwhere this button is.
   */
  private final AbstractTreeTableOption panel;

  /**
   * Initialize this action.
   * @param panel the panel wwhere this button is.
   */
  public SelectJobsAction(final AbstractTreeTableOption panel) {
    this.panel = panel;
    setupIcon("/org/jppf/ui/resources/select_jobs.gif");
    putValue(NAME, localize("job.select.jobs.label"));
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    setEnabled(true);
  }

  /**
   * Perform the action.
   * @param event not used.
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    synchronized(panel) {
      JPPFTreeTable treeTable = panel.getTreeTable();
      List<TreePath> selectionPath = new ArrayList<>();
      DefaultMutableTreeNode root = panel.getTreeTableRoot();
      for (int i=0; i<root.getChildCount(); i++) {
        DefaultMutableTreeNode driver = (DefaultMutableTreeNode) root.getChildAt(i);
        for (int j=0; j<driver.getChildCount(); j++) {
          DefaultMutableTreeNode job = (DefaultMutableTreeNode) driver.getChildAt(j);
          selectionPath.add(treeTable.getPathForNode(job));
        }
      }
      TreeTableModelAdapter m = (TreeTableModelAdapter) treeTable.getModel();
      m.setSelectedPaths(selectionPath.toArray(new TreePath[selectionPath.size()]));
    }
  }
}
