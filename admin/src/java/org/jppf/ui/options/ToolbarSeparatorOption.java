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

import java.awt.Dimension;

import javax.swing.*;

/**
 * An option for boolean values, represented as a checkbox.
 * @author Laurent Cohen
 */
public class ToolbarSeparatorOption extends AbstractOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public ToolbarSeparatorOption()
	{
		//UIComponent = new JToolBar.Separator();
		UIComponent = new JLabel("|");
	}

	/**
	 * Initialize this boolean option with the specified parameters.
	 * @param width the filler's width
	 * @param height the filler's height.
	 */
	public ToolbarSeparatorOption(int width, int height)
	{
		//UIComponent = new JToolBar.Separator();
		UIComponent = new JLabel("|");
		((JLabel) UIComponent).setHorizontalAlignment(SwingConstants.CENTER);
		((JLabel) UIComponent).setVerticalAlignment(SwingConstants.CENTER);
		UIComponent.setForeground(UIComponent.getForeground().brighter());
		if ((width > 0) && (height > 0))
		{
			UIComponent.setPreferredSize(new Dimension(width, height));
		}
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
	}

	/**
	 * Get the current value for this option.
	 * @return null.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		return null;
	}

	/**
	 * Propagate the state changes of the underlying checkbox to the listeners to this option.
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
