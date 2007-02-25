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

package org.jppf.node;

import java.util.*;

import org.jppf.comm.socket.*;
import org.jppf.node.event.*;
import org.jppf.node.event.NodeEvent.EventType;
import org.jppf.utils.SerializationHelper;

/**
 * Abstract implementation of the <code>MonitoredNode</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractMonitoredNode implements MonitoredNode
{
	/**
	 * Utility for deserialization and serialization.
	 */
	protected SerializationHelper helper = null;
	/**
	 * Wrapper around the underlying server connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	protected SocketInitializer socketInitializer = new SocketInitializer();
	/**
	 * The list of listeners that receive notifications from this node.
	 */
	protected List<NodeListener> listeners = new ArrayList<NodeListener>();
	/**
	 * This flag is true if there is at least one listener, and false otherwise.
	 */
	protected boolean notifying = false;
	/**
	 * Used to programmatically stop this node.
	 */
	protected boolean stopped = false;
	/**
	 * Total number of tasks executed.
	 */
	protected int taskCount = 0;
	/**
	 * This node's universal identifier.
	 */
	protected String uuid = null;

	/**
	 * Add a listener to the list of listener for this node.
	 * @param listener the listener to add.
	 * @see org.jppf.node.MonitoredNode#addNodeListener(org.jppf.node.event.NodeListener)
	 */
	public void addNodeListener(NodeListener listener)
	{
		if (listener == null) return;
		listeners.add(listener);
		notifying = true;
	}

	/**
	 * Remove a listener from the list of listener for this node.
	 * @param listener the listener to remove.
	 * @see org.jppf.node.MonitoredNode#removeNodeListener(org.jppf.node.event.NodeListener)
	 */
	public void removeNodeListener(NodeListener listener)
	{
		if (listener == null) return;
		listeners.remove(listener);
		if (listeners.size() <= 0) notifying = false;
	}

	/**
	 * Notify all listeners that an event has occurred.
	 * @param eventType the type of the event as an enumerated value.
	 * @see org.jppf.node.MonitoredNode#fireNodeEvent(org.jppf.node.event.NodeEvent.EventType)
	 */
	public void fireNodeEvent(EventType eventType)
	{
		NodeEvent event = new NodeEvent(eventType);
		for (NodeListener listener : listeners) listener.eventOccurred(event);
	}

	/**
	 * Get the underlying socket wrapper used by this node.
	 * @return a <code>SocketWrapper</code> instance.
	 */
	public SocketWrapper getSocketWrapper()
	{
		return socketClient;
	}

	/**
	 * Get the underlying socket wrapper used by this node.
	 * @param wrapper a <code>SocketWrapper</code> instance.
	 */
	public void setSocketWrapper(SocketWrapper wrapper)
	{
		this.socketClient = wrapper;
	}

	/**
	 * Determine whether this node has at least one listener to notify of internal events.
	 * @return true if there is at least one listener, and false otherwise.
	 */
	public boolean isNotifying()
	{
		return notifying;
	}

	/**
	 * Stop this node and release the resources it is using.
	 * @param closeSocket determines whether the underlying socket should be closed.
	 * @see org.jppf.node.MonitoredNode#stopNode(boolean)
	 */
	public abstract void stopNode(boolean closeSocket);
}
