/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import org.jppf.utils.JPPFBuffer;
import org.slf4j.*;

/**
 * An output stream implementation that minimizes memory usage.
 * @author Laurent Cohen
 * @exclude
 */
public class MultipleBuffersInputStream extends InputStream {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(MultipleBuffersInputStream.class);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Contains the data written to this output stream, as a sequence of {@link JPPFBuffer} instances.
   */
  private final JPPFBuffer[] list;
  /**
   * The JPPFBuffer currently being read from.
   */
  private JPPFBuffer currentBuffer;
  /**
   * Current index in the list of buffers.
   */
  private int bufferIndex = -1;
  /**
   * Determines whether end of file was reached.
   */
  private boolean eofReached;

  /**
   * Initialize this input stream with the specified buffers.
   * @param buffers an array of {@link JPPFBuffer} instances.
   */
  public MultipleBuffersInputStream(final JPPFBuffer[] buffers) {
    list = buffers;
  }

  /**
   * Read a single byte from this input stream.
   * @return the data read.
   * @throws IOException if any error occurs.
   */
  @Override
  public int read() throws IOException {
    if (eofReached) return -1;
    if ((currentBuffer == null) || (currentBuffer.length - currentBuffer.pos < 1)) nextBuffer();
    final byte b = currentBuffer.buffer[currentBuffer.pos];
    currentBuffer.pos++;
    if (traceEnabled) log.trace("read one byte '" + b + "' from " + this);
    return b;
  }

  /**
   * Read from this input stream into the specified byte array.
   * @param b buffer that receives the data read form this stream.
   * @param off the start offset in the buffer.
   * @param len the number of bytes to read.
   * @return the number of bytes read from the stream, or -1 if end of file was reached.
   * @throws IOException if any error occurs.
   */
  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (eofReached) return -1;
    int count = 0;
    while (count < len) {
      if ((currentBuffer == null) || (currentBuffer.length <= currentBuffer.pos)) nextBuffer();
      if (eofReached) break;
      final int n = Math.min(currentBuffer.length - currentBuffer.pos, len - count);
      //final int n = Math.min(currentBuffer.remainingFromPos(), len - count);
      if (n == 1) b[off + count++] = currentBuffer.buffer[currentBuffer.pos++];
      else {
        System.arraycopy(currentBuffer.buffer, currentBuffer.pos, b, off + count, n);
        count += n;
        currentBuffer.pos += n;
      }
    }
    return count;
  }

  /**
   * Read from this input stream into the specified byte array.
   * @param b buffer that receives the data read form this stream.
   * @return the number of bytes read from the stream, or -1 if end of file was reached.
   * @throws IOException if any error occurs.
   */
  @Override
  public int read(final byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  /**
   * Get to the next buffer in the list and set it as the current buffer.
   */
  private void nextBuffer() {
    bufferIndex++;
    if (bufferIndex >= list.length) {
      eofReached = true;
      currentBuffer = null;
    } else {
      currentBuffer = list[bufferIndex];
      currentBuffer.pos = 0;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append(", nbBuffers=").append(list.length);
    sb.append(", bufferIndex=").append(bufferIndex);
    if (currentBuffer == null) sb.append(", currentBuffer=null");
    else {
      sb.append(", currentBuffer.pos=").append(currentBuffer.pos);
      sb.append(", currentBuffer.length=").append(currentBuffer.length);
    }
    sb.append(']');
    return sb.toString();
  }
}
