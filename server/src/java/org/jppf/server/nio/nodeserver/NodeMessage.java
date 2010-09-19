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
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.data.transform.JPPFDataTransformFactory;
import org.jppf.io.*;
import org.jppf.server.nio.NioObject;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeMessage
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeMessage.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean traceEnabled = log.isTraceEnabled();
	/**
	 * The current count of bytes sent or received.
	 */
	private int count = 0;
	/**
	 * The total length of data to send or receive.
	 */
	private int length = 0;
	/**
	 * The data location objects abstracting the data to send or receive.
	 */
	private LinkedList<DataLocation> locations = new LinkedList<DataLocation>();
	/**
	 * The current position in the list of data locations.
	 */
	private int position = 0;
	/**
	 * The number of objects toread or write.
	 */
	private int nbObjects = -1;
	/**
	 * The length of the location at the current position.
	 */
	private int currentLength = 0;
	/**
	 * Object storing the length of the object currently being read or written.
	 */
	private NioObject currentLengthObject = null;
	/**
	 * Object storing the object currently being read or written.
	 */
	private NioObject currentObject = null;
	/**
	 * The latest bundle that was sent or received.
	 */
	private JPPFTaskBundle bundle = null;

	/**
	 * Add a location to the data locations of this message.
	 * @param location the location to add.
	 */
	public void addLocation(DataLocation location)
	{
		locations.add(location);
	}

	/**
	 * Read data from the channel.
	 * @param channel the channel to read from.
	 * @return true if the data has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	public boolean read(ReadableByteChannel channel) throws Exception
	{
		try
		{
		if (nbObjects <= 0)
		{
			if (!readNextObject(channel)) return false;
			InputStream is = locations.get(0).getInputStream();
			byte[] data = FileUtils.getInputStreamAsByte(is);
			data = JPPFDataTransformFactory.transform(false, data, 0, data.length);
			SerializationHelper helper = new SerializationHelperImpl();
			bundle = (JPPFTaskBundle) helper.getSerializer().deserialize(data);
			nbObjects = bundle.getTaskCount() + 1;
		}
		while (position < nbObjects)
		{
			if (!readNextObject(channel)) return false;
		}
		return true;
		}
		catch(Error e)
		{
			log.error(e.getMessage(), e);
			throw e;
		}
	}

	/**
	 * Read the next serializable object from the specified channel.
	 * @param channel the channel to read from.
	 * @return true if the object has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	private boolean readNextObject(ReadableByteChannel channel) throws Exception
	{
		if (currentLengthObject == null) currentLengthObject = new NioObject(4, false);
		InputSource is = new ChannelInputSource(channel);
		if (!currentLengthObject.read(is)) return false;
		if (currentLength <= 0)
		{
			currentLength = ((ByteBufferLocation) currentLengthObject.getData()).buffer().getInt();
			count += 4;
			if (traceEnabled) log.trace(StringUtils.getRemoteHost(channel) + " : position=" + position + ", count = " + count + ", object size=" + currentLength);
		}
		if (currentObject == null)
		{
			DataLocation location = IOHelper.createDataLocationMemorySensitive(currentLength);
			currentObject = new NioObject(location, false);
		}
		if (!currentObject.read(is)) return false;
		count += currentLength;
		locations.add(currentObject.getData());
		if (traceEnabled) log.trace(StringUtils.getRemoteHost(channel) + " : position=" + position + ", count = " + count + ", object size=" + currentLength + ", read complete");
		currentLengthObject = null;
		currentObject = null;
		currentLength = 0;
		position++;
		return true;
	}

	/**
	 * Read data from the channel.
	 * @param channel the channel to write to.
	 * @return true if the data has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	public boolean write(WritableByteChannel channel) throws Exception
	{
		if (nbObjects <= 0)
		{
			nbObjects = bundle.getTaskCount() + 2;
		}
		//if (!writeLength(channel)) return false;
		while (position < nbObjects)
		{
			if (!writeNextObject(channel)) return false;
		}
		return true;
	}

	/**
	 * Write the next object to the specified channel.
	 * @param channel - the channel to write to.
	 * @return true if the object has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	private boolean writeNextObject(WritableByteChannel channel) throws Exception
	{
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

	/**
	 * Get the data location objects abstracting the data to send or receive.
	 * @return a list of <code>DataLocation</code> objects.
	 */
	public List<DataLocation> getLocations()
	{
		return locations;
	}

	/**
	 * Get the total length of data to send or receive.
	 * @return the length as an int. 
	 */
	public int getLength()
	{
		return length;
	}

	/**
	 * Get the latest bundle that was sent or received.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle getBundle()
	{
		return bundle;
	}

	/**
	 * Set the latest bundle that was sent or received.
	 * @param bundle - a <code>JPPFTaskBundle</code> instance.
	 */
	public void setBundle(JPPFTaskBundle bundle)
	{
		this.bundle = bundle;
	}
}
