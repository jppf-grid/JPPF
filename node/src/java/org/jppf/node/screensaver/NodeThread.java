/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.node.screensaver;

import org.jppf.*;
import org.jppf.node.*;
import org.jppf.node.event.NodeListener;

/**
 * Instances of this class encapsulate separate threads in which nodes are launched.
 * @author Laurent Cohen
 */
public class NodeThread extends Thread
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
				try
				{
					node = NodeLauncher.createNode();
					if (node != null)
					{
						node.addNodeListener(listener);
					}
					while (true)
					{
						if (node != null) node.run();
						//goToSleep();
					}
				}
				catch (JPPFNodeReloadNotification e)
				{
					//if (node != null) node.removeNodeListener(listener);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Temporarily suspend this thread.
	 */
	public synchronized void goToSleep()
	{
		try
		{
			wait();
		}
		catch(InterruptedException e)
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