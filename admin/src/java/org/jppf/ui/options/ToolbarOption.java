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

/**
 * This option class encapsulates a JToolBar.
 * @author Laurent Cohen
 */
public class ToolbarOption extends AbstractOptionContainer
{
  /**
   * Initialize the split pane with 2 fillers as left (or top) and right (or bottom) components.
   */
  public ToolbarOption()
  {
  }

  @Override
  public void createUI()
  {
    JToolBar toolbar = new JToolBar(SwingConstants.HORIZONTAL);
    toolbar.setFloatable(false);
    UIComponent = toolbar;
    toolbar.setOpaque(false);
  }

  @Override
  public void add(final OptionElement element)
  {
    super.add(element);
    JToolBar toolbar = (JToolBar) UIComponent;
    toolbar.add(element.getUIComponent());
  }

  @Override
  public void remove(final OptionElement element)
  {
    super.remove(element);
    JToolBar toolbar = (JToolBar) UIComponent;
    toolbar.remove(element.getUIComponent());
  }
}
