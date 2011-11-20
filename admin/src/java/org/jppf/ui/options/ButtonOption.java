/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
    this.name = name;
    this.label = label;
    setToolTipText(tooltip);
    createUI();
  }

  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI()
  {
    JButton button = new JButton();
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
    JButton button = (JButton) UIComponent;
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
}
