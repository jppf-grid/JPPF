/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.classloader.resource;

import java.io.*;

/**
 * Abstraction for a resource stored in memory.
 * This implementation of the {@link Resource} interface allows writing to and reading from a <code>byte</code> array.
 * @author Laurent Cohen
 */
public class MemoryResource extends AbstractResource<byte[]>
{
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
   * @param size the size of the buffer handled by this memory location.
   */
  public MemoryResource(final int size)
  {
    this(new byte[size], 0, size);
  }

  /**
   * Initialize this location with the specified buffer.
   * @param buffer an array of bytes.
   */
  public MemoryResource(final byte[] buffer)
  {
    this(buffer, 0, buffer.length);
  }

  /**
   * Initialize this location with the specified byte array.
   * @param buffer an array of bytes.
   * @param offset the start position in the array of bytes.
   * @param len the length of the buffer.
   */
  public MemoryResource(final byte[] buffer, final int offset, final int len)
  {
    super(buffer);
    this.offset = offset;
    this.len = len;
  }

  @Override
  public InputStream getInputStream() throws Exception
  {
    return new ByteArrayInputStream(path, offset, len);
  }

  @Override
  public OutputStream getOutputStream() throws Exception
  {
    return new BoundedByteArrayOutputStream(path, offset, len);
  }

  @Override
  public long size()
  {
    return len;
  }

  @Override
  public byte[] toByteArray() throws Exception
  {
    return path;
  }
}
