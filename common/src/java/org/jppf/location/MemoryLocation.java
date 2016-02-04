/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.location;

import java.io.*;

import org.jppf.utils.streams.BoundedByteArrayOutputStream;

/**
 * Wrapper fro manipulating a block of data in memory.
 * This implementation of the {@link Location} interface allows writing to and reading from a <code>byte</code> array.
 * @author Laurent Cohen
 */
public class MemoryLocation extends AbstractLocation<byte[]> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Start offset in the byte array.
   */
  private int offset = 0;
  /**
   * Length of data to handle.
   */
  private int len = -1;

  /**
   * Initialize this location and create a buffer of the specified size.
   * The size is cast to an {@code int} value before the internal buffer is initialized.
   * @param size the size of the buffer handled by this memory location.
   */
  public MemoryLocation(final long size) {
    this(new byte[(int) size], 0, (int) size);
  }

  /**
   * Initialize this location with the specified buffer.
   * @param buffer an array of bytes.
   */
  public MemoryLocation(final byte[] buffer) {
    this(buffer, 0, buffer.length);
  }

  /**
   * Initialize this location with the specified byte array.
   * @param buffer an array of bytes.
   * @param offset the start position in the array of bytes.
   * @param len the length of the buffer.
   */
  public MemoryLocation(final byte[] buffer, final int offset, final int len) {
    super(buffer);
    this.offset = offset;
    this.len = len;
  }

  @Override
  public InputStream getInputStream() throws Exception {
    return new ByteArrayInputStream(path, offset, len);
  }

  @Override
  public OutputStream getOutputStream() throws Exception {
    return new BoundedByteArrayOutputStream(path, offset, len);
  }

  /**
   * Get the size of the file this location points to.
   * @return the size as a long value, or -1 if the file does not exist.
   */
  @Override
  public long size() {
    return len;
  }

  /**
   * Get the content at this location as an array of bytes. This method is
   * overridden from {@link AbstractLocation#toByteArray() AbstractLocation.toByteArray()} for improved performance.
   * @return a byte array with a length equals to this location's size and starting at the offset specified in the constructor, if any.
   */
  @Override
  public byte[] toByteArray() {
    if ((offset == 0) && (len == path.length)) return path;
    byte[] buf = new byte[len];
    System.arraycopy(path, offset, buf, 0, len);
    return buf;
  }
}
