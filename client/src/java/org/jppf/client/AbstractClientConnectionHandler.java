/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.DISCONNECTED;

import java.util.*;

import org.jppf.client.event.*;
import org.jppf.comm.socket.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractClientConnectionHandler implements ClientConnectionHandler
{
	/**
	 * The socket client uses to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	protected SocketInitializer socketInitializer = createSocketInitializer();
	/**
	 * The name or IP address of the host the class server is running on.
	 */
	protected String host = null;
	/**
	 * The TCP port the class server is listening to.
	 */
	protected int port = -1;
	/**
	 * The client connection which owns this connection handler.
	 */
	protected JPPFClientConnection owner = null;
	/**
	 * The name given to this connection handler.
	 */
	protected String name = null;
	/**
	 * Status of the connection.
	 */
	protected JPPFClientConnectionStatus status = DISCONNECTED;
	/**
	 * List of status listeners for this connection.
	 */
	protected List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();

	/**
	 * Initialize this connection with the specified owner.
	 * @param owner the client connection which owns this connection handler.
	 */
	protected AbstractClientConnectionHandler(JPPFClientConnection owner)
	{
		this.owner = owner;
		if (owner != null) this.name = owner.getName();
	}

	/**
	 * Get the status of this connection.
	 * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.ClientConnectionHandler#getStatus()
	 */
	public synchronized JPPFClientConnectionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this connection.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.ClientConnectionHandler#setStatus(org.jppf.client.JPPFClientConnectionStatus)
	 */
	public synchronized void setStatus(JPPFClientConnectionStatus status)
	{
		this.status = status;
		fireStatusChanged();
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 * @see org.jppf.client.ClientConnectionHandler#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
	 * @see org.jppf.client.ClientConnectionHandler#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public void removeClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Create a socket initializer for this connection handler.
	 * @return a <code>SocketInitializer</code> instance.
	 */
	protected abstract SocketInitializer createSocketInitializer();

	/**
	 * Notify all listeners that the status of this connection has changed.
	 */
	protected synchronized void fireStatusChanged()
	{
		ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this);
		// to avoid ConcurrentModificationException
		ClientConnectionStatusListener[] array = listeners.toArray(new ClientConnectionStatusListener[0]);
		for (ClientConnectionStatusListener listener: array)
		{
			listener.statusChanged(event);
		}
	}

	/**
	 * Get the socket client uses to communicate over a socket connection.
	 * @return a <code>SocketWrapper</code> instance.
	 */
	public SocketWrapper getSocketClient()
	{
		return socketClient;
	}
}
