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
 * Class SocketHandler
 * @author Laurent Cohen
 */
public class SocketHandler extends Thread
{
	private static Logger log = Logger.getLogger(SocketHandler.class);

	private SocketClient socketClient = null;
	private boolean stop = false;
	private boolean closed = false;
	private ExecutionService execService = null;

	public SocketHandler(Socket socket, ExecutionService execService) throws Exception
	{
		socketClient = new SocketClient(socket);
		this.execService = execService;
		try
		{
			init();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(),e);
		}
	}

	protected void init() throws Exception
	{
	}

	/**
	 * Thread main processing loop
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
				socketClient.send(response);
			}
		}
		catch (Throwable t)
		{
			log.error(t.getMessage(), t);
			setClosed();
		}
	}
	
	public synchronized void setStopped(boolean stopped)
	{
		stop = stopped;
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
		setStopped(true);
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
