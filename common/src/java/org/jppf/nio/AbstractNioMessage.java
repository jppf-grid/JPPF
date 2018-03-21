/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.nio;

import java.io.InputStream;
import java.util.*;

import org.jppf.io.*;
import org.jppf.serialization.SerializationUtils;
import org.slf4j.*;

/**
 * Common abstract superclass representing a message sent or received by a channel.
 * A message is the transformation of a sequence of objects into a more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractNioMessage extends AbstractNioMessageBase {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractNioMessage.class);
  /**
   * The total length of data to send or receive, used for tracing and debugging purposes only.
   */
  protected int length;
  /**
   * The data location objects abstracting the data to send or receive.
   */
  protected final List<DataLocation> locations = new ArrayList<>();
  /**
   * The current position in the list of data locations.
   */
  protected int position;
  /**
   * The number of objects to read or write.
   */
  protected int nbObjects = -1;

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   * @param debug to enable debug-level logging.
   */
  protected AbstractNioMessage(final NioContext<?> channel, final boolean debug) {
    super(channel, debug);
  }

  /**
   * Initialize this nio message with the specified sll flag.
   * @param channel the channel to read from or write to.
   */
  protected AbstractNioMessage(final ChannelWrapper<?> channel) {
    this(channel.getContext(), false);
  }

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   * @param debug to enable debug-level logging.
   */
  protected AbstractNioMessage(final ChannelWrapper<?> channel, final boolean debug) {
    this(channel.getContext(), debug);
  }

  /**
   * Add a location to the data locations of this message.
   * @param location the location to add.
   */
  public void addLocation(final DataLocation location) {
    locations.add(location);
  }

  @Override
  public boolean read() throws Exception {
    if (nbObjects <= 0) {
      if (!readNextObject()) return false;
      afterFirstRead();
    }
    while (position < nbObjects) {
      if (!readNextObject()) return false;
    }
    return true;
  }

  @Override
  public boolean write() throws Exception {
    if (nbObjects <= 0) {
      if (debug) {
        for (DataLocation dl: locations) length += dl.getSize();
        length += 4 * locations.size();
      }
      beforeFirstWrite();
    }
    while (position < nbObjects) {
      if (!writeNextObject()) return false;
    }
    return true;
  }

  /**
   * Read the next serializable object from the specified channel.
   * @return true if the object has been completely read from the channel, false otherwise.
   * @throws Exception if an IO error occurs.
   */
  protected boolean readNextObject() throws Exception {
    if (currentLengthObject == null) {
      lengthBuf.reset();
      currentLengthObject = ssl ? new SSLNioObject(lengthBuf, sslHandler) : new PlainNioObject(channel.getSocketChannel(), lengthBuf);
    }
    if (currentLength < 0) {
      try {
        if (!currentLengthObject.read()) return false;
      } catch(final Exception e) {
        updateCounts(currentLengthObject.getChannelCount(), READ);
        throw e;
      }
      updateCounts(currentLengthObject.getChannelCount(), READ);
      try (InputStream is = currentLengthObject.getData().getInputStream()) {
        currentLength = SerializationUtils.readInt(is);
      }
      count += 4;
    }
    if (currentLength > 0) {
      if (currentObject == null) {
        final DataLocation location = IOHelper.createDataLocationMemorySensitive(currentLength);
        currentObject = ssl ? new SSLNioObject(location, sslHandler) : new PlainNioObject(channel.getSocketChannel(), location);
      }
      try {
        if (!currentObject.read()) return false;
      } catch(final Exception e) {
        updateCounts(currentObject.getChannelCount(), READ);
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) updateCounts(currentObject.getChannelCount(), READ);
    locations.add(currentObject == null ? null : currentObject.getData());
    currentLengthObject = null;
    currentObject = null;
    currentLength = -1;
    if (debug) log.debug("channel id={} read object at position {}", channel.getChannel().getId(), position);
    position++;
    return true;
  }

  /**
   * Write the next object to the specified channel.
   * @return true if the object has been completely written the channel, false otherwise.
   * @throws Exception if an IO error occurs.
   */
  protected boolean writeNextObject() throws Exception {
    if (currentLengthObject == null) {
      currentDataLocation = locations.get(position);
      SerializationUtils.writeInt(currentDataLocation.getSize(), lengthBuf.reset().getBuffer(0).buffer, 0);
      currentLengthObject = ssl ? new SSLNioObject(lengthBuf, sslHandler) : new PlainNioObject(channel.getSocketChannel(), lengthBuf);
    }
    if (currentLength < 0) {
      try {
        if (!currentLengthObject.write()) return false;
      } catch(final Exception e) {
        updateCounts(currentLengthObject.getChannelCount(), WRITE);
        throw e;
      }
      currentLength = currentDataLocation.getSize();
      count += 4;
      updateCounts(currentLengthObject.getChannelCount(), WRITE);
    }
    if (currentLength > 0) {
      if (currentObject == null) {
        final DataLocation loc = currentDataLocation.copy();
        currentObject = ssl ? new SSLNioObject(loc, sslHandler) : new PlainNioObject(channel.getSocketChannel(), loc);
      }
      try {
        if (!currentObject.write()) return false;
      } catch(final Exception e) {
        updateCounts(currentObject.getChannelCount(), WRITE);
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) updateCounts(currentObject.getChannelCount(), WRITE);
    if (debug) log.debug("channel id={} wrote object at position {}", channel.getChannel().getId(), position);
    position++;
    currentLengthObject = null;
    currentObject = null;
    currentLength = -1;
    currentDataLocation = null;
    return true;
  }

  /**
   * Get the data location objects abstracting the data to send or receive.
   * @return a list of <code>DataLocation</code> objects.
   */
  public List<DataLocation> getLocations() {
    return locations;
  }

  /**
   * Get the total length of data to send or receive.
   * @return the length as an int.
   */
  public int getLength() {
    return length;
  }

  /**
   * Actions to take after the first object in the message has been fully read.
   * @throws Exception if an IO error occurs.
   */
  protected void afterFirstRead() throws Exception {
  }

  /**
   * Actions to take before the first object in the message is written.
   * @throws Exception if an IO error occurs.
   */
  protected void beforeFirstWrite() throws Exception {
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("nb locations=").append(locations == null ? -1 : locations.size());
    sb.append(", position=").append(position);
    sb.append(", nbObjects=").append(nbObjects);
    sb.append(", length=").append(length);
    sb.append(", count=").append(count);
    sb.append(", currentObject=").append(currentObject);
    sb.append(']');
    return sb.toString();
  }
}
