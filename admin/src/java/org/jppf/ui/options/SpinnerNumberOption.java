/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.text.NumberFormat;

import javax.swing.*;
import javax.swing.event.*;

import org.jppf.utils.StringUtils;

/**
 * An option that uses a JSpinner control to edit its value.
 * @author Laurent Cohen
 */
public class SpinnerNumberOption extends AbstractOption
{
  /**
   * The combo box used to select one among several.
   */
  private JSpinner spinner = null;
  /**
   * Label associated with the <code>JSpinner</code> field.
   */
  protected JLabel spinnerLabel = null;
  /**
   * Minimum value for the spinner control.
   */
  protected Number min = Integer.valueOf(0);
  /**
   * Maximum value for the spinner control.
   */
  protected Number max = Integer.valueOf(0);
  /**
   * Step size for the spinner control.
   */
  protected Number step = Integer.valueOf(0);
  /**
   * Use to parse from string values to <code>Number</code> instances.
   */
  protected NumberFormat nf = NumberFormat.getInstance();
  /**
   * The number format pattern.
   */
  protected String pattern = "0";

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public SpinnerNumberOption()
  {
  }

  /**
   * Initialize this spinner option with the specified parameters.
   * @param name this component's name.
   * @param label the label displayed with the checkbox.
   * @param tooltip the tooltip associated with the combobox.
   * @param value the initial value of this component.
   * @param min the minimum value that can be set in the spinner.
   * @param max the maximum value that can be set in the spinner.
   * @param step the step size for the spinner.
   */
  public SpinnerNumberOption(final String name, final String label, final String tooltip, final Number value, final Number min, final Number max, final Number step)
  {
    this.name = name;
    this.label = label;
    setToolTipText(tooltip);
    this.value = value;
    this.min = min;
    this.max = max;
    this.step = step;
    createUI();
  }

  @Override
  public void createUI()
  {
    SpinnerNumberModel model = new SpinnerNumberModel((Number) value, (Comparable) min, (Comparable) max, step);
    spinner = new JSpinner(model);
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner.getEditor();
    editor.getFormat().applyPattern(pattern);
    if (editor.getTextField().getColumns() < 5) editor.getTextField().setColumns(5);
    spinnerLabel = new JLabel(label);
    if (toolTipText != null)
    {
      spinner.setToolTipText(toolTipText);
      spinnerLabel.setToolTipText(toolTipText);
    }
    UIComponent = layoutComponents(spinnerLabel, spinner);
    setupValueChangeNotifications();
  }

  /**
   * Get the current value for this option.
   * @return a <code>Number</code> instance.
   * @see org.jppf.ui.options.AbstractOption#getValue()
   */
  @Override
  public Object getValue()
  {
    value = spinner.getModel().getValue();
    return value;
  }

  /**
   * Set the current value for this option.
   * @param value a <code>Number</code> instance.
   * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
   */
  @Override
  public void setValue(final Object value)
  {
    if (value instanceof String) this.value = StringUtils.parseNumber((String) value, min);
    else if (!(value instanceof Number)) this.value = min;
    else this.value = value;
    if (spinner != null) spinner.getModel().setValue(this.value);
  }

  /**
   * Propagate the selection changes in the underlying combo box to the listeners to this option.
   * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
   */
  @Override
  protected void setupValueChangeNotifications()
  {
    SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
    model.addChangeListener(new ChangeListener()
    {
      @Override
      public void stateChanged(final ChangeEvent e)
      {
        getValue();
        fireValueChanged();
      }
    });
  }

  /**
   * Enable or disable this option.
   * @param enabled true to enable this option, false to disable it.
   * @see org.jppf.ui.options.Option#setEnabled(boolean)
   */
  @Override
  public void setEnabled(final boolean enabled)
  {
    spinner.setEnabled(enabled);
    spinnerLabel.setEnabled(enabled);
  }

  /**
   * Get the maximum value for the spinner control.
   * @return the value as a <code>Number</code>.
   */
  public Number getMax()
  {
    return max;
  }

  /**
   * Set the maximum value for the spinner control.
   * @param max the value as a <code>Number</code>.
   */
  public void setMax(final Number max)
  {
    this.max = max;
  }

  /**
   * Get the minimum value for the spinner control.
   * @return the value as a <code>Number</code>.
   */
  public Number getMin()
  {
    return min;
  }

  /**
   * Set the minimum value for the spinner control.
   * @param min the value a <code>Number</code>.
   */
  public void setMin(final Number min)
  {
    this.min = min;
  }

  /**
   * Get ttep size for the spinner control.
   * @return the step size as a <code>Number</code>.
   */
  public Number getStep()
  {
    return step;
  }

  /**
   * Set ttep size for the spinner control.
   * @param step the step size as a <code>Number</code>.
   */
  public void setStep(final Number step)
  {
    this.step = step;
  }

  @Override
  public String toString()
  {
    return "SpinnerNumberOption [label=" + label + ", min=" + asString(min) + ", max=" + asString(max) + ", step=" + asString(step) + ", value=" + asString((Number) value) + "]";
  }

  /**
   * 
   * @param n .
   * @return .
   */
  private String asString(final Number n)
  {
    return n.getClass().getSimpleName() + '(' + n.doubleValue() + ')';
  }

  /**
   * Get the number format pattern.
   * @return the pattern as a string.
   */
  public String getPattern()
  {
    return pattern;
  }

  /**
   * Set the number format pattern.
   * @param pattern the pattern as a string.
   */
  public void setPattern(final String pattern)
  {
    this.pattern = pattern;
  }

  /**
   * Get tabel associated with the <code>JSpinner</code> field.
   * @return a <code>JLabel</code> instance.
   */
  public JLabel getSpinnerLabel()
  {
    return spinnerLabel;
  }
}
