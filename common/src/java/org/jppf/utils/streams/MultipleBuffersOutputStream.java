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

package org.jppf.utils.streams;

import java.io.*;
import java.util.*;

import org.jppf.io.IO;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * An output stream implementation that minimizes memory usage.
 * @author Laurent Cohen
 * @exclude
 */
public class MultipleBuffersOutputStream extends OutputStream {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MultipleBuffersOutputStream.class);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Contains the data written to this output stream, as a sequence of {@link JPPFBuffer} instances.
   */
  private final List<JPPFBuffer> list;
  /**
   * The JPPFBuffer currently being written to.
   */
  private JPPFBuffer currentBuffer;
  /**
   * The total number of bytes written into this output stream.
   */
  private int totalSize;
  /**
   * Determines whether this output stream was created with an initial list of buffers.
   */
  private boolean hasInitialBuffers;
  /**
   * Current position in the list of buffers.
   */
  private int bufferIndex;

  /**
   * Initialize this output stream with a default buffer length of 32768.
   */
  public MultipleBuffersOutputStream() {
    list = new ArrayList<>();
    hasInitialBuffers = false;
  }

  /**
   * Initialize this output stream with a default buffer length of 32768.
   * @param initialList contains the data that is written to this output stream.
   */
  public MultipleBuffersOutputStream(final List<JPPFBuffer> initialList) {
    list = new ArrayList<>(initialList.size() + 10);
    for (JPPFBuffer buf : initialList) this.list.add(new JPPFBuffer(buf.buffer, 0));
    hasInitialBuffers = true;
  }

  /**
   * Write a single byte into this output stream.
   * @param b the data to write.
   * @throws IOException if any error occurs.
   */
  @Override
  public void write(final int b) throws IOException {
    if ((currentBuffer == null) || (currentBuffer.remaining() < 1)) newCurrentBuffer(IO.TEMP_BUFFER_SIZE);
    currentBuffer.buffer[currentBuffer.length] = (byte) b;
    currentBuffer.length++;
    totalSize++;
    if (traceEnabled) log.trace("wrote one byte '" + b + "' to " + this);
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array starting at offset <code>off</code> to this output stream.
   * @param b the data.
   * @param off the start offset in the data.
   * @param len the number of bytes to write.
   * @throws IOException if any error occurs.
   */
  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    if ((currentBuffer == null) || (currentBuffer.remaining() < len)) newCurrentBuffer(Math.max(IO.TEMP_BUFFER_SIZE, len));
    System.arraycopy(b, off, currentBuffer.buffer, currentBuffer.length, len);
    currentBuffer.length += len;
    totalSize += len;
  }

  /**
   * Writes the specified byte array to this output stream.
   * @param b the data to write.
   * @throws IOException if any error occurs.
   */
  @Override
  public void write(final byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  /**
   * Create a new current buffer with the specified size and a length of 0, and add it to the list of buffers.
   * @param size the size of the new buffer.
   */
  private void newCurrentBuffer(final int size) {
    if (traceEnabled) log.trace("creating new buffer with size=" + size + " for " + this);
    if (hasInitialBuffers) {
      currentBuffer = list.get(bufferIndex++);
      currentBuffer.length = 0;
    } else {
      byte[] bytes = (size == IO.TEMP_BUFFER_SIZE) ? IO.TEMP_BUFFER_POOL.get() : new byte[size];
      currentBuffer = new JPPFBuffer(bytes, 0);
      list.add(currentBuffer);
    }
  }

  /**
   * Get the size of the content of this output stream.
   * @return the size as an int value.
   */
  public int size() {
    return totalSize;
  }

  /**
   * Get the content of the output stream as a list of {@link JPPFBuffer} instances.
   * @return a list of {@link JPPFBuffer} instances.
   */
  public List<JPPFBuffer> toBufferList() {
    return list;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append(", totalSize=").append(totalSize);
    sb.append(", nbBuffers=").append(list.size());
    if (currentBuffer == null) sb.append(", currentBuffer=null");
    else sb.append(", currentBuffer.length=").append(currentBuffer.length);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public void close() throws IOException {
    super.close();
    //if (list != null) list.clear();
  }
}
