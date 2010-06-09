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

package org.jppf.classloader;

import static java.nio.channels.SelectionKey.*;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.*;
import org.jppf.comm.socket.IOHandler;
import org.jppf.server.nio.*;
import org.jppf.utils.*;

/**
 * Channel wrapper and I/O implementation for the class loader of an in-VM node.
 * @author Laurent Cohen
 */
public class LocalClassLoaderWrapperHandler extends AbstractChannelWrapper<AbstractNioContext> implements IOHandler
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(LocalClassLoaderWrapperHandler.class);
	/**
	 * Determines whether trace-level logging is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * This channel's key ops.
	 */
	private AtomicInteger keyOps = new AtomicInteger(0);
	/**
	 * This channel's ready ops.
	 */
	private AtomicInteger readyOps = new AtomicInteger(0);
	/**
	 * The mesage currently being read.
	 */
	private NioMessage message = null;

	/**
	 * Initialize this I/O handler with the specified context.
	 * @param context the context used as communication channel.
	 */
	public LocalClassLoaderWrapperHandler(AbstractNioContext context)
	{
		super(context);
		if (traceEnabled) log.trace("created " + this); 
	}

	/**
	 * {@inheritDoc}
	 */
	public NioContext getContext()
	{
		return getChannel();
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
	public int getReadyOps()
	{
		return readyOps.get();
	}

	/**
	 * Set the operations for which this channel is ready.
	 * @param readyOps the bitwise operations as an int value.
	 */
	protected void setReadyOps(int readyOps)
	{
		this.readyOps.set(readyOps);
		if (traceEnabled) log.debug("readyOps = " + readyOps + ", keyOps = " + keyOps);
		if (selector != null) selector.wakeUp();
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
		setReadyOps(OP_WRITE);
		AbstractNioContext context = getChannel();
		//NioMessage message = context.getMessage();
		while ((message == null) || !message.lengthWritten || (context.writeByteCount < message.length))
		{
			goToSleep();
			//message = context.getMessage();
		}
		if (traceEnabled) log.trace("read " + message);
		return new JPPFBuffer(message.buffer.array(), message.length);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] data, int offset, int len) throws Exception
	{
		if (traceEnabled) log.trace("" + this + " writing data length = " + len + " offset = " + offset);
		this.message = null;
		AbstractNioContext context = getChannel();
		if (context.getMessage() == null) context.setMessage(new NioMessage());
		NioMessage message = context.getMessage();
		message.length = len;
		message.buffer = ByteBuffer.wrap(data, offset, len);
		message.buffer.position(len);
		context.readByteCount = len;
		if (traceEnabled) log.trace("" + this + " written " + message);
		setReadyOps(OP_READ);
		wakeUp();
		goToSleep();
		//Thread.sleep(100);
		//wakeUp();
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeInt(int value) throws Exception
	{
	}

	/**
	 * Get the message currently being read.
	 * @return a {@link NioMessage} instance.
	 */
	public NioMessage getMessage()
	{
		return message;
	}

	/**
	 * Set the message currently being read.
	 * @param message a {@link NioMessage} instance.
	 */
	public void setMessage(NioMessage message)
	{
		this.message = message;
	}
}
