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

import java.nio.channels.SocketChannel;

import org.jppf.io.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Instances of this class represent a data frame read asynchronously from an input source.
 * @author Laurent Cohen
 */
public class PlainNioObject extends AbstractNioObject
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PlainNioObject.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Where to read the data from (a socket channel)
   */
  private InputSource source = null;
  /**
   * Where to write the data to (a socket channel)
   */
  private OutputDestination dest = null;
  /**
   * The channel from which to read or write the data.
   */
  private final ChannelWrapper<?> channel;

  /**
   * Initialize this NioObject with the specified channel and size.
   * @param channel where to read or write the data.
   * @param size the size of the internal buffer.
   */
  public PlainNioObject(final ChannelWrapper<?> channel, final int size)
  {
    this(channel, new MultipleBuffersLocation(size));
  }

  /**
   * Initialize this NioObject with the specified size.
   * @param channel where to read or write the data.
   * @param location the location of the data to read from or write to.
   */
  public PlainNioObject(final ChannelWrapper<?> channel, final DataLocation location)
  {
    super(location, location.getSize());
    this.channel = channel;
  }

  /**
   * Read the current block of data.
   * @return <code>true</code> if the data has been read fully, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  @Override
  public boolean read() throws Exception
  {
    if (count >= size) return true;
    if (source == null)
    {
      SocketChannel socketChannel = (SocketChannel) ((SelectionKeyWrapper) channel).getChannel().channel();
      source = new ChannelInputSource(socketChannel);
    }
    int n;
    do
    {
      n = location.transferFrom(source, false);
      if (n > 0)
      {
        count += n;
        channelCount = count;
      }
      if (debugEnabled) log.debug("read {} bytes for {}", n, this);
    }
    while ((n > 0) && (count < size));
    return count >= size;
  }

  /**
   * Write the current data object.
   * @return <code>true</code> if the data has been written fully, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  @Override
  public boolean write() throws Exception
  {
    if (count >= size) return true;
    if (dest == null)
    {
      SocketChannel socketChannel = (SocketChannel) ((SelectionKeyWrapper) channel).getChannel().channel();
      dest = new ChannelOutputDestination(socketChannel);
    }
    int n;
    do
    {
      n = location.transferTo(dest, false);
      if (n > 0)
      {
        count += n;
        channelCount = count;
      }
      if (debugEnabled) log.debug("read {} bytes for {}", n, this);
    }
    while ((n > 0) && (count < size));
    return count >= size;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("channel id=").append(channel.getId());
    sb.append(", size=").append(size);
    sb.append(", count=").append(count);
    sb.append(", source=").append(source);
    sb.append(", dest=").append(dest);
    sb.append(", location=").append(location);
    sb.append("]");
    return sb.toString();
  }
}
