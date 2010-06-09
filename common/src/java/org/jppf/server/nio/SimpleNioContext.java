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

package org.jppf.server.nio;

import java.nio.ByteBuffer;
import java.nio.channels.*;

import org.apache.commons.logging.*;
import org.jppf.utils.*;

/**
 * Context associated with an open socket channel.
 * @param <S> the type of states associated with this context.
 * @author Laurent Cohen
 */
public abstract class SimpleNioContext<S extends Enum> extends AbstractNioContext<S>
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(SimpleNioContext.class);
	/**
	 * Determines whther TRACE logging level is enabled.
	 */
	protected static boolean traceEnabled = log.isTraceEnabled();

	/**
	 * Read data from a channel.
	 * @param wrapper the channel to read the data from.
	 * @return true if all the data has been read, false otherwise.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public boolean readMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		ReadableByteChannel channel = (ReadableByteChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (message == null) message = new NioMessage();
		if (message.length <= 0)
		{
			message.length = SerializationUtils.readInt(channel);
			message.buffer = ByteBuffer.allocate(message.length);
			readByteCount = 0;
		}
		readByteCount += channel.read(message.buffer);
		if (traceEnabled)
		{
			log.trace("[" + getShortClassName() + "] " + "read " + readByteCount + " bytes out of " +
				message.length + " for " + StringUtils.getRemoteHost((SocketChannel) channel));
		}
		return readByteCount >= message.length;
	}

	/**
	 * Write data to a channel.
	 * @param wrapper the channel to write the data to.
	 * @return true if all the data has been written, false otherwise.
	 * @throws Exception if an error occurs while writing the data.
	 */
	public boolean writeMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		WritableByteChannel channel = (WritableByteChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (!message.lengthWritten)
		{
			SerializationUtils.writeInt(channel, message.length);
			message.lengthWritten = true;
			writeByteCount = 0;
		}
		writeByteCount += channel.write(message.buffer);
		if (traceEnabled)
		{
			log.trace("[" + getShortClassName() + "] " + "written " + writeByteCount + " bytes out of " +
				message.length + " for " + StringUtils.getRemoteHost((SelectableChannel) channel));
		}
		return writeByteCount >= message.length;
	}
}
