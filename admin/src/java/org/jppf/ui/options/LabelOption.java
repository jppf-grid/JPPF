/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.ui.utils.GuiUtils;

/**
 * An option for simple labels that have text, an icon, or both.
 * @author Laurent Cohen
 */
public class LabelOption extends AbstractOption {
  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI() {
    JLabel lab = new JLabel();
    lab.setHorizontalAlignment(SwingConstants.RIGHT);
    if (label != null) lab.setText((String) label);
    if (iconPath != null) {
      ImageIcon icon = GuiUtils.loadIcon(iconPath);
      if (icon != null) lab.setIcon(icon);
    }
    if (toolTipText != null) lab.setToolTipText(toolTipText);
    UIComponent = lab;
  }

  /**
   * Set the value of this option.
   * @param value the value as an <code>Object</code> instance.
   */
  @Override
  public void setValue(final Object value) {
    this.value = value;
    if (UIComponent != null) ((JLabel) UIComponent).setText((String) value);
  }

  @Override
  protected void setupValueChangeNotifications() {
  }

  /**
   * Enable or disable this option.
   * @param enabled true to enable this option, false to disable it.
   */
  @Override
  public void setEnabled(final boolean enabled) {
    UIComponent.setEnabled(enabled);
  }

  /**
   * This method always returns false, since labels have no value to persist.
   * @return false.
   */
  @Override
  public boolean isPersistent() {
    return false;
  }
}
