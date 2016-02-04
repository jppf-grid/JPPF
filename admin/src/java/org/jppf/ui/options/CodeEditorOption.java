/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.io.InputStream;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jppf.ui.utils.GuiUtils;

/**
 * An option that uses a code editor to edit its value.
 * <p><b>See also:</b> the code editor is provided by the <a href="https://github.com/bobbylight/RSyntaxTextArea">RSyntaxTextArea project</a>.
 * @author Laurent Cohen
 * @since 5.2
 */
public class CodeEditorOption extends AbstractOption {
  /**
   * Default theme to use.
   */
  private static final Theme THEME = loadTheme();
  /**
   * The underlying UI component used to edit the value of this option.
   */
  private RSyntaxTextArea textArea = null;
  /**
   * Determines whether the text area is editable.
   */
  private boolean editable = false;
  /**
   * The language used by the code editor.
   */
  private String language;

  /**
   * Constructor provided as a convenience to facilitate the creation of option elements through reflexion.
   */
  public CodeEditorOption() {
  }

  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI() {
    textArea = new RSyntaxTextArea();
    Object o = SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE;
    textArea.setSyntaxEditingStyle(language);
    textArea.setCodeFoldingEnabled(false);
    textArea.setCloseCurlyBraces(false);
    textArea.setCloseMarkupTags(false);
    textArea.setTabsEmulated(true);
    textArea.setTabSize(2);
    textArea.setUseFocusableTips(false);
    textArea.setMarginLineEnabled(false);
    textArea.setMarginLinePosition(0);
    textArea.setBorder(BorderFactory.createEmptyBorder());
    if (THEME != null) THEME.apply(textArea);
    if (toolTipText != null) textArea.setToolTipText(toolTipText);
    textArea.setEditable(editable);
    if (scrollable) {
      RTextScrollPane scrollPane = new RTextScrollPane(textArea);
      GuiUtils.adjustScrollbarsThickness(scrollPane);
      scrollPane.setOpaque(false);
      scrollPane.setLineNumbersEnabled(false);
      scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
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
      textArea.setText(value.toString());
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
    sb.append(value);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        textArea.append(sb.toString());
        textArea.setCaretPosition(textArea.getDocument().getLength());
        CodeEditorOption.this.value = CodeEditorOption.this.textArea.getText();
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
  public RSyntaxTextArea getTextArea() {
    return textArea;
  }

  /**
   * Get the language used by the code editor.
   * @return the language in mime type format, e.g. "text/java".
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Set the language used by the code editor.
   * @param language the language in mime type format, e.g. "text/java".
   */
  public void setLanguage(final String language) {
    this.language = language;
  }

  /**
   * Load the default syntax theme.
   * @return a {@link Theme} instance.
   */
  private static Theme loadTheme() {
    Theme theme = null;
    try {
      InputStream is = CodeEditorOption.class.getClassLoader().getResourceAsStream("org/jppf/ui/filtering/jppf_theme.xml");
      theme = Theme.load(is);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return theme;
  }
}
