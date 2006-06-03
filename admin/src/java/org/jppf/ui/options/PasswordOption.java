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

import java.awt.Dimension;
import javax.swing.*;

/**
 * Option for a password value.
 * @author Laurent Cohen
 */
public class PasswordOption extends TextOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public PasswordOption()
	{
	}

	/**
	 * Initialize this text option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the checkbox.
	 * @param value the initial value of this component.
	 */
	public PasswordOption(String name, String label, String tooltip, String value)
	{
		super(name, label, tooltip, value);
		createUI();
		field.setPreferredSize(new Dimension(100, 20));
	}

	/**
	 * Create the text field that holds the value of this password option.
	 * @return a <code>JPasswordField</code> instance.
	 * @see org.jppf.ui.options.TextOption#createField()
	 */
	protected JTextField createField()
	{
		field = new JPasswordField((String) value);
		return field;
	}

	/**
	 * Get the password in the password field.
	 * @return the password as a string value.
	 * @see org.jppf.ui.options.TextOption#getValue()
	 */
	public Object getValue()
	{
		value = new String(((JPasswordField) field).getPassword());
		return value;
	}
}
