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
package org.jppf.ui.monitoring.node.actions;

import java.awt.Point;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.monitoring.data.NodeInfoHolder;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class NodeThreadsAction extends JPPFAbstractNodeAction
{
	/**
	 * Determines whether the "OK" button was pressed.
	 */
	private boolean isOk = false;
	/**
	 * Panel containing the dialog for entering the number of threads and their priority.
	 */
	private OptionElement panel = null;
	/**
	 * Number of threads.
	 */
	private int nbThreads = 1;
	/**
	 * Threads priority.
	 */
	private int priority = Thread.NORM_PRIORITY;
	/**
	 * Location at which to display the entry dialog. 
	 */
	private Point location = null;

	/**
	 * Initialize this action.
	 * @param nodeInfoHolders the jmx client used to update the thread pool size.
	 * @param location location at which to display the entry dialog.
	 */
	public NodeThreadsAction(Point location, NodeInfoHolder...nodeInfoHolders)
	{
		super(nodeInfoHolders);
		this.location = location;
		setupIcon("/org/jppf/ui/resources/threads.gif");
		putValue(NAME, "Set thread pool size");
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
			panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/NodeThreadPoolPanel.xml");
			if (nodeInfoHolders.length == 1)
			{
				nbThreads = nodeInfoHolders[0].getState().getThreadPoolSize();
				priority = nodeInfoHolders[0].getState().getThreadPriority();
			}
			((AbstractOption) panel.findFirstWithName("nbThreads")).setValue(nbThreads);
			((AbstractOption) panel.findFirstWithName("threadPriority")).setValue(priority);

			JButton okBtn = (JButton) panel.findFirstWithName("/nodeThreadsOK").getUIComponent();
			JButton cancelBtn = (JButton) panel.findFirstWithName("/nodeThreadsCancel").getUIComponent();
			final JFrame frame = new JFrame("Enter the number of threads and their priority");
			frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/threads.gif").getImage());
			okBtn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					frame.setVisible(false);
					frame.dispose();
					doOK();
				}
			});
			cancelBtn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent event)
				{
					frame.setVisible(false);
					frame.dispose();
				}
			});
			frame.getContentPane().add(panel.getUIComponent());
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setLocation(location);
			frame.setVisible(true);
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
		}
	}

	/**
	 * Perform the action.
	 */
	private void doOK()
	{
		AbstractOption nbThreadsOption = (AbstractOption) panel.findFirstWithName("nbThreads");
		AbstractOption priorityOption = (AbstractOption) panel.findFirstWithName("threadPriority");
		nbThreads = (Integer) nbThreadsOption.getValue();
		priority = (Integer) priorityOption.getValue();
		Runnable r = new Runnable()
		{
			public void run()
			{
				for (NodeInfoHolder connection: nodeInfoHolders)
				{
					try
					{
						connection.getJmxClient().updateThreadPoolSize(nbThreads);
						connection.getJmxClient().updateThreadsPriority(priority);
					}
					catch(Exception e)
					{
						log.error(e.getMessage(), e);
					}
				}
			}
		};
		new Thread(r).start();
	}
}