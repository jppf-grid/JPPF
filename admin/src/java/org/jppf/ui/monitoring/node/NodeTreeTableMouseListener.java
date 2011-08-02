/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.ui.actions.*;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.treetable.JPPFTreeTable;
import org.jppf.ui.utils.GuiUtils;
import org.slf4j.*;

/**
 * Mouse listener for the node data panel.
 * Processes right-click events to display popup menus.
 * @author laurentcohen
 */
public class NodeTreeTableMouseListener extends MouseAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(NodeTreeTableMouseListener.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Path to the cancel icon resource.
	 */
	private static final String CANCEL_ICON = "/org/jppf/ui/resources/stop.gif";
	/**
	 * Path to the restart icon resource.
	 */
	private static final String RESTART_ICON = "/org/jppf/ui/resources/restart.gif";
	/**
	 * Array of current corresponding jmx connections.
	 */
	private TopologyData[] data = null;
	/**
	 * The object that handles tollbar and menu actions.
	 */
	private JTreeTableActionHandler actionHandler = null;

	/**
	 * Initialize this mouse listener.
	 * @param actionHandler the object that handles tollbar and menu actions.
	 */
	public NodeTreeTableMouseListener(JTreeTableActionHandler actionHandler)
	{
		this.actionHandler = actionHandler;
	}

	/**
	 * Processes right-click events to display popup menus.
	 * @param event the mouse event to process.
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent event)
	{
		Component comp = event.getComponent();
		if (!(comp instanceof JPPFTreeTable)) return;
		JPPFTreeTable treeTable = (JPPFTreeTable) comp;
		JTree tree = treeTable.getTree();
		int x = event.getX();
		int y = event.getY();
		List<TopologyData> dataList = new ArrayList<TopologyData>();
		//int[] rows = tree.getSelectionRows();
		int[] rows = treeTable.getSelectedRows();
		if ((rows == null) || (rows.length == 0))
		{
			TreePath path = tree.getPathForLocation(x, y);
			if (path == null) return;
			rows = new int[] { tree.getRowForPath(path) };
		}
		for (int row: rows)
		{
			TreePath path = tree.getPathForRow(row);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
			if (!(node.getUserObject() instanceof TopologyData)) continue;
			TopologyData td = (TopologyData) node.getUserObject();
			if (TopologyDataType.NODE.equals(td.getType())) dataList.add((TopologyData) node.getUserObject());
		}
		data = dataList.toArray(new TopologyData[0]);
		
		int button = event.getButton();
		if (button == MouseEvent.BUTTON3)
		{
			JPopupMenu menu = createPopupMenu(event);
			menu.show(treeTable, x, y);
		}
	}

	/**
	 * Create the popup menu.
	 * @param event the mouse event to process.
	 * @return a <code>JPopupMenu</code> instance.
	 */
	private JPopupMenu createPopupMenu(MouseEvent event)
	{
		Component comp = event.getComponent();
		Point p = comp.getLocationOnScreen();
		JPopupMenu menu = new JPopupMenu();
		menu.add(createMenuItem(actionHandler.getAction("shutdown.restart.driver"), p));
		menu.add(createMenuItem(actionHandler.getAction("driver.reset.statistics"), p));
		menu.addSeparator();
		menu.add(createMenuItem(actionHandler.getAction("show.information"), p));
		menu.add(createMenuItem(actionHandler.getAction("update.configuration"), p));
		menu.add(createMenuItem(actionHandler.getAction("update.threads"), p));
		boolean singleSelection = data.length == 1;
		JMenu cancel = new JMenu("Cancel task");
		cancel.setIcon(GuiUtils.loadIcon(CANCEL_ICON));
		JMenu restart = new JMenu("Restart task");
		restart.setIcon(GuiUtils.loadIcon(RESTART_ICON));
		if (singleSelection)
		{
			if (data[0].getNodeState() != null)
			{
				Set<String> idSet = data[0].getNodeState().getAllTaskIds();
				if (debugEnabled) log.debug("set of ids: " + idSet);
				if (!idSet.isEmpty())
				{
					for (String id: idSet)
					{
						cancel.add(new JMenuItem(new CancelTaskAction(id, data[0])));
						restart.add(new JMenuItem(new RestartTaskAction(id, data[0])));
					}
				}
			}
		}
		if (restart.getItemCount() <= 0)
		{
			cancel.setEnabled(false);
			restart.setEnabled(false);
		}
		menu.add(cancel);
		menu.add(restart);
		menu.add(createMenuItem(actionHandler.getAction("restart.node"), p));
		menu.add(createMenuItem(actionHandler.getAction("shutdown.node"), p));
		menu.add(createMenuItem(actionHandler.getAction("reset.counter"), p));
		return menu;
	}

	/**
	 * Create a menu item.
	 * @param action the action associated with the neu item.
	 * @param location the location to use for any window create by the action.
	 * @return a <code>JMenuItem</code> instance.
	 */
	private JMenuItem createMenuItem(Action action, Point location)
	{
		if (action instanceof AbstractUpdatableAction) ((AbstractUpdatableAction) action).setLocation(location);
		return new JMenuItem(action);
	}
}
