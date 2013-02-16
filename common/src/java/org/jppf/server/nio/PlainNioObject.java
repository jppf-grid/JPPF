/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.nio.channels.SocketChannel;

import org.jppf.io.*;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the I/O performed by this object are blocking.
   */
  private boolean blocking = false;
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
  private ChannelWrapper<?> channel = null;

  /**
   * Initialize this NioObject with the specified channel and size.
   * @param channel where to read or write the data.
   * @param size the size of the internal buffer.
   * @param blocking specifies whether the I/O performed by this object are blocking.
   */
  public PlainNioObject(final ChannelWrapper<?> channel, final int size, final boolean blocking)
  {
    this(channel, new MultipleBuffersLocation(size), blocking);
  }

  /**
   * Initialize this NioObject with the specified size.
   * @param channel where to read or write the data.
   * @param location the location of the data to read from or write to.
   * @param blocking specifies whether the I/O performed by this object are blocking.
   */
  public PlainNioObject(final ChannelWrapper<?> channel, final DataLocation location, final boolean blocking)
  {
    this.channel = channel;
    this.size = location.getSize();
    this.location = location;
  }

  /**
   * Read the current frame.
   * @return true if the frame has been read fully, false otherwise.
   * @throws Exception if any error occurs.
   */
  public boolean read() throws Exception
  {
    if (source == null)
    {
      SocketChannel socketChannel = (SocketChannel) ((SelectionKeyWrapper) channel).getChannel().channel();
      source = new ChannelInputSource(socketChannel);
    }
    if (count >= size) return true;
    int n = location.transferFrom(source, blocking);
    if (n > 0) count += n;
    if (debugEnabled) log.debug("read " + n + " bytes from input source, count/size = " + count + '/' + size);
    if (count >= size)
    {
      if (debugEnabled) log.debug("count = " + count + ", size = " + size);
      return true;
    }
    return false;
  }

  /**
   * Write the current data object.
   * @return true if the data has been written fully, false otherwise.
   * @throws Exception if any error occurs.
   */
  public boolean write() throws Exception
  {
    if (dest == null)
    {
      SocketChannel socketChannel = (SocketChannel) ((SelectionKeyWrapper) channel).getChannel().channel();
      dest = new ChannelOutputDestination(socketChannel);
    }
    if (count >= size) return true;
    int n = location.transferTo(dest, blocking);
    if (n > 0) count += n;
    if (debugEnabled) log.debug("wrote " + n + " bytes to output destination, count/size = " + count + '/' + size + " (dl = " + location + ')');
    if (count > size)
    {
      int breakpoint = 0;
    }
    if (count >= size)
    {
      return true;
    }
    return false;
  }

  /**
   * Location of the data to read or write.
   * @return a <code>DataLocation</code> instance.
   */
  public DataLocation getData()
  {
    return location;
  }

  /**
   * Number of bytes read from or written to the message.
   * @return  the number of bytes as an int.
   */
  public int getCount()
  {
    return count;
  }
}
