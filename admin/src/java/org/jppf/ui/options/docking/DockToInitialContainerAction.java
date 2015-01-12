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

package org.jppf.ui.options.docking;

import java.awt.Component;
import java.awt.event.ActionEvent;

/**
 * Action to dock a tab to its initial view/container.
 * @author Laurent Cohen
 */
public class DockToInitialContainerAction extends AbstractDockingAction {
  /**
   * Initialize this action.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   */
  public DockToInitialContainerAction(final Component comp, final String label) {
    super(comp, label);
  }

  /**
   * Initialize this action.
   * @param comp the id of the view to which the component will be attached.
   * @param label the text of the corresponding menu item.
   * @param iconPath the path to an optional icon.
   */
  public DockToInitialContainerAction(final Component comp, final String label, final String iconPath) {
    super(comp, label, iconPath, null);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    DockingManager.getInstance().dockToInitialContainer(comp);
  }
}
