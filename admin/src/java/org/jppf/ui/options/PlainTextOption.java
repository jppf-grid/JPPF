/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

import javax.swing.JTextField;

/**
 * Option for a plain string value.
 * @author Laurent Cohen
 */
public class PlainTextOption extends TextOption
{
	/**
	 * Number of columns (characters) displayed in the text field.
	 */
	private int columns = 16;

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

	/**
	 * Get the number of columns displayed in the text field.
	 * @return the number of columns as an int.
	 */
	public int getColumns()
	{
		return columns;
	}

	/**
	 * Set the number of columns displayed in the text field.
	 * @param columns the number of columns as an int.
	 */
	public void setColumns(int columns)
	{
		this.columns = columns;
		if (field != null)
		{
			field.setColumns(columns);
		}
	}
}
