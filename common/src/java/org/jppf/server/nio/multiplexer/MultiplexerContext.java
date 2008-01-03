/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.nio.multiplexer;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.nio.*;
import org.jppf.utils.JPPFByteArrayOutputStream;

/**
 * Context obect associated with a socket channel used by the class server of the JPPF driver. 
 * @author Laurent Cohen
 */
public class MultiplexerContext extends NioContext<MultiplexerState>
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
	 * Port on which the connection was initially established.
	 */
	private int boundPort = 0;

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
			LOG.error(ignored.getMessage(), ignored);
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
	 * Get the number of pending resource requests for a resource provider.
	 * @return a the number of requests as an int. 
	 */
	public synchronized int getNbPendingRequests()
	{
		List<SelectionKey> reqs = getPendingRequests();
		return (reqs == null ? 0 : reqs.size()) + (getCurrentRequest() == null ? 0 : 1);
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
