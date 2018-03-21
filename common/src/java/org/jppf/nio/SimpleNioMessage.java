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

import org.jppf.io.*;
import org.jppf.serialization.SerializationUtils;

/**
 * An {@link NioMessage} that reads or writes a single object.
 * @author Laurent Cohen
 */
public class SimpleNioMessage extends AbstractNioMessageBase {
  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   */
  public SimpleNioMessage(final NioContext<?> channel) {
    super(channel, false);
  }

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   */
  public SimpleNioMessage(final ChannelWrapper<?> channel) {
    this(channel.getContext());
  }

  /**
   * Add a location to the data locations of this message.
   * @param location the location to add.
   */
  public void setCurrentDataLocation(final DataLocation location) {
    this.currentDataLocation = location;
  }

  @Override
  public boolean read() throws Exception {
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
      if (!ssl) channelReadCount += currentLengthObject.getChannelCount();
      try (InputStream is = currentLengthObject.getData().getInputStream()) {
        currentLength = SerializationUtils.readInt(is);
      }
      count += 4;
    }
    if (currentLength > 0) {
      if (currentObject == null) {
        currentDataLocation = IOHelper.createDataLocationMemorySensitive(currentLength);
        currentObject = ssl ? new SSLNioObject(currentDataLocation, sslHandler) : new PlainNioObject(channel.getSocketChannel(), currentDataLocation);
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
    return true;
  }

  @Override
  public boolean write() throws Exception {
    if (currentLengthObject == null) {
      lengthBuf.reset();
      SerializationUtils.writeInt(currentDataLocation.getSize(), lengthBuf.getBuffer(0).buffer, 0);
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
      if (currentObject == null) currentObject = ssl ? new SSLNioObject(currentDataLocation, sslHandler) : new PlainNioObject(channel.getSocketChannel(), currentDataLocation);
      try {
        if (!currentObject.write()) return false;
      } catch(final Exception e) {
        updateCounts(currentObject.getChannelCount(), WRITE);
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) updateCounts(currentObject.getChannelCount(), WRITE);
    return true;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append(", count=").append(count);
    sb.append(", currentObject=").append(currentObject);
    sb.append(']');
    return sb.toString();
  }
}
