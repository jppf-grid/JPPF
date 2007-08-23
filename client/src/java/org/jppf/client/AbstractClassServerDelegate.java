/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.DISCONNECTED;

import java.util.*;

import org.jppf.classloader.ResourceProvider;
import org.jppf.client.event.*;
import org.jppf.comm.socket.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractClassServerDelegate implements ClassServerDelegate
{
	/**
	 * The socket client uses to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Indicates whether this socket handler should be terminated and stop processing.
	 */
	protected boolean stop = false;
	/**
	 * Indicates whether this socket handler is closed, which means it can't handle requests anymore.
	 */
	protected boolean closed = false;
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();
	/**
	 * Unique identifier for this class server delegate, obtained from the local JPPF client.
	 */
	protected String appUuid = null;
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
	 * The client connection which owns this delegate.
	 */
	protected JPPFClientConnection owner = null;
	/**
	 * The name given to this delegate.
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
	 * Default instantiation of this class is not permitted.
	 */
	protected AbstractClassServerDelegate()
	{
	}

	/**
	 * Determine whether the socket connection is closed
	 * @return true if the socket connection is closed, false otherwise
	 * @see org.jppf.client.ClassServerDelegate#isClosed()
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Get the name of this delegate.
	 * @return the name as a string.
	 * @see org.jppf.client.ClassServerDelegate#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this delegate.
	 * @param name the name as a string.
	 * @see org.jppf.client.ClassServerDelegate#setName(java.lang.String)
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Initialize this delegate's resources.
	 * @throws Exception if an error is raised during initialization.
	 * @see org.jppf.client.ClassServerDelegate#initSocketClient()
	 */
	public void initSocketClient() throws Exception
	{
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}

	/**
	 * Create a socket initializer for this delegate.
	 * @return a <code>SocketInitializer</code> instance.
	 */
	protected abstract SocketInitializer createSocketInitializer();

	/**
	 * Get the status of this delegate.
	 * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.event.ClientConnectionStatusHandler#getStatus()
	 */
	public JPPFClientConnectionStatus getStatus()
	{
		return status;
	}

	/**
	 * Set the status of this delegate.
	 * @param status  a <code>JPPFClientConnectionStatus</code> enumerated value.
	 * @see org.jppf.client.event.ClientConnectionStatusHandler#setStatus(org.jppf.client.JPPFClientConnectionStatus)
	 */
	public void setStatus(JPPFClientConnectionStatus status)
	{
		this.status = status;
	}

	/**
	 * Add a connection status listener to this connection's list of listeners.
	 * @param listener the listener to add to the list.
	 * @see org.jppf.client.event.ClientConnectionStatusHandler#addClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public void addClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.add(listener);
	}

	/**
	 * Remove a connection status listener from this connection's list of listeners.
	 * @param listener the listener to remove from the list.
	 * @see org.jppf.client.event.ClientConnectionStatusHandler#removeClientConnectionStatusListener(org.jppf.client.event.ClientConnectionStatusListener)
	 */
	public void removeClientConnectionStatusListener(ClientConnectionStatusListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notify all listeners that the status of this connection has changed.
	 */
	protected synchronized void fireStatusChanged()
	{
		ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, getStatus());
		// to avoid ConcurrentModificationException
		ClientConnectionStatusListener[] array = listeners.toArray(new ClientConnectionStatusListener[0]);
		for (ClientConnectionStatusListener listener: array)
		{
			listener.statusChanged(event);
		}
	}
}
