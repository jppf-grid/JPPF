/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.classloader;

import java.io.IOException;
import java.net.*;
import java.util.*;
import org.jppf.JPPFException;
import org.jppf.utils.*;

/**
 * This class is a wrapper around a server socket, listenening to incoming connections to and from nodes and client
 * applications. The connections are created as independant threads, so that requests from remote class loaders
 * are processed asynchronously.
 * @author Laurent Cohen
 */
public class ClassServer extends Thread
{
	/**
	 * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
	 * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine
	 * in which application classpath to look for the requested resources.
	 */
	protected Map<String, ClassServerConnection> providerConnections = new Hashtable<String, ClassServerConnection>();
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
	 * Initialize this socket server with a specified port number.
	 * @param port the port this socket server is listening to.
	 * @throws JPPFBootstrapException if the underlying server socket can't be opened.
	 */
	public ClassServer(int port) throws JPPFBootstrapException
	{
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
			while (!stop)
			{
				Socket socket = server.accept();
				serve(socket);
			}
			end();
		}
		catch (Throwable t)
		{
			t.printStackTrace();
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
		ClassServerConnection handler = createConnection(socket);
		handler.start();
	}
	
	/**
	 * Instantiate a wrapper for the socket connection opened by this socket server.
	 * Subclasses must implement this method.
	 * @param socket the socket connection obtained through a call to
	 * {@link java.net.ServerSocket#accept() ServerSocket.accept()}.
	 * @return a <code>JPPFServerConnection</code> instance.
	 * @throws JPPFException if an exception is raised while creating the socket handler.
	 */
	protected ClassServerConnection createConnection(Socket socket) throws JPPFException
	{
		return new ClassServerConnection(this, socket);
	}

	/**
	 * Initialize the underlying server socket with a specified port.
	 * @param port the port the underlying server listens to.
	 * @throws JPPFBootstrapException if the server socket can't be opened on the specified port.
	 */
	protected void init(int port) throws JPPFBootstrapException
	{
		Exception e = null;
		try
		{
			server = new ServerSocket();
			InetSocketAddress addr = new InetSocketAddress(port);
			int size = 32*1024;
			server.setReceiveBufferSize(size);
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
			throw new JPPFBootstrapException(e.getMessage(), e);
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
				server.close();
				for (ClassServerConnection sc: providerConnections.values()) sc.setClosed();
				providerConnections.clear();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
		}
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

	/**
	 * Get the list of active resource provider connections from this class server.
	 * @return a list of <code>JPPFServerConnection</code> instances.
	 */
	public Map<String, ClassServerConnection> getProviderConnections()
	{
		return providerConnections;
	}
}
