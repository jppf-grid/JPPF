/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.utils.StringUtils;

/**
 * Context associated with an open socket channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class NioContext<S extends Enum>
{
	/**
	 * Logger for this class.
	 */
	protected static final Log LOG = LogFactory.getLog(NioContext.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	protected static final boolean DEBUG_ENABLED = LOG.isDebugEnabled();
	/**
	 * The current state of the channel this context is associated with.
	 */
	protected S state = null;
	/**
	 * Container for the current message data.
	 */
	protected NioMessage message = null;
	/**
	 * Count of bytes read.
	 */
	protected int readByteCount = 0;
	/**
	 * Count of bytes written.
	 */
	protected int writeByteCount = 0;
	/**
	 * Uuid for this node context.
	 */
	protected String uuid = null;

	/**
	 * Get the current state of the channel this context is associated with.
	 * @return a state enum value.
	 */
	public S getState()
	{
		return state;
	}

	/**
	 * Set the current state of the channel this context is associated with.
	 * @param state a state enum value.
	 */
	public void setState(S state)
	{
		this.state = state;
	}

	/**
	 * Read data from a channel.
	 * @param channel the channel to read the data from.
	 * @return true if all the data has been read, false otherwise.
	 * @throws IOException if an error occurs while reading the data.
	 */
	public boolean readMessage(ReadableByteChannel channel) throws IOException
	{
		if (message == null) message = new NioMessage();
		if (message.length <= 0)
		{
			message.length = readInt(channel);
			//message.buffer = ByteBuffer.wrap(new byte[message.length]);
			message.buffer = ByteBuffer.allocateDirect(message.length);
			readByteCount = 0;
		}
		readByteCount += channel.read(message.buffer);
		if (DEBUG_ENABLED)
		{
			LOG.debug("[" + getShortClassName() + "] " + "read " + readByteCount + " bytes out of " +
				message.length + " for " + StringUtils.getRemoteHost((SocketChannel) channel));
		}
		return readByteCount >= message.length;
	}

	/**
	 * Read an integer value from a channel.
	 * @param channel the channel to read from.
	 * @return the value read from the channel.
	 * @throws IOException if an error occurs while reading the data.
	 */
	public int readInt(ReadableByteChannel channel) throws IOException
	{
		ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
		int count = 0;
		while (count < 4)
		{
			count += channel.read(buf);
			if (count < 0) throw new ClosedChannelException();
		}
		buf.flip();
		return buf.getInt();
	}

	/**
	 * Write data to a channel.
	 * @param channel the channel to write the data to.
	 * @return true if all the data has been written, false otherwise.
	 * @throws IOException if an error occurs while writing the data.
	 */
	public boolean writeMessage(WritableByteChannel channel) throws IOException
	{
		if (!message.lengthWritten)
		{
			writeInt(channel, message.length);
			message.lengthWritten = true;
			writeByteCount = 0;
		}
		writeByteCount += channel.write(message.buffer);
		if (DEBUG_ENABLED)
		{
			LOG.debug("[" + getShortClassName() + "] " + "written " + writeByteCount + " bytes out of " +
				message.length + " for " + StringUtils.getRemoteHost((SelectableChannel) channel));
		}
		return writeByteCount >= message.length;
	}

	/**
	 * Wrtie an integer value to a channel.
	 * @param channel the channel to write to.
	 * @param value the value to write.
	 * @throws IOException if an error occurs while writing the data.
	 */
	public void writeInt(WritableByteChannel channel, int value) throws IOException
	{
		ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
		buf.putInt(value);
		buf.flip();
		int count = 0;
		while (count < 4)
		{
			count += channel.write(buf);
			if (count < 0) throw new ClosedChannelException();
		}
	}

	/**
	 * Get the container for the current message data.
	 * @return an <code>NioMessage</code> instance.
	 */
	public NioMessage getMessage()
	{
		return message;
	}

	/**
	 * Set the container for the current message data.
	 * @param message an <code>NioMessage</code> instance.
	 */
	public void setMessage(NioMessage message)
	{
		this.message = message;
	}

	/**
	 * Get the uuid for this node context.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Set the uuid for this node context.
	 * @param uuid the uuid as a string.
	 */
	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	/**
	 * Give the non qualified name of the class of this instance.
	 * @return a class name as a string.
	 */
	protected String getShortClassName()
	{
		String fqn = getClass().getName();
		int idx = fqn.lastIndexOf(".");
		return fqn.substring(idx + 1);
	}

	/**
	 * Handle the cleanup when an exception occurs on the channel.
	 * @param channel the channel that threw the exception.
	 */
	public abstract void handleException(SocketChannel channel);
}
