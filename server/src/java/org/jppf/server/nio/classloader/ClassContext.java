/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

package org.jppf.server.nio.classloader;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.nio.*;
import org.jppf.utils.JPPFByteArrayOutputStream;

/**
 * 
 * @author Laurent Cohen
 */
public class ClassContext extends NioContext<ChannelState>
{
	/**
	 * The resource read from or written to the associated channel.
	 */
	private JPPFResourceWrapper resource = null;
	/**
	 * The list of pending resource requests for a resource provider.
	 */
	private List<SelectionKey> pendingRequests = null;
	/**
	 * The request currently processed.
	 */
	private SelectionKey currentRequest = null;

	/**
	 * Deserialize a resource wrapper from an array of bytes.
	 * @return a <code>JPPFResourceWrapper</code> instance.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public JPPFResourceWrapper deserializeResource() throws Exception
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(message.buffer.array());
		ObjectInputStream ois = new ObjectInputStream(bais);
		resource = (JPPFResourceWrapper) ois.readObject();
		ois.close();
		return resource;
	}

	/**
	 * Serialize a resource wrapper to an array of bytes.
	 * @throws Exception if an error occurs while serializing.
	 */
	public void serializeResource() throws Exception
	{
		ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		try
		{
			oos.writeObject(resource);
		}
		catch (Exception e)
		{
			if (e instanceof IOException) throw (IOException) e;
			else throw new IOException(e.getMessage());
		}
		oos.close();
		byte[] data = baos.toByteArray();
		if (message == null) message = new NioMessage();
		message.length = data.length;
		message.buffer = ByteBuffer.wrap(data);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 * @see org.jppf.server.nio.NioContext#handleException(java.nio.channels.SocketChannel)
	 */
	public void handleException(SocketChannel channel)
	{
		try
		{
			channel.close();
		}
		catch(Exception ignored)
		{
			log.error(ignored.getMessage(), ignored);
		}
	}

	/**
	 * Get the resource read from or written to the associated channel.
	 * @return the resource a <code>JPPFResourceWrapper</code> instance.
	 */
	public JPPFResourceWrapper getResource()
	{
		return resource;
	}

	/**
	 * Set the resource read from or written to the associated channel.
	 * @param resource a <code>JPPFResourceWrapper</code> instance.
	 */
	public void setResource(JPPFResourceWrapper resource)
	{
		this.resource = resource;
	}

	/**
	 * Add a new pending request to this resource provider.
	 * @param request the request as a <code>SelectionKey</code> instance. 
	 */
	public synchronized void addRequest(SelectionKey request)
	{
		pendingRequests.add(request);
	}

	/**
	 * Get the request currently processed.
	 * @return a <code>SelectionKey</code> instance.
	 */
	public synchronized SelectionKey getCurrentRequest()
	{
		return currentRequest;
	}

	/**
	 * Set the request currently processed.
	 * @param currentRequest a <code>SelectionKey</code> instance. 
	 */
	public synchronized void setCurrentRequest(SelectionKey currentRequest)
	{
		this.currentRequest = currentRequest;
	}

	/**
	 * Get the list of pending resource requests for a resource provider.
	 * @return a <code>List</code> of <code>SelectionKey</code> instances. 
	 */
	public List<SelectionKey> getPendingRequests()
	{
		return pendingRequests;
	}

	/**
	 * Set the list of pending resource requests for a resource provider.
	 * @param pendingRequests a <code>List</code> of <code>SelectionKey</code> instances. 
	 */
	public void setPendingRequests(List<SelectionKey> pendingRequests)
	{
		this.pendingRequests = pendingRequests;
	}
}
