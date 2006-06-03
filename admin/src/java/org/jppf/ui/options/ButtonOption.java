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

import javax.swing.JButton;

/**
 * Option implementation with a JButton as the underlying component.
 * @author Laurent Cohen
 */
public class ButtonOption extends AbstractOption
{
	/**
	 * Action associated with the button.
	 */
	private OptionAction action = null;

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
		this.toolTipText = tooltip;
		this.action = null;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		JButton button = new JButton(label);
		if (toolTipText != null) button.setToolTipText(toolTipText);
		if (action != null)
		{
			button.addActionListener(action);
			action.setOption(this);
		}
		UIComponent = button;
		setupValueChangeNotifications();
	}

	/**
	 * This method does nothing.
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
		((JButton) UIComponent).setEnabled(enabled);
	}

	/**
	 * Get the action associated with the button.
	 * @return an <code>OptionAction</code> instance.
	 */
	public OptionAction getAction()
	{
		return action;
	}

	/**
	 * Set the action associated with the button.
	 * @param action an <code>OptionAction</code> instance.
	 */
	public void setAction(OptionAction action)
	{
		this.action = action;
	}
}
