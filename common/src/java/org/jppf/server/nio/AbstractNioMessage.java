/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.io.*;
import java.util.*;

import org.jppf.io.*;
import org.jppf.utils.SerializationUtils;
import org.jppf.utils.streams.StreamUtils;

/**
 * Common abstract superclass representing a message sent or received by a node.
 * A message is the transformation of a job into an more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractNioMessage implements NioMessage
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
   * <code>true</code> is data is read from or wirtten an SSL connection, <code>false</code> otherwise.
   */
  protected boolean ssl = false;

  /**
   * Initialize this nio message with the specified sll flag.
   * @param ssl <code>true</code> is data is read from or wirtten an SSL connection, <code>false</code> otherwise.
   */
  protected AbstractNioMessage(final boolean ssl)
  {
    this.ssl = ssl;
  }

  /**
   * Add a location to the data locations of this message.
   * @param location the location to add.
   */
  public void addLocation(final DataLocation location)
  {
    locations.add(location);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean read(final ChannelWrapper<?> channel) throws Exception
  {
    if (nbObjects <= 0)
    {
      if (position != 0) position = 0;
      if (!readNextObject(channel)) return false;
      afterFirstRead();
    }
    while (position < nbObjects)
    {
      if (!readNextObject(channel)) return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean write(final ChannelWrapper<?> channel) throws Exception
  {
    if (nbObjects <= 0)
    {
      position = 0;
      beforeFirstWrite();
    }
    while (position < nbObjects)
    {
      if (!writeNextObject(channel)) return false;
    }
    return true;
  }

  /**
   * Read the next serializable object from the specified channel.
   * @param channel the channel to read from.
   * @return true if the object has been completely read from the channel, false otherwise.
   * @throws Exception if an IO error occurs.
   */
  protected boolean readNextObject(final ChannelWrapper<?> channel) throws Exception
  {
    if (currentLengthObject == null)
    {
      currentLengthObject = ssl ? new SSLNioObject(channel, 4) : new PlainNioObject(channel, 4, false);
    }
    if (!currentLengthObject.read()) return false;
    if (currentLength <= 0)
    {
      InputStream is = currentLengthObject.getData().getInputStream();
      try
      {
        currentLength = SerializationUtils.readInt(is);
      }
      finally
      {
        StreamUtils.close(is);
      }
      count += 4;
    }
    if (currentObject == null)
    {
      DataLocation location = IOHelper.createDataLocationMemorySensitive(currentLength);
      currentObject = ssl ? new SSLNioObject(channel, location) : new PlainNioObject(channel, location, false);
    }
    if (!currentObject.read()) return false;
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
   * @param channel the channel to write to.
   * @return true if the object has been completely written the channel, false otherwise.
   * @throws Exception if an IO error occurs.
   */
  protected boolean writeNextObject(final ChannelWrapper<?> channel) throws Exception
  {
    if (currentLengthObject == null)
    {
      currentLengthObject = ssl ? new SSLNioObject(channel, 4) : new PlainNioObject(channel, 4, false);
      OutputStream os = currentLengthObject.getData().getOutputStream();
      try
      {
        SerializationUtils.writeInt(locations.get(position).getSize(), os);
      }
      finally
      {
        StreamUtils.close(os);
      }
    }
    if (!currentLengthObject.write()) return false;
    if (currentObject == null)
    {
      DataLocation loc = locations.get(position);
      currentObject = ssl ? new SSLNioObject(channel, loc.copy()) : new PlainNioObject(channel, loc.copy(), false);
    }
    if (!currentObject.write()) return false;
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
   * Actions to take after the first object in the message has been fully read.
   * @throws Exception if an IO error occurs.
   */
  protected void afterFirstRead() throws Exception
  {
  }

  /**
   * Actions to take before the first object in the message is written.
   * @throws Exception if an IO error occurs.
   */
  protected void beforeFirstWrite() throws Exception
  {
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

  @Override
  public boolean isSSL()
  {
    return ssl;
  }
}
