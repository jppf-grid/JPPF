/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
import javax.swing.tree.TreePath;

import org.apache.commons.logging.*;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.ui.monitoring.data.NodeInfoHolder;
import org.jppf.ui.monitoring.node.actions.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * Mouse listener for the node data panel.
 * Processes right-click events to display popup menus.
 * @author laurentcohen
 */
public class TreeTableMouseListener extends MouseAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(TreeTableMouseListener.class);
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
	 * List of current node info holders.
	 */
	private List<NodeInfoHolder> infoHolderList = null;
	/**
	 * Array of current corresponding jmx connections.
	 */
	private NodeInfoHolder[] infoHolders = null;

	/**
	 * Processes right-click events to display popup menus.
	 * @param event the mouse event to process.
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent event)
	{
		Component comp = event.getComponent();
		if (!(comp instanceof JXTreeTable)) return;
		JXTreeTable treeTable = (JXTreeTable) comp;
		int x = event.getX();
		int y = event.getY();
		List<NodeInfoHolder> infoHolderList = new ArrayList<NodeInfoHolder>();
		int[] rows = treeTable.getSelectedRows();
		if ((rows == null) || (rows.length == 0))
		{
			TreePath path = treeTable.getPathForLocation(x, y);
			if (path == null) return;
			rows = new int[] { treeTable.getRowForPath(path) };
		}
		for (int row: rows)
		{
			TreePath path = treeTable.getPathForRow(row);
			DefaultMutableTreeTableNode node = (DefaultMutableTreeTableNode) path.getLastPathComponent();
			if (!(node.getUserObject() instanceof NodeInfoHolder)) continue;
			infoHolderList.add((NodeInfoHolder) node.getUserObject());
		}
		infoHolders = infoHolderList.toArray(new NodeInfoHolder[0]);
		
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
		JPopupMenu menu = new JPopupMenu();
		menu.add(new JMenuItem(new NodeInformationAction(infoHolders)));
		Component comp = event.getComponent();
		Point p = comp.getLocationOnScreen();
		menu.add(new JMenuItem(new NodeThreadsAction(new Point(p.x + event.getX(), p.y + event.getY()), infoHolders)));
		boolean singleSelection = infoHolders.length == 1;
		JMenu cancel = new JMenu("Cancel task");
		cancel.setIcon(GuiUtils.loadIcon(CANCEL_ICON));
		JMenu restart = new JMenu("Restart task");
		restart.setIcon(GuiUtils.loadIcon(RESTART_ICON));
		if (singleSelection)
		{
			JMXNodeConnectionWrapper connection = infoHolders[0].getJmxClient();
			Set<String> idSet = infoHolders[0].getState().getAllTaskIds();
			log.info("set of ids: " + idSet);
			if (!idSet.isEmpty())
			{
				for (String id: idSet)
				{
					cancel.add(new JMenuItem(new CancelTaskAction(id, infoHolders[0])));
					restart.add(new JMenuItem(new RestartTaskAction(id, infoHolders[0])));
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
		menu.add(new JMenuItem(new RestartNodeAction(infoHolders)));
		menu.add(new JMenuItem(new ShutdownNodeAction(infoHolders)));
		menu.add(new JMenuItem(new ResetTaskCounterAction(infoHolders)));
		return menu;
	}
}
