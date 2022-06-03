/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.ui.actions.*;
import org.jppf.ui.treetable.JPPFTreeTable;

/**
 * Mouse listener for the node data panel.
 * Processes right-click events to display popup menus.
 * @author Laurent Cohen
 */
public abstract class AbstractTopologyMouseListener extends MouseAdapter {
  /**
   * Constant for an empty <code>TopologyData</code> array.
   */
  protected static final AbstractTopologyComponent[] EMPTY_TOPOLOGY_DATA_ARRAY = new AbstractTopologyComponent[0];
  /**
   * Path to the cancel icon resource.
   */
  protected static final String CANCEL_ICON = "/org/jppf/ui/resources/stop.gif";
  /**
   * Path to the restart icon resource.
   */
  protected static final String RESTART_ICON = "/org/jppf/ui/resources/restart.gif";
  /**
   * Array of current corresponding jmx connections.
   */
  protected AbstractTopologyComponent[] data = null;
  /**
   * The object that handles toolbar and menu actions.
   */
  protected JTreeTableActionHandler actionHandler = null;

  /**
   * Initialize this mouse listener.
   * @param actionHandler the object that handles toolbar and menu actions.
   */
  public AbstractTopologyMouseListener(final JTreeTableActionHandler actionHandler) {
    this.actionHandler = actionHandler;
  }

  /**
   * Processes right-click events to display popup menus.
   * @param event the mouse event to process.
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(final MouseEvent event) {
    final Component comp = event.getComponent();
    if (!(comp instanceof JPPFTreeTable)) return;
    final JPPFTreeTable treeTable = (JPPFTreeTable) comp;
    final JTree tree = treeTable.getTree();
    final int x = event.getX();
    final int y = event.getY();
    final List<AbstractTopologyComponent> dataList = new ArrayList<>();
    int[] rows = treeTable.getSelectedRows();
    if ((rows == null) || (rows.length == 0)) {
      final TreePath path = tree.getPathForLocation(x, y);
      if (path == null) return;
      rows = new int[] { tree.getRowForPath(path) };
    }
    for (final int row: rows) {
      final TreePath path = tree.getPathForRow(row);
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
      if (!(node.getUserObject() instanceof AbstractTopologyComponent)) continue;
      final AbstractTopologyComponent td = (AbstractTopologyComponent) node.getUserObject();
      if (td.isNode()) dataList.add((AbstractTopologyComponent) node.getUserObject());
    }
    data = dataList.toArray(new AbstractTopologyComponent[dataList.size()]);

    final int button = event.getButton();
    if (button == MouseEvent.BUTTON3) {
      final JPopupMenu menu = createPopupMenu(event);
      menu.show(treeTable, x, y);
    }
  }

  /**
   * Create the popup menu.
   * @param event the mouse event to process.
   * @return a <code>JPopupMenu</code> instance.
   */
  protected abstract JPopupMenu createPopupMenu(final MouseEvent event);

  /**
   * Create a menu item and add it to the specified menu.
   * @param menu the popoup menu to add the item to.
   * @param actionName the name of action associated with the new item.
   * @param location the location to use for any window create by the action.
   */
  protected void addItem(final JPopupMenu menu, final String actionName, final Point location) {
    final Action action = actionHandler.getAction(actionName);
    if (action instanceof AbstractUpdatableAction) ((AbstractUpdatableAction) action).setLocation(location);
    menu.add(new JMenuItem(action));
  }
}
