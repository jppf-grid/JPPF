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

package org.jppf.ui.monitoring.diagnostics;

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
public class HealthTreeCellRenderer extends AbstractTreeCellRenderer {
  /**
   * Default constructor.
   */
  public HealthTreeCellRenderer() {
    defaultNonSelectionBackground = getBackgroundNonSelectionColor();
    defaultSelectionBackground = getBackgroundSelectionColor();
  }

  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
    if (value instanceof DefaultMutableTreeNode) {
      this.selected = sel;
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
      if (!node.isRoot()) {
        final AbstractTopologyComponent data = (AbstractTopologyComponent) node.getUserObject();
        setText(TopologyUtils.getDisplayName(data, isShowIP()));
        String path = null;
        final Color background = defaultNonSelectionBackground;
        final Color backgroundSelected = defaultSelectionBackground;
        if (data.isDriver()) {
          path = ((TopologyDriver) data).getConnection().getStatus().isWorkingStatus() ? DRIVER_ICON : DRIVER_INACTIVE_ICON;
        } else if (data.isNode()) {
          path = GuiUtils.computeNodeIconKey((TopologyNode) data);
        }
        final ImageIcon icon = GuiUtils.loadIcon(path);
        setIcon(icon);
        setBackgroundNonSelectionColor(background);
        setBackgroundSelectionColor(backgroundSelected);
        setBackground(sel ? backgroundSelected : background);
        setForeground(sel ? DEFAULT_SELECTION_FOREGROUND : DEFAULT_FOREGROUND);
      }
    }
    return this;
  }
}
