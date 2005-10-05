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
import org.jppf.task.ExecutionServiceException;
import org.jppf.task.admin.EventSubscriber;
import org.jppf.task.event.AdminEvent;

/**
 * Instances of this class listen, through a TCP socket connection, to events from a remote
 * service manager and forward them to an <code>EventSubscriber</code>.
 * @author Laurent Cohen
 */
public class RemoteSubscriberHandler extends AbstractSocketHandler
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(RemoteSubscriberHandler.class);

	/**
	 * The event subscriber to which remote events are forwarded.
	 */
	private EventSubscriber subscriber = null;

	/**
	 * Initialize this socket connection handler with a specified socket and subscriber.
	 * @param socket the socket to listen to for remote events.
	 * @param subscriber the subscriber to forward events to.
	 * @throws ExecutionServiceException if a socket communication error occurs.
	 */
	public RemoteSubscriberHandler(Socket socket, EventSubscriber subscriber) throws ExecutionServiceException
	{
		super(socket, null);
		this.subscriber = subscriber;
	}

	/**
	 * Process a request that was just received, by extracting the event it contents
	 * and forwarding to the subscriber.
	 * @param request the request to process.
	 * @see org.jppf.comm.socket.AbstractSocketHandler#perform(org.jppf.comm.Request)
	 */
	protected void perform(Request request)
	{
		RequestImpl<?> notification = (RequestImpl) request;
		AdminEvent event = (AdminEvent) notification.getContent();
		subscriber.eventOccurred(event);
	}
}
