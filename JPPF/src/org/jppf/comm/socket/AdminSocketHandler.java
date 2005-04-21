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

import static org.jppf.task.event.AdminEvent.EventType.*;

import java.net.Socket;
import org.apache.log4j.Logger;
import org.jppf.comm.*;
import org.jppf.task.admin.ServiceManager;
import org.jppf.task.event.*;

public class AdminSocketHandler extends AbstractSocketHandler
{
	private static Logger log = Logger.getLogger(AdminSocketHandler.class);

	public AdminSocketHandler(Socket socket, ServiceManager manager) throws Exception
	{
		super(socket, manager);
	}
	
	private ServiceManager getManager()
	{
		return (ServiceManager) execService;
	}

	protected void perform(Request request) throws Exception
	{
		RequestImpl<AdminEvent> notification = (RequestImpl<AdminEvent>) request;
		AdminEvent event = notification.getContent();
		ServiceManager manager = getManager();
		if (STATUS.equals(event.getEventType()))
		{
			if ("local-socket".equals(event.getSource()))
			{
				int i=0;
			}
			manager.statusChanged((StatusEvent) event);
		}
		else if (PROFILING.equals(event.getEventType()))
		{
			manager.profilingDataReceived((ProfilingEvent) event);
		}
	}
}
