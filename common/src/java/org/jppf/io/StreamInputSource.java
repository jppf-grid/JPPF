/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
 * Input source that takes an input stream as its source.
 * @author Laurent Cohen
 */
public class StreamInputSource implements InputSource {
  /**
   * The input stream to read from.
   */
  private InputStream is = null;

  /**
   * Initialize this stream input source with the specified input stream.
   * @param is the input stream to read from.
   */
  public StreamInputSource(final InputStream is) {
    this.is = is;
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
    int n = is.read(data, offset, len);
    if (n < 0) throw new EOFException();
    return n;
  }

  /**
   * Read data from this input source into a byte buffer.
   * @param buffer the buffer into which to write.
   * @return the number of bytes actually read, or -1 if end of stream was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int read(final ByteBuffer buffer) throws Exception {
    int pos = buffer.position();
    byte[] bytes =  IO.TEMP_BUFFER_POOL.get();
    try {
      while (buffer.remaining() > 0) {
        int n = read(bytes, 0, Math.min(buffer.remaining(), bytes.length));
        if (n <= 0) break;
        buffer.put(bytes, 0, n);
      }
      return buffer.position() - pos;
    } finally {
      IO.TEMP_BUFFER_POOL.put(bytes);
    }
  }

  /**
   * Read an int value from this input source.
   * @return the value read, or -1 if an end of file condition was reached.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int readInt() throws Exception {
    byte[] value = IO.LENGTH_BUFFER_POOL.get();
    try {
      read(value, 0, 4);
      return SerializationUtils.readInt(value, 0);
    } finally {
      IO.LENGTH_BUFFER_POOL.put(value);
    }
  }

  /**
   * Skip <code>n</code> bytes of data form this input source.
   * @param n the number of bytes to skip.
   * @return the number of bytes actually skipped.
   * @throws Exception if an IO error occurs.
   */
  @Override
  public int skip(final int n) throws Exception {
    return (int) is.skip(n);
  }

  /**
   * Close this input source and release any system resources associated with it.
   * @throws IOException if an IO error occurs.
   */
  @Override
  public void close() throws IOException {
    is.close();
  }
}
