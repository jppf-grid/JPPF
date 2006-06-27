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

import java.awt.Color;
import javax.swing.*;
import javax.swing.event.*;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.utils.SubstanceColorUtilities;
import org.ujac.ui.editor.*;

/**
 * This option encapsulates an XML editor with syntax highlighting.
 * @author Laurent Cohen
 */
public class XMLEditorOption extends AbstractOption
{
	/**
	 * The underlying UI component used to edit the value of this option.
	 */
	private TextArea textArea = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public XMLEditorOption()
	{
	}

	/**
	 * Initialize this text area option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the text area. 
	 * @param tooltip the tooltip associated with the text area.
	 * @param value the initial value of this component.
	 */
	public XMLEditorOption(String name, String label, String tooltip, String value)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		textArea = new TextArea("text/xml");
		textArea.setText("");
		textArea.setBorder(BorderFactory.createEmptyBorder());
		if (toolTipText != null) textArea.setToolTipText(toolTipText);
		textArea.setEditable(false);
		textArea.setOpaque(false);
		if (scrollable)
		{
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setOpaque(false);
			UIComponent = scrollPane;
		}
		else UIComponent = textArea;
		setupValueChangeNotifications();
		//setBackgroundColor();
	}

	/**
	 * Get the current value for this option.
	 * @return a <code>String</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		value = textArea.getText();
		return value;
	}

	/**
	 * Set the current value for this option.
	 * @param value a <code>String</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		this.value = value;
		textArea.setText((String) value);
		//setBackgroundColor();
	}

	/**
	 * Add a listener to the underlying text document, to receive and propagate change events.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
		XmlTextDocument doc = (XmlTextDocument) textArea.getDocument();
		doc.addDocumentListener(new DocumentListener()
		{
			public void changedUpdate(DocumentEvent e)
			{
				fireValueChanged();
			}

			public void insertUpdate(DocumentEvent e)
			{
				fireValueChanged();
			}

			public void removeUpdate(DocumentEvent e)
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
		((JTextArea) UIComponent).setEnabled(enabled);
	}

	/**
	 * Set the editor's background color.
	 */
	protected void setBackgroundColor()
	{
		ColorScheme cs = SubstanceLookAndFeel.getColorScheme();
		if (cs != null)
		{
			Color c = SubstanceColorUtilities.getInterpolatedColor(cs.getLightColor(), cs.getUltraDarkColor(), 0.5d);
			textArea.setBackground(c);
		}
	}
}
