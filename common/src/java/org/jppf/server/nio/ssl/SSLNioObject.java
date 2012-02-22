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

package org.jppf.server.nio.ssl;

import java.io.*;
import java.nio.ByteBuffer;

import org.jppf.io.*;
import org.jppf.server.nio.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class SSLNioObject extends AbstractNioObject
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLNioObject.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The source from which the data is read.
   */
  private InputStream is;
  /**
   * The destination to which the data is written.
   */
  private OutputStream os;
  /**
   * 
   */
  private SSLEngineManager engineManager;
  /**
   * 
   */
  private int statefulCount = 0;

  /**
   * Construct this SSLMessage.
   * @param channel the channel wrapper for the network connection.
   * @param size the size of the internal buffer.
   * @param engineManager the SSLEngineManager to use with this nio object.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final ChannelWrapper<?> channel, final int size, final SSLEngineManager engineManager) throws Exception
  {
    this(channel, new MultipleBuffersLocation(size), engineManager);
  }

  /**
   * Construct this SSLMessage.
   * @param channel the channel wrapper for the network connection.
   * @param location the location of the data to read from or write to.
   * @param engineManager the SSLEngineManager to use with this nio object.
   * @throws Exception if any error occurs.
   */
  public SSLNioObject(final ChannelWrapper<?> channel, final DataLocation location, final SSLEngineManager engineManager) throws Exception
  {
    this.location = location;
    this.size = location.getSize();
    this.engineManager = engineManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean read() throws Exception
  {
    ByteBuffer buf = engineManager.getAppRecvBuffer();
    if (os == null)
    {
      os = location.getOutputStream();
      statefulCount = engineManager.getLastReadCount();
    }
    while (count < size)
    {
      if (statefulCount <= 0)
      {
        statefulCount = engineManager.read();
        if (statefulCount > 0) buf.flip();
      }
      if (traceEnabled) log.trace("lastReadCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
      if (statefulCount <= 0) break;
      engineManager.setLastReadCount(statefulCount);
      while (buf.hasRemaining() && (count < size))
      {
        int pos = buf.position();
        int n = Math.min(buf.remaining(), size - count);
        if (n > 0)
        {
          os.write(buf.array(), pos, n);
          count += n;
          statefulCount -= n;
          buf.position(pos + n);
          if (traceEnabled) log.trace("wrote " + n + " bytes to location, lastReadCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
        }
      }
      if (!buf.hasRemaining()) buf.clear();
    }
    engineManager.setLastReadCount(statefulCount);
    boolean b = count >= size;
    if (b)
    {
      StreamUtils.close(os, log);
      os = null;
    }
    return b;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean write() throws Exception
  {
    ByteBuffer buf = engineManager.getAppSendBuffer();
    if (is == null)
    {
      is = location.getInputStream();
      statefulCount = 0;
    }
    if (count < size)
    //while (count < size)
    {
      if (traceEnabled) log.trace("statefulCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
      if (buf.hasRemaining() && (statefulCount < size))
      {
        int min = Math.min(size-statefulCount, buf.remaining());
        if (min > 0)
        {
          int read = is.read(buf.array(), buf.position(), min);
          if (read > 0)
          {
            statefulCount += read;
            buf.position(buf.position() + read);
          }
        }
      }
      if (traceEnabled) log.trace("statefulCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);

      //int pos = buf.position();
      int n = engineManager.write();
      if (n > 0) count += n;
      engineManager.flush();
      if (traceEnabled) log.trace("n=" + n + ", statefulCount=" + statefulCount + ", count=" + count + ", size=" + size + ", buf=" + buf);
      if (!buf.hasRemaining()) buf.clear();
    }
    boolean b = count >= size;
    if (b)
    {
      engineManager.flush();
      buf.clear();
      StreamUtils.close(is, log);
      is = null;
    }
    return b;
  }
}
