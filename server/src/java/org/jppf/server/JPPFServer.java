/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.server;

import java.io.IOException;
import java.net.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.classloader.ClassServer;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.utils.*;

/**
 * This class is a common abstract superclass for servers listening to incoming connections from
 * execution nodes or client connections, and whose role is to handle execution requests.
 * @author Laurent Cohen
 */
public abstract class JPPFServer extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFServer.class);
	/**
	 * Server socket listening for requests on the configured port.
	 */
	protected ServerSocket server = null;
	/**
	 * Flag indicating that this socket server is closed.
	 */
	protected boolean stop = false;
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
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public JPPFServer(int port,String name) throws JPPFException
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
			while (!stop && !JPPFDriver.getInstance().isShuttingDown())
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
	 * @throws JPPFException if the new connection can't be initialized.
	 */
	protected void serve(Socket socket) throws JPPFException
	{
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
		if (!stop)
		{
			try
			{
				stop = true;
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
	public void removeConnection(JPPFConnection connection)
	{
		connections.remove(connection);
	}

	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!stop) return;
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
	 * Start this class server from the command line.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			TypedProperties props = JPPFConfiguration.getProperties();
			new ClassServer(props.getInt("class.server.port", 11111)).start();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}
}
