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

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.jppf.io.*;
import org.jppf.server.nio.*;

/**
 * Representation of a message sent or received by a remote node.
 * @author Laurent Cohen
 */
public class RemoteNodeMessage extends AbstractNodeMessage
{
	/**
	 * The length of the location at the current position.
	 */
	protected int currentLength = 0;
	/**
	 * Object storing the length of the object currently being read or written.
	 */
	protected NioObject currentLengthObject = null;
	/**
	 * Object storing the object currently being read or written.
	 */
	protected NioObject currentObject = null;

	/**
	 * Read the next serializable object from the specified channel.
	 * @param wrapper the channel to read from.
	 * @return true if the object has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected boolean readNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		SocketChannel channel = (SocketChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (currentLengthObject == null) currentLengthObject = new NioObject(4, false);
		InputSource is = new ChannelInputSource(channel);
		if (!currentLengthObject.read(is)) return false;
		if (currentLength <= 0)
		{
			currentLength = ((ByteBufferLocation) currentLengthObject.getData()).buffer().getInt();
			count += 4;
		}
		if (currentObject == null)
		{
			DataLocation location = IOHelper.createDataLocationMemorySensitive(currentLength);
			currentObject = new NioObject(location, false);
		}
		if (!currentObject.read(is)) return false;
		count += currentLength;
		locations.add(currentObject.getData());
		currentLengthObject = null;
		currentObject = null;
		currentLength = 0;
		position++;
		return true;
	}

	/**
	 * Write the next object to the specified channel.
	 * @param wrapper the channel to write to.
	 * @return true if the object has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected boolean writeNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		SocketChannel channel = (SocketChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (currentLengthObject == null)
		{
			currentLengthObject = new NioObject(4, false);
			ByteBuffer buffer = ((ByteBufferLocation) currentLengthObject.getData()).buffer();
			buffer.putInt(locations.get(position).getSize());
			buffer.flip();
		}
		OutputDestination od = new ChannelOutputDestination(channel);
		if (!currentLengthObject.write(od)) return false;
		if (currentObject == null)
		{
			DataLocation loc = locations.get(position);
			currentObject = new NioObject(loc.copy(), false);
		}
		if (!currentObject.write(od)) return false;
		count += 4 + locations.get(position).getSize();
		position++;
		currentLengthObject = null;
		currentObject = null;
		return true;
	}
}
