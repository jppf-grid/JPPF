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

package org.jppf.ui.options.docking;

import java.awt.Component;
import java.awt.event.*;
import java.util.Map;

import javax.swing.*;

/**
 * Mouse adapter that handles the popup menus on the tabs,
 * along with the processing of the tab selection via mouse click.
 */
class UndockingMouseAdapter extends MouseAdapter
{
  @Override
  public void mousePressed(final MouseEvent event)
  {
    if (event.getButton() != MouseEvent.BUTTON3) return;
    Component comp = event.getComponent();
    int x = event.getX();
    int y = event.getY();
    JPopupMenu menu = createPopupMenu(comp);
    menu.show(comp, x, y);
  }

  /**
   * 
   * @param comp the component to move.
   * @return a popup menu.
   */
  protected JPopupMenu createPopupMenu(final Component comp)
  {
    DockingManager dm = DockingManager.getInstance();
    DetachableComponentDescriptor desc = dm.getComponentFromListenerComp(comp);
    Component realComp = desc.getComponent().getUIComponent();
    JPopupMenu menu = new JPopupMenu();
    if (desc.getInitialContainer() != desc.getCurrentContainer())
      menu.add(new JMenuItem(new DockToInitialContainerAction(realComp, DockingManager.localize("attach.to.initial.container"))));
    menu.add(new JMenuItem(new DockToNewViewAction(realComp, DockingManager.localize("attach.to.new.view"))));
    Map<String, ViewDescriptor> viewMap = dm.getViewMap();
    if (viewMap.size() > 1)
    {
      JMenu subMenu = new JMenu(DockingManager.localize("attach.to.existing.view"));
      menu.add(subMenu);
      for (String id: viewMap.keySet())
      {
        if (!id.equals(desc.getViewId()) && (!id.equals(DockingManager.INITIAL_VIEW))) subMenu.add(new JMenuItem(new DockToExistingViewAction(realComp, id, id)));
      }
    }
    return menu;
  }

  @Override
  public void mouseClicked(final MouseEvent e)
  {
    if (e.getButton() == MouseEvent.BUTTON1)
    {
      Component comp = e.getComponent();
      DetachableComponentDescriptor desc = DockingManager.getInstance().getComponentFromListenerComp(comp);
      if (desc == null) return;
      JTabbedPane pane = (JTabbedPane) desc.getCurrentContainer().getUIComponent();
      pane.setSelectedComponent(desc.getComponent().getUIComponent());
    }
  }
}
