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

package org.jppf.nio;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;

/**
 * 
 * @author Laurent Cohen
 */
public interface SSLHandler {

  /**
   * Read from the channel via the SSLEngine into the application receive buffer.
   * Called in blocking mode when input is expected, or in non-blocking mode when the channel is readable.
   * @return the number of bytes read from the application receive buffer.
   * @throws Exception if any error occurs.
   */
  int read() throws Exception;

  /**
   * Write from the application send buffer to the channel via the SSLEngine.
   * @return the number of bytes consumed from the application.
   * @throws Exception if any error occurs.
   */
  int write() throws Exception;

  /**
   * Flush the underlying channel.
   * @return the number of bytes flushed.
   * @throws IOException if any error occurs.
   */
  int flush() throws IOException;

  /**
   * Close the underlying channel and SSL engine.
   * @throws Exception if any error occurs.
   */
  void close() throws Exception;

  /**
   * Get the application receive buffer.
   * @return a {@link ByteBuffer} instance.
   */
  ByteBuffer getApplicationReceiveBuffer();

  /**
   * Get the application send buffer.
   * @return a {@link ByteBuffer} instance.
   */
  ByteBuffer getApplicationSendBuffer();

  /**
   * Get the channel receive buffer.
   * @return a {@link ByteBuffer} instance.
   */
  ByteBuffer getChannelReceiveBuffer();

  /**
   * Get the channel send buffer.
   * @return a {@link ByteBuffer} instance.
   */
  ByteBuffer getChannelSendBuffer();

  /**
   * Get the count of bytes read from the channel.
   * @return the byte count as a long value.
   */
  long getChannelReadCount();

  /**
   * Get the count of bytes written to the channel.
   * @return the byte count as a long value.
   */
  long getChannelWriteCount();

  /**
   * @return the SSLEngine.
   */
  SSLEngine getSslEngine();

}