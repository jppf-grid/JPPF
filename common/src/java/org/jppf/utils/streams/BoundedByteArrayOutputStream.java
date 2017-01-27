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

/**
 * A byte ArrayOutputStream with a maximum size of data that can be written into it.
 * @author Laurent Cohen
 */
public class BoundedByteArrayOutputStream extends OutputStream
{
  /**
   * The backing buffer for this stream.
   */
  private final byte[] buf;
  /**
   * The maximum total size of data that can be written into this stream.
   */
  private final int length;
  /**
   * The start offset int he backing buffer.
   */
  private final int offset;
  /**
   * The current position in the backing buffer.
   */
  private int pos = 0;
  /**
   * Indicates whether the end of the backing buffer has been reached.
   */
  private boolean eof = false;

  /**
   * Initialize this stream.
   * @param buf the backing buffer.
   * @param offset start position in the backing buffer.
   * @param length maximum length of data that can be written.
   */
  public BoundedByteArrayOutputStream(final byte[] buf, final int offset, final int length)
  {
    this.buf = buf;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public void write(final int b) throws IOException
  {
    if (eof) throw new EOFException("buffer overflow");
    buf[offset + pos++] = (byte) b;
    eof = pos >= length - 1;
  }

  @Override
  public void write(final byte[] b) throws IOException
  {
    write(b, 0, b.length);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException
  {
    if (eof) throw new EOFException("buffer overflow");
    int min = Math.min(len, length - pos);
    System.arraycopy(b, off, buf, offset + pos, min);
    pos += min;
    eof = pos >= length - 1;
    if (eof && (len > min)) throw new EOFException("buffer overflow, could only write " + min + " bytes out of " + len);
  }
}
