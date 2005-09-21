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
import org.jppf.comm.*;
import org.jppf.task.ExecutionServiceException;
import org.jppf.task.admin.*;
import org.jppf.task.event.*;
import org.jppf.task.impl.ServiceConfigurationImpl;

/**
 * Implementation of a socket handler for an administration service, solely dedicated to listening to
 * events from other, remote service managers and dispatching those events to eventual subscribers. 
 * @author Laurent Cohen
 */
public class AdminSocketHandler extends AbstractSocketHandler
{
	/**
	 * Initialize this socket handler with a specified socket and service manager.
	 * @param socket the socket connection to listen to.
	 * @param manager the service manager the events are dispatched to.
	 * @throws ExecutionServiceException if an error occurs during initialization.
	 */
	public AdminSocketHandler(Socket socket, ServiceManager manager) throws ExecutionServiceException
	{
		super(socket, manager);
	}

	/**
	 * Get a reference to the underlying service manager.
	 * @return a <code>ServiceManager</code> instance.
	 */
	private ServiceManager getManager()
	{
		return (ServiceManager) execService;
	}

	/**
	 * Perform the actual request execution.
	 * @param request the request wrapping an event notification.
	 * @see org.jppf.comm.socket.AbstractSocketHandler#perform(org.jppf.comm.Request)
	 */
	protected void perform(Request request)
	{
		RequestImpl<?> notification = (RequestImpl) request;
		AdminEvent event = (AdminEvent) notification.getContent();
		ServiceManager manager = getManager();
		if (event.getManagerName() == null)
		{
			event.setManagerName(manager.getName());
			ServiceConfigurationImpl cfg = (ServiceConfigurationImpl) event.getConfig();
			cfg.setManagerName(manager.getName());
		}
		switch (event.getEventType())
		{
			case STATUS:
				manager.statusChanged((StatusEvent) event);
				break;
			case EXCEPTION:
			case SERVICE_REMOVED:
				manager.serviceRemoved(event);
				break;
			case PROFILING:
				((AbstractServiceManager) manager).passThru(event);
				break;
			case NEW_SERVICE:
				manager.newService(event);
				((AbstractServiceManager) manager).passThru(event);
				break;
			default:
				break;
		}
	}
}
