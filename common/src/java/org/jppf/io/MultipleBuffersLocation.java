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

package org.jppf.io;

import java.io.*;
import java.util.*;

import org.jppf.utils.JPPFBuffer;
import org.jppf.utils.streams.*;
import org.slf4j.*;

/**
 * Data location backed by a list of {@link JPPFBuffer}.
 * @author Laurent Cohen
 */
public class MultipleBuffersLocation extends AbstractDataLocation
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MultipleBuffersLocation.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The list of buffers that contain the data.
   */
  private List<JPPFBuffer> list = null;
  /**
   * The current count of bytes read from/written to the underlying file.
   */
  private int count = 0;
  /**
   * Current buffer being read.
   */
  private JPPFBuffer currentBuffer = null;
  /**
   * Index of the current buffer.
   */
  private int currentBufferIndex = 0;

  /**
   * Initialize this location with the specified size.
   * @param size the total size of the data.
   */
  public MultipleBuffersLocation(final int size)
  {
    list = new ArrayList<JPPFBuffer>();
    list.add(new JPPFBuffer(new byte[size], size));
    this.size = size;
  }

  /**
   * Initialize this location with the specified list of buffers and size.
   * @param list the list of buffers that contain the data.
   * @param size the total size of the data.
   */
  public MultipleBuffersLocation(final List<JPPFBuffer> list, final int size)
  {
    this.list = list;
    this.size = size;
  }

  /**
   * Initialize this location from a single buffer.
   * @param buffer the buffer that contains the data.
   */
  public MultipleBuffersLocation(final JPPFBuffer buffer)
  {
    this.list = new ArrayList<JPPFBuffer>();
    this.list.add(buffer);
    this.size = buffer.length;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int transferFrom(final InputSource source, final boolean blocking) throws Exception
  {
    if (!transferring)
    {
      transferring = true;
      //list = new ArrayList<JPPFBuffer>();
      //currentBuffer = new JPPFBuffer(new byte[size], size);
      //list.add(currentBuffer);
      currentBuffer = list.get(0);
      currentBufferIndex = 0;
      currentBuffer.pos = 0;
      if (!blocking)
      {
        list.clear();
        list.add(currentBuffer);
      }
      count = 0;
    }
    try
    {
      int n = blocking ? blockingTransferFrom(source) : nonBlockingTransferFrom(source);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    }
    catch(Exception e)
    {
      transferring = false;
      throw e;
    }
  }

  /**
   * Perform a blocking transfer to this data location from the specified input source.
   * @param source the input source to transfer from.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  private int blockingTransferFrom(final InputSource source) throws Exception
  {
    //if (debugEnabled) log.debug("blocking transfer: size=" + size);
    while (count < size)
    {
      int remaining = size - count;
      int n = source.read(currentBuffer.buffer, currentBuffer.pos, remaining);
      //if (debugEnabled) log.debug("blocking transfer: remaining=" + remaining + ", read " + n +" bytes from source=" + source +
      //	", bytes=" + StringUtils.dumpBytes(currentBuffer.buffer, currentBuffer.pos, Math.min(100, n)));
      if (n < 0) throw new EOFException();
      if (n < remaining) currentBuffer.pos += n;
      count += n;
    }
    transferring = false;
    return count;
  }

  /**
   * Perform a non-blocking transfer to this data location from the specified input source.
   * @param source the input source to transfer from.
   * @return the number of bytes actually transferred.
   * @throws Exception if an IO error occurs.
   */
  private int nonBlockingTransferFrom(final InputSource source) throws Exception
  {
    int remaining = size - count;
    //if (debugEnabled) log.debug("blocking transfer: size="+size+", remaining="+remaining);
    int n = source.read(currentBuffer.buffer, currentBuffer.pos, remaining);
    if (n > 0)
    {
      count += n;
      currentBuffer.pos += n;
    }
    if ((n < 0) || (count >= size)) transferring = false;
    return n;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int transferTo(final OutputDestination dest, final boolean blocking) throws Exception
  {
    if (!transferring)
    {
      transferring = true;
      if (!blocking)
      {
        currentBuffer = list.get(0);
        currentBuffer.pos = 0;
        currentBufferIndex = 0;
      }
      count = 0;
    }
    try
    {
      int n = blocking ? blockingTransferTo(dest) : nonBlockingTransferTo(dest);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    }
    catch(Exception e)
    {
      transferring = false;
      throw e;
    }
  }

  /**
   * Write the data to the specified destination in a blocking way.
   * @param dest the destination to write to.
   * @return the number of bytes that were written.
   * @throws Exception if any I/O error occurs.
   */
  private int blockingTransferTo(final OutputDestination dest) throws Exception
  {
    count = 0;
    for (JPPFBuffer buf: list)
    {
      int nbRead = 0;
      buf.pos = 0;
      while (nbRead < buf.length)
      {
        int remaining = buf.remainingFromPos();
        int n = dest.write(buf.buffer, buf.pos, remaining);
        if (n <= 0) break;
        if (n < remaining) buf.pos += n;
        count += n;
        nbRead += n;
      }
    }
    return count;
  }

  /**
   * Write the data to the specified destination in a non-blocking way.
   * @param dest the destination to write to.
   * @return the number of bytes that were written.
   * @throws Exception if any I/O error occurs.
   */
  private int nonBlockingTransferTo(final OutputDestination dest) throws Exception
  {
    if (currentBuffer == null) return -1;
    int remaining = currentBuffer.remainingFromPos();
    int n = 0;
    try
    {
      n = dest.write(currentBuffer.buffer, currentBuffer.pos, remaining);
    }
    catch(Error e)
    {
      log.error(e.getMessage(), e);
    }
    if (traceEnabled) {
      log.trace("count/size=" + count + '/' + size + ", n/remaining=" + n + '/' + remaining +
          ", currentBufferIndex/listSize=" + currentBufferIndex + '/' + list.size() + ", pos=" + currentBuffer.pos + " (" + this + ')');
    }
    if (n > 0) count += n;
    if (n < remaining) currentBuffer.pos += n;
    else
    {
      if (currentBufferIndex < list.size() - 1)
      {
        currentBufferIndex++;
        currentBuffer = list.get(currentBufferIndex);
        currentBuffer.pos = 0;
      }
      else
      {
        currentBuffer = null;
        //transferring = false;
      }
    }
    return n;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getInputStream() throws Exception
  {
    return new MultipleBuffersInputStream(list);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OutputStream getOutputStream() throws Exception
  {
    return new MultipleBuffersOutputStream(list);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataLocation copy()
  {
    List<JPPFBuffer> copyList = new ArrayList<JPPFBuffer>();
    for (JPPFBuffer buf: list) copyList.add(new JPPFBuffer(buf.buffer, buf.length));
    return new MultipleBuffersLocation(copyList, size);
  }
}
