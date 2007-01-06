/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.ui.utils.colorscheme;

import java.awt.Component;

import javax.swing.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFListCellRenderer extends JLabel implements ListCellRenderer
{

	/**
	 * .
	 * 
	 * @param list .
	 * @param value .
	 * @param index .
	 * @param isSelected .
	 * @param cellHasFocus .
	 * @return .
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList,
	 *      java.lang.Object, int, boolean, boolean)
	 */
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
	{
		ColorItem item = (ColorItem) value;
		setText(item.name);
		setBackground(item.color);
		//setOpaque(true);
		return this;
	}
}
