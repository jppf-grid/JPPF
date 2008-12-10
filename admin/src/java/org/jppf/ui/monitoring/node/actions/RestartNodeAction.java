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

import java.awt.event.ActionEvent;

import org.jppf.ui.monitoring.data.NodeInfoHolder;

/**
 * This action restarts a node.
 */
public class RestartNodeAction extends JPPFAbstractNodeAction
{
	/**
	 * Initialize this action.
	 * @param nodeInfoHolders the jmx client used to update the thread pool size.
	 */
	public RestartNodeAction(NodeInfoHolder...nodeInfoHolders)
	{
		super(nodeInfoHolders);
		setupIcon("/org/jppf/ui/resources/traffic_light_red_green.gif");
		putValue(NAME, "Node Restart");
		if (nodeInfoHolders.length < 1) setEnabled(false);
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
			for (NodeInfoHolder connection: nodeInfoHolders)
			{
				try
				{
					Runnable r = new Runnable()
					{
						public void run()
						{
							for (NodeInfoHolder connection: nodeInfoHolders)
							{
								try
								{
									connection.getJmxClient().restart();
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
				catch(Exception e)
				{
					log.error(e.getMessage(), e);
				}
			}
		}
		catch(NumberFormatException ignored)
		{
		}
	}
}