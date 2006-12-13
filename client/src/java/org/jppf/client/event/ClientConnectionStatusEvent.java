/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.client.event;

import java.util.EventObject;

import org.jppf.client.JPPFClientConnection;

/**
 * Event sent to notify of a status chnage for a client connection.
 * @author Laurent Cohen
 */
public class ClientConnectionStatusEvent extends EventObject
{
	/**
	 * Initialize this event with a client connection as source.
	 * @param source the event source.
	 */
	public ClientConnectionStatusEvent(JPPFClientConnection source)
	{
		super(source);
	}

	/**
	 * Get the source of this event.
	 * @return the event as a <code>JPPFClientConnection</code> instance.
	 */
	public JPPFClientConnection getJPPFClientConnection()
	{
		return (JPPFClientConnection) getSource();
	}
}
