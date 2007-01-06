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

import javax.swing.*;
import javax.swing.text.*;

/**
 * Text option displayed as a text field.
 * @author Laurent Cohen
 */
public abstract class TextOption extends AbstractOption
{
	/**
	 * Text field containing the option value as text.
	 */
	protected JTextField field = null;
	/**
	 * Label associated with the text field.
	 */
	protected JLabel fieldLabel = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public TextOption()
	{
	}

	/**
	 * Initialize this text option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the checkbox.
	 * @param value the initial value of this component.
	 */
	public TextOption(String name, String label, String tooltip, String value)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
	}
	
	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		fieldLabel = new JLabel(label);
		field = createField();
		field.setColumns(10);
		if (toolTipText != null)
		{
			field.setToolTipText(toolTipText);
			fieldLabel.setToolTipText(toolTipText);
		}
		UIComponent = layoutComponents(fieldLabel, field);;
		setupValueChangeNotifications();
	}

	/**
	 * Create the text field that holds the value of this option.
	 * @return a JTextField instance.
	 */
	protected abstract JTextField createField();

	/**
	 * Get the text in the text field.
	 * @return a string value.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		Document doc = field.getDocument();
		try
		{
			value = doc.getText(0, doc.getLength());
		}
		catch(BadLocationException e)
		{
		}
		return value;
	}

	/**
	 * Add a listener to the underlying text document, to receive and propagate change events.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		field.setEnabled(enabled);
		fieldLabel.setEnabled(enabled);
	}
}
