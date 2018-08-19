/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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
  private final JPPFBuffer[] list;
  /**
   * The current count of bytes read from/written to the underlying file.
   */
  private int count;
  /**
   * Current buffer being read.
   */
  private JPPFBuffer currentBuffer;
  /**
   * Index of the current buffer.
   */
  private int currentBufferIndex;
  /**
   * Current buffer being read.
   */
  private final JPPFBuffer firstBuffer;

  /**
   * Initialize this location with the specified size.
   * @param size the total size of the data.
   */
  public MultipleBuffersLocation(final int size) {
    list = new JPPFBuffer[] { firstBuffer = new JPPFBuffer(new byte[size], size) };
    this.size = size;
  }

  /**
   * Initialize this location with the specified list of buffers and size.
   * @param list the list of buffers that contain the data.
   * @param size the total size of the data.
   */
  public MultipleBuffersLocation(final List<JPPFBuffer> list, final int size) {
    this.list = list.toArray(new JPPFBuffer[list.size()]);
    firstBuffer = this.list[0];
    this.size = size;
  }

  /**
   * Initialize this location with the specified list of buffers.
   * The size is computed as the sum of used sizes of all buffers.
   * @param buffers the list of buffers that contain the data.
   */
  public MultipleBuffersLocation(final List<JPPFBuffer> buffers) {
    this.list = buffers.toArray(new JPPFBuffer[buffers.size()]);
    firstBuffer = this.list[0];
    this.size = 0;
    for (JPPFBuffer buf : buffers) this.size += buf.length;
  }

  /**
   * Initialize this location from an array of buffers.
   * @param buffers the buffers that contain the data.
   */
  public MultipleBuffersLocation(final JPPFBuffer... buffers) {
    this.list = buffers;
    this.size = 0;
    firstBuffer = buffers[0];
    for (JPPFBuffer buf : buffers) this.size += buf.length;
  }

  /**
   * Initialize this location from an array of buffers.
   * @param size the data total size.
   * @param buffers the buffers that contain the data.
   */
  public MultipleBuffersLocation(final int size, final JPPFBuffer... buffers) {
    this.list = buffers;
    this.size = size;
    firstBuffer = buffers[0];
  }

  /**
   * Initialize this location from an array of buffers.
   * @param buffers the buffers that contain the data.
   */
  public MultipleBuffersLocation(final byte[]... buffers) {
    this.list = new JPPFBuffer[buffers.length];
    this.list[0] = (firstBuffer =  new JPPFBuffer(buffers[0]));
    this.size = firstBuffer.length;
    for (int i=1; i<buffers.length; i++) {
      final JPPFBuffer jppfBuffer = new JPPFBuffer(buffers[i]);
      this.list[i] = jppfBuffer;
      this.size += jppfBuffer.length;
    }
  }

  @Override
  public int transferFrom(final InputSource source, final boolean blocking) throws Exception {
    if (!transferring) {
      transferring = true;
      currentBuffer = firstBuffer;
      currentBufferIndex = 0;
      currentBuffer.pos = 0;
      count = 0;
    }
    try {
      final int n = blocking ? blockingTransferFrom(source) : nonBlockingTransferFrom(source);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    } catch (final Exception e) {
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
    while (count < size) {
      final int remaining = size - count;
      final int n = source.read(currentBuffer.buffer, currentBuffer.pos, remaining);
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
    final int remaining = size - count;
    final int n = source.read(currentBuffer.buffer, currentBuffer.pos, remaining);
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
        currentBuffer = firstBuffer;
        currentBuffer.pos = 0;
        currentBufferIndex = 0;
      }
      count = 0;
    }
    try {
      final int n = blocking ? blockingTransferTo(dest) : nonBlockingTransferTo(dest);
      if ((n < 0) || (count >= size)) transferring = false;
      return n;
    } catch (final Exception e) {
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
        final int remaining = buf.remainingFromPos();
        final int n = dest.write(buf.buffer, buf.pos, remaining);
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
    final int remaining = currentBuffer.remainingFromPos();
    int n = 0;
    if (remaining > 0) {
      try {
        n = dest.write(currentBuffer.buffer, currentBuffer.pos, remaining);
      } catch (final Error e) {
        log.error(e.getMessage(), e);
      }
    }
    if (traceEnabled) log.trace("count/size={}/{}, n/remaining={}/{}, currentBufferIndex/listSize={}/{}, pos={} ({})",
      count, size, n, remaining, currentBufferIndex, list.length, currentBuffer.pos, this);
    if (n > 0) {
      count += n;
      if (n < remaining) currentBuffer.pos += n;
      else {
        if (currentBufferIndex < list.length - 1) {
          currentBufferIndex++;
          currentBuffer = list[currentBufferIndex];
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
    return new MultipleBuffersOutputStream(Arrays.asList(list));
  }

  @Override
  public DataLocation copy() {
    return new MultipleBuffersLocation(size, copyList());
  }

  /**
   * Make a shallow copy of the list of buffers. The internal byte[] are not copied, they are merely referenced.
   * @return a list of {@link JPPFBuffer} instances.
   */
  private JPPFBuffer[] copyList() {
    final JPPFBuffer[] copy = new JPPFBuffer[list.length];
    //System.arraycopy(list, 0, copy, 0, list.length);
    for (int i=0; i<list.length; i++) {
      final JPPFBuffer buf = list[i];
      copy[i] = new JPPFBuffer(buf.buffer, buf.length);
    }
    return copy;
  }

  /**
   * Get the buffer at the specified index..
   * @param n the index of the buffer to get.
   * @return a list of {@link JPPFBuffer} instances.
   */
  public JPPFBuffer getBuffer(final int n) {
    return (n == 0) ? firstBuffer : list[n];
  }

  /**
   * Reset the state of this location.
   * @return this location, for method call chaining.
   */
  public MultipleBuffersLocation reset() {
    count = 0;
    currentBuffer = null;
    currentBufferIndex = 0;
    transferring = false;
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
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
