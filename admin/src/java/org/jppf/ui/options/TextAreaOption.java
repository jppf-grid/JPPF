/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public TextAreaOption()
	{
	}

	/**
	 * Initialize this text area option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the text area. 
	 * @param tooltip the tooltip associated with the text area.
	 * @param value the initial value of this component.
	 */
	public TextAreaOption(String name, String label, String tooltip, String value)
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
		textArea = new JTextArea((String) value);
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
		SwingUtilities.invokeLater( new Runnable()
		{
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
	protected void setupValueChangeNotifications()
	{
		Document doc = (Document) textArea.getDocument();
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
}
