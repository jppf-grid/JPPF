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
import javax.swing.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * Option implementation with a JButton as the underlying component.
 * @author Laurent Cohen
 */
public class ButtonOption extends AbstractOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public ButtonOption()
	{
	}

	/**
	 * Initialize this text option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the checkbox.
	 * @param action the action to execute when the button is pressed.
	 */
	public ButtonOption(String name, String label, String tooltip, OptionAction action)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		JButton button = new JButton();
		if (label != null) button.setText(label);
		if (iconPath != null)
		{
			ImageIcon icon = GuiUtils.loadIcon(iconPath);
			if (icon != null) button.setIcon(icon);
		}
		if (toolTipText != null) button.setToolTipText(toolTipText);
		UIComponent = button;
		setupValueChangeNotifications();
	}

	/**
	 * This method does nothing.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
		((JButton) UIComponent).addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
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
		((JButton) UIComponent).setEnabled(enabled);
	}

	/**
	 * This method always returns false, since buttons have no value to persist.
	 * @return false.
	 * @see org.jppf.ui.options.AbstractOption#isPersistent()
	 */
	public boolean isPersistent()
	{
		return false;
	}
}
