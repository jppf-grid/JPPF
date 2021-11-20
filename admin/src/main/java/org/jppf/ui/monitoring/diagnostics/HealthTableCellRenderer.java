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

import static org.jppf.ui.treetable.AbstractTreeCellRenderer.*;

import java.awt.Component;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.management.diagnostics.provider.MonitoringConstants;
import org.jppf.ui.monitoring.diagnostics.Thresholds.Name;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.jppf.ui.utils.GuiUtils;
import org.slf4j.*;

/**
 * Table cell renderer used to render the alignment of cell values in the table.
 * @author Laurent Cohen
 */
public class HealthTableCellRenderer extends DefaultTableCellRenderer {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HealthTableCellRenderer.class);
  /**
   * The insets for this renderer.
   */
  private final Border border = BorderFactory.createEmptyBorder(0, 2, 0, 2);
  /**
   * The JVM Health Panel option.
   */
  private final JVMHealthPanel healthPanel;

  /**
   * Initialize this renderer.
   * @param healthPanel the JVM Health Panel option.
   */
  public HealthTableCellRenderer(final JVMHealthPanel healthPanel) {
    this.healthPanel = healthPanel;
  }

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean selected, final boolean hasFocus, final int row, final int column) {
    try {
      final int actualCol = (Integer) table.getColumnModel().getColumn(column).getIdentifier();
      if ((actualCol < 0) || healthPanel.isColumnHidden(actualCol)) return this;
      final int alignment = (actualCol == 0) ? SwingConstants.LEFT : SwingConstants.RIGHT;
      final JPPFTreeTable treeTable = (JPPFTreeTable) table;
      final TreePath path = treeTable.getPathForRow(row);
      String iconPath = null;
      if (path != null) {
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        setForeground(selected ? table.getSelectionForeground() : table.getForeground());
        final Object o = node.getUserObject();
        if (o instanceof AbstractTopologyComponent) {
          final AbstractTopologyComponent data = (AbstractTopologyComponent) o;
          final HealthSnapshot health = data.getHealthSnapshot();
          final JVMHealthTreeTableModel model = (JVMHealthTreeTableModel) healthPanel.getModel();
          final String name = model.getBaseColumnName(actualCol);
          switch(name) {
            case MonitoringConstants.LIVE_THREADS_COUNT:
            case MonitoringConstants.DEADLOCKED:
              if (health.getBoolean(MonitoringConstants.DEADLOCKED)) {
                setBackground(selected ? INACTIVE_SELECTION_COLOR : INACTIVE_COLOR);
                iconPath = CRITICAL_ICON;
                setIconTextGap(5);
              } else {
                setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
              }
              break;
            case MonitoringConstants.HEAP_USAGE_MB:
            case MonitoringConstants.HEAP_USAGE_RATIO:
              computeColor(this, table, health.getDouble(MonitoringConstants.HEAP_USAGE_RATIO), selected, Name.MEMORY_WARNING, Name.MEMORY_CRITICAL);
              break;
            case MonitoringConstants.NON_HEAP_USAGE_MB:
            case MonitoringConstants.NON_HEAP_USAGE_RATIO:
              computeColor(this, table, health.getDouble(MonitoringConstants.NON_HEAP_USAGE_RATIO), selected, Name.MEMORY_WARNING, Name.MEMORY_CRITICAL);
              break;
            case MonitoringConstants.RAM_USAGE_MB:
            case MonitoringConstants.RAM_USAGE_RATIO:
              computeColor(this, table, health.getDouble(MonitoringConstants.RAM_USAGE_RATIO), selected, Name.MEMORY_WARNING, Name.MEMORY_CRITICAL);
              break;
            case MonitoringConstants.PROCESS_CPU_LOAD:
              computeColor(this, table, health.getDouble(MonitoringConstants.PROCESS_CPU_LOAD), selected, Name.CPU_WARNING, Name.CPU_CRITICAL);
              break;
            case MonitoringConstants.SYSTEM_CPU_LOAD:
              computeColor(this, table, health.getDouble(MonitoringConstants.SYSTEM_CPU_LOAD), selected, Name.CPU_WARNING, Name.CPU_CRITICAL);
              break;
            default:
              this.setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
              break;
          }
        }
      }
      setIcon((iconPath != null) ? GuiUtils.loadIcon(iconPath) : null);
      setHorizontalAlignment(alignment);
      setBorder(border);
      setText(value == null ? "" : value.toString());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return this;
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
  private void computeColor(final DefaultTableCellRenderer renderer, final JTable table, final double value, final boolean selected, final Name warning, final Name critical) {
    final Thresholds thr = healthPanel.getThresholds();
    if (value < 100d * thr.getValue(warning)) renderer.setBackground(selected ? table.getSelectionBackground() : ACTIVE_COLOR);
    else if (value < 100d * thr.getValue(critical)) renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : SUSPENDED_COLOR);
    else renderer.setBackground(selected ? INACTIVE_SELECTION_COLOR : INACTIVE_COLOR);
  }
}
