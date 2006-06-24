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

import javax.swing.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * An option that uses a <code>JTextArea</code> to edit its value.
 * @author Laurent Cohen
 */
public class TextAreaOption extends AbstractOption
{
	/**
	 * The underlying UI component used to edit the value of this option.
	 */
	private JTextArea textArea = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public TextAreaOption()
	{
	}

	/**
	 * Initialize this text area option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the text area. 
	 * @param tooltip the tooltip associated with the text area.
	 * @param value the initial value of this component.
	 */
	public TextAreaOption(String name, String label, String tooltip, String value)
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
		textArea = new JTextArea((String) value);
		textArea.setBorder(BorderFactory.createEmptyBorder());
		if (toolTipText != null) textArea.setToolTipText(toolTipText);
		JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		mainPanel.setBorder(BorderFactory.createTitledBorder(label));
		textArea.setEditable(false);
		textArea.setOpaque(false);
		mainPanel.add(textArea);
		UIComponent = mainPanel;
		setupValueChangeNotifications();
	}

	/**
	 * Get the current value for this option.
	 * @return a <code>String</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		value = textArea.getText();
		return value;
	}

	/**
	 * Set the current value for this option.
	 * @param value a <code>String</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		this.value = value;
		textArea.setText((String) value);
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
		((JTextArea) UIComponent).setEnabled(enabled);
	}
}
