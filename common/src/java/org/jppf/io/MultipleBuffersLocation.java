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

package org.jppf.io;

import java.io.*;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.streams.*;
import org.slf4j.*;

/**
 * Data location backed by a list of {@link JPPFBuffer}.
 * @author Laurent Cohen
 */
public class MultipleBuffersLocation extends AbstractDataLocation {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MultipleBuffersLocation.class);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The list of buffers that contain the data.
   */
  private final List<JPPFBuffer> list;
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
  public MultipleBuffersLocation(final int size) {
    list = new ArrayList<>();
    list.add(new JPPFBuffer(new byte[size], size));
    this.size = size;
  }

  /**
   * Initialize this location with the specified list of buffers and size.
   * @param list the list of buffers that contain the data.
   * @param size the total size of the data.
   */
  public MultipleBuffersLocation(final List<JPPFBuffer> list, final int size) {
    this.list = list;
    this.size = size;
  }

  /**
   * Initialize this location with the specified list of buffers.
   * The size is computed as the sum of used sizes of all buffers.
   * @param buffers the list of buffers that contain the data.
   */
  public MultipleBuffersLocation(final List<JPPFBuffer> buffers) {
    this.list = buffers;
    this.size = 0;
    for (JPPFBuffer buf : buffers) this.size += buf.length;
  }

  /**
   * Initialize this location from an array of buffers.
   * @param buffers the buffers that contain the data.
   */
  public MultipleBuffersLocation(final JPPFBuffer... buffers) {
    this.list = new ArrayList<>(buffers.length);
    this.size = 0;
    for (JPPFBuffer buf : buffers) {
      this.list.add(buf);
      this.size += buf.length;
    }
  }

  /**
   * Initialize this location from an array of buffers.
   * @param buffers the buffers that contain the data.
   */
  public MultipleBuffersLocation(final byte[]... buffers) {
    this.list = new ArrayList<>(buffers.length);
    this.size = 0;
    for (byte[] buf : buffers) {
      this.list.add(new JPPFBuffer(buf));
      this.size += buf.length;
    }
  }

  @Override
  public int transferFrom(final InputSource source, final boolean blocking) throws Exception {
    if (!transferring) {
      transferring = true;
      currentBuffer = list.get(0);
      currentBufferIndex = 0;
      currentBuffer.pos = 0;
      if (!blocking) {
        list.clear();
        list.add(currentBuffer);
      }
      count = 0;
    }
    try {
      int n = blocking ? blockingTransferFrom(source) : nonBlockingTransferFrom(source);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    } catch (Exception e) {
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
  private int blockingTransferFrom(final InputSource source) throws Exception {
    //if (traceEnabled) log.trace("blocking transfer: size=" + size);
    while (count < size) {
      int remaining = size - count;
      int n = source.read(currentBuffer.buffer, currentBuffer.pos, remaining);
      //if (traceEnabled) log.trace("blocking transfer: remaining=" + remaining + ", read " + n +" bytes from source=" + source +
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
  private int nonBlockingTransferFrom(final InputSource source) throws Exception {
    int remaining = size - count;
    //if (traceEnabled) log.trace("blocking transfer: size="+size+", remaining="+remaining);
    int n = source.read(currentBuffer.buffer, currentBuffer.pos, remaining);
    if (n > 0) {
      count += n;
      currentBuffer.pos += n;
    }
    if ((n < 0) || (count >= size)) transferring = false;
    return n;
  }

  @Override
  public int transferTo(final OutputDestination dest, final boolean blocking) throws Exception {
    if (!transferring) {
      transferring = true;
      if (!blocking) {
        currentBuffer = list.get(0);
        currentBuffer.pos = 0;
        currentBufferIndex = 0;
      }
      count = 0;
    }
    try {
      int n = blocking ? blockingTransferTo(dest) : nonBlockingTransferTo(dest);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    } catch (Exception e) {
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
  private int blockingTransferTo(final OutputDestination dest) throws Exception {
    count = 0;
    for (JPPFBuffer buf : list) {
      int nbRead = 0;
      buf.pos = 0;
      while (nbRead < buf.length) {
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
  private int nonBlockingTransferTo(final OutputDestination dest) throws Exception {
    if (currentBuffer == null) return -1;
    int remaining = currentBuffer.remainingFromPos();
    int n = 0;
    if (remaining > 0) {
      try {
        n = dest.write(currentBuffer.buffer, currentBuffer.pos, remaining);
      } catch (Error e) {
        log.error(e.getMessage(), e);
      }
    }
    if (traceEnabled) log.trace(String.format("count/size=%d/%d, n/remaining=%d/%d, currentBufferIndex/listSize=%d/%d, pos=%d (%s)",
      count, size, n, remaining, currentBufferIndex, list.size(), currentBuffer.pos, this));
    if (n > 0) {
      count += n;
      if (n < remaining) currentBuffer.pos += n;
      else {
        if (currentBufferIndex < list.size() - 1) {
          currentBufferIndex++;
          currentBuffer = list.get(currentBufferIndex);
          currentBuffer.pos = 0;
        } else {
          currentBuffer = null;
        }
      }
    }
    return n;
  }

  @Override
  public InputStream getInputStream() throws Exception {
    return new MultipleBuffersInputStream(list);
  }

  @Override
  public OutputStream getOutputStream() throws Exception {
    return new MultipleBuffersOutputStream(list);
  }

  @Override
  public DataLocation copy() {
    return new MultipleBuffersLocation(copyList(), size);
  }

  /**
   * Make a shallow copy of the list of buffers. The internal byte[] are not copied, they are merely referenced.
   * @return a list of {@link JPPFBuffer} instances.
   */
  private List<JPPFBuffer> copyList() {
    List<JPPFBuffer> copy = new ArrayList<>();
    for (JPPFBuffer buf : list) copy.add(new JPPFBuffer(buf.buffer, buf.length));
    return copy;
  }

  /**
   * Get the list of buffers that contain the data.
   * @return a list of {@link JPPFBuffer} instances.
   */
  public List<JPPFBuffer> getBufferList() {
    return list;
  }

  /**
   * Get the buffer at the specified index..
   * @param n the index of the buffer to get.
   * @return a list of {@link JPPFBuffer} instances.
   */
  public JPPFBuffer getBuffer(final int n) {
    return list.get(n);
  }

  /**
   * Reset the state of this location.
   * @return this location, for method call chaining.
   */
  public MultipleBuffersLocation reset() {
    count = 0;
    currentBuffer = null;
    currentBufferIndex = 0;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("size=").append(size);
    sb.append(", count=").append(count);
    sb.append(", currentBuffer=").append(currentBuffer);
    sb.append(", currentBufferIndex=").append(currentBufferIndex);
    sb.append(", transferring=").append(transferring);
    sb.append(", list=").append(list);
    sb.append(']');
    return sb.toString();
  }
}
