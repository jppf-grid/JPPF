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
package org.jppf.node.screensaver;

import org.jppf.node.*;
import org.jppf.node.event.NodeListener;

/**
 * Instances of this class encapsulate separate threads in which nodes are launched.
 * @author Laurent Cohen
 */
class NodeThread extends Thread
{
	/**
	 * Reference to the underlying JPPF node.
	 */
	private MonitoredNode node = null;
	/**
	 * Receives event notifications from the node.
	 */
	private NodeListener listener = null;

	/**
	 * Initialize this node thread with a specified listener.
	 * @param listener receives notifications of events occurring within the node. 
	 */
	public NodeThread(NodeListener listener)
	{
		super("NodeThread thread");
		this.listener = listener;
	}

	/**
	 * Launch a JPPF node.
	 * @see java.lang.Thread#run()
	 */
	public void run()
	{
		try
		{
			while (true)
			{
				//try
				{
					node = NodeRunner.createNode();
					node.addNodeListener(listener);
					while (true)
					{
						node.run();
						//goToSleep();
					}
				}
				/*
				catch (JPPFNodeReloadNotification e)
				{
					if (node != null) node.removeNodeListener(listener);
				}
				*/
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Wakeup this thread.
	 */
	public synchronized void startNode()
	{
		notify();
	}

	/**
	 * Stop the underlying node.
	 */
	public void stopNode()
	{
		if (node != null) node.stopNode(true);
	}

	/**
	 * Get a reference to the underlying JPPF node.
	 * @return a <code>MonitoredNode</code> instance.
	 */
	public MonitoredNode getNode()
	{
		return node;
	}
}
