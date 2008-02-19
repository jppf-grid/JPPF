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

import javax.swing.*;

import org.jppf.management.*;
import org.jppf.utils.StringUtils;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class NodeInformationAction implements ActionListener
{
	/**
	 * The jmx client used to update the thread pool size.
	 */
	private JMXNodeConnectionWrapper jmx = null;

	/**
	 * Initialize this action.
	 * @param jmx the jmx client used to update the thread pool size.
	 */
	public NodeInformationAction(JMXNodeConnectionWrapper jmx)
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
		String s = null;
		try
		{
			JPPFSystemInformation info = jmx.systemInformation();
			PropertiesTableFormat format = new HTMLPropertiesTableFormat("information for node " + jmx.getId());
			format.start();
			format.formatTable(info.getSystem(), "System Properties");
			format.formatTable(info.getEnv(), "Environment Variables");
			format.formatTable(info.getRuntime(), "Runtime Information");
			format.formatTable(info.getJppf(), "JPPF configuration");
			format.formatTable(info.getNetwork(), "Network configuration");
			format.end();
			s = format.getText();
		}
		catch(Exception e)
		{
			s = StringUtils.getStackTrace(e);
		}
		final JFrame frame = new JFrame("Node System Information");
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				frame.dispose();
			}
		});
		JEditorPane textArea = new JEditorPane("text/html", s);
		textArea.setEditable(false);
		textArea.setOpaque(false);
		frame.getContentPane().add(new JScrollPane(textArea));
		frame.setSize(400, 400);
		frame.setVisible(true);
	}
}