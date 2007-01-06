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
import org.jppf.ui.utils.GuiUtils;

/**
 * An option for simple labels that have text, an icon, or both.
 * @author Laurent Cohen
 */
public class LabelOption extends AbstractOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public LabelOption()
	{
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		JLabel lab = new JLabel();
		lab.setHorizontalAlignment(SwingConstants.RIGHT);
		if (value != null) lab.setText((String) value);
		if (iconPath != null)
		{
			ImageIcon icon = GuiUtils.loadIcon(iconPath);
			if (icon != null) lab.setIcon(icon);
		}
		if (toolTipText != null) lab.setToolTipText(toolTipText);
		UIComponent = lab;
	}

	/**
	 * Set the value of this option.
	 * @param value the value as an <code>Object</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		this.value = value;
		if (UIComponent != null) ((JLabel) UIComponent).setText((String) value);
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
		UIComponent.setEnabled(enabled);
	}

	/**
	 * This method always returns false, since labels have no value to persist.
	 * @return false.
	 * @see org.jppf.ui.options.AbstractOption#isPersistent()
	 */
	public boolean isPersistent()
	{
		return false;
	}
}
