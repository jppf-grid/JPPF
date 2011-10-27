/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
public class JobTableCellRenderer extends DefaultTableCellRenderer
{
	/**
	 * Returns the default table cell renderer.
	 * @param table the JTable to which this renderer applies.
	 * @param value the value of the rendered cell.
	 * @param isSelected determines whether the cell is selected.
	 * @param hasFocus  determines whether the cell has the focus.
	 * @param row the row of the rendered cell.
	 * @param column the column of the rendered cell.
	 * @return the default table cell renderer.
	 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	@Override
	public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus,
			final int row, final int column)
	{
		DefaultTableCellRenderer renderer =  (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if (column != 0)
		{
			int alignment = SwingConstants.LEFT;
			switch(column)
			{
				case JobTreeTableModel.INITIAL_TASK_COUNT:
				case JobTreeTableModel.TASK_COUNT:
				case JobTreeTableModel.PRIORITY:
					alignment = SwingConstants.RIGHT;
					break;
				case JobTreeTableModel.MAX_NODES:
					alignment = "\u221E".equals(value) ? SwingConstants.CENTER : SwingConstants.RIGHT;
					break;
			}
			renderer.setHorizontalAlignment(alignment);
		}
		return renderer;
	}
}
