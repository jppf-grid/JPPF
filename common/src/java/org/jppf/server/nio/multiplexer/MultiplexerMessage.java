/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.nio.multiplexer;

import java.nio.channels.SocketChannel;

import org.jppf.classloader.JPPFResourceWrapper;
import org.jppf.io.*;
import org.jppf.server.nio.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class encapsulates messages sent to or from a multiplexer, with the associated read and write operations (non blocking). 
 * @author Laurent Cohen
 */
public class MultiplexerMessage
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(MultiplexerMessage.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * The location containing the data to send or receive.
	 */
	DataLocation location = null;
	/**
	 * The size of the data.
	 */
	int length = -1;
	/**
	 * The current count of bytes sent or received.
	 */
	int count = 0;
	/**
	 * Object storing the length of the object currently being read or written.
	 */
	NioObject currentLengthObject = null;
	/**
	 * Object storing the object currently being read or written.
	 */
	NioObject currentObject = null;

	/**
	 * Read the next serializable object from the specified channel.
	 * @param wrapper the channel to read from.
	 * @return true if the object has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected boolean read(ChannelWrapper<?> wrapper) throws Exception
	{
		SocketChannel channel = (SocketChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (currentLengthObject == null) currentLengthObject = new NioObject(4, false);
		InputSource is = new ChannelInputSource(channel);
		if (!currentLengthObject.read(is)) return false;
		if (length <= 0)
		{
			length = SerializationUtils.readInt(currentLengthObject.getData().getInputStream());
			count += 4;
			if (traceEnabled) log.trace("read length=" + length + " from channel " + wrapper);
		}
		if (currentObject == null)
		{
			DataLocation location = IOHelper.createDataLocationMemorySensitive(length);
			currentObject = new NioObject(location, false);
		}
		if (!currentObject.read(is)) return false;
		count += length;
		location = currentObject.getData();
		currentLengthObject = null;
		currentObject = null;
		length = 0;
		if (traceEnabled) deserialize();
		return true;
	}

	/**
	 * Write the next object to the specified channel.
	 * @param wrapper the channel to write to.
	 * @return true if the object has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected boolean write(ChannelWrapper<?> wrapper) throws Exception
	{
		SocketChannel channel = (SocketChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (currentLengthObject == null)
		{
			currentLengthObject = new NioObject(4, false);
			SerializationUtils.writeInt(location.getSize(), currentLengthObject.getData().getOutputStream());
			if (traceEnabled) log.trace("writing length=" + location.getSize() + " to channel " + wrapper);
		}
		OutputDestination od = new ChannelOutputDestination(channel);
		if (!currentLengthObject.write(od)) return false;
		if (currentObject == null) currentObject = new NioObject(location.copy(), false);
		if (!currentObject.write(od)) return false;
		count += 4 + location.getSize();
		currentLengthObject = null;
		currentObject = null;
		return true;
	}

	/**
	 * This method is for debugging purposes ONLY.
	 */
	void deserialize()
	{
		if (traceEnabled)
		{
			try
			{
				Object o = new ObjectSerializerImpl().deserialize(location.getInputStream());
				if (o instanceof JPPFResourceWrapper) o = toString((JPPFResourceWrapper) o);
				log.trace("current object in message=" + o);
			}
			catch(Throwable t)
			{
				log.trace(t.getClass().getSimpleName() + " : " + t.getMessage());
			}
		}
	}

	/**
	 * Prints a resource wrapper to a string.
	 * @param res the resource to print.
	 * @return astring representation of the resource wrapper.
	 */
	String toString(JPPFResourceWrapper res)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(JPPFResourceWrapper.class.getSimpleName()).append("[");
		sb.append("name=").append(res.getName());
		sb.append(",state=").append(res.getState());
		sb.append(",definition=");
		byte[] def = res.getDefinition();
		if (def == null) sb.append("null");
		else sb.append("byte[").append(def.length).append("]");
		sb.append("]");
		return sb.toString();
	}
}
