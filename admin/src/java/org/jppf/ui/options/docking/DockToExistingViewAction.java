/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

/**
 * Action to dock a tab to an existing view.
 * @author Laurent Cohen
 */
public class DockToExistingViewAction extends AbstractDockingAction {
  /**
   * Initialize this action with the specified view id.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   * @param viewId the id of the view to which the component will be attached.
   */
  public DockToExistingViewAction(final Component comp, final String label, final String viewId) {
    super(comp, label, null, viewId);
  }

  /**
   * Initialize this action with the specified view id.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   * @param iconPath the path to an optional icon.
   * @param viewId the id of the view to which the component will be attached.
   */
  public DockToExistingViewAction(final Component comp, final String label, final String iconPath, final String viewId) {
    super(comp, label, iconPath, viewId);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    DockingManager dmgr = DockingManager.getInstance();
    DetachableComponentDescriptor desc = dmgr.getComponent(comp);
    dmgr.attach(desc.getComponent(), viewId);
  }
}
