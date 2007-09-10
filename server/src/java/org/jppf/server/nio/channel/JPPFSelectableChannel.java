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

package org.jppf.server.nio.channel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of an in-memory selectable channel. Read and write operations can be performed concurrently,
 * with only one thread at at time allowed to read, and only one thread at a time allowed to write. 
 * @author Laurent Cohen
 */
public class JPPFSelectableChannel extends AbstractSelectableChannel implements ByteChannel
{
	/**
	 * Default size of the read and write buffers.
	 */
	public static final int BUFFER_SIZE = 32768;
	/**
	 * Buffer used for read operations.
	 */
	private ByteBuffer buffer = ByteBuffer.wrap(new byte[BUFFER_SIZE]);
	/**
	 * Buffer used for read operations.
	 */
	private ByteBuffer readBuffer = ByteBuffer.wrap(new byte[BUFFER_SIZE]);
	/**
	 * Buffer used for write operations.
	 */
	private ByteBuffer writeBuffer = ByteBuffer.wrap(new byte[BUFFER_SIZE]);
	/**
	 * Determines whether this channel is currently performing a read operation.
	 */
	private boolean reading = false;
	/**
	 * Determines whether this channel is currently performing a write operation.
	 */
	private boolean writing = false;
	/**
	 * Determines the last operation that was performed.
	 */
	private int lastOperation = 0;
	/**
	 * The other end-point of this channel.
	 */
	private JPPFSelectableChannel channel = null;
	/**
	 * Used to prevent more than one thread from writing to this channel.
	 */
	private ReentrantLock writeLock = new ReentrantLock();
	/**
	 * Used to prevent more than one thread from reading from this channel.
	 */
	private ReentrantLock readLock = new ReentrantLock();

	/**
	 * Initializes a new instance of this class.
	 */
	private JPPFSelectableChannel()
	{
		super(null);
	}

	/**
	 * Initializes a new instance of this class with the specified channel.
	 * @param channel the channel used as tne other end point for this one.
	 */
	public JPPFSelectableChannel(JPPFSelectableChannel channel)
	{
		this();
		this.channel = channel;
		if ((channel != null) && (channel.getChannel() != this)) channel.setChannel(this);
	}

	/**
	 * Closes this selectable channel.
	 * This method is invoked by the close method in order to perform the actual work of closing the channel.
	 * This method is only invoked if the channel has not yet been closed, and it is never invoked more than once.
	 * An implementation of this method must arrange for any other thread that is blocked in an I/O operation upon
	 * this channel to return immediately, either by throwing an exception or by returning normally.
	 * @throws IOException If an I/O error occurs.
	 * @see java.nio.channels.spi.AbstractSelectableChannel#implCloseSelectableChannel()
	 */
	protected void implCloseSelectableChannel() throws IOException
	{
	}

	/**
	 * Adjusts this channel's blocking mode.
	 * If the given blocking mode is different from the current blocking mode then this method
	 * invokes the implConfigureBlocking method, while holding the appropriate locks, in order to change the mode. 
	 * @param block If true then this channel will be placed in blocking mode; if false then it will be placed non-blocking mode.
	 * @throws IOException If an I/O error occurs or this channel is closed.
	 * @see java.nio.channels.spi.AbstractSelectableChannel#implConfigureBlocking(boolean)
	 */
	protected void implConfigureBlocking(boolean block) throws IOException
	{
	}

	/**
	 * Returns an operation set identifying this channel's supported operations.
	 * The bits that are set in this integer value denote exactly the operations that are valid for this channel.
	 * This method always returns the same value for a given concrete channel class.
	 * @return The valid operation set.
	 * @see java.nio.channels.SelectableChannel#validOps()
	 */
	public int validOps()
	{
		return SelectionKey.OP_READ | SelectionKey.OP_WRITE;
	}

	/**
	 * Writes a sequence of bytes to this channel from the given buffer.
	 * @param src The buffer from which bytes are to be retrieved.
	 * @return The number of bytes written, possibly zero.
	 * @throws IOException If some other I/O error occurs.
	 * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
	 */
	public int write(ByteBuffer src) throws IOException
	{
		if (!isOpen()) throw new ClosedChannelException();
		//writeLock.lock();
		try
		{
			if (buffer.remaining() == 0)
			{
				if (isBlocking()) sleep();
				else return 0;
			}
			//ByteBuffer dest = channel.getReadBuffer();
			ByteBuffer dest = buffer;
			int writeSize = 0;
			if (src.remaining() <= dest.remaining()) writeSize = src.remaining();
			else writeSize = dest.remaining();
			byte[] buf = new byte[writeSize];
			src.get(buf);
			dest.put(buf);
			return writeSize;
		}
		finally
		{
			//writeLock.unlock();
		}
	}

	/**
	 * Reads a sequence of bytes from this channel into the given buffer.
	 * @param dst The buffer into which bytes are to be transferred.
	 * @return The number of bytes read, possibly zero, or -1 if the channel has reached end-of-stream.
	 * @throws IOException If some other I/O error occurs.
	 * @see java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)
	 */
	public int read(ByteBuffer dst) throws IOException
	{
		readLock.lock();
		try
		{
			ByteBuffer src = channel.getWriteBuffer();
			int readSize = 0;
			if (dst.remaining() <= src.remaining()) readSize = dst.remaining();
			else readSize = src.remaining();
			byte[] buf = new byte[readSize];
			src.get(buf);
			dst.put(buf);
			return readSize;
		}
		finally
		{
			readLock.unlock();
		}
	}

	/**
	 * Determine whether this channel is currently performing a read operation.
	 * @return true if this channel is currently reading, false otherwise.
	 */
	public synchronized boolean isReading()
	{
		return reading;
	}

	/**
	 * Specify whether this channel is currently performing a read operation.
	 * @param reading true to set this channel as reading, false otherwise.
	 */
	public synchronized void setReading(boolean reading)
	{
		this.reading = reading;
	}

	/**
	 * Determine whether this channel is currently performing a writing operation.
	 * @return true if this channel is currently writing, false otherwise.
	 */
	public synchronized boolean isWriting()
	{
		return writing;
	}

	/**
	 * Specify whether this channel is currently performing a write operation.
	 * @param writing true to set this channel as writing, false otherwise.
	 */
	public synchronized void setWriting(boolean writing)
	{
		this.writing = writing;
	}

	/**
	 * Get the buffer used for read operations.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public ByteBuffer getReadBuffer()
	{
		return readBuffer;
	}

	/**
	 * Get the buffer used for write operations.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public ByteBuffer getWriteBuffer()
	{
		return writeBuffer;
	}

	/**
	 * Get the other end-point of this channel.
	 * @return a <code>JPPFSelectableChannel</code> instance.
	 */
	public JPPFSelectableChannel getChannel()
	{
		return channel;
	}

	/**
	 * Set the other end-point of this channel.
	 * @param channel a <code>JPPFSelectableChannel</code> instance.
	 */
	public void setChannel(JPPFSelectableChannel channel)
	{
		this.channel = channel;
	}

	/**
	 * Suspend the current thread.
	 */
	private synchronized void sleep()
	{
		try
		{
			wait();
		}
		catch(InterruptedException e)
		{
		}
	}

	/**
	 * Notify all the threads waiting on this channel.
	 */
	private synchronized void wakeup()
	{
		notifyAll();
	}
}
