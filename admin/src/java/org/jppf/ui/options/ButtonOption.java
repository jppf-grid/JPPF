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

import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.utils.GuiUtils;
import org.slf4j.*;

/**
 * Option implementation with a JButton as the underlying component.
 * @author Laurent Cohen
 */
public class ButtonOption extends AbstractOption
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ButtonOption.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether this object is a <code>JToggleButton</code> or a simple <code>JButton</code>.
   */
  private boolean toggle = false;

  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public ButtonOption()
  {
  }

  /**
   * Initialize this text option with the specified parameters.
   * @param name this component's name.
   * @param label the label displayed with the checkbox.
   * @param tooltip the tooltip associated with the checkbox.
   */
  public ButtonOption(final String name, final String label, final String tooltip)
  {
    this(name, label, tooltip, false);
  }

  /**
   * Initialize this text option with the specified parameters.
   * @param name this component's name.
   * @param label the label displayed with the checkbox.
   * @param tooltip the tooltip associated with the checkbox.
   * @param toggle specifies
   */
  public ButtonOption(final String name, final String label, final String tooltip, final boolean toggle)
  {
    this.name = name;
    this.label = label;
    this.toggle = toggle;
    setToolTipText(tooltip);
    createUI();
  }

  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI()
  {
    AbstractButton button = toggle ? new JToggleButton() : new JButton();
    if (label != null) button.setText(label);
    if (iconPath != null)
    {
      ImageIcon icon = GuiUtils.loadIcon(iconPath);
      if (icon != null) button.setIcon(icon);
    }
    if (toolTipText != null) button.setToolTipText(toolTipText);
    UIComponent = button;
    setupValueChangeNotifications();
  }

  /**
   * This method does nothing.
   * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
   */
  @Override
  protected void setupValueChangeNotifications()
  {
    AbstractButton button = (AbstractButton) UIComponent;
    button.addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(final ActionEvent e)
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
  @Override
  public void setEnabled(final boolean enabled)
  {
    UIComponent.setEnabled(enabled);
  }

  /**
   * This method always returns false, since buttons have no value to persist.
   * @return false.
   * @see org.jppf.ui.options.AbstractOption#isPersistent()
   */
  @Override
  public boolean isPersistent()
  {
    return false;
  }

  /**
   * Determine whether this object is a <code>JToggleButton</code> or a simple <code>JButton</code>.
   * @return <code>true</code> if this object is a toggle button, <code>false</code> otherwise.
   */
  public boolean isToggle()
  {
    return toggle;
  }

  /**
   * Specify whether this object is a <code>JToggleButton</code> or a simple <code>JButton</code>.
   * @param toggle <code>true</code> if this object is a toggle button, <code>false</code> otherwise.
   */
  public void setToggle(final boolean toggle)
  {
    this.toggle = toggle;
  }
}
