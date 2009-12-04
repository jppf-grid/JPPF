/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.nio.channels.*;
import java.util.*;

import org.jppf.io.*;
import org.jppf.utils.SerializationUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeMessage
{
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
	private List<DataLocation> locations = new ArrayList<DataLocation>();
	/**
	 * The current position in the list of data locations.
	 */
	private int position = -1;
	/**
	 * The current number of bytes send or received for the location at the current position.
	 */
	private int locationCount = 0;
	/**
	 * The length of the location at the current position.
	 */
	private int locationLength = 0;
	/**
	 * An input source wrapping the channel from where data is read.
	 */
	private InputSource is = null;
	/**
	 * An input source wrapping the channel from where data is read.
	 */
	private OutputDestination od = null;
	/**
	 * Determine whether writing to or reading from a channel has started for the next bundle. 
	 */
	private boolean started = false;

	/**
	 * Add a location to the data locations of this message.
	 * @param location the location to add.
	 */
	public void addLocation(DataLocation location)
	{
		locations.add(location);
	}

	/**
	 * Read a bundle from the channel.
	 * @param channel the channel to read from.
	 * @return true if the bundle has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	public boolean read(ReadableByteChannel channel) throws Exception
	{
		if (!started)
		{
			started = true;
			length = SerializationUtils.readInt(channel);
		}
		DataLocation location = null;
		if (locationCount < locationLength)
		{
			location = locations.get(position);
		}
		else
		{
			//int n = is.readInt();
			int n = SerializationUtils.readInt(channel);
			location = IOHelper.createDataLocationMemorySensitive(n);
			locations.add(location);
			locationLength = location.getSize();
			position++;
			count += 4;
		}
		//int n = location.transferFrom(is, false);
		int n = location.transferFrom(channel, false);
		if (n > 0) locationCount += n;
		if ((n == -1) || (locationCount >= locationLength))
		{
			count += locationLength;
			locationCount = 0;
			locationLength = 0;
		}
		return count >= length;
	}

	/**
	 * Write a bundle to the channel.
	 * @param channel the channel to write to.
	 * @return true if the bundle has been completely written to the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	public boolean write(WritableByteChannel channel) throws Exception
	{
		if (!started)
		{
			started = true;
			for (DataLocation dl: locations) length += 4 + dl.getSize();
			SerializationUtils.writeInt(channel, length);
			position = 0;
		}
		DataLocation location = locations.get(position);
		if (locationCount == 0)
		{
			//od.writeInt(location.getSize());
			SerializationUtils.writeInt(channel, location.getSize());
			locationLength = location.getSize();
			count += 4;
		}
		//int n = location.transferTo(od, false);
		int n = location.transferTo(channel, false);
		if (n > 0) locationCount += n;
		if ((n == -1) || (locationCount >= locationLength))
		{
			count += locationLength;
			locationCount = 0;
			locationLength = 0;
			position++;
		}
		return count >= length;
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
}
