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

import javax.swing.JTextField;

/**
 * Option for a plain string value.
 * @author Laurent Cohen
 */
public class PlainTextOption extends TextOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public PlainTextOption()
	{
	}

	/**
	 * Initialize this text option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the checkbox.
	 * @param value the initial value of this component.
	 */
	public PlainTextOption(String name, String label, String tooltip, String value)
	{
		super(name, label, tooltip, value);
		createUI();
	}

	/**
	 * Create the text field that holds the value of this option.
	 * @return a JTextField instance.
	 * @see org.jppf.ui.options.TextOption#createField()
	 */
	protected JTextField createField()
	{
		return new JTextField((String) value);
	}

	/**
	 * Set the value of this option.
	 * @param value the value as an <code>Object</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		this.value = value;
		if ((value != null) && (field != null)) field.setText(value.toString());
	}
}
