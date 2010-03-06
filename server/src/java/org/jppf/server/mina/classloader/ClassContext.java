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

package org.jppf.server.mina.classloader;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.jppf.data.transform.*;
import org.jppf.io.*;
import org.jppf.node.JPPFResourceWrapper;
import org.jppf.server.mina.*;
import org.jppf.server.nio.NioObject;
import org.jppf.server.nio.classloader.ClassState;
import org.jppf.utils.*;

/**
 * Context obect associated with a socket channel used by the class server of the JPPF driver. 
 * @author Laurent Cohen
 */
public class ClassContext extends MinaContext<ClassState>
{
	/**
	 * The resource read from or written to the associated channel.
	 */
	private JPPFResourceWrapper resource = null;
	/**
	 * The list of pending resource requests for a resource provider.
	 */
	private List<IoSession> pendingRequests = null;
	/**
	 * The request currently processed.
	 */
	private IoSession currentRequest = null;
	/**
	 * Container for the current message data.
	 */
	protected ClassServerMessage message = null;
	/**
	 * Count of bytes read.
	 */
	protected int readByteCount = 0;
	/**
	 * Count of bytes written.
	 */
	protected int writeByteCount = 0;
	/**
	 * Currently read length.
	 */
	private int currentLength = 0;

	/**
	 * Deserialize a resource wrapper from an array of bytes.
	 * @return a <code>JPPFResourceWrapper</code> instance.
	 * @throws Exception if an error occurs while deserializing.
	 */
	public JPPFResourceWrapper deserializeResource() throws Exception
	{
		InputStream is = message.message.getData().getInputStream();
		//InputStream is = message.getData().getInputStream();
		byte[] data = FileUtils.getInputStreamAsByte(is);
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null) data = transform.unwrap(data);
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
		JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
		if (transform != null) data = transform.wrap(data);
		//if (message == null)
		currentLength = 0;
		message = new ClassServerMessage();
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(data.length);
		buffer.flip();
		message.lengthObject = new NioObject(new ByteBufferLocation(buffer), false);
		message.message = new NioObject(new ByteBufferLocation(data, 0, data.length), false);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 * @see org.jppf.server.nio.NioContext#handleException(java.nio.channels.SocketChannel)
	 */
	public void handleException(IoSession channel)
	{
		try
		{
			channel.close(true);
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
	public synchronized void addRequest(IoSession request)
	{
		pendingRequests.add(request);
	}

	/**
	 * Get the request currently processed.
	 * @return a <code>IoSession</code> instance.
	 */
	public synchronized IoSession getCurrentRequest()
	{
		return currentRequest;
	}

	/**
	 * Set the request currently processed.
	 * @param currentRequest a <code>IoSession</code> instance. 
	 */
	public synchronized void setCurrentRequest(IoSession currentRequest)
	{
		this.currentRequest = currentRequest;
	}

	/**
	 * Get the number of pending resource requests for a resource provider.
	 * @return a the number of requests as an int. 
	 */
	public synchronized int getNbPendingRequests()
	{
		List<IoSession> reqs = getPendingRequests();
		return (reqs == null ? 0 : reqs.size()) + (getCurrentRequest() == null ? 0 : 1);
	}

	/**
	 * Get the list of pending resource requests for a resource provider.
	 * @return a <code>List</code> of <code>IoSession</code> instances. 
	 */
	public List<IoSession> getPendingRequests()
	{
		return pendingRequests;
	}

	/**
	 * Set the list of pending resource requests for a resource provider.
	 * @param pendingRequests a <code>List</code> of <code>IoSession</code> instances. 
	 */
	public void setPendingRequests(List<IoSession> pendingRequests)
	{
		this.pendingRequests = pendingRequests;
	}

	/**
	 * Read data from a channel.
	 * @param buffer the channel to read the data from.
	 * @return true if all the data has been read, false otherwise.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public boolean readMessage(IoBuffer buffer) throws Exception
	{
		if (message == null)
		{
			message = new ClassServerMessage();
			message.lengthObject = new NioObject(4, false);
			currentLength = 0;
		}
		InputSource is = new IoBufferInputSource(buffer);
		if (!message.lengthObject.read(is)) return false;
		if (currentLength <= 0)
		{
			currentLength = ((ByteBufferLocation) message.lengthObject.getData()).buffer().getInt();
			message.message = new NioObject(currentLength, false);
		}
		return message.message.read(is);
	}

	/**
	 * Write data to a channel.
	 * @param buffer the channel to write the data to.
	 * @return true if all the data has been written, false otherwise.
	 * @throws Exception if an error occurs while writing the data.
	 */
	public boolean writeMessage(IoBuffer buffer) throws Exception
	{
		OutputDestination od = new IoBufferOutputDestination(buffer);
		if (!message.lengthObject.write(od)) return false;
		return message.message.write(od);
	}

	/**
	 * Get the container for the current message data.
	 * @return an <code>ClassServerMessage</code> instance.
	 */
	public ClassServerMessage getMessage()
	{
		return message;
	}

	/**
	 * Set the container for the current message data.
	 * @param message an <code>ClassServerMessage</code> instance.
	 */
	public void setMessage(ClassServerMessage message)
	{
		this.message = message;
	}
}
