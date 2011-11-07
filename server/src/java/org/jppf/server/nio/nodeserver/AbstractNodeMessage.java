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

package org.jppf.server.nio.nodeserver;

import java.util.*;

import org.jppf.io.*;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.SerializationHelperImpl;

/**
 * Common abstract superclass representing a message sent or received by a node.
 * A message is the transformation of a job into an more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractNodeMessage
{
	/**
	 * The current count of bytes sent or received.
	 */
	protected int count = 0;
	/**
	 * The total length of data to send or receive.
	 */
	protected int length = 0;
	/**
	 * The data location objects abstracting the data to send or receive.
	 */
	protected List<DataLocation> locations = new ArrayList<DataLocation>();
	/**
	 * The current position in the list of data locations.
	 */
	protected int position = 0;
	/**
	 * The number of objects to read or write.
	 */
	protected int nbObjects = -1;
	/**
	 * The latest bundle that was sent or received.
	 */
	protected JPPFTaskBundle bundle = null;

	/**
	 * Add a location to the data locations of this message.
	 * @param location the location to add.
	 */
	public void addLocation(final DataLocation location)
	{
		locations.add(location);
	}

	/**
	 * Read data from the channel.
	 * @param channel the channel to read from.
	 * @return true if the data has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	public boolean read(final ChannelWrapper<?> channel) throws Exception
	{
		if (nbObjects <= 0)
		{
			if (position != 0) position = 0;
			if (!readNextObject(channel)) return false;
			bundle = (JPPFTaskBundle) IOHelper.unwrappedData(locations.get(0), new SerializationHelperImpl().getSerializer());
			nbObjects = bundle.getTaskCount() + 1;
		}
		while (position < nbObjects)
		{
			if (!readNextObject(channel)) return false;
		}
		return true;
	}

	/**
	 * Read the next serializable object from the specified channel.
	 * @param channel the channel to read from.
	 * @return true if the object has been completely read from the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected abstract boolean readNextObject(ChannelWrapper<?> channel) throws Exception;

	/**
	 * Read data from the channel.
	 * @param channel the channel to write to.
	 * @return true if the data has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	public boolean write(final ChannelWrapper<?> channel) throws Exception
	{
		if (nbObjects <= 0)
		{
			nbObjects = bundle.getTaskCount() + 2;
			position = 0;
		}
		while (position < nbObjects)
		{
			if (!writeNextObject(channel)) return false;
		}
		return true;
	}

	/**
	 * Write the next object to the specified channel.
	 * @param channel the channel to write to.
	 * @return true if the object has been completely written the channel, false otherwise.
	 * @throws Exception if an IO error occurs.
	 */
	protected abstract boolean writeNextObject(ChannelWrapper<?> channel) throws Exception;

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
	public void setBundle(final JPPFTaskBundle bundle)
	{
		this.bundle = bundle;
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
		sb.append(']');
		return sb.toString();
	}
}
