/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package org.jppf.server;

import java.net.Socket;
import org.apache.commons.logging.*;
import org.jppf.JPPFException;
import org.jppf.classloader.ResourceProvider;
import org.jppf.comm.socket.*;

/**
 * Wrapper around an incoming socket connection, whose role is to receive the names of classes
 * to load from the classpath, then send the class files' contents to the remote client.
 * <p>Instances of this class are part of the JPPF dynamic class loading mechanism. The enable remote nodes
 * to dynamically load classes from the JVM that runs the class server.
 * @author Laurent Cohen
 */
public abstract class JPPFConnection extends Thread
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(JPPFConnection.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The socket client used to communicate over a socket connection.
	 */
	protected SocketWrapper socketClient = null;
	/**
	 * Indicates whether this socket handler should be terminated and stop processing.
	 */
	private boolean stopped = false;
	/**
	 * Indicates whether this socket handler is closed, which means it can't handle requests anymore.
	 */
	protected boolean closed = false;
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();
	/**
	 * The server that created this connection.
	 */
	protected JPPFServer server = null;

	/**
	 * Initialize this connection with an open socket connection to a remote client.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
	 * @param server the class server that created this connection.
	 * @throws JPPFException if this socket handler can't be initialized.
	 */
	public JPPFConnection(JPPFServer server, Socket socket) throws JPPFException
	{
		this.server = server;
		socketClient = new SocketClient(socket);
	}
	
	/**
	 * Main processing loop for this socket handler. During each loop iteration,
	 * the following operations are performed:
	 * <ol>
	 * <li>if the stop flag is set to true, exit the loop</li>
	 * <li>block until an execution request is received</li>
	 * <li>when a request is received, dispatch it to the execution queue</li>
	 * <li>wait until the execution is complete</li>
	 * <li>send the execution result back to the client application</li>
	 * </ol>
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			while (!isStopped())
			{
				perform();
			}
		}
		catch (Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.warn(e);
			setClosed();
			server.removeConnection(this);
		}
	}

	/**
	 * Execute this thread's main action.
	 * @throws Exception if the execution failed.
	 */
	public abstract void perform() throws Exception;

	/**
	 * Set the stop flag to true, indicating that this socket handler should be closed as
	 * soon as possible.
	 */
	public synchronized void setStopped()
	{
		stopped = true;
	}

	/**
	 * Determine whether the socket connection is closed
	 * @return true if the socket connection is closed, false otherwise
	 */
	public synchronized boolean isClosed()
	{
		return closed;
	}

	/**
	 * Set the closed state of the socket connection to true. This will cause this socket handler
	 * to terminate as soon as the current request execution is complete.
	 */
	public synchronized void setClosed()
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

	/**
	 * Get a string representation of this connection.
	 * @return a string representation of this connection.
	 * @see java.lang.Thread#toString()
	 */
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		if (socketClient != null) sb.append(socketClient.getHost()).append(":").append(socketClient.getPort());
		else sb.append("socket is null");
		return sb.toString();
	}

	/**
	 * Set the stopped state of this connection.
	 * @param stopped true if this connection is to be stopped, false otherwise.
	 */
	protected synchronized void setStopped(boolean stopped)
	{
		this.stopped = stopped;
	}

	/**
	 * Get the stopped state of this connection.
	 * @return stopped true if this connection is stopped, false otherwise.
	 */
	protected synchronized boolean isStopped()
	{
		return stopped;
	}
}
