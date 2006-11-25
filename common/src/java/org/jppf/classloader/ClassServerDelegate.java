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
package org.jppf.classloader;

import org.apache.log4j.Logger;
import org.jppf.comm.socket.*;
import org.jppf.node.JPPFResourceWrapper;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents (or bytecode) to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. They enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class ClassServerDelegate extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ClassServerDelegate.class);
	/**
	 * Determines whether the debug level is enabled in the log4j configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
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
	private String appUuid = null;
	/**
	 * Used to synchronize access to the underlying socket from multiple threads.
	 */
	private SocketInitializer socketInitializer = new SocketInitializer();
	/**
	 * The name or IP address of the host the class server is running on.
	 */
	private String host = null;
	/**
	 * The TCP port the class server is listening to.
	 */
	private int port = -1;

	/**
	 * Default instantiation of this class is not permitted.
	 */
	private ClassServerDelegate()
	{
	}

	/**
	 * Initialize class server delegate with a spceified application uuid.
	 * @param uuid the unique identifier for the local JPPF client.
	 * @param host the name or IP address of the host the class server is running on.
	 * @param port the TCP port the class server is listening to.
	 * @throws Exception if the connection could not be opended.
	 */
	public ClassServerDelegate(String uuid, String host, int port) throws Exception
	{
		this.appUuid = uuid;
		this.host = host;
		this.port = port;
		init();
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public final void init() throws Exception
	{
		if (socketClient == null) initSocketClient();
		System.out.println("ClassServerDelegate.init(): Attempting connection to the class server");
		socketInitializer.initializeSocket(socketClient);
		System.out.println("ClassServerDelegate.init(): Reconnected to the class server");
	}

	/**
	 * Initialize this node's resources.
	 * @throws Exception if an error is raised during initialization.
	 */
	public void initSocketClient() throws Exception
	{
		socketClient = new SocketClient();
		socketClient.setHost(host);
		socketClient.setPort(port);
	}

	/**
	 * Main processing loop for this thread.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			JPPFResourceWrapper resource = new JPPFResourceWrapper();
			resource.setState(JPPFResourceWrapper.State.PROVIDER_INITIATION);
			resource.addUuid(appUuid);
			socketClient.send(resource);
			while (!stop)
			{
				try
				{
					resource = (JPPFResourceWrapper) socketClient.receive();
					String name = resource.getName();
					if  (debugEnabled) log.debug("resource requested:" + name);
					byte[] b = resourceProvider.getResourceAsBytes(name);
					if (b == null) b = new byte[0];
					resource.setState(JPPFResourceWrapper.State.PROVIDER_RESPONSE);
					resource.setDefinition(b);
					socketClient.send(resource);
					if  (debugEnabled) log.debug("sent resource " + name + " (" + b.length + " bytes)");
				}
				catch(Exception e)
				{
					log.warn("caught " + e + ", will re-initialise ...", e);
					init();
				}
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
			setClosed();
		}
	}

	/**
	 * Set the stop flag to true, indicating that this socket handler should be closed as
	 * soon as possible.
	 */
	private void setStopped()
	{
		stop = true;
	}

	/**
	 * Determine whether the socket connection is closed
	 * @return true if the socket connection is closed, false otherwise
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Set the closed state of the socket connection to true. This will cause this socket handler
	 * to terminate as soon as the current request execution is complete.
	 */
	public void setClosed()
	{
		setStopped();
		close();
	}

	/**
	 * Close the socket connection.
	 */
	public void close()
	{
		try
		{
			socketClient.close();
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		closed = true;
	}
}
