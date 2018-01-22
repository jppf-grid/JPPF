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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.jppf.serialization.SerializationUtils;
import org.jppf.utils.pooling.DirectBufferPool;

/**
 * Output destination backed by a {@link java.nio.channels.WritableByteChannel WritableByteChannel}.
 * @author Laurent Cohen
 */
public class ChannelOutputDestination implements OutputDestination {
  /**
   * The backing <code>WritableByteChannel</code>.
   */
  protected WritableByteChannel channel = null;

  /**
   * Initialize this output destination with the specified <code>SocketWrapper</code>.
   * @param channel the backing <code>SocketWrapper</code>.
   */
  public ChannelOutputDestination(final WritableByteChannel channel) {
    this.channel = channel;
  }

  /**
   * Write data to this output destination from an array of bytes.
   * @param data the buffer containing the data to write.
   * @param offset the position in the buffer where to start reading the data.
   * @param len the size in bytes of the data to write.
   * @return the number of bytes actually written, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int write(final byte[] data, final int offset, final int len) throws Exception {
    final ByteBuffer tmpBuffer = DirectBufferPool.provideBuffer();
    try {
      final int cap = tmpBuffer.capacity();
      int count = 0;
      while (count < len) {
        tmpBuffer.clear();
        final int size = Math.min(cap, len - count);
        tmpBuffer.put(data, offset + count, size);
        tmpBuffer.flip();
        final int n = channel.write(tmpBuffer);
        if (n <= 0) break;
        count += n;
        if (n < size) break;
      }
      return count;
    } finally {
      if (tmpBuffer != null) DirectBufferPool.releaseBuffer(tmpBuffer);
    }
  }

  /**
   * Write data to this output destination from a byte buffer.
   * @param data the buffer containing the data to write.
   * @return the number of bytes actually written, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int write(final ByteBuffer data) throws Exception {
    return channel.write(data);
  }

  /**
   * Write an int value to this output destination.
   * @param value the value to write.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public void writeInt(final int value) throws Exception {
    SerializationUtils.writeInt(channel, value);
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
    final StringBuilder builder = new StringBuilder();
    builder.append("ChannelOutputDestination[channel=").append(channel).append("]");
    return builder.toString();
  }
}
