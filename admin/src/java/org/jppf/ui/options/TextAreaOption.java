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
import javax.swing.event.*;
import javax.swing.text.Document;

import org.jppf.ui.utils.GuiUtils;

/**
 * An option that uses a <code>JTextArea</code> to edit its value.
 * @author Laurent Cohen
 */
public class TextAreaOption extends AbstractOption
{
	/**
	 * The underlying UI component used to edit the value of this option.
	 */
	private JTextArea textArea = null;
	/**
	 * Determines whether the text area is editable.
	 */
	private boolean editable = false;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public TextAreaOption()
	{
	}

	/**
	 * Initialize this text area option with the specified parameters.
	 * @param name - this component's name.
	 * @param label - the label displayed with the text area.
	 * @param tooltip - the tooltip associated with the text area.
	 * @param value - the initial value of this component.
	 */
	public TextAreaOption(final String name, final String label, final String tooltip, final String value)
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
	@Override
	public void createUI()
	{
		textArea = new JTextArea((String) value);
		textArea.setBorder(BorderFactory.createEmptyBorder());
		if (toolTipText != null) textArea.setToolTipText(toolTipText);
		textArea.setEditable(editable);
		//textArea.setOpaque(false);
		if (scrollable)
		{
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setOpaque(false);
			UIComponent = scrollPane;
		}
		else
		{
			JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
			mainPanel.setBorder(BorderFactory.createTitledBorder(label));
			mainPanel.add(textArea);
			UIComponent = mainPanel;
		}
		setupValueChangeNotifications();
	}

	/**
	 * Get the current value for this option.
	 * @return a <code>String</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	@Override
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
	@Override
	public void setValue(final Object value)
	{
		this.value = value;
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				textArea.setText((String) TextAreaOption.this.value);
			}
		});
	}

	/**
	 * Set the current value for this option.
	 * @param value a <code>String</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void append(final String value)
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				textArea.append(value);
			}
		});
	}

	/**
	 * Add a listener to the underlying text document, to receive and propagate change events.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	@Override
	protected void setupValueChangeNotifications()
	{
		Document doc = textArea.getDocument();
		doc.addDocumentListener(new DocumentListener()
		{
			@Override
			public void changedUpdate(final DocumentEvent e)
			{
				fireValueChanged();
			}

			@Override
			public void insertUpdate(final DocumentEvent e)
			{
				fireValueChanged();
			}

			@Override
			public void removeUpdate(final DocumentEvent e)
			{
				fireValueChanged();
			}
		});
	}

	/**
	 * Determine whether the text area is editable.
	 * @return true if the text area is editable, false otherwise.
	 */
	public boolean isEditable()
	{
		return editable;
	}

	/**
	 * Specifiy whether the text area is editable.
	 * @param editable true if the text area is editable, false otherwise.
	 */
	@Override
	public void setEditable(final boolean editable)
	{
		this.editable = editable;
		if (textArea != null) textArea.setEditable(editable);
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(final boolean enabled)
	{
		textArea.setEnabled(enabled);
	}

	/**
	 * Get the underlying <code>JTextArea</code>.
	 * @return an instance of {@link JTextArea}.
	 */
	public JTextArea getTextArea()
	{
		return textArea;
	}
}
