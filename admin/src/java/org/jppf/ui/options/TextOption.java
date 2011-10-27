/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
	public TextOption(final String name, final String label, final String tooltip, final String value)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
	}

	/**
	 * Create the UI components for this option.
	 */
	@Override
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
		//UIComponent = layoutComponents(fieldLabel, field);
		//UIComponent = layoutComponents(fieldLabel, "align left, growx 0, pushx", field, "gap rel, grow");
		UIComponent = layoutComponents(fieldLabel, "align left, grow 0", field, "gap rel, grow, push");
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
	@Override
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
	@Override
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(final boolean enabled)
	{
		field.setEnabled(enabled);
		fieldLabel.setEnabled(enabled);
	}
}
