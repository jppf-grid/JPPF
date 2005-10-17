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
import org.jppf.comm.*;
import org.jppf.task.*;
import org.jppf.task.admin.*;

/**
 * Instances of this class handle execution requests sent over a TCP socket connection.
 * @author Laurent Cohen
 */
public class ExecutionSocketHandler extends AbstractSocketHandler
{
	/**
	 * Initialize this socket handler with an open socket connection to a remote client, and
	 * the execution service that will perform the tasks execution.
	 * @param socket the socket connection from which requests are received and to which responses are sent.
	 * @param execService the execution service used by this socket handler.
	 * @throws ExecutionServiceException if this socket handler can't be initialized.
	 */
	public ExecutionSocketHandler(Socket socket, ExecutionService execService) throws ExecutionServiceException
	{
		super(socket, execService);
		if (execService instanceof SocketExceptionListener)
			socketClient.addSocketExceptionListener((SocketExceptionListener) execService);
	}

	/**
	 * Perform the actual request execution.
	 * @param request the request to execute.
	 * @throws ExecutionServiceException if an error occurs during the request execution.
	 * @see org.jppf.comm.socket.AbstractSocketHandler#perform(org.jppf.comm.Request)
	 */
	protected void perform(Request request) throws ExecutionServiceException
	{
		Exception e = null;
		try
		{
			RequestQueue requestQueue = RequestQueueFactory.getLocalQueue();
			Response<?> response = requestQueue.submitBlocking(request);
			//Response<?> response = execService.executeRequest(request);
			socketClient.send(response);
			response.clearContent();
		}
		catch(ConnectException ce)
		{
			e = ce;
		}
		catch(IOException ioe)
		{
			e = ioe;
		}
		if (e != null)
		{
			throw new ExecutionServiceException(e.getMessage(), e);
		}
	}
}
