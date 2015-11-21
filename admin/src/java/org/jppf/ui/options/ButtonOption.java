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

import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.utils.GuiUtils;

/**
 * Option implementation with a {@link JButton} or {@link JToggleButton}  as the underlying component.
 * @author Laurent Cohen
 */
public class ButtonOption extends AbstractOption {
  /**
   * Determines whether this object is a <code>JToggleButton</code> or a simple <code>JButton</code>.
   */
  private boolean toggle = false;
  /**
   * WHther the toggle button is selected or not.
   */
  private boolean selected = false;

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public ButtonOption() {
  }

  @Override
  public void createUI() {
    AbstractButton button = toggle ? new JToggleButton() : new JButton();
    if (label != null) button.setText(label);
    if (iconPath != null) {
      ImageIcon icon = GuiUtils.loadIcon(iconPath);
      if (icon != null) button.setIcon(icon);
    }
    if (toolTipText != null) button.setToolTipText(toolTipText);
    UIComponent = button;
    setupValueChangeNotifications();
  }

  @Override
  protected void setupValueChangeNotifications() {
    AbstractButton button = (AbstractButton) UIComponent;
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        fireValueChanged();
      }
    });
  }

  /**
   * Enable or disable this option.
   * @param enabled true to enable this option, false to disable it.
   */
  @Override
  public void setEnabled(final boolean enabled) {
    UIComponent.setEnabled(enabled);
  }

  @Override
  public boolean isPersistent() {
    return toggle ? super.isPersistent() : false;
  }

  /**
   * Determine whether this object is a <code>JToggleButton</code> or a simple <code>JButton</code>.
   * @return <code>true</code> if this object is a toggle button, <code>false</code> otherwise.
   */
  public boolean isToggle() {
    return toggle;
  }

  /**
   * Specify whether this object is a <code>JToggleButton</code> or a simple <code>JButton</code>.
   * @param toggle <code>true</code> if this object is a toggle button, <code>false</code> otherwise.
   */
  public void setToggle(final boolean toggle) {
    this.toggle = toggle;
  }

  @Override
  public Object getValue() {
    return toggle ? ((JToggleButton) UIComponent).isSelected() : null;
  }

  @Override
  public void setValue(final Object value) {
    if (!toggle || (value == null)) return;
    boolean b = (value instanceof Boolean) ? (Boolean) value : Boolean.valueOf(value.toString());
    JToggleButton toggleBtn = ((JToggleButton) UIComponent);
    if (b != toggleBtn.isSelected()) {
      super.setValue(b);
      toggleBtn.doClick();
      //toggleBtn.setSelected(b);
    }
  }
}
