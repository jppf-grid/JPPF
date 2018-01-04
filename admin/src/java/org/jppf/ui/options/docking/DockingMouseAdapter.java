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
import java.awt.event.*;
import java.util.Map;

import javax.swing.*;

/**
 * Mouse adapter that handles the popup menus on the tabs, along with the processing of the tab selection via mouse click.
 */
class DockingMouseAdapter extends MouseAdapter {
  @Override
  public void mousePressed(final MouseEvent event) {
    if (event.getButton() != MouseEvent.BUTTON3) return;
    final Component comp = event.getComponent();
    final JPopupMenu menu = createPopupMenu(comp);
    menu.show(comp, event.getX(), event.getY());
  }

  /**
   * Create and display the popup menu of docking actions.
   * @param comp the component to move.
   * @return a popup menu.
   */
  private static JPopupMenu createPopupMenu(final Component comp) {
    final DockingManager dm = DockingManager.getInstance();
    final DetachableComponentDescriptor desc = dm.getComponentFromListenerComp(comp);
    final Component realComp = desc.getComponent().getUIComponent();
    final JPopupMenu menu = new JPopupMenu();
    if (desc.getInitialContainer() != desc.getCurrentContainer())
      menu.add(new JMenuItem(new DockToInitialContainerAction(realComp, DockingManager.localize("attach.to.initial.container"))));
    menu.add(new JMenuItem(new DockToNewViewAction(realComp, DockingManager.localize("attach.to.new.view"))));
    final Map<String, ViewDescriptor> viewMap = dm.getViewMap();
    // exclude current and main view
    if (viewMap.size() > 2) {
      final JMenu subMenu = new JMenu(DockingManager.localize("attach.to.existing.view"));
      menu.add(subMenu);
      for (final String id: viewMap.keySet()) {
        if (!id.equals(desc.getViewId()) && (!id.equals(DockingManager.INITIAL_VIEW))) subMenu.add(new JMenuItem(new DockToExistingViewAction(realComp, id, id)));
      }
    }
    return menu;
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON1) {
      final Component comp = e.getComponent();
      final DetachableComponentDescriptor desc = DockingManager.getInstance().getComponentFromListenerComp(comp);
      if (desc == null) return;
      final JTabbedPane pane = (JTabbedPane) desc.getCurrentContainer().getUIComponent();
      pane.setSelectedComponent(desc.getComponent().getUIComponent());
    }
  }
}
