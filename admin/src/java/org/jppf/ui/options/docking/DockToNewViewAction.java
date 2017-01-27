/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
import java.awt.event.ActionEvent;

import org.jppf.ui.options.factory.OptionsHandler;

/**
 * Action to dock a tab to a new view.
 * @author Laurent Cohen
 */
public class DockToNewViewAction extends AbstractDockingAction
{
  /**
   * Initialize this action.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   * @param iconPath the path to an optional icon.
   */
  public DockToNewViewAction(final Component comp, final String label, final String iconPath) {
    super(comp, label, iconPath);
  }

  /**
   * Initialize this action.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   */
  public DockToNewViewAction(final Component comp, final String label) {
    super(comp, label);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    DockingManager dmgr = DockingManager.getInstance();
    String id = dmgr.createView();
    ViewDescriptor view = dmgr.getView(id);
    DetachableComponentDescriptor desc = dmgr.getComponent(comp);
    dmgr.attach(desc.getComponent(), id);
    //view.getFrame().pack();
    view.getFrame().setSize(OptionsHandler.getMainWindow().getSize());
    view.getFrame().setVisible(true);
  }
}
