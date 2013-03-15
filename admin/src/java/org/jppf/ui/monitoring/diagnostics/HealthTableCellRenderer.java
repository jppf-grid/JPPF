/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import static org.jppf.ui.treetable.AbstractTreeCellRenderer.*;

import java.awt.Component;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;

import org.jppf.management.diagnostics.*;
import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.jppf.ui.utils.GuiUtils;

/**
 * Table cell renderer used to render the alignment of cell values in the table.
 * @author Laurent Cohen
 */
public class HealthTableCellRenderer extends DefaultTableCellRenderer
{
  /**
   * The insets for this renderer.
   */
  private Border border = BorderFactory.createEmptyBorder(0, 2, 0, 2);

  /**
   * Returns the default table cell renderer.
   * @param table the JTable to which this renderer applies.
   * @param value the value of the rendered cell.
   * @param selected determines whether the cell is selected.
   * @param hasFocus determines whether the cell has the focus.
   * @param row the row of the rendered cell.
   * @param column the column of the rendered cell.
   * @return the default table cell renderer.
   * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
   */
  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean selected, final boolean hasFocus, final int row, final int column) {
    DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
    if (column >= 0) {
      int alignment = SwingConstants.LEFT;
      switch(column) {
        case JVMHealthTreeTableModel.HEAP_MEM_MB:
        case JVMHealthTreeTableModel.NON_HEAP_MEM_MB:
        case JVMHealthTreeTableModel.LIVE_THREADS:
          alignment = SwingConstants.RIGHT;
          break;

        case JVMHealthTreeTableModel.CPU_LOAD:
        case JVMHealthTreeTableModel.DEADLOCK_STATUS:
        case JVMHealthTreeTableModel.HEAP_MEM_PCT:
        case JVMHealthTreeTableModel.NON_HEAP_MEM_PCT:
          alignment = SwingConstants.CENTER;
          break;
      }
      JPPFTreeTable treeTable = (JPPFTreeTable) table;
      TreePath path = treeTable.getPathForRow(row);
      String iconPath = null;
      if (path != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        renderer.setForeground(selected ? table.getSelectionForeground() : table.getForeground());
        Object o = node.getUserObject();
        if (o instanceof TopologyData) {
          TopologyData data = (TopologyData) o;
          HealthSnapshot health = data.getHealthSnapshot();
          switch(column) {
            case JVMHealthTreeTableModel.DEADLOCK_STATUS:
              if (health.isDeadlocked()) {
                renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : INACTIVE_COLOR);
                iconPath = CRITICAL_ICON;
              } else {
                renderer.setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
              }
              break;
            case JVMHealthTreeTableModel.HEAP_MEM_MB:
            case JVMHealthTreeTableModel.HEAP_MEM_PCT:
              computeColor(renderer, table, health.getHeapLevel(), selected);
              break;
            case JVMHealthTreeTableModel.NON_HEAP_MEM_MB:
            case JVMHealthTreeTableModel.NON_HEAP_MEM_PCT:
              computeColor(renderer, table, health.getNonheapLevel(), selected);
              break;
            default:
              renderer.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
              break;
          }
        }
      }
      ImageIcon icon = iconPath != null ? GuiUtils.loadIcon(iconPath) : null;
      renderer.setIcon(icon);
      renderer.setHorizontalAlignment(alignment);
      renderer.setBorder(border);
    }
    return renderer;
  }

  /**
   * Compute a background and foreground color based on an alert level and selection state.
   * @param renderer the component onto which to set the colors.
   * @param table the JTable to render.
   * @param level the alert level.
   * @param selected the selection state.
   */
  private void computeColor(final DefaultTableCellRenderer renderer, final JTable table, final AlertLevel level, final boolean selected)
  {
    switch(level)
    {
      case UNKNOWN:
        renderer.setBackground(selected ? table.getSelectionBackground() : table.getBackground());
        break;
      case NORMAL:
        renderer.setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
        break;
      case WARNING:
        renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : SUSPENDED_COLOR);
        break;
      case CRITICAL:
        renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : INACTIVE_COLOR);
        break;
    }
  }
}
