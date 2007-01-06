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
package org.jppf.ui.options;

import java.awt.event.*;
import javax.swing.JCheckBox;

/**
 * An option for boolean values, represented as a checkbox.
 * @author Laurent Cohen
 */
public class BooleanOption extends AbstractOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public BooleanOption()
	{
	}

	/**
	 * Initialize this boolean option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the checkbox.
	 * @param value the initial value of this component.
	 */
	public BooleanOption(String name, String label, String tooltip, Boolean value)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		JCheckBox checkBox = new JCheckBox(label, (Boolean) value);
		if (toolTipText != null) checkBox.setToolTipText(toolTipText);
		UIComponent = checkBox;
		setupValueChangeNotifications();
	}

	/**
	 * Get the current value for this option.
	 * @return a <code>Boolean</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		value = ((JCheckBox) UIComponent).isSelected();
		return value;
	}

	/**
	 * Set the value of this option.
	 * @param value the value as an <code>Object</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		if (value instanceof String) value = "true".equalsIgnoreCase((String) value);
		super.setValue(value);
		if (UIComponent != null)
		{
			((JCheckBox) UIComponent).setSelected((Boolean) value);
			fireValueChanged();
		}
	}

	/**
	 * Propagate the state changes of the underlying checkbox to the listeners to this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
		((JCheckBox) UIComponent).addActionListener(new ActionListener()
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
		((JCheckBox) UIComponent).setEnabled(enabled);
	}
}
