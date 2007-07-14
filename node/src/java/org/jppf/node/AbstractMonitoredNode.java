/*
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
	protected SocketInitializer socketInitializer = new SocketInitializerImpl();
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
