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

import org.apache.commons.logging.*;
import org.jppf.comm.socket.IOHandler;
import org.jppf.server.nio.*;
import org.jppf.utils.JPPFBuffer;

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
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * This channel's key ops.
	 */
	private int keyOps = 0;
	/**
	 * This channel's ready ops.
	 */
	private int readyOps = 0;

	/**
	 * Initialize this I/O handler with the specified context.
	 * @param context the context used as communication channel.
	 */
	public LocalClassLoaderWrapperHandler(AbstractNioContext context)
	{
		super(context);
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
	public synchronized int getKeyOps()
	{
		return keyOps;
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized void setKeyOps(int keyOps)
	{
		// to avoid exception when testing isReadable() when channel is write-ready.
		//if ((keyOps & OP_WRITE) != 0) readyOps = keyOps & ~OP_READ;
		if ((keyOps & OP_WRITE) != 0) readyOps &= ~OP_READ;
		this.keyOps = keyOps;
		if (debugEnabled) log.debug("readyOps = " + readyOps + ", keyOps = " + keyOps);
		if (getSelector() != null) getSelector().wakeup();
	}

	/**
	 * {@inheritDoc}
	 */
	public synchronized int getReadyOps()
	{
		return this.readyOps;
	}

	/**
	 * Set the operations for which this channel is ready.
	 * @param readyOps the bitwise operations as an int value.
	 */
	protected synchronized void setReadyOps(int readyOps)
	{
		this.readyOps = readyOps;
		if (debugEnabled) log.debug("readyOps = " + readyOps + ", keyOps = " + keyOps);
		if (getSelector() != null) getSelector().wakeup();
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
		while ((context.getMessage() == null) || !context.getMessage().lengthWritten ||
			(context.writeByteCount < context.getMessage().length)) goToSleep();
		//while (readPosition >= channel.getNodeMessage().getLocations().size()) goToSleep();
		NioMessage message = context.getMessage();
		setReadyOps(0);
		return new JPPFBuffer(message.buffer.array(), message.length);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] data, int offset, int len) throws Exception
	{
		AbstractNioContext context = getChannel();
		if (context.getMessage() == null) context.setMessage(new NioMessage());
		NioMessage message = context.getMessage();
		message.length = len;
		message.lengthWritten = true;
		message.buffer = ByteBuffer.wrap(data, offset, len);
		context.readByteCount = len;
		setReadyOps(OP_READ);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeInt(int value) throws Exception
	{
	}

	/**
	 * Cause the current thread to wait until notified.
	 */
	public synchronized void goToSleep()
	{
		try
		{
			wait();
		}
		catch(InterruptedException ignored)
		{
		}
	}

	/**
	 * Notify the threads currently waiting on this object that they can resume.
	 */
	public synchronized void wakeUp()
	{
		notifyAll();
	}


	/**
	 * {@inheritDoc}
	 */
	public String toString()
	{
		return LocalClassLoaderWrapperHandler.class.getSimpleName() + ":" + id;
	}
}
