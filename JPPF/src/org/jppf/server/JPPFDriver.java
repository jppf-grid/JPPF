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
package org.jppf.server;

import org.jppf.classloader.ClassServer;
import org.jppf.server.app.JPPFApplicationServer;
import org.jppf.server.node.JPPFNodeServer;
import org.jppf.utils.*;

/**
 * This class serves as an initializer for the entire JPPF server. It follows the singleton pattern and provides access,
 * accross the JVM, to the tasks execution queue.
 * <p>It also holds a server for incoming client connections, a server for incoming node connections, along with a class server
 * to handle requests to and from remote class loaders. 
 * @author Laurent Cohen
 */
public class JPPFDriver
{
	/**
	 * Singleton instance of the JPPFDriver.
	 */
	private static JPPFDriver instance = null;
	/**
	 * The queue that handles the tasks to execute. Objects are added to, and removed from, this queue, asynchronously and by
	 * multiple threads.
	 */
	private JPPFQueue taskQueue = null;
	/**
	 * Serves the execution requests coming from client applications.
	 */
	private JPPFApplicationServer applicationServer = null;
	/**
	 * Serves the JPPF nodes.
	 */
	private JPPFNodeServer nodeServer = null;
	/**
	 * Serves class loading requests from the JPPF nodes.
	 */
	private ClassServer classServer = null;
	
	/**
	 * Initialize and start this driver.
	 * @throws Exception if the initialization fails.
	 */
	public void run() throws Exception
	{
		taskQueue = new JPPFQueue();
		TypedProperties props = JPPFConfiguration.getProperties();

		int port = props.getInt("class.server.port", 11111);
		classServer = new ClassServer(port);
		classServer.start();

		port = props.getInt("app.server.port", 11112);
		applicationServer = new JPPFApplicationServer(port);
		applicationServer.start();

		port = props.getInt("node.server.port", 11113);
		nodeServer = new JPPFNodeServer(port);
		nodeServer.start();
	}
	
	/**
	 * Get the singleton instance of the JPPFDriver.
	 * @return a <code>JPPFDriver</code> instance.
	 */
	public static JPPFDriver getInstance()
	{
		if (instance == null) instance = new JPPFDriver();
		return instance;
	}

	/**
	 * Get the queue that handles the tasks to execute.
	 * @return a JPPFQueue instance.
	 */
	public JPPFQueue getTaskQueue()
	{
		return taskQueue;
	}
	
	/**
	 * Start the JPPF server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			getInstance().run();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
