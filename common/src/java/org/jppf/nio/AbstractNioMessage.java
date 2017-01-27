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
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Common abstract superclass representing a message sent or received by a channel.
 * A message is the transformation of a sequence of objects into a more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractNioMessage implements NioMessage {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNioMessage.class);
  /**
   * The current count of bytes sent or received.
   */
  protected int count = 0;
  /**
   * The total length of data to send or receive, used for tracing and debugging purposes only.
   */
  protected int length = 0;
  /**
   * The data location objects abstracting the data to send or receive.
   */
  protected List<DataLocation> locations = new ArrayList<>();
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
  protected int currentLength = -1;
  /**
   * Object storing the length of the object currently being read or written.
   */
  protected NioObject currentLengthObject = null;
  /**
   * Object storing the object currently being read or written.
   */
  protected NioObject currentObject = null;
  /**
   * <code>true</code> is data is read from or written an SSL connection, <code>false</code> otherwise.
   */
  protected final boolean ssl;
  /**
   * Wraps a channel associated with an <code>SSLEngine</code>.
   */
  protected final SSLHandler sslHandler;
  /**
   * Determines whteher some low-level traces should be logged.
   */
  protected final boolean debug;
  /**
   * The channel to read from or write to.
   */
  protected final ChannelWrapper<?> channel;
  /**
   * Temporary holder used as a local performance optimization for write operations.
   */
  private DataLocation currentDataLocation = null;
  /**
   * Actual bytes sent to or received from the underlying channel.
   */
  protected long channelCount = 0L;

  /**
   * Initialize this nio message with the specified sll flag.
   * @param channel the channel to read from or write to.
   */
  protected AbstractNioMessage(final ChannelWrapper<?> channel) {
    this(channel, false);
  }

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   * @param debug to enable debug-level logging.
   */
  protected AbstractNioMessage(final ChannelWrapper<?> channel, final boolean debug) {
    this.channel = channel;
    this.sslHandler = channel.getContext().getSSLHandler();
    this.ssl = sslHandler != null;
    this.debug = debug;
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
    if (currentLengthObject == null) currentLengthObject = ssl ? new SSLNioObject(4, sslHandler) : new PlainNioObject(channel, 4);
    if (currentLength < 0) {
      try {
        if (!currentLengthObject.read()) return false;
      } catch(Exception e) {
        channelCount += currentLengthObject.getChannelCount();
        throw e;
      }
      channelCount += currentLengthObject.getChannelCount();
      InputStream is = currentLengthObject.getData().getInputStream();
      try {
        currentLength = SerializationUtils.readInt(is);
      } finally {
        StreamUtils.close(is);
      }
      count += 4;
    }
    if (currentLength > 0) {
      if (currentObject == null) {
        DataLocation location = IOHelper.createDataLocationMemorySensitive(currentLength);
        currentObject = ssl ? new SSLNioObject(location, sslHandler) : new PlainNioObject(channel, location);
      }
      try {
        if (!currentObject.read()) return false;
      } catch(Exception e) {
        channelCount += currentObject.getChannelCount();
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) channelCount += currentObject.getChannelCount();
    locations.add(currentObject == null ? null : currentObject.getData());
    currentLengthObject = null;
    currentObject = null;
    currentLength = -1;
    if (debug) log.debug("channel id={} read object at position {}", channel.getId(), position);
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
      byte[] bytes = SerializationUtils.writeInt(currentDataLocation.getSize());
      DataLocation dl = new MultipleBuffersLocation(bytes);
      currentLengthObject = ssl ? new SSLNioObject(dl, sslHandler) : new PlainNioObject(channel, dl);
    }
    if (currentLength < 0) {
      try {
        if (!currentLengthObject.write()) return false;
      } catch(Exception e) {
        channelCount += currentLengthObject.getChannelCount();
        throw e;
      }
      currentLength = currentDataLocation.getSize();
      count += 4;
      channelCount += currentLengthObject.getChannelCount();
    }
    if (currentLength > 0) {
      if (currentObject == null) {
        DataLocation loc = currentDataLocation.copy();
        currentObject = ssl ? new SSLNioObject(loc, sslHandler) : new PlainNioObject(channel, loc);
      }
      try {
        if (!currentObject.write()) return false;
      } catch(Exception e) {
        channelCount += currentObject.getChannelCount();
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) channelCount += currentObject.getChannelCount();
    if (debug) log.debug("channel id={} wrote object at position {}", channel.getId(), position);
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
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("nb locations=").append(locations == null ? -1 : locations.size());
    sb.append(", position=").append(position);
    sb.append(", nbObjects=").append(nbObjects);
    sb.append(", length=").append(length);
    sb.append(", count=").append(count);
    sb.append(", currentObject=").append(currentObject);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public boolean isSSL() {
    return ssl;
  }

  @Override
  public long getChannelCount() {
    return channelCount;
  }
}
