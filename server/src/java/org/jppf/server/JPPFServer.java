/*
 * Java Parallel Processing Framework.
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
package org.jppf.server;

import java.io.IOException;
import java.net.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.comm.socket.SocketWrapper;

/**
 * This class is a common abstract superclass for servers listening to incoming connections from
 * execution nodes or client connections, and whose role is to handle execution requests.
 * @author Laurent Cohen
 */
public abstract class JPPFServer extends Thread
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFServer.class);
	/**
	 * Server socket listening for requests on the configured port.
	 */
	protected ServerSocket server = null;
	/**
	 * Flag indicating that this socket server is closed.
	 */
	private boolean stopped = false;
	/**
	 * The port this socket server is listening to.
	 */
	protected int port = -1;
	/**
	 * The list of connections accepted by this server.
	 */
	protected List<JPPFConnection> connections = new ArrayList<JPPFConnection>();

	/**
	 * Initialize this socket server with a specified execution service and port number.
	 * @param port the port this socket server is listening to.
	 * @param name the name given to the thread in which this server runs.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public JPPFServer(int port, String name) throws JPPFException
	{
		super(name);
		this.port = port;
		init(port);
	}
	
	/**
	 * Start the underlying server socket by making it accept incoming connections.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			while (!isStopped() && !JPPFDriver.getInstance().isShuttingDown())
			{
				Socket socket = server.accept();
				if (JPPFDriver.getInstance().isShuttingDown())
				{
					socket.close();
					break;
				}
				serve(socket);
			}
			end();
		}
		catch (Throwable t)
		{
			log.error(t.getMessage(), t);
			end();
		}
	}
	
	/**
	 * Start serving a new incoming connection.
	 * @param socket the socket connecting with this socket server.
	 * @throws Exception if the new connection can't be initialized.
	 */
	protected void serve(Socket socket) throws Exception
	{
		socket.setSendBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
		JPPFConnection connection = createConnection(socket);
		connections.add(connection);
		connection.start();
	}
	
	/**
	 * Instanciate a wrapper for the socket connection opened by this socket server.
	 * Subclasses must implement this method.
	 * @param socket the socket connection obtained through a call to
	 * {@link java.net.ServerSocket#accept() ServerSocket.accept()}.
	 * @return a <code>JPPFServerConnection</code> instance.
	 * @throws JPPFException if an exception is raised while creating the socket handler.
	 */
	protected abstract JPPFConnection createConnection(Socket socket) throws JPPFException;

	/**
	 * Initialize the underlying server socket with a specified port.
	 * @param port the port the underlying server listens to.
	 * @throws JPPFException if the server socket can't be opened on the specified port.
	 */
	protected void init(int port) throws JPPFException
	{
		Exception e = null;
		try
		{
			server = new ServerSocket();
			InetSocketAddress addr = new InetSocketAddress(port);
			server.setReceiveBufferSize(SocketWrapper.SOCKET_RECEIVE_BUFFER_SIZE);
			server.bind(addr);
		}
		catch(IllegalArgumentException iae)
		{
			e = iae;
		}
		catch(IOException ioe)
		{
			e = ioe;
		}
		if (e != null)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Close the underlying server socket and stop this socket server.
	 */
	public synchronized void end()
	{
		if (!isStopped())
		{
			try
			{
				setStopped(true);
				if (!server.isClosed()) server.close();
				removeAllConnections();
			}
			catch(IOException ioe)
			{
				log.error(ioe.getMessage(), ioe);
			}
		}
	}

	/**
	 * Remove the specified connection from the list of active connections of this server.
	 * @param connection the connection to remove.
	 */
	public synchronized void removeConnection(JPPFConnection connection)
	{
		connections.remove(connection);
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!isStopped()) return;
		for (JPPFConnection connection: connections)
		{
			try
			{
				connection.setClosed();
			}
			catch(Exception e)
			{
				log.error("["+connection.toString()+"] "+e.getMessage(), e);
			}
		}
		connections.clear();
	}

	/**
	 * Set this server in the specified stopped state.
	 * @param stopped true if this server is stopped, false otherwise.
	 */
	protected synchronized void setStopped(boolean stopped)
	{
		this.stopped = stopped;
	}

	/**
	 * Get the stopped state of this server.
	 * @return  true if this server is stopped, false otherwise.
	 */
	protected synchronized boolean isStopped()
	{
		return stopped;
	}
}
