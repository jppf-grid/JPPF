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

import java.net.Socket;
import org.apache.log4j.Logger;
import org.jppf.comm.Request;
import org.jppf.task.*;

/**
 * Common abstract superclass for classes handling a socket connection to a remote host,
 * obtained from the {@link java.net.ServerSocket#accept() ServerSocket.accept()} method.
 * @author Laurent Cohen
 */
public abstract class AbstractSocketHandler extends Thread
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(SocketHandler.class);

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
	 * The execution service to which tasks execution is delegated.
	 */
	protected ExecutionService execService = null;

	/**
	 * Initialize this socket handler with an open socket connection to a remote client, and
	 * the execution service that will perform the tasks execution.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
	 * @param execService the execution service used by this socket handler.
	 * @throws ExecutionServiceException if this socket handler can't be initialized.
	 */
	public AbstractSocketHandler(Socket socket, ExecutionService execService) throws ExecutionServiceException
	{
		socketClient = new SocketClient(socket);
		this.execService = execService;
	}

	/**
	 * Main processing loop for this socket handler. During each loop iteration,
	 * the following operations are performed:
	 * <ol>
	 * <li>if the stop flag is set to true, exit the loop</li>
	 * <li>block until a request is received</li>
	 * <li>when an execution request is received, delegate the execution to the associated execution service</li>
	 * <li>send the response</li>
	 * </ol>
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			while (!stop)
			{
				Request request = (Request) socketClient.receive();
				perform(request);
			}
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		finally
		{
			setClosed();
		}
	}

	/**
	 * Perform the actual request execution. Subclasses must implement this method.
	 * @param request the request to execute.
	 * @throws ExecutionServiceException if an error occurs during the request execution.
	 */
	protected abstract void perform(Request request) throws ExecutionServiceException;

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
