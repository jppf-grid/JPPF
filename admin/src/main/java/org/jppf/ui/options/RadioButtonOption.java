/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.awt.event.*;

import javax.swing.JRadioButton;

/**
 * An option for boolean values, represented as a radio button.
 * @author Laurent Cohen
 */
public class RadioButtonOption extends AbstractOption {
  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public RadioButtonOption() {
  }

  /**
   * Initialize this boolean option with the specified parameters.
   * @param name this component's name.
   * @param label the label displayed with the checkbox.
   * @param tooltip the tooltip associated with the checkbox.
   * @param value the initial value of this component.
   */
  public RadioButtonOption(final String name, final String label, final String tooltip, final Boolean value) {
    this.name = name;
    this.label = label;
    setToolTipText(tooltip);
    this.value = value;
    createUI();
  }

  @Override
  public void createUI() {
    final JRadioButton radioButton = new JRadioButton(label, (Boolean) value);
    if (toolTipText != null) radioButton.setToolTipText(toolTipText);
    UIComponent = radioButton;
    setupValueChangeNotifications();
  }

  /**
   * Get the current value for this option.
   * @return a <code>Boolean</code> instance.
   * @see org.jppf.ui.options.AbstractOption#getValue()
   */
  @Override
  public Object getValue() {
    value = ((JRadioButton) UIComponent).isSelected();
    return value;
  }

  /**
   * Set the value of this option.
   * @param value the value as an <code>Object</code> instance.
   * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
   */
  @Override
  public void setValue(final Object value) {
    Object val = value;
    if (value instanceof String) val = "true".equalsIgnoreCase((String) value);
    super.setValue(val);
    if (UIComponent != null) {
      ((JRadioButton) UIComponent).setSelected((Boolean) val);
      fireValueChanged();
    }
  }

  /**
   * Propagate the state changes of the underlying checkbox to the listeners to this option.
   * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
   */
  @Override
  protected void setupValueChangeNotifications() {
    ((JRadioButton) UIComponent).addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
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
  public void setEnabled(final boolean enabled) {
    UIComponent.setEnabled(enabled);
  }
}
