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

package org.jppf.ui.monitoring.job;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.actions.*;
import org.jppf.ui.treetable.JPPFTreeTable;

/**
 * Mouse listener for the node data panel.
 * Processes right-click events to display popup menus.
 * @author Laurent Cohen
 */
public class JobTreeTableMouseListener extends MouseAdapter {
  /**
   * The object that handles toolbar and menu actions.
   */
  private JTreeTableActionHandler actionHandler = null;

  /**
   * Initialize this mouse listener.
   * @param actionHandler - the object that handles toolbar and menu actions.
   */
  public JobTreeTableMouseListener(final JTreeTableActionHandler actionHandler) {
    this.actionHandler = actionHandler;
  }

  /**
   * Processes right-click events to display popup menus.
   * @param event the mouse event to process.
   */
  @Override
  public void mousePressed(final MouseEvent event) {
    Component comp = event.getComponent();
    if (!(comp instanceof JPPFTreeTable)) return;
    JPPFTreeTable treeTable = (JPPFTreeTable) comp;
    int x = event.getX();
    int y = event.getY();
    int button = event.getButton();
    if (button == MouseEvent.BUTTON3) {
      JPopupMenu menu = createPopupMenu(event);
      menu.show(treeTable, x, y);
    }
  }

  /**
   * Create the popup menu.
   * @param event the mouse event to process.
   * @return a <code>JPopupMenu</code> instance.
   */
  private JPopupMenu createPopupMenu(final MouseEvent event) {
    Component comp = event.getComponent();
    Point p = comp.getLocationOnScreen();
    JPopupMenu menu = new JPopupMenu();
    menu.add(createMenuItem(actionHandler.getAction("cancel.job"), p));
    menu.add(createMenuItem(actionHandler.getAction("suspend.job"), p));
    menu.add(createMenuItem(actionHandler.getAction("suspend_requeue.job"), p));
    menu.add(createMenuItem(actionHandler.getAction("resume.job"), p));
    menu.addSeparator();
    menu.add(createMenuItem(actionHandler.getAction("max.nodes.job"), p));
    menu.add(createMenuItem(actionHandler.getAction("update.priority.job"), p));
    return menu;
  }

  /**
   * Create a menu item.
   * @param action - the action associated with the neu item.
   * @param location - the location to use for any window create by the action.
   * @return a <code>JMenuItem</code> instance.
   */
  private static JMenuItem createMenuItem(final Action action, final Point location) {
    if (action instanceof AbstractUpdatableAction) ((AbstractUpdatableAction) action).setLocation(location);
    return new JMenuItem(action);
  }
}
