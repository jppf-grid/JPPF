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
import org.jppf.comm.*;
import org.jppf.task.ExecutionService;

/**
 * Instances of this class handle execution requests sent over a TCP socket connections.
 * @author Laurent Cohen
 */
public class SocketHandler extends Thread
{
	private static Logger log = Logger.getLogger(SocketHandler.class);

	/**
	 * The socket client used to communicate over a socket connection.
	 */
	private SocketClient socketClient = null;
	/**
	 * Indicates whether this socket handler should be terminated and stop processing.
	 */
	private boolean stop = false;
	/**
	 * Indicates whether this socket handler is closed, which means it can't hadle requests anymore.
	 */
	private boolean closed = false;
	/**
	 * The execution service to which tasks execution is delegated.
	 */
	private ExecutionService execService = null;

	/**
	 * Initialize this socket handler with an open socket connection to a remote client, and
	 * the execution service that will perform the tasks execution.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
	 * @param execService the execution service used by this socket handler.
	 * @throws Exception if this socket handler can't be initialized.
	 */
	public SocketHandler(Socket socket, ExecutionService execService) throws Exception
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
				ExecutionRequest request = (ExecutionRequest) socketClient.receive();
				execService.executeMulti(request.getContent());
				ExecutionResponse response = new ExecutionResponse();
				response.setContent(request.getContent());
				response.setCorrelationId(request.getId());
				socketClient.send(response);
			}
		}
		catch (Throwable t)
		{
			log.error(t.getMessage(), t);
			setClosed();
		}
	}
	
	/**
	 * Set the stop flag to true, indicated that this socket handler hsould be closed as
	 * soon as possible.
	 */
	public synchronized void setStopped()
	{
		stop = true;
	}
	
	/**
	 * Determines whether the socket connection is closed
	 * @return true if the socket connection is closed, false otherwise
	 */
	public boolean isClosed()
	{
		return closed;
	}
	
	/**
	 * Sets the closed state of the socket connection to true
	 * (Mostly useful for differed actual close with non blocking connections)
	 */
	public void setClosed()
	{
		setStopped();
		closed = true;
		close();
		//sgHandler.stopService();
	}

	/**
	 * Closes the socket connection
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
