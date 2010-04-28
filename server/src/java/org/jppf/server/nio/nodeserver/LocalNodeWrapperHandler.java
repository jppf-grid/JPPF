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

import java.io.InputStream;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.comm.socket.IOHandler;
import org.jppf.io.*;
import org.jppf.server.nio.*;
import org.jppf.utils.*;

/**
 * Wrapper implementation for a local node's communication channel.
 * @author Laurent Cohen
 */
public class LocalNodeWrapperHandler extends ChannelWrapper<LocalNodeContext> implements IOHandler
{
	/**
	 * This channel's key ops.
	 */
	private int keyOps = 0;
	/**
	 * This channel's ready ops.
	 */
	private int readyOps = 0;

	/**
	 * Determines whether this handler has data to read.
	 */
	private AtomicBoolean readable = new AtomicBoolean(false);
	/**
	 * Position of the next block of data to read or write.
	 */
	private int position = 0;
	/**
	 * The current object being read or written.
	 */
	private DataLocation currentLocation = null;
	/**
	 * Count of bytes read or written in the current data location.
	 */
	private int currentCount = 0;

	/**
	 * Initialize this channel wrapper with the specified node context.
	 * @param context the node context used as channel.
	 */
	public LocalNodeWrapperHandler(LocalNodeContext context)
	{
		super(context);
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
		return keyOps;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setKeyOps(int keyOps)
	{
		// to avoid exception when testing isReadable() when channel is write-ready.
		if ((keyOps & SelectionKey.OP_WRITE) != 0) readyOps = keyOps & ~SelectionKey.OP_READ;
		this.keyOps = keyOps;
	}

	/**
	 * {@inheritDoc}
	 */
	protected int getReadyOps()
	{
		return this.readyOps;
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
		while (!readable.get()) goToSleep();
		position++;
		DataLocation dl = getChannel().getNodeMessage().getLocations().get(position);
		InputStream is = dl.getInputStream();
		return new JPPFBuffer(FileUtils.getInputStreamAsByte(is), dl.getSize());
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(int len, byte[]...data) throws Exception
	{
		if (position >= 0) position = -1;
		position++;
		DataLocation dl = IOHelper.createDataLocationMemorySensitive(len);
		for (int i=0; i<data.length; i++)
		{
			InputSource is = new ByteBufferInputSource(data[i], 0, data[i].length);
			dl.transferFrom(is, true);
		}
		getChannel().getNodeMessage().addLocation(dl);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] data, int offset, int len) throws Exception
	{
		InputSource is = new ByteBufferInputSource(data, offset, len);
		int n = currentLocation.transferFrom(is, true);
		getChannel().getNodeMessage().addLocation(currentLocation);
		((LocalNodeMessage) getChannel().getNodeMessage()).wakeUp();
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeInt(int value) throws Exception
	{
		currentLocation = IOHelper.createDataLocationMemorySensitive(value);
		currentCount = 0;
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
}
