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

package org.jppf.ui.monitoring.node;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.ui.utils.*;

/**
 * Renderer used to render the tree nodes (1st column) in the node data panel.
 * @author Laurent Cohen
 */
public class NodeRenderer extends AbstractTreeCellRenderer {
  /**
   * Default constructor.
   */
  public NodeRenderer() {
    defaultNonSelectionBackground = getBackgroundNonSelectionColor();
    defaultSelectionBackground = getBackgroundSelectionColor();
  }

  /**
   * Configures the renderer based on the passed in components.
   * @param tree the tree on which to apply this renderer.
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
    final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    if (value instanceof DefaultMutableTreeNode) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
      if (!node.isRoot()) {
        final AbstractTopologyComponent data = (AbstractTopologyComponent) node.getUserObject();
        String path = null;
        Color background = defaultNonSelectionBackground;
        Color backgroundSelected = defaultSelectionBackground;
        Color foreground = sel ? DEFAULT_SELECTION_FOREGROUND : DEFAULT_FOREGROUND;
        final Font f = renderer.getFont();
        Font font = getPlainFont(f);
        renderer.setText(TopologyUtils.getDisplayName(data, isShowIP()));
        if (data.isDriver()) {
          final TopologyDriver driver = (TopologyDriver) data;
          if (driver.getConnection().getStatus().isWorkingStatus()) {
            path = DRIVER_ICON;
            background = ACTIVE_COLOR;
            font = getBoldFont(f);
          } else {
            path = DRIVER_INACTIVE_ICON;
            background = INACTIVE_COLOR;
            backgroundSelected = INACTIVE_SELECTION_COLOR;
            font = getBoldItalicFont(f);
          }
        } else if (data.isPeer()) {
          path = DRIVER_ICON;
          font = getBoldItalicFont(f);
          foreground = DIMMED_FOREGROUND;
        } else if (data.isNode()) {
          final TopologyNode nodeData = (TopologyNode) data;
          path = GuiUtils.computeNodeIconKey(nodeData);
          if (!TopologyNodeStatus.UP.equals(nodeData.getStatus())) {
            background = INACTIVE_COLOR;
            backgroundSelected = INACTIVE_SELECTION_COLOR;
            font = getItalicFont(f);
          } else if (!nodeData.getManagementInfo().isActive()) {
            background = SUSPENDED_COLOR;
            backgroundSelected = INACTIVE_SELECTION_COLOR;
          }
        }
        if (font != null) setFont(font);
        final ImageIcon icon = GuiUtils.loadIcon(path);
        renderer.setIcon(icon);
        renderer.setBackgroundNonSelectionColor(background);
        renderer.setBackgroundSelectionColor(backgroundSelected);
        renderer.setBorderSelectionColor(backgroundSelected);
        renderer.setBackground(sel ? backgroundSelected : background);
        renderer.setForeground(foreground);
      }
    }
    return renderer;
  }
}
