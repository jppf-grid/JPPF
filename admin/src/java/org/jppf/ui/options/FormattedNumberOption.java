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

import java.awt.Dimension;
import java.text.*;

import javax.swing.*;

import org.slf4j.*;

/**
 * Option class for numbers formatted using a pattern, as defined in class
 * {@link java.text.DecimalFormat}.
 * @author Laurent Cohen
 */
public class FormattedNumberOption extends AbstractOption
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(FormattedNumberOption.class);
  /**
   * The field used to edit the value.
   */
  private JFormattedTextField field = null;
  /**
   * Used to format the value according to the pattern specified in the constructor.
   */
  private NumberFormat format = null;
  /**
   * Label associated with the formatted field.
   */
  protected JLabel fieldLabel = null;
  /**
   * The pattern that defines the field format.
   */
  protected String pattern = null;

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public FormattedNumberOption()
  {
  }

  /**
   * Initialize this text option with the specified parameters.
   * @param name this component's name.
   * @param label the label displayed with the text field.
   * @param tooltip the tooltip associated with the text field.
   * @param value the initial value of this component.
   * @param pattern the pattern defining the format of the number used as value.
   * @see java.text.DecimalFormat
   */
  public FormattedNumberOption(final String name, final String label, final String tooltip, final Number value, final String pattern)
  {
    this.name = name;
    this.label = label;
    setToolTipText(tooltip);
    this.value = value;
    this.pattern = pattern;
    format = new DecimalFormat(pattern);
    //format = NumberFormat.;
    createUI();
  }

  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI()
  {
    fieldLabel = new JLabel(label);
    field = createField();
    if (toolTipText != null)
    {
      field.setToolTipText(toolTipText);
      fieldLabel.setToolTipText(toolTipText);
    }
    UIComponent = layoutComponents(fieldLabel, field);
    setupValueChangeNotifications();
  }

  /**
   * Create the text field that holds the value of this password option.
   * @return a <code>JPasswordField</code> instance.
   * @see org.jppf.ui.options.TextOption#createField()
   */
  protected JFormattedTextField createField()
  {
    field = new JFormattedTextField(format);
    field.setValue(value);
    if (pattern != null) field.setColumns(pattern.length());
    else field.setPreferredSize(new Dimension(60, 20));
    field.setHorizontalAlignment(SwingConstants.RIGHT);
    return field;
  }

  /**
   * Get the value of the edited number.
   * @return the value of the formatted text field as a <code>Number</code>.
   * @see org.jppf.ui.options.TextOption#getValue()
   */
  @Override
  public Object getValue()
  {
    JFormattedTextField tmp = field;
    return tmp.getValue();
  }

  /**
   * Set the value of this option.
   * @param value the value as an <code>Object</code> instance.
   * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
   */
  @Override
  public void setValue(final Object value)
  {
    Object val = value;
    if (val instanceof String)
    {
      try
      {
        val = Double.parseDouble((String) value);
      }
      catch(NumberFormatException e)
      {
        log.debug(e.getMessage(), e);
        val = 0;
      }
    }
    if (field != null) field.setValue(val);
    this.value = val;
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

  /**
   * Get the pattern that defines the field format.
   * @return the pattern as a string.
   */
  public String getPattern()
  {
    return pattern;
  }

  /**
   * Set the pattern that defines the field format.
   * @param pattern the pattern as a string.
   */
  public void setPattern(final String pattern)
  {
    this.pattern = pattern;
    format = new DecimalFormat(pattern);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setEditable(final boolean editable)
  {
    if (field != null) field.setEditable(editable);
  }
}
