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

import java.awt.Component;
import java.awt.event.*;
import java.util.Set;

import javax.swing.*;
import javax.swing.tree.TreePath;

import org.apache.commons.logging.*;
import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.ui.monitoring.data.NodeInfoHolder;
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
	 * Path to the node threads icon resource.
	 */
	private static final String THREADS_ICON = "/org/jppf/ui/resources/threads.gif";
	/**
	 * Path to the node system information icon resource.
	 */
	private static final String INFO_ICON = "/org/jppf/ui/resources/info.gif";
	/**
	 * Path to the cancel icon resource.
	 */
	private static final String CANCEL_ICON = "/org/jppf/ui/resources/stop.gif";
	/**
	 * Path to the restart icon resource.
	 */
	private static final String RESTART_ICON = "/org/jppf/ui/resources/restart.gif";
	/**
	 * Path to the restart node icon resource.
	 */
	private static final String RESTART_NODE_ICON = "/org/jppf/ui/resources/traffic_light_red_green.gif";
	/**
	 * Path to the shutdown node icon resource.
	 */
	private static final String SHUTDOWN_NODE_ICON = "/org/jppf/ui/resources/traffic_light_red.gif";
	/**
	 * Path to the reset tasks counter icon resource.
	 */
	private static final String RESET_ICON = "/org/jppf/ui/resources/reset.gif";

	/**
	 * Processes right-click events to display popup menus.
	 * @param event the mouse event to process
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	public void mousePressed(MouseEvent event)
	{
		Component comp = event.getComponent();
		if (!(comp instanceof JXTreeTable)) return;

		JXTreeTable treeTable = (JXTreeTable) comp;
		int x = event.getX();
		int y = event.getY();
		TreePath path = treeTable.getPathForLocation(x, y);
		if (path == null) return;

		DefaultMutableTreeTableNode node = (DefaultMutableTreeTableNode) path.getLastPathComponent();
		if (!(node.getUserObject() instanceof NodeInfoHolder)) return;
		NodeInfoHolder infoHolder = (NodeInfoHolder) node.getUserObject();
		int button = event.getButton();
		if (button == MouseEvent.BUTTON3)
		{
			Set<String> idSet = infoHolder.getState().getAllTaskIds();
			log.info("set of ids: " + idSet);
			final JMXNodeConnectionWrapper jmx = infoHolder.getJmxClient();
			JPopupMenu menu = new JPopupMenu();
			JMenuItem item = new JMenuItem("Node System Information");
			item.addActionListener(new NodeInformationAction(jmx));
			item.setIcon(GuiUtils.loadIcon(INFO_ICON));
			menu.add(item);
			item = new JMenuItem("Set thread pool size");
			item.addActionListener(new NodeThreadPoolSizeAction(jmx));
			item.setIcon(GuiUtils.loadIcon(THREADS_ICON));
			menu.add(item);
			if (!idSet.isEmpty())
			{
				JMenu cancel = new JMenu("Cancel");
				cancel.setIcon(GuiUtils.loadIcon(CANCEL_ICON));
				JMenu restart = new JMenu("Restart");
				restart.setIcon(GuiUtils.loadIcon(RESTART_ICON));
				for (String id: idSet)
				{
					cancel.add(createTaskMenuItem(id, true, jmx));
					restart.add(createTaskMenuItem(id, false, jmx));
				}
				menu.add(cancel);
				menu.add(restart);
			}
			//JMenu nodeMenu = new JMenu("Node");
			menu.add(createNodeMenuItem("Node Restart", false, jmx));
			menu.add(createNodeMenuItem("Node Shutdown", true, jmx));

			item = new JMenuItem("Reset task counter", GuiUtils.loadIcon(RESET_ICON));
			item.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					try
					{
						jmx.resetTaskCounter();
					}
					catch(Exception ignored)
					{
					}
				}
			});
			menu.add(item);

			//menu.add(nodeMenu);
			menu.show(treeTable, x, y);
		}
	}

	/**
	 * Create a menu item for the specified text and type of action.
	 * @param label the id of the task to whihc the menu item applies.
	 * @param isCancel cancel action if true, restart action otherwise.
	 * @param jmx the JMX ocnnection to the corresponding node..
	 * @return a <code>JMenuItem</code> instance.
	 */
	private JMenuItem createTaskMenuItem(final String label, boolean isCancel, final JMXNodeConnectionWrapper jmx)
	{
		Icon icon = GuiUtils.loadIcon(isCancel ? CANCEL_ICON : RESTART_ICON);
		String text = "Task id " + label;
		JMenuItem item = new JMenuItem(text, icon);
		if (isCancel) item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					jmx.cancelTask(label);
				}
				catch(Exception ignored)
				{
				}
			}
		});
		else item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					jmx.restartTask(label);
				}
				catch(Exception ignored)
				{
				}
			}
		});
		return item;
	}


	/**
	 * Create a menu item for the specified text and type of action.
	 * @param label the id of the task to whihc the menu item applies.
	 * @param isShutdown cancel action if true, restart action otherwise.
	 * @param jmx the JMX ocnnection to the corresponding node..
	 * @return a <code>JMenuItem</code> instance.
	 */
	private JMenuItem createNodeMenuItem(final String label, boolean isShutdown, final JMXNodeConnectionWrapper jmx)
	{
		Icon icon = GuiUtils.loadIcon(isShutdown ? SHUTDOWN_NODE_ICON : RESTART_NODE_ICON);
		JMenuItem item = new JMenuItem(label, icon);
		if (isShutdown) item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					jmx.shutdown();
				}
				catch(Exception ignored)
				{
				}
			}
		});
		else item.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				try
				{
					jmx.restart();
				}
				catch(Exception ignored)
				{
				}
			}
		});
		return item;
	}
}
