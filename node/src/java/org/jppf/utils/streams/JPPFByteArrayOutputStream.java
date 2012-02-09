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
package org.jppf.utils.streams;

import java.io.ByteArrayOutputStream;

/**
 * Extension of {@link java.io.ByteArrayOutputStream ByteArrayOutputStream}, providing
 * a faster toByteArray() method that does not involve copying its internal buffer.
 * @author Laurent Cohen
 */
public class JPPFByteArrayOutputStream extends ByteArrayOutputStream
{
  /**
   * Instantiate this stream with a default size.
   */
  public JPPFByteArrayOutputStream()
  {
    super();
  }

  /**
   * Instantiate this stream with the specified size.
   * @param size the initial size of the underlying buffer.
   */
  public JPPFByteArrayOutputStream(final int size)
  {
    super(size);
  }

  /**
   * Override of <code>toByteArray()</code> that returns a reference to the internal buffer
   * instead of copy of it, significantly increasing the performance of this operation.
   * @return the content of the stream as an array of bytes.
   * @see java.io.ByteArrayOutputStream#toByteArray()
   */
  @Override
  public synchronized byte[] toByteArray()
  {
    return buf.length == count ? buf : super.toByteArray();
  }

  /**
   * Provide access to the internal buffer.
   * @return an array of bytes.
   */
  public byte[] getBuf()
  {
    return buf;
  }
}
