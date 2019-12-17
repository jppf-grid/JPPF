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

import java.awt.Component;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Table cell renderer used to render the alignment of cell values in the table.
 * @author Laurent Cohen
 */
public class JobTableCellRenderer extends DefaultTableCellRenderer {
  /**
   * The panel which holds the tree table.
   */
  private final JobDataPanel panel;

  /**
   * Initialize this renderer.
   * @param panel he panel which holds the tree table.
   */
  public JobTableCellRenderer(final JobDataPanel panel) {
    this.panel = panel;
  }

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
    final int actualCol = (Integer) table.getColumnModel().getColumn(column).getIdentifier();
    if ((actualCol < 0) || panel.isColumnHidden(actualCol)) return this;
      int alignment = SwingConstants.LEFT;
    switch(actualCol) {
      case JobTreeTableModel.INITIAL_TASK_COUNT:
      case JobTreeTableModel.TASK_COUNT:
      case JobTreeTableModel.PRIORITY:
        alignment = SwingConstants.RIGHT;
        break;
      case JobTreeTableModel.MAX_NODES:
        alignment = "\u221E".equals(value) ? SwingConstants.CENTER : SwingConstants.RIGHT;
        break;
    }
    setHorizontalAlignment(alignment);
    setText(value == null ? "" : value.toString());
    setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
    setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
    return this;
  }
}
