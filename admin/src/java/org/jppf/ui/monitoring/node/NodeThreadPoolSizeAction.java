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

import java.awt.event.*;

import javax.swing.JOptionPane;

import org.jppf.management.JMXNodeConnectionWrapper;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class NodeThreadPoolSizeAction implements ActionListener
{
	/**
	 * The jmx client used to update the thread pool size.
	 */
	private JMXNodeConnectionWrapper jmx = null;
	/**
	 * Initialize this action.
	 * @param jmx the jmx client used to update the thread pool size.
	 */
	public NodeThreadPoolSizeAction(JMXNodeConnectionWrapper jmx)
	{
		this.jmx = jmx;
	}

	/**
	 * Perform the action.
	 * @param event not used.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event)
	{
		try
		{
			String s = JOptionPane.showInputDialog(null, "Enter the number of threads",
				"Enter the number of threads", JOptionPane.PLAIN_MESSAGE);
			if ((s == null) || ("".equals(s.trim()))) return;
			try
			{
				int n = Integer.valueOf(s);
				jmx.updateThreadPoolSize(n);
			}
			catch(NumberFormatException ignored)
			{
			}
		}
		catch(Exception ignored)
		{
		}
	}
}