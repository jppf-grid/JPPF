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

import java.awt.event.*;
import java.util.List;
import javax.swing.*;

/**
 * Option that uses a combo box to select its value.
 * @author Laurent Cohen
 */
public class ComboBoxOption extends AbstractOption
{
	/**
	 * The combo box used to select one among several.
	 */
	private JComboBox combo = null;
	/**
	 * Label associated with the combo box.
	 */
	protected JLabel comboLabel = null;
	/**
	 * The items in the drop dwon list.
	 */
	protected List items = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public ComboBoxOption()
	{
	}

	/**
	 * Initialize this combo box option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the combobox.
	 * @param value the initially selected value of this component.
	 * @param items the initial list of items in the combo box.
	 */
	public ComboBoxOption(String name, String label, String tooltip, Object value, List<Object> items)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		this.items = items;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		combo = new JComboBox();
		if (items != null)
		{
			for (Object o: items) combo.addItem(o);
			if (value != null) combo.setSelectedItem(value);
		}
		JLabel comboLabel = new JLabel(label);
		if (toolTipText != null)
		{
			combo.setToolTipText(toolTipText);
			comboLabel.setToolTipText(toolTipText);
		}

		UIComponent = layoutComponents(comboLabel, combo);
		setupValueChangeNotifications();
	}

	/**
	 * Get the current value for this option.
	 * @return a <code>Boolean</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		value = combo.getSelectedItem();
		return value;
	}

	/**
	 * Set the current value for this option.
	 * @param value a <code>Boolean</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		if (value instanceof String)
		{
			for (Object o: items)
			{
				if (((String) value).equals(o.toString()))
				{
					value = o;
					break;
				}
			}
		}
		this.value = value;
		if (combo != null) combo.setSelectedItem(value);
	}

	/**
	 * Propagate the selection changes in the underlying combo box to the listeners to this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
		combo.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				getValue();
				fireValueChanged();
			}
		});
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		combo.setEnabled(enabled);
		comboLabel.setEnabled(enabled);
	}

	/**
	 * Get the list of items in the combo box.
	 * @return a list of <code>Object</code> instances.
	 */
	public List getItems()
	{
		return items;
	}

	/**
	 * Set the list of items in the combo box.
	 * @param items a list of <code>Object</code> instances.
	 */
	public void setItems(List items)
	{
		this.items = items;
		if (combo != null)
		{
			combo.removeAllItems();
			for (Object o: items) combo.addItem(o);
		}
	}
}
