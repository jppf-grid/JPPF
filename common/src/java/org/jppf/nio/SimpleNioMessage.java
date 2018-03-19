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
public class SimpleNioMessage implements NioMessage {
  /**
   * The current count of bytes sent or received.
   */
  protected int count;
  /**
   * The data location objects abstracting the data to send or receive.
   */
  protected DataLocation location;
  /**
   * The length of the location at the current position.
   */
  protected int currentLength = -1;
  /**
   * Object storing the length of the object currently being read or written.
   */
  protected NioObject currentLengthObject;
  /**
   * Object storing the object currently being read or written.
   */
  protected NioObject currentObject;
  /**
   * <code>true</code> is data is read from or written an SSL connection, <code>false</code> otherwise.
   */
  protected final boolean ssl;
  /**
   * Wraps a channel associated with an <code>SSLEngine</code>.
   */
  protected final SSLHandler sslHandler;
  /**
   * The channel to read from or write to.
   */
  protected final NioContext<?> channel;
  /**
   * Actual bytes sent to or received from the underlying channel.
   */
  public long channelCount;
  /**
   * Reusable buffer for reading/writing serialized object lengths.
   */
  protected final MultipleBuffersLocation lengthBuf = new MultipleBuffersLocation(4);

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   */
  public SimpleNioMessage(final NioContext<?> channel) {
    this.channel = channel;
    this.sslHandler = channel.getSSLHandler();
    this.ssl = sslHandler != null;
  }

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   */
  public SimpleNioMessage(final ChannelWrapper<?> channel) {
    this.channel = channel.getContext();
    this.sslHandler = channel.getContext().getSSLHandler();
    this.ssl = sslHandler != null;
  }

  /**
   * Add a location to the data locations of this message.
   * @param location the location to add.
   */
  public void setLocation(final DataLocation location) {
    this.location = location;
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
        channelCount += currentLengthObject.getChannelCount();
        throw e;
      }
      channelCount += currentLengthObject.getChannelCount();
      try (InputStream is = currentLengthObject.getData().getInputStream()) {
        currentLength = SerializationUtils.readInt(is);
      }
      count += 4;
    }
    if (currentLength > 0) {
      if (currentObject == null) {
        location = IOHelper.createDataLocationMemorySensitive(currentLength);
        currentObject = ssl ? new SSLNioObject(location, sslHandler) : new PlainNioObject(channel.getSocketChannel(), location);
      }
      try {
        if (!currentObject.read()) return false;
      } catch(final Exception e) {
        channelCount += currentObject.getChannelCount();
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) channelCount += currentObject.getChannelCount();
    return true;
  }

  @Override
  public boolean write() throws Exception {
    if (currentLengthObject == null) {
      lengthBuf.reset();
      SerializationUtils.writeInt(location.getSize(), lengthBuf.getBuffer(0).buffer, 0);
      currentLengthObject = ssl ? new SSLNioObject(lengthBuf, sslHandler) : new PlainNioObject(channel.getSocketChannel(), lengthBuf);
    }
    if (currentLength < 0) {
      try {
        if (!currentLengthObject.write()) return false;
      } catch(final Exception e) {
        channelCount += currentLengthObject.getChannelCount();
        throw e;
      }
      currentLength = location.getSize();
      count += 4;
      channelCount += currentLengthObject.getChannelCount();
    }
    if (currentLength > 0) {
      if (currentObject == null) currentObject = ssl ? new SSLNioObject(location, sslHandler) : new PlainNioObject(channel.getSocketChannel(), location);
      try {
        if (!currentObject.write()) return false;
      } catch(final Exception e) {
        channelCount += currentObject.getChannelCount();
        throw e;
      }
    }
    count += currentLength;
    if (currentObject != null) channelCount += currentObject.getChannelCount();
    return true;
  }

  /**
   * Get the data location objects abstracting the data to send or receive.
   * @return a <code>DataLocation</code> object.
   */
  public DataLocation getLocation() {
    return location;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
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
