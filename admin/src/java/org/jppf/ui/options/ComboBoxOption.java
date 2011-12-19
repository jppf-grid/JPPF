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
		comboLabel = new JLabel(label);
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
			Object val = "";
			int maxLen = 0;
			for (Object o: items)
			{
				String s = o.toString();
				if (s.length() > maxLen)
				{
					maxLen = s.length();
					val = s + "  ";
				}
			}
			combo.setPrototypeDisplayValue(val);
			for (Object o: items) combo.addItem(o);
		}
	}

	/**
	 * Get the combo box used to select one among several items.
	 * @return a <code>JComboBox</code> instance.
	 */
	public JComboBox getComboBox()
	{
		return combo;
	}
}
