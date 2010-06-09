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

package org.jppf.server.nio.nodeserver;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.IOHandler;
import org.jppf.server.nio.*;
import org.jppf.utils.JPPFBuffer;

/**
 * Wrapper implementation for a local node's communication channel.
 * @author Laurent Cohen
 */
public class LocalNodeWrapperHandler extends AbstractChannelWrapper<LocalNodeContext> implements IOHandler
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(LocalNodeWrapperHandler.class);
	/**
	 * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
	 */
	protected static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * This channel's key ops.
	 */
	private AtomicInteger keyOps = new AtomicInteger(0);
	/**
	 * This channel's ready ops.
	 */
	private AtomicInteger readyOps = new AtomicInteger(0);
	/**
	 * Position of the next block of data to be read by the node.
	 */
	private int readPosition = 0;
	/**
	 * Position of the next block of data to be written by the node.
	 */
	private int writePosition = 0;
	/**
	 * The message currently being read.
	 */
	private LocalNodeMessage message = null;

	/**
	 * Initialize this channel wrapper with the specified node context.
	 * @param context the node context used as channel.
	 */
	public LocalNodeWrapperHandler(LocalNodeContext context)
	{
		super(context);
		if (traceEnabled) log.trace("created " + this); 
	}

	/**
	 * {@inheritDoc}
	 */
	public NioContext getContext()
	{
		return channel;
	}

	/**
	 * {@inheritDoc}
	 */
	public int getKeyOps()
	{
		return keyOps.get();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setKeyOps(int keyOps)
	{
		this.keyOps.set(keyOps);
		if (traceEnabled) log.trace("readyOps = " + readyOps + ", keyOps = " + keyOps);
		if (getSelector() != null) getSelector().wakeUp();
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized int getReadyOps()
	{
		return readyOps.get();
	}

	/**
	 * Set the operations for which this channel is ready.
	 * @param readyOps the bitwise operations as an int value.
	 */
	public synchronized void setReadyOps(int readyOps)
	{
		this.readyOps.set(readyOps);
		if (traceEnabled) log.trace("readyOps = " + readyOps + ", keyOps = " + keyOps);
		if (getSelector() != null) getSelector().wakeUp();
	}

	/**
	 * {@inheritDoc}
	 */
	public void flush() throws Exception
	{
	}

	/**
	 * {@inheritDoc}
	 */
	public JPPFBuffer read() throws Exception
	{
		/*
		writePosition = 0;
		setReadyOps(OP_WRITE);
		if (traceEnabled) log.trace("" + this + " reading data, message = " + message);
		while ((message == null) || message.isReading() || (readPosition >= message.getLocations().size())) goToSleep();
		DataLocation dl = message.getLocations().get(readPosition++);
		if (traceEnabled) log.trace("" + this + " data received, size = " + dl.getSize() + ", message = " + message);
		InputStream is = dl.getInputStream();
		return new JPPFBuffer(FileUtils.getInputStreamAsByte(is), dl.getSize());
		*/
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] data, int offset, int len) throws Exception
	{
		/*
		readPosition = 0;
		message = null;
		if (channel.getNodeMessage() == null) channel.setNodeMessage(new LocalNodeMessage());
		LocalNodeMessage message = (LocalNodeMessage) channel.getNodeMessage();
		if (writePosition <= 0) message.getLocations().clear();
		if (traceEnabled) log.trace("" + this + " writing data length = " + len + " offset = " + offset);
		InputSource is = new ByteBufferInputSource(data, offset, len);
		DataLocation location = IOHelper.createDataLocationMemorySensitive(len);
		int n = location.transferFrom(is, true);
		message.addLocation(location);
		writePosition++;
		setReadyOps(OP_READ);
		wakeUp();
		*/
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeInt(int value) throws Exception
	{
	}

	/**
	 * Get the message currently being read.
	 * @return an {@link AbstractNodeMessage} instance.
	 */
	public synchronized LocalNodeMessage getMessage()
	{
		return message;
	}

	/**
	 * Set the message currently being read.
	 * @param message an {@link AbstractNodeMessage} instance.
	 */
	public synchronized void setMessage(LocalNodeMessage message)
	{
		log.trace("setting message " + message);
		/*
		if (message == null)
		{
			Exception e = new Exception("debug stack");
			log.trace(e.getMessage(), e);
		}
		*/
		this.message = message;
		wakeUp();
	}
}
