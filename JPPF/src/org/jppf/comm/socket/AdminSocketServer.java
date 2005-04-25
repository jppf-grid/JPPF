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
import org.jppf.task.admin.ServiceManager;

/**
 * Socket server implementation for a service manager.
 * @author Laurent Cohen
 */
public class AdminSocketServer extends AbstractSocketServer
{
	/**
	 * Initialize this socket server with the specified  service manager and TCP port.
	 * @param manager this socket server's underlying service manager.
	 * @param port the port to listen to.
	 * @throws Exception if an error occurs during initialization.
	 */
	public AdminSocketServer(ServiceManager manager, int port) throws Exception
	{
		super(manager, port);
	}

	/**
	 * Instanciate a socket handler that will handle the socket connection obtained through
	 * this socket server. 
	 * @param socket the socket conneciton to handle.
	 * @return an <code>AbstractSocketHandler</code> instance.
	 * @throws Exception if an error occurs while instanciating the socket handler.
	 * @see org.jppf.comm.socket.AbstractSocketServer#createHandler(java.net.Socket)
	 */
	protected AbstractSocketHandler createHandler(Socket socket) throws Exception
	{
		return new AdminSocketHandler(socket, (ServiceManager) execService);
	}
}
