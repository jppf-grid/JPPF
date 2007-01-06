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

package org.jppf.server.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.apache.log4j.Logger;
import org.jppf.utils.StringUtils;

/**
 * Context associated with an open socket channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class NioContext<S extends Enum>
{
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(NioContext.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
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
	int readByteCount = 0;
	/**
	 * Count of bytes written.
	 */
	int writeByteCount = 0;
	/**
	 * Uuid for this node context.
	 */
	private String uuid = null;

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
			ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
			int count = 0;
			while (count < 4)
			{
				count += channel.read(buf);
				if (count < 0) throw new ClosedChannelException();
			}
			buf.flip();
			message.length = buf.getInt();
			message.buffer = ByteBuffer.wrap(new byte[message.length]);
			readByteCount = 0;
		}
		readByteCount += channel.read(message.buffer);
		if (debugEnabled)
		{
			log.debug(
					"[" + getNonQualifiedClassName() + "] " +  
				"read " + readByteCount + " bytes out of " + message.length +
				" for " + StringUtils.getRemostHost((SocketChannel) channel));
		}
		return readByteCount >= message.length;
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
			ByteBuffer buf = ByteBuffer.wrap(new byte[4]);
			buf.putInt(message.length);
			buf.flip();
			int count = 0;
			while (count < 4)
			{
				count += channel.write(buf);
				if (count < 0) throw new ClosedChannelException();
			}
			message.lengthWritten = true;
			writeByteCount = 0;
		}
		writeByteCount += channel.write(message.buffer);
		if (debugEnabled)
		{
			log.debug(
				"[" + getNonQualifiedClassName() + "] " +  
				"written " + writeByteCount + " bytes out of " + message.length +
				" for " + StringUtils.getRemostHost((SocketChannel) channel));
		}
		return writeByteCount >= message.length;
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
	 * Gt the uuid for this node context.
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
	private String getNonQualifiedClassName()
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
