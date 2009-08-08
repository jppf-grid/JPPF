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

package org.jppf.ui.monitoring.job;

import java.awt.Component;

import javax.swing.*;
import javax.swing.tree.*;

import org.jppf.ui.utils.GuiUtils;

/**
 * Renderer used to render the tree nodes in the job data panel.
 * @author Laurent Cohen
 */
public class JobNodeRenderer extends DefaultTreeCellRenderer
{
	/**
	 * Path to the icon used for a driver.
	 */
	private static final String DRIVER_ICON = "/org/jppf/ui/resources/mainframe_icon_32.gif";
	/**
	 * Path to the icon used for a job.
	 */
	private static final String JOB_ICON = "/org/jppf/ui/resources/rack_icon_32.gif";
	/**
	 * Path to the icon used for a node.
	 */
	private static final String NODE_ICON = "/org/jppf/ui/resources/buggi_server_icon_32.gif";

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
				JobData data = (JobData) node.getUserObject();
				String path = null;
				switch(data.getType())
				{
					case DRIVER:
						path = DRIVER_ICON;
						break;
					case JOB:
						path = JOB_ICON;
						break;
					case SUB_JOB:
						path = NODE_ICON;
						break;
				}
				ImageIcon icon = GuiUtils.loadIcon(path);
				renderer.setIcon(icon);
			}
		}
		return renderer;
	}
}
