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
package org.jppf.classloader;

import java.io.IOException;
import java.net.ConnectException;
import org.apache.log4j.Logger;
import org.jppf.comm.socket.SocketClient;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. The enable remote nodes
 * to dynamically load classes from the JVM that run's the class server.
 * @author Laurent Cohen
 */
public class ClassServerDelegate extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ClassServerDelegate.class);
	/**
	 * The socket client uses to communicate over a socket connection.
	 */
	protected SocketClient socketClient = null;
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
	 * Initialize this connection with an open socket connection to a remote client.
	 * @param host the host name of the class server.
	 * @param port the port the class server is listening to.
	 * @throws ConnectException if the connection could not be opended.
	 * @throws IOException if the connection could not be opended.
	 */
	public ClassServerDelegate(String host, int port) throws ConnectException, IOException
	{
		socketClient = new SocketClient(host, port);
	}

	/**
	 * Main processing loop for this socket handler. During each loop iteration,
	 * the following operations are performed:
	 * <ol>
	 * <li>if the stop flag is set to true, exit the loop</li>
	 * <li>block until a class name is received</li>
	 * <li>when a class name is received, read the class file into a byte array from the classpath</li>
	 * <li>send back the byte array defining the class to load remotely</li>
	 * </ol>
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			socketClient.send("provider");
			while (!stop)
			{
				String name = (String) socketClient.receive();
				byte[] b = resourceProvider.getResourceAsBytes(name);
				socketClient.send(b);
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
	private synchronized void setStopped()
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
