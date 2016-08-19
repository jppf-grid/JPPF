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

package org.jppf.ui.monitoring.node;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.jppf.ui.actions.JTreeTableActionHandler;

/**
 * Mouse listener for the node data panel.
 * Processes right-click events to display popup menus.
 * @author Laurent Cohen
 */
public class NodeTreeTableMouseListener extends AbstractTopologyMouseListener {
  /**
   * Initialize this mouse listener.
   * @param actionHandler the object that handles toolbar and menu actions.
   */
  public NodeTreeTableMouseListener(final JTreeTableActionHandler actionHandler) {
    super(actionHandler);
  }

  @Override
  protected JPopupMenu createPopupMenu(final MouseEvent event) {
    Component comp = event.getComponent();
    Point p = comp.getLocationOnScreen();
    JPopupMenu menu = new JPopupMenu();
    addItem(menu, "show.information", p);
    menu.addSeparator();
    addItem(menu, "shutdown.restart.driver", p);
    addItem(menu, "driver.reset.statistics", p);
    menu.addSeparator();
    addItem(menu, "update.configuration", p);
    addItem(menu, "update.threads", p);
    addItem(menu, "reset.counter", p);
    menu.addSeparator();
    addItem(menu, "restart.node", p);
    addItem(menu, "restart.node.deferred", p);
    addItem(menu, "shutdown.node", p);
    addItem(menu, "shutdown.node.deferred", p);
    addItem(menu, "cancel.deferred.action", p);
    menu.addSeparator();
    addItem(menu, "toggle.active", p);
    addItem(menu, "node.provisioning", p);
    return menu;
  }
}
