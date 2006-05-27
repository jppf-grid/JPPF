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
package org.jppf.classloader;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.jppf.server.Request;

/**
 * This class encapsulate a resource request to a resource provider.
 * @author Domingos Creado
 */
class RemoteClassRequest
{
	/**
	 * Buffer used to read the data from a socket channel.
	 */
	private ByteBuffer resource;
	/**
	 * This object encapsulates the request to send to the provider.
	 */
	private Request request = new Request();
	/**
	 * The socket channel encapsulating a non-blocking socket connection.
	 */
	private SocketChannel channel;
	/**
	 * String containing the provider uuid.
	 */
	private String res;

	/**
	 * Initialize this remote request with a specified request string, data buffer and socket channel.
	 * @param res string containing the provider uuid.
	 * @param resource buffer used to read the data from a socket channel.
	 * @param channel the socket channel encapsulating a non-blocking socket connection.
	 */
	public RemoteClassRequest(String res, ByteBuffer resource, SocketChannel channel)
	{
		super();
		this.res = res;
		this.resource = resource;
		this.channel = channel;
	}

	/**
	 * Get the socket channel encapsulating a non-blocking socket connection.
	 * @return a <code>SocketChannel</code> instance.
	 */
	public SocketChannel getChannel()
	{
		return channel;
	}

	/**
	 * Get the buffer used to read the data from a socket channel.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public ByteBuffer getResource()
	{
		return resource;
	}

	/**
	 * Get the object that encapsulates the request to send to the provider.
	 * @return a <code>Request</code> instance.
	 */
	public Request getRequest()
	{
		return request;
	}

	/**
	 * Get the string containing the provider uuid.
	 * @return the uuid as a string.
	 */
	public String getResourceName()
	{
		return res;
	}
}