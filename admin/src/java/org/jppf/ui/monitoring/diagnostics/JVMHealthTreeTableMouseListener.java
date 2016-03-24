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

package org.jppf.ui.monitoring.diagnostics;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import org.jppf.ui.actions.JTreeTableActionHandler;
import org.jppf.ui.monitoring.node.AbstractTopologyMouseListener;

/**
 * Mouse listener for the JVM health panel.
 * Processes right-click events to display popup menus.
 * @author Laurent Cohen
 */
public class JVMHealthTreeTableMouseListener extends AbstractTopologyMouseListener
{
  /**
   * Initialize this mouse listener.
   * @param actionHandler the object that handles toolbar and menu actions.
   */
  public JVMHealthTreeTableMouseListener(final JTreeTableActionHandler actionHandler)
  {
    super(actionHandler);
  }

  @Override
  protected JPopupMenu createPopupMenu(final MouseEvent event)
  {
    Component comp = event.getComponent();
    Point p = comp.getLocationOnScreen();
    JPopupMenu menu = new JPopupMenu();
    addItem(menu, "health.gc", p);
    addItem(menu, "health.thread.dump", p);
    addItem(menu, "health.heap.dump", p);
    return menu;
  }
}
