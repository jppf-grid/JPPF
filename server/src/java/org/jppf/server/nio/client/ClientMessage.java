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

package org.jppf.server.nio.client;

import java.nio.channels.*;

import org.jppf.io.*;
import org.jppf.server.nio.*;
import org.jppf.server.nio.nodeserver.AbstractNodeMessage;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Representation of a message sent or received by a remote node.
 * @author Laurent Cohen
 */
public class ClientMessage extends AbstractNodeMessage
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(ClientMessage.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
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
	 * Read data from the channel.
	 * @param wrapper the channel to read from.
	 * @return true if the data has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	@Override
    public boolean read(ChannelWrapper<?> wrapper) throws Exception
	{
		if (nbObjects <= 0)
		{
			if (position != 0) position = 0;
			if (!readNextObject(wrapper)) return false;
			bundle = (JPPFTaskBundle) IOHelper.unwrappedData(locations.get(0), new SerializationHelperImpl().getSerializer());
			nbObjects = bundle.getTaskCount() + 2;
			if (debugEnabled) log.debug("received header from client, data length = " + locations.get(0).getSize());
			if (bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME_MILLIS) == null)
				bundle.setParameter(BundleParameter.JOB_RECEIVED_TIME_MILLIS, System.currentTimeMillis());
		}
		while (position < nbObjects)
		{
			if (!readNextObject(wrapper)) return false;
		}
		return true;
	}

	/**
	 * Read data from the channel.
	 * @param wrapper the channel to write to.
	 * @return true if the data has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	@Override
    public boolean write(ChannelWrapper<?> wrapper) throws Exception
	{
		if (nbObjects <= 0)
		{
			nbObjects = bundle.getTaskCount() + 1;
			position = 0;
		}
		while (position < nbObjects)
		{
			if (!writeNextObject(wrapper)) return false;
		}
		return true;
	}

	/**
	 * Read the next serializable object from the specified channel.
	 * @param wrapper the channel to read from.
	 * @return true if the object has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	@Override
    protected boolean readNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		SocketChannel channel = (SocketChannel) ((SelectionKeyWrapper) wrapper).getChannel().channel();
		if (currentLengthObject == null)
		{
			currentLengthObject = new NioObject(4, false);
		}
		InputSource is = new ChannelInputSource(channel);
		if (!currentLengthObject.read(is)) return false;
		if (currentLength <= 0)
		{
			//currentLength = ((ByteBufferLocation) currentLengthObject.getData()).buffer().getInt();
			currentLength = SerializationUtils.readInt(currentLengthObject.getData().getInputStream());
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
	@Override
    protected boolean writeNextObject(ChannelWrapper<?> wrapper) throws Exception
	{
		SocketChannel channel = (SocketChannel) ((SelectionKey) wrapper.getChannel()).channel();
		if (currentLengthObject == null)
		{
			currentLengthObject = new NioObject(4, false);
			SerializationUtils.writeInt(locations.get(position).getSize(), currentLengthObject.getData().getOutputStream());
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

	/**
	 * {@inheritDoc}
	 */
	@Override
    public String toString()
	{
		StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
		sb.append("nb locations=").append(locations == null ? -1 : locations.size());
		sb.append(", position=").append(position);
		sb.append(", nbObjects=").append(nbObjects);
		sb.append(", length=").append(length);
		sb.append(", count=").append(count);
		sb.append(", currentLength=").append(currentLength);
		sb.append(']');
		return sb.toString();
	}
}
