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
import java.nio.ByteBuffer;

import org.jppf.serialization.SerializationUtils;

/**
 * Output destination backed by an {@link java.io.OutputStream OutputStream}.
 * @author Laurent Cohen
 */
public class StreamOutputDestination implements OutputDestination {
  /**
   * The output stream to write to.
   */
  private OutputStream os = null;

  /**
   * Initialize this input source with the specified data.
   * @param os the output stream to write to.
   */
  public StreamOutputDestination(final OutputStream os) {
    this.os = os;
  }

  /**
   * Write data to this output destination from an array of bytes.
   * @param buffer the buffer containing the data to write.
   * @param offset the position in the buffer where to start reading the data.
   * @param len the size in bytes of the data to write.
   * @return the number of bytes actually written, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int write(final byte[] buffer, final int offset, final int len) throws Exception {
    os.write(buffer, offset, len);
    return len;
  }

  /**
   * Write data to this output destination from a byte buffer.
   * @param buffer the buffer containing the data to write.
   * @return the number of bytes actually written, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int write(final ByteBuffer buffer) throws Exception {
    final int pos = buffer.position();
    final byte[] bytes =  IO.TEMP_BUFFER_POOL.get();
    try {
      while (buffer.remaining() > 0) {
        int n = buffer.position();
        buffer.get(bytes, 0, Math.min(buffer.remaining(), bytes.length));
        n = buffer.position() - n;
        if (n <= 0) break;
        os.write(bytes, 0, n);
      }
      return buffer.position() - pos;
    } finally {
      IO.TEMP_BUFFER_POOL.put(bytes);
    }
  }

  /**
   * Write an int value to this output destination.
   * @param value the value to write.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public void writeInt(final int value) throws Exception {
    final byte[] bytes = SerializationUtils.writeInt(value);
    os.write(bytes);
  }

  /**
   * Close this output destination and release any system resources associated with it.
   * @throws IOException if an IO error occurs.
   */
  @Override
  public void close() throws IOException {
    os.close();
  }
}
