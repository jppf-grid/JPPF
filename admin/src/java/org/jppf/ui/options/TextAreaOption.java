/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;

import org.jppf.ui.monitoring.diagnostics.ThreadDumpAction;
import org.jppf.ui.utils.GuiUtils;

/**
 * An option that uses a <code>JTextArea</code> to edit its value.
 * @author Laurent Cohen
 */
public class TextAreaOption extends AbstractOption {
  /**
   * The underlying UI component used to edit the value of this option.
   */
  private JTextArea textArea = null;
  /**
   * Determines whether the text area is editable.
   */
  private boolean editable = false;
  /**
   * Format of an optional timestamp displayed before the message.
   */
  private String timestampFormat = null;
  /**
   * The date format if specified.
   */
  private SimpleDateFormat dateFormat = null;

  /**
   * Constructor provided as a convenience to facilitate the creation of option elements through reflexion.
   */
  public TextAreaOption() {
  }

  /**
   * Initialize this text area option with the specified parameters.
   * @param name this component's name.
   * @param label the label displayed with the text area.
   * @param tooltip the tooltip associated with the text area.
   * @param value the initial value of this component.
   */
  public TextAreaOption(final String name, final String label, final String tooltip, final String value) {
    this.name = name;
    this.label = label;
    setToolTipText(tooltip);
    this.value = value;
    //this.bordered = true;
    createUI();
  }

  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI() {
    textArea = new JTextArea((String) value);
    textArea.setBorder(BorderFactory.createEmptyBorder());
    if (toolTipText != null) textArea.setToolTipText(toolTipText);
    textArea.setEditable(editable);
    textArea.addMouseListener(new EditorMouseListener());
    //if (!bordered) textArea.setBorder(BorderFactory.createEmptyBorder());
    //textArea.setOpaque(false);
    if (scrollable) {
      JScrollPane scrollPane = new JScrollPane(textArea);
      scrollPane.setOpaque(false);
      if (!bordered) scrollPane.setBorder(BorderFactory.createEmptyBorder());
      UIComponent = scrollPane;
    } else {
      JPanel mainPanel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
      mainPanel.setBorder(BorderFactory.createTitledBorder(label));
      mainPanel.add(textArea);
      if (!bordered) mainPanel.setBorder(BorderFactory.createEmptyBorder());
      UIComponent = mainPanel;
    }
    setupValueChangeNotifications();
  }

  /**
   * Get the current value for this option.
   * @return a <code>String</code> instance.
   */
  @Override
  public Object getValue() {
    value = textArea.getText();
    return value;
  }

  /**
   * Set the current value for this option.
   * @param value a <code>String</code> instance.
   */
  @Override
  public void setValue(final Object value) {
    this.value = value;
    if ((value == null) || "".equals(value)) textArea.setText((String) value);
    else {
      StringBuilder sb = new StringBuilder();
      if (dateFormat != null) sb.append(dateFormat.format(new Date())).append(' ');
      sb.append(this.value);
      textArea.setText(sb.toString());
      textArea.setCaretPosition(textArea.getDocument().getLength());
    }
  }

  /**
   * Set the current value for this option.
   * @param value a <code>String</code> instance.
   */
  public void append(final String value) {
    if ((value == null) || "".equals(value)) return;
    final StringBuilder sb = new StringBuilder();
    if ((this.value != null) && !"".equals(this.value)) sb.append('\n');
    if (dateFormat != null) sb.append(dateFormat.format(new Date())).append(' ');
    sb.append(value);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        textArea.append(sb.toString());
        textArea.setCaretPosition(textArea.getDocument().getLength());
        TextAreaOption.this.value = TextAreaOption.this.textArea.getText();
      }
    });
  }

  /**
   * Clear the text.
   */
  private void clear() {
    setValue("");
  }

  /**
   * Add a listener to the underlying text document, to receive and propagate change events.
   */
  @Override
  protected void setupValueChangeNotifications() {
    Document doc = textArea.getDocument();
    doc.addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(final DocumentEvent e) {
        fireValueChanged();
      }

      @Override
      public void insertUpdate(final DocumentEvent e) {
        fireValueChanged();
      }

      @Override
      public void removeUpdate(final DocumentEvent e) {
        fireValueChanged();
      }
    });
  }

  /**
   * Determine whether the text area is editable.
   * @return true if the text area is editable, false otherwise.
   */
  public boolean isEditable() {
    return editable;
  }

  /**
   * Specify whether the text area is editable.
   * @param editable true if the text area is editable, false otherwise.
   */
  @Override
  public void setEditable(final boolean editable) {
    this.editable = editable;
    if (textArea != null) textArea.setEditable(editable);
  }

  @Override
  public void setEnabled(final boolean enabled) {
    textArea.setEnabled(enabled);
  }

  /**
   * Get the underlying <code>JTextArea</code>.
   * @return an instance of {@link JTextArea}.
   */
  public JTextArea getTextArea() {
    return textArea;
  }

  /**
   * Get the format of an optional timestamp displayed before the message.
   * @return the format as a string.
   */
  public String getTimestampFormat() {
    return timestampFormat;
  }

  /**
   * Set the format of an optional timestamp displayed before the message.
   * @param timestampFormat the format as a string.
   */
  public void setTimestampFormat(final String timestampFormat) {
    if (timestampFormat != null) {
      this.timestampFormat = timestampFormat;
      dateFormat = new SimpleDateFormat(this.timestampFormat);
    }
  }

  /**
   * 
   */
  private class EditorMouseListener extends MouseAdapter {
    /**
     * The string to copy to the clipboard.
     */
    private String text;

    /**
     * Processes right-click events to display popup menus.
     * @param event the mouse event to process.
     */
    @Override
    public void mousePressed(final MouseEvent event) {
      Component comp = event.getComponent();
      int x = event.getX();
      int y = event.getY();
      int button = event.getButton();
      if (button == MouseEvent.BUTTON3) {
        JPopupMenu menu = new JPopupMenu();
        AbstractAction clipboardAction = new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            try {
              Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
              clip.setContents(new StringSelection(textArea.getText()), null);
            } catch (Exception e2) {
            }
          }
        };
        clipboardAction.putValue(ThreadDumpAction.NAME, "Copy to clipboard");
        AbstractAction clearAction = new AbstractAction() {
          @Override
          public void actionPerformed(final ActionEvent e) {
            clear();
          }
        };
        clearAction.putValue(ThreadDumpAction.NAME, "Clear");
        menu.add(new JMenuItem(clearAction));
        menu.add(new JMenuItem(clipboardAction));
        menu.show(event.getComponent(), x, y);
      }
    }
  }
}
