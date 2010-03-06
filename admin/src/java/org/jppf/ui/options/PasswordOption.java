/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
