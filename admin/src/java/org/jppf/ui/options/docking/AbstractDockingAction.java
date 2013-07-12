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

package org.jppf.ui.options.docking;

import java.awt.Component;

import javax.swing.*;

import org.jppf.ui.utils.GuiUtils;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractDockingAction extends AbstractAction
{
  /**
   * The component to move from one view to another.
   */
  protected final Component comp;
  /**
   * The id of the view to which the component will be attached.
   */
  protected final String viewId;

  /**
   * Initialize this action.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   * @param iconPath the path to an optional icon.
   */
  public AbstractDockingAction(final Component comp, final String label, final String iconPath)
  {
    this(comp, label, iconPath, null);
  }

  /**
   * Initialize this action.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   */
  public AbstractDockingAction(final Component comp, final String label)
  {
    this(comp, label, null, null);
  }


  /**
   * Initialize this action with the specified view id.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   * @param iconPath the path to an optional icon.
   * @param viewId the id of the view to which the component will be attached.
   */
  public AbstractDockingAction(final Component comp, final String label, final String iconPath, final String viewId)
  {
    this.comp = comp;
    this.viewId = viewId;
    putValue(NAME, label);
    if (iconPath != null) putValue(Action.SMALL_ICON, GuiUtils.loadIcon(iconPath));
  }
}
