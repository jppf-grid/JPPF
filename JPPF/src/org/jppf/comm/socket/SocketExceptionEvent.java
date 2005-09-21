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

import java.util.EventObject;

/**
 * This type of event capture exceptions occurring within a socket client.
 * The goal is to detect when a socket connection is lost, so that appropriate action can be taken.
 * @author Laurent Cohen
 */
public class SocketExceptionEvent extends EventObject
{

	/**
	 * Initialze this event with a specified exception.
	 * @param source the exception reported by this event.
	 */
	public SocketExceptionEvent(Exception source)
	{
		super(source);
	}
	
	/**
	 * Get the exception source of this event. This method is provided as a convenience, since it
	 * merely invokes the {@link java.util.EventObject#getSource() EventObject.getSource()} method.
	 * @return the Exception source of this event.
	 */
	public Exception getException()
	{
		return (Exception) getSource();
	}
}
