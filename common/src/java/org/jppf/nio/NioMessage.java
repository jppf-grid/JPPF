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

/**
 * Common interface for sending data over a communication channel.
 * A channel is a wrapper over a socket connection or in-memory pipe.
 * @author Laurent Cohen
 */
public interface NioMessage {
  /**
   * Read operation.
   */
  int READ = 0;
  /**
   * Write operation.
   */
  int WRITE = 1;

  /**
   * Read data from a channel.
   * @return true if the data has been completely read from the channel, false otherwise.
   * @throws Exception if an IO error occurs.
   */
  boolean read() throws Exception;

  /**
   * Read data from a channel.
   * @return true if the data has been completely written the channel, false otherwise.
   * @throws Exception if an IO error occurs.
   */
  boolean write() throws Exception;

  /**
   * Determines whether this message read from / writes to an SSL connection.
   * @return <code>true</code> is data is read from or written an SSL connection, <code>false</code> otherwise.
   */
  boolean isSSL();

  /**
   * Get the actual bytes received from the underlying channel.
   * @return the number of bytes as a long value.
   */
  long getChannelReadCount();

  /**
   * Get the actual bytes sent to the underlying channel.
   * @return the number of bytes as a long value.
   */
  long getChannelWriteCount();
}
