/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.awt.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.monitoring.diagnostics.Thresholds.Name;
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
   * The JVM Health Panel option.
   */
  private final JVMHealthPanel healthPanel;

  /**
   * Initialize this renderer.
   * @param healthPanel the JVM Health Panel option.
   */
  public HealthTableCellRenderer(final JVMHealthPanel healthPanel)
  {
    this.healthPanel = healthPanel;
  }

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
        case JVMHealthTreeTableModel.THREADS:
          alignment = SwingConstants.RIGHT;
          break;

        case JVMHealthTreeTableModel.CPU_LOAD:
        //case JVMHealthTreeTableModel.DEADLOCK_STATUS:
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
        if (o instanceof AbstractTopologyComponent) {
          AbstractTopologyComponent data = (AbstractTopologyComponent) o;
          HealthSnapshot health = data.getHealthSnapshot();
          switch(column) {
            case JVMHealthTreeTableModel.THREADS:
              if (health.isDeadlocked()) {
                renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : INACTIVE_COLOR);
                iconPath = CRITICAL_ICON;
                Rectangle r = table.getCellRect(row, column, false);
                int n = r == null ? 4 : r.width - 36;
                renderer.setIconTextGap(n < 4 ? 4 : n);
              } else {
                renderer.setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
              }
              break;
            case JVMHealthTreeTableModel.HEAP_MEM_MB:
            case JVMHealthTreeTableModel.HEAP_MEM_PCT:
              computeColor(renderer, table, health.getHeapUsedRatio(), selected, Name.MEMORY_WARNING, Name.MEMORY_CRITICAL);
              break;
            case JVMHealthTreeTableModel.NON_HEAP_MEM_MB:
            case JVMHealthTreeTableModel.NON_HEAP_MEM_PCT:
              computeColor(renderer, table, health.getNonheapUsedRatio(), selected, Name.MEMORY_WARNING, Name.MEMORY_CRITICAL);
              break;
            case JVMHealthTreeTableModel.CPU_LOAD:
              computeColor(renderer, table, health.getCpuLoad(), selected, Name.CPU_WARNING, Name.CPU_CRITICAL);
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
   * @param value the value to compare to thresholds.
   * @param selected the selection state.
   * @param warning name of the threshold for warning level of the value.
   * @param critical name of the threshold for critical level of the value.
   */
  private void computeColor(final DefaultTableCellRenderer renderer, final JTable table, final double value, final boolean selected, final Name warning, final Name critical)
  {
    Thresholds thr = healthPanel.getThresholds();
    if (value < thr.getValue(warning)) renderer.setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
    else if (value < thr.getValue(critical)) renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : SUSPENDED_COLOR);
    else renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : INACTIVE_COLOR);
  }
}
