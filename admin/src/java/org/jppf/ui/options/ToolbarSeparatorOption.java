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

import javax.swing.*;

/**
 * An option for boolean values, represented as a checkbox.
 * @author Laurent Cohen
 */
public class ToolbarSeparatorOption extends AbstractOption
{
  /**
   * Constructor provided as a convenience to facilitate the creation of
   * option elements through reflexion.
   */
  public ToolbarSeparatorOption()
  {
    //UIComponent = new JToolBar.Separator();
    UIComponent = new JLabel("|");
  }

  /**
   * Initialize this separator with the specified parameters.
   * @param width the filler's width
   * @param height the filler's height.
   */
  public ToolbarSeparatorOption(final int width, final int height)
  {
    this(" ", width, height);
  }

  /**
   * Initialize this separator with the specified parameters.
   * @param labelText the filler's text.
   * @param width the filler's width
   * @param height the filler's height.
   */
  public ToolbarSeparatorOption(final String labelText, final int width, final int height)
  {
    //UIComponent = new JToolBar.Separator();
    UIComponent = new JLabel(labelText);
    ((JLabel) UIComponent).setForeground(Color.GRAY.darker());
    ((JLabel) UIComponent).setHorizontalAlignment(SwingConstants.CENTER);
    ((JLabel) UIComponent).setVerticalAlignment(SwingConstants.CENTER);
    UIComponent.setForeground(UIComponent.getForeground().brighter());
    if ((width > 0) && (height > 0))
    {
      UIComponent.setPreferredSize(new Dimension(width, height));
    }
  }

  /**
   * Create the UI components for this option.
   */
  @Override
  public void createUI()
  {
  }

  /**
   * Get the current value for this option.
   * @return null.
   * @see org.jppf.ui.options.AbstractOption#getValue()
   */
  @Override
  public Object getValue()
  {
    return null;
  }

  /**
   * Propagate the state changes of the underlying checkbox to the listeners to this option.
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
