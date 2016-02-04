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

  /**
   * Returns the default table cell renderer.
   * @param table the JTable to which this renderer applies.
   * @param value the value of the rendered cell.
   * @param isSelected determines whether the cell is selected.
   * @param hasFocus determines whether the cell has the focus.
   * @param row the row of the rendered cell.
   * @param column the column of the rendered cell.
   * @return the default table cell renderer.
   */
  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
    DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    int actualCol = (Integer) table.getColumnModel().getColumn(column).getIdentifier();
    if ((actualCol < 0) || panel.isColumnHidden(actualCol)) return renderer;
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
    JPPFTreeTable treeTable = (JPPFTreeTable) table;
    TreePath path = treeTable.getPathForRow(row);
    if (path != null) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
      Object o = node.getUserObject();
      if (o instanceof AbstractTopologyComponent) {
        AbstractTopologyComponent data = (AbstractTopologyComponent) o;
        if (data.isNode()) {
          if (((TopologyNode) data).getStatus() == TopologyNodeStatus.DOWN) renderer.setForeground(UNMANAGED_COLOR);
          else {
            if (!data.getManagementInfo().isActive())
              renderer.setBackground(isSelected ? INACTIVE_SELECTION_COLOR : SUSPENDED_COLOR);
            else renderer.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            renderer.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
          }
        } else {
          renderer.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
          renderer.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        }
      }
    }
    renderer.setHorizontalAlignment(alignment);
    renderer.setBorder(border);
    return renderer;
  }
}
