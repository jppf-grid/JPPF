/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.ui.options;

import java.util.List;
import javax.swing.*;

/**
 * Option holding a JList as its UI component.
 * The list selection mode can be either simple selection or multiple interval selection.
 * @author Laurent Cohen
 */
public class ListOption extends AbstractOption
{
	/**
	 * The underlying JList component.
	 */
	protected JList list = null;
	/**
	 * The label accompanying this list.
	 */
	protected JLabel listLabel = null;
	/**
	 * The items contained in the list.
	 */
	protected List<Object> items = null;

	/**
	 * Initialize this combo box option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the combobox.
	 * @param value the initially selected value of this component.
	 * @param items the initial list of items in the combo box.
	 * @param selectionMode the mode of selection for the list, can be either
	 * {@link javax.swing.ListSelectionModel#SINGLE_SELECTION SINGLE_SELECTION} or
	 * {@link javax.swing.ListSelectionModel#MULTIPLE_INTERVAL_SELECTION MULTIPLE_INTERVAL_SELECTION}. 
	 */
	public ListOption(String name, String label, String tooltip, List<Object> value, List<Object> items,
			int selectionMode)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		this.items = items;
		createUI();
	}

	/**
	 * Define a selection listener that will forward selection changes to
	 * all value change lsiteners that registered with this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Create the UI components for this option.
	 * @see org.jppf.ui.options.AbstractOptionElement#createUI()
	 */
	public void createUI()
	{
		listLabel = new JLabel(label);
		
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		list.setEnabled(true);
	}
}
