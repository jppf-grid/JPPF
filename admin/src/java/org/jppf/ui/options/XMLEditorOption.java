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

import java.awt.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.PlainDocument;
import org.ujac.ui.editor.*;
import org.ujac.ui.editor.TextArea;

/**
 * This option encapsulates an XML editor with syntax highlighting.
 * @author Laurent Cohen
 */
public class XMLEditorOption extends AbstractOption
{
	/**
	 * The default syntactic styles for the editor.
	 */
	private static SyntaxStyle[] styles = getDefaultStyles();
	/**
	 * The underlying UI component used to edit the value of this option.
	 */
	private TextArea textArea = null;
	/**
	 * Use to detect when the parent's backgorunbd color has changed,
	 * to set that of the text area accordingly.
	 */
	private PropertyChangeListener backgroundChangeListener = null;

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
		textArea.setStyles(styles);
		textArea.setOpaque(true);
    textArea.getDocument().getDocumentProperties().put(PlainDocument.tabSizeAttribute, new Integer(2));
		textArea.setText("");
		textArea.setEditable(true);
		textArea.setEnabled(true);
		textArea.setEOLMarkersPainted(false);
		textArea.setBorder(BorderFactory.createEmptyBorder());
		if (toolTipText != null) textArea.setToolTipText(toolTipText);
		if (scrollable)
		{
			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setOpaque(false);
			UIComponent = scrollPane;
		}
		else UIComponent = textArea;
		setupValueChangeNotifications();
		backgroundChangeListener = new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				Object val = evt.getNewValue();
				if (val instanceof Color)
				{
					textArea.setBackground((Color) val);
				}
			}
		};
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
		setBackgroundColor();
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
	 * Get the underlying text editor component.
	 * @return a <code>TextArea</code> instance.
	 */
	public TextArea getTextArea()
	{
		return textArea;
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
		Color c = textArea.getParent().getBackground();
		textArea.setBackground(c);
	}

	/**
	 * Set the parent panel for this option.
	 * @param parent an <code>ElementOption</code> instance.
	 * @see org.jppf.ui.options.AbstractOptionElement#setParent(org.jppf.ui.options.OptionElement)
	 */
	public void setParent(OptionElement parent)
	{
		if (this.parent != null)
		{
			Component c = this.getParent().getUIComponent();
			if (c != null) c.removePropertyChangeListener("background", backgroundChangeListener);
		}
		super.setParent(parent);
		if (parent != null)
		{
			Component c = this.getParent().getUIComponent();
			if (c != null) c.addPropertyChangeListener("background", backgroundChangeListener);
		}
	}

	/**
	 * Get the default syntactic styles for the editor.
	 * @return an array of <code>SyntaxStyle</code> instances.
	 */
	private static SyntaxStyle[] getDefaultStyles()
	{
    SyntaxStyle[] styles = new SyntaxStyle[Token.ID_COUNT];

    styles[Token.COMMENT1] = new SyntaxStyle(Color.green.darker().darker(), true, false);
    styles[Token.COMMENT2] = new SyntaxStyle(new Color(0x990033), true, false);
    styles[Token.KEYWORD1] = new SyntaxStyle(Color.blue.darker(), false, true);
    styles[Token.KEYWORD2] = new SyntaxStyle(Color.blue, false, false);
    styles[Token.KEYWORD3] = new SyntaxStyle(new Color(0x009600), false, false);
    styles[Token.LITERAL1] = new SyntaxStyle(new Color(96, 192, 192), false, false);
    styles[Token.LITERAL2] = new SyntaxStyle(new Color(0x650099), false, true);
    styles[Token.LABEL] = new SyntaxStyle(new Color(0x990033), false, true);
    styles[Token.OPERATOR] = new SyntaxStyle(Color.blue, false, true);
    styles[Token.INVALID] = new SyntaxStyle(Color.red, false, true);

    return styles;
	}
}
