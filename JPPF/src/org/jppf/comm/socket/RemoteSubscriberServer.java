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
import org.jppf.task.ExecutionServiceException;
import org.jppf.task.admin.EventSubscriber;

/**
 * Instances of this class allow service managers to connect to a remote event subscriber so they can
 * forward events to it.
 * @author Laurent Cohen
 */
public class RemoteSubscriberServer extends AbstractSocketServer
{
	/**
	 * The subscriber to which socket connections handled by this server forwards events to.
	 */
	private EventSubscriber subscriber = null;
	/**
	 * A listener that reacts to exceptions thrown by the socket connections created by this server.
	 */
	private SocketExceptionListener listener = null;
	/**
	 * Initialize this socket server with a specified port number.
	 * @param port the port to listen to for incoming connections.
	 * @param subscriber the subscriber to which socket connections handled by this server forwards events to.
	 * @param listener a listener that reacts to exceptions thrown by the socket connections created by this server.
	 * @throws ExecutionServiceException if an error occurs while initializing the underlying
	 * <code>ServerSocket</code>.
	 */
	public RemoteSubscriberServer(int port, EventSubscriber subscriber, SocketExceptionListener listener)
		throws ExecutionServiceException
	{
		super(null, port);
		this.subscriber = subscriber;
		this.listener = listener;
	}

	/**
	 * @see org.jppf.classloader.ClassServer#createHandler(java.net.Socket)
	 */
	protected AbstractSocketHandler createHandler(Socket socket) throws ExecutionServiceException
	{
		RemoteSubscriberHandler handler = new RemoteSubscriberHandler(socket, subscriber);
		if (listener != null) handler.addSocketExceptionListener(listener);
		return handler;
	}
}
