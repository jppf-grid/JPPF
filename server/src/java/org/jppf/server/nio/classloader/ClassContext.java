/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.nio.classloader;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.io.ByteBufferInputStream;
import org.jppf.server.nio.*;
import org.jppf.utils.*;

/**
 * Context object associated with a socket channel used by the class server of the JPPF driver. 
 * @author Laurent Cohen
 */
public class ClassContext extends SimpleNioContext<ClassState>
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(NioContext.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The resource read from or written to the associated channel.
	 */
	private JPPFResourceWrapper resource = null;
	/**
	 * The list of pending resource requests for a resource provider.
	 */
	private List<ChannelWrapper> pendingRequests = null;
	/**
	 * The request currently processed.
	 */
	private ChannelWrapper currentRequest = null;

	/**
	 * Deserialize a resource wrapper from an array of bytes.
	 * @return a <code>JPPFResourceWrapper</code> instance.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public JPPFResourceWrapper deserializeResource() throws Exception
	{
		ByteBufferInputStream bbis = new ByteBufferInputStream(message.buffer, true);
		byte[] data = FileUtils.getInputStreamAsByte(bbis);
		data = JPPFDataTransformFactory.transform(false, data, 0, data.length);
		ObjectSerializer serializer = new ObjectSerializerImpl();
		resource = (JPPFResourceWrapper) serializer.deserialize(data);
		return resource;
	}

	/**
	 * Serialize a resource wrapper to an array of bytes.
	 * @throws Exception if an error occurs while serializing.
	 */
	public void serializeResource() throws Exception
	{
		ObjectSerializer serializer = new ObjectSerializerImpl();
		byte[] data = serializer.serialize(resource).getBuffer();
		data = JPPFDataTransformFactory.transform(true, data, 0, data.length);
		if (message == null) message = new NioMessage();
		message.length = data.length;
		message.buffer = ByteBuffer.wrap(data);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 * @see org.jppf.server.nio.NioContext#handleException(java.nio.channels.SocketChannel)
	 */
	public void handleException(ChannelWrapper channel)
	{
		try
		{
			channel.close();
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.warn(e);
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
	public synchronized void addRequest(ChannelWrapper request)
	{
		pendingRequests.add(request);
	}

	/**
	 * Get the request currently processed.
	 * @return a <code>SelectionKey</code> instance.
	 */
	public synchronized ChannelWrapper getCurrentRequest()
	{
		return currentRequest;
	}

	/**
	 * Set the request currently processed.
	 * @param currentRequest a <code>SelectionKey</code> instance. 
	 */
	public synchronized void setCurrentRequest(ChannelWrapper currentRequest)
	{
		this.currentRequest = currentRequest;
	}

	/**
	 * Get the number of pending resource requests for a resource provider.
	 * @return a the number of requests as an int. 
	 */
	public synchronized int getNbPendingRequests()
	{
		List<ChannelWrapper> reqs = getPendingRequests();
		return (reqs == null ? 0 : reqs.size()) + (getCurrentRequest() == null ? 0 : 1);
	}

	/**
	 * Get the list of pending resource requests for a resource provider.
	 * @return a <code>List</code> of <code>SelectionKey</code> instances. 
	 */
	public List<ChannelWrapper> getPendingRequests()
	{
		return pendingRequests;
	}

	/**
	 * Set the list of pending resource requests for a resource provider.
	 * @param pendingRequests a <code>List</code> of <code>SelectionKey</code> instances. 
	 */
	public void setPendingRequests(List<ChannelWrapper> pendingRequests)
	{
		this.pendingRequests = pendingRequests;
	}
}
