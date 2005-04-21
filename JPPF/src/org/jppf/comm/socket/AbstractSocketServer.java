/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005 Laurent Cohen.
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
package org.jppf.comm.socket;

import java.io.IOException;
import java.net.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.task.ExecutionService;
import org.jppf.utils.PropertyManager;

public abstract class AbstractSocketServer extends Thread
{
	private static Logger log = Logger.getLogger(AbstractSocketServer.class);

	protected List<AbstractSocketHandler> connections = new Vector<AbstractSocketHandler>();
	/**
	 * Server socket listening for requests on the configured port.
	 */
	protected ServerSocket server = null;
	/**
	 * The execution service to which execution requests are delegated.
	 */
	protected ExecutionService execService = null;
	/**
	 * Flag indicating that this socket server is closed.
	 */
	protected boolean stop = false;
	/**
	 * The port this socket server is listening to.
	 */
	protected int port = -1;

	/**
	 * Initialize this socket server with a specified execution service and port number.
	 * @param execService the execution service to which execution requests are delegated.
	 * @param port the port this socket server is listening to.
	 * @throws Exception if the underlying server socket can't be opened.
	 */
	public AbstractSocketServer(ExecutionService execService, int port) throws Exception
	{
		this.port = port;
		this.execService = execService;
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
		AbstractSocketHandler sc = createHandler(socket);
		connections.add(sc);
		sc.start();
	}
	
	protected abstract AbstractSocketHandler createHandler(Socket socket) throws Exception;

	/**
	 * Initialize the underlying server socket with a specified port.
	 * @param port the port the underlying server listens to.
	 * @throws Exception if the server socket can't be opened on the specified port.
	 */
	protected void init(int port) throws Exception
	{
		server = new ServerSocket();
		InetSocketAddress addr = new InetSocketAddress(port);
		int size = PropertyManager.getInt("test", "receive.buffer.size", 1024*1024);
		server.setReceiveBufferSize(size);
		server.bind(addr);
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
				for (AbstractSocketHandler sc: connections) sc.setClosed();
			}
			catch(IOException ioe)
			{
				log.error(ioe.getMessage(), ioe);
			}
		}
	}
}
