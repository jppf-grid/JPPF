/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.utils;

import org.jppf.io.IO;

/**
 * buffer for the data read from or written to a socket connection.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @exclude
 */
public class JPPFBuffer {
  /**
   * The actual buffer, intended to contain a serialized object graph.
   */
  public byte[] buffer = IO.EMPTY_BYTES;
  /**
   * The length of the buffer.
   */
  public int length = 0;
  /**
   * Current position in this buffer.
   */
  public int pos = 0;

  /**
   * Initialize this buffer.
   */
  public JPPFBuffer() {
  }

  /**
   * Initialize this buffer with the specified String, using UTF-8 encoding.
   * @param str the string whose contents will be put into this buffer.
   */
  public JPPFBuffer(final String str) {
    this.buffer = str.getBytes(StringUtils.UTF_8);
    this.length = buffer.length;
  }

  /**
   * Initialize this buffer with a specified buffer.
   * @param buffer the buffer to use.
   */
  public JPPFBuffer(final byte[] buffer) {
    this.buffer = buffer;
    this.length = buffer.length;
  }

  /**
   * Initialize this buffer with a specified buffer and buffer length.
   * @param buffer the buffer to use.
   * @param length the number of bytes to use in the buffer.
   */
  public JPPFBuffer(final byte[] buffer, final int length) {
    this.buffer = buffer;
    this.length = length;
  }

  /**
   * Set the buffered data.
   * @param buffer an array of bytes containing the data.
   */
  public void setBuffer(final byte[] buffer) {
    this.buffer = buffer;
  }

  /**
   * Get the buffered data.
   * @return an array of bytes containing the data.
   */
  public byte[] getBuffer() {
    return buffer;
  }

  /**
   * Set the length of the buffered data.
   * @param length the length as an int.
   */
  public void setLength(final int length) {
    this.length = length;
  }

  /**
   * Get the length of the buffered data.
   * @return the length as an int.
   */
  public int getLength() {
    return length;
  }

  /**
   * Return the number of bytes available for writing in this buffer.
   * @return the available bytes as an int value.
   */
  public int remaining() {
    return buffer.length - length;
  }

  /**
   * Return the number of bytes available for reading in this buffer.
   * @return the available bytes as an int value.
   */
  public int remainingFromPos() {
    return length - pos;
  }

  /**
   * Transform this buffer into a string using UTF-8 encoding.
   * @return the content of this buffer as a string.
   */
  public String asString() {
    return new String(buffer, pos, length, StringUtils.UTF_8);
  }
}
