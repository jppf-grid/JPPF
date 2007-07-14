/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
