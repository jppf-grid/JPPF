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

package org.jppf.ui.monitoring.node;

import static org.jppf.ui.treetable.AbstractTreeCellRenderer.*;

import java.awt.Component;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.treetable.JPPFTreeTable;

/**
 * Table cell renderer used to render the alignment of cell values in the table.
 * @author Laurent Cohen
 */
public class NodeTableCellRenderer extends DefaultTableCellRenderer {
  /**
   * The insets for this renderer.
   */
  private Border border = BorderFactory.createEmptyBorder(0, 2, 0, 2);
  /**
   * The panel which holds the tree table.
   */
  private final NodeDataPanel panel;

  /**
   * Initialize this renderer.
   * @param panel he panel which holds the tree table.
   */
  public NodeTableCellRenderer(final NodeDataPanel panel) {
    this.panel = panel;
  }

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
    final int actualCol = (Integer) table.getColumnModel().getColumn(column).getIdentifier();
    if ((actualCol < 0) || panel.isColumnHidden(actualCol)) return this;
    int alignment = SwingConstants.LEFT;
    switch(actualCol) {
      case NodeTreeTableModel.NB_TASKS:
      case NodeTreeTableModel.NB_SLAVES:
        alignment = SwingConstants.RIGHT;
        break;

      case NodeTreeTableModel.NODE_THREADS:
        alignment = SwingConstants.CENTER;
        break;
    }
    final JPPFTreeTable treeTable = (JPPFTreeTable) table;
    final TreePath path = treeTable.getPathForRow(row);
    if (path != null) {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
      final Object o = node.getUserObject();
      if (o instanceof AbstractTopologyComponent) {
        final AbstractTopologyComponent data = (AbstractTopologyComponent) o;
        if (data.isNode()) {
          if (((TopologyNode) data).getStatus() == TopologyNodeStatus.DOWN) setForeground(UNMANAGED_COLOR);
          else {
            if (!data.getManagementInfo().isActive())
              setBackground(isSelected ? INACTIVE_SELECTION_COLOR : SUSPENDED_COLOR);
            else setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
          }
        } else {
          setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
          setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        }
      }
    }
    setHorizontalAlignment(alignment);
    setBorder(border);
    setText(value == null ? "" : value.toString());
    return this;
  }
}
