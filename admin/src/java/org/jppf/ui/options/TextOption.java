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

import static org.jppf.ui.utils.GuiUtils.addLayoutComp;
import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import org.jppf.ui.utils.GuiUtils;

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
	public TextOption(String name, String label, String tooltip, String value)
	{
		this.name = name;
		this.label = label;
		this.toolTipText = tooltip;
		this.value = value;
	}
	
	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		fieldLabel = new JLabel(label);
		field = createField();
		field.setPreferredSize(new Dimension(60, 20));
		JPanel panel = new JPanel();
		GridBagLayout g = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);
		panel.setLayout(g);
		if (toolTipText != null)
		{
			field.setToolTipText(toolTipText);
			fieldLabel.setToolTipText(toolTipText);
		}
		c.anchor = GridBagConstraints.LINE_START;
		addLayoutComp(panel, g, c, fieldLabel);
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1.0;
		JComponent filler = GuiUtils.createFiller(1, 1);
		addLayoutComp(panel, g, c, filler);
		c.anchor = GridBagConstraints.LINE_END;
		c.weightx = 0.0;
		addLayoutComp(panel, g, c, field);
		UIComponent = panel;
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
		field.setEnabled(enabled);
		fieldLabel.setEnabled(enabled);
	}
}
