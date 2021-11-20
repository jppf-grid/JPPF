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

package org.jppf.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.jppf.serialization.SerializationUtils;
import org.jppf.utils.pooling.DirectBufferPool;

/**
 * Input source backed by a {@link ReadableByteChannel}.
 * @author Laurent Cohen
 */
public class ChannelInputSource implements InputSource {
  /**
   * The backing {@link ReadableByteChannel}.
   */
  protected ReadableByteChannel channel = null;

  /**
   * Initialize this input source with the specified <code>SocketWrapper</code>.
   * @param channel the backing <code>SocketWrapper</code>.
   */
  public ChannelInputSource(final ReadableByteChannel channel) {
    this.channel = channel;
  }

  /**
   * Read data from this input source into an array of bytes.
   * @param data the buffer into which to write.
   * @param offset the position in the buffer where to start storing the data.
   * @param len the size in bytes of the data to read.
   * @return the number of bytes actually read, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int read(final byte[] data, final int offset, final int len) throws Exception {
    return read(ByteBuffer.wrap(data, offset, len));
  }

  /**
   * Read data from this input source into a byte buffer.
   * <p><b>Implementation details</b>:<br/>
   * We read the data by small chunks of max {@link IO#TEMP_BUFFER_SIZE} bytes wrapped in a direct ByteBuffer, to work around the fact that Sun NIO implementation of SocketChannelImpl.read() attempts
   * to allocate a direct buffer of the requested data size (i.e. <code>data</code>.remaining() in our case), <i>if the destination ByteBuffer is not direct</i>.<br/>
   * This implementation can result in a &quot;OutOfMemoryError: Direct buffer space&quot; when the size of the data to read is too large.<br/>
   * See <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4879883">Sun Bug ID: 4879883</a> for details.
   * @param data the buffer into which to write.
   * @return the number of bytes actually read, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int read(final ByteBuffer data) throws Exception {
    final ByteBuffer tmpBuffer = DirectBufferPool.provideBuffer();
    try {
      final int remaining = data.remaining();
      int count = 0;
      while (count < remaining) {
        final int dataRemaining = (count == 0) ? remaining: data.remaining();
        if (dataRemaining < tmpBuffer.remaining()) tmpBuffer.limit(dataRemaining);
        final int n = channel.read(tmpBuffer);
        if (n < 0) throw new EOFException();
        else if (n == 0) break;
        count += n;
        tmpBuffer.flip();
        data.put(tmpBuffer);
        tmpBuffer.clear();
      }
      return count;
    } finally {
      if (tmpBuffer != null) DirectBufferPool.releaseBuffer(tmpBuffer);
    }
  }

  /**
   * Read an int value from this input source.
   * @return the value read, or -1 if an end of file condition was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int readInt() throws Exception {
    return SerializationUtils.readInt(channel);
  }

  /**
   * Skip <code>n</code> bytes of data form this input source.
   * @param n the number of bytes to skip.
   * @return the number of bytes actually skipped.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int skip(final int n) throws Exception {
    final ByteBuffer buf = ByteBuffer.allocate(n);
    read(buf);
    return buf.position();
  }

  /**
   * This method does nothing.
   * @throws IOException if an IO error occurs.
   */
  @Override
  public void close() throws IOException {
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ChannelInputSource[channel=").append(channel).append("]");
    return sb.toString();
  }
}
