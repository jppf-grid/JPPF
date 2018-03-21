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

import org.jppf.io.*;

/**
 * Common abstract superclass representing a message sent or received by a channel.
 * A message is the transformation of a sequence of objects into a more easily transportable format.
 * @author Laurent Cohen
 */
public abstract class AbstractNioMessageBase implements NioMessage {
  /**
   * The current count of bytes sent or received.
   */
  protected int count;
  /**
   * Temporary holder used as a local performance optimization for write operations.
   */
  protected DataLocation currentDataLocation;
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
   * Actual bytes sent to or received from the underlying channel.
   */
  protected long channelReadCount, channelWriteCount;
  /**
   * The channel to read from or write to.
   */
  protected final NioContext<?> channel;
  /**
   * Reusable buffer for reading/writing serialized object lengths.
   */
  protected final MultipleBuffersLocation lengthBuf = new MultipleBuffersLocation(4);
  /**
   * Determines whteher some low-level traces should be logged.
   */
  protected final boolean debug;
  /**
   * 
   */
  protected long sslHandlerReadCount, sslHandlerWriteCount;

  /**
   * Initialize this nio message.
   * @param channel the channel to read from or write to.
   * @param debug to enable debug-level logging.
   */
  protected AbstractNioMessageBase(final NioContext<?> channel, final boolean debug) {
    this.channel = channel;
    this.sslHandler = channel.getSSLHandler();
    this.ssl = sslHandler != null;
    this.debug = debug;
    if (ssl) {
      sslHandlerReadCount = sslHandler.getChannelReadCount();
      sslHandlerWriteCount = sslHandler.getChannelWriteCount();
    }
  }

  @Override
  public boolean isSSL() {
    return ssl;
  }

  @Override
  public long getChannelReadCount() {
    return channelReadCount;
  }

  @Override
  public long getChannelWriteCount() {
    return channelWriteCount;
  }

  /**
   * Get the data location objects abstracting the data to send or receive.
   * @return a <code>DataLocation</code> object.
   */
  public DataLocation getCurrentDataLocation() {
    return currentDataLocation;
  }

  /**
   * 
   * @param update the update value.
   * @param op the type of operation, and therefore which counter to upddate.
   */
  protected void updateCounts(final long update, final int op) {
    if (ssl) {
      channelReadCount = sslHandler.getChannelReadCount() - sslHandlerReadCount;
      channelWriteCount = sslHandler.getChannelWriteCount() - sslHandlerWriteCount;
    } else {
      if (op == READ) channelReadCount += update;
      else channelWriteCount += update;
    }
  }
}
