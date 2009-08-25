/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.ui.utils.GuiUtils;

/**
 * Renderer used to render the tree nodes in the node data panel.
 * @author Laurent Cohen
 */
public class NodeRenderer extends DefaultTreeCellRenderer
{
	/**
	 * Path to the location of the icon files.
	 */
	private static final String RESOURCES = "/org/jppf/ui/resources/";
	/**
	 * Path to the icon used for a driver.
	 */
	private static final String DRIVER_ICON = RESOURCES + "mainframe.gif";
	/**
	 * Path to the icon used for an inactive driver connection.
	 */
	private static final String DRIVER_INACTIVE_ICON = RESOURCES + "mainframe_inactive.gif";
	/**
	 * Path to the icon used for a node.
	 */
	private static final String NODE_ICON = RESOURCES + "buggi_server.gif";

	/**
   * Configures the renderer based on the passed in components.
	 * @param tree - the tree of which to apply this renderer.
	 * @param value - the node to render. 
	 * @param sel - determines whether the node is selected.
	 * @param expanded - determines whether the node is expanded.
	 * @param leaf - determines whether the node is a leaf.
	 * @param row - the node's row number. 
	 * @param hasFocus - determines whether the node has the focus. 
	 * @return a component used to paint the node.
	 * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
	 */
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
			int row, boolean hasFocus)
	{
		DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		if (value instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
			if (!node.isRoot())
			{
				TopologyData data = (TopologyData) node.getUserObject();
				String path = null;
				Color background = Color.WHITE;
				switch(data.getType())
				{
					case DRIVER:
						if (JPPFClientConnectionStatus.ACTIVE.equals(data.getClientConnection().getStatus()))
						{
							path = DRIVER_ICON;
							background = Color.GREEN;
						}
						else
						{
							path = DRIVER_INACTIVE_ICON;
							background = Color.RED;
						}
						break;
					case NODE:
						path = NODE_ICON;
						break;
				}
				ImageIcon icon = GuiUtils.loadIcon(path);
				renderer.setIcon(icon);
				renderer.setBackgroundNonSelectionColor(background);
			}
		}
		return renderer;
	}
}
