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

package org.jppf.ui.monitoring.job;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.ui.utils.*;

/**
 * Renderer used to render the tree nodes in the job data panel.
 * @author Laurent Cohen
 */
public class JobRenderer extends AbstractTreeCellRenderer {
  /**
   * Configures the renderer based on the passed in components.
   * @param tree the tree of which to apply this renderer.
   * @param value the node to render.
   * @param sel determines whether the node is selected.
   * @param expanded determines whether the node is expanded.
   * @param leaf determines whether the node is a leaf.
   * @param row the node's row number.
   * @param hasFocus determines whether the node has the focus.
   * @return a component used to paint the node.
   */
  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
    if (value instanceof DefaultMutableTreeNode) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
      if (!node.isRoot()) {
        this.selected = sel;
        final AbstractJobComponent data = (AbstractJobComponent) node.getUserObject();
        String path = null;
        Color background = defaultNonSelectionBackground;
        Color backgroundSelected = defaultSelectionBackground;
        if (data instanceof JobDriver) {
          final JobDriver driver = (JobDriver) data;
          setText(TopologyUtils.getDisplayName(driver.getTopologyDriver(), isShowIP()));
          if (((JobDriver) data).getTopologyDriver().getConnection().getStatus().isWorkingStatus()) {
            path = DRIVER_ICON;
            background = ACTIVE_COLOR;
          } else {
            path = DRIVER_INACTIVE_ICON;
            background = INACTIVE_COLOR;
            backgroundSelected = INACTIVE_SELECTION_COLOR;
          }
        } else if (data instanceof Job) {
          final Job job = (Job) data;
          path = JOB_ICON;
          if (job.getJobInformation().isSuspended()) {
            background = SUSPENDED_COLOR;
            backgroundSelected = INACTIVE_SELECTION_COLOR;
          }
          setText(job.getDisplayName());
        } else if (data instanceof JobDispatch) {
          final JobDispatch dispatch = (JobDispatch) data;
          final TopologyNode nodeData = dispatch.getNode();
          if (nodeData != null) {
            final JPPFManagementInfo nodeInfo = nodeData.getManagementInfo();
            path = nodeData.isPeer() ? DRIVER_ICON : GuiUtils.computeNodeIconKey(nodeData);
            setText((StatsHandler.getInstance().getShowIPHandler().isShowIP() ? nodeInfo.getIpAddress() : nodeInfo.getHost()) + ":" + nodeInfo.getPort());
          }
        }
        final ImageIcon icon = GuiUtils.loadIcon(path);
        setIcon(icon);
        setBackgroundNonSelectionColor(background);
        setBackgroundSelectionColor(backgroundSelected);
        setBorderSelectionColor(backgroundSelected);
        setBackground(sel ? backgroundSelected : background);
        setForeground(sel ? DEFAULT_SELECTION_FOREGROUND : DEFAULT_FOREGROUND);
      }
    }
    return this;
  }
}
