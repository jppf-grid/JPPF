/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

import java.io.Closeable;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.ConfigurationHelper;

/**
 * Super interface for all input source and output destination implementations.
 * @author Laurent Cohen
 */
public interface IO extends Closeable
{
  /**
   * Size of receive buffer size for socket connections.
   */
  int SOCKET_BUFFER_SIZE = ConfigurationHelper.getInt("jppf.socket.buffer.size", 32*1024, 1024, 64 * 1024);
  /**
   * Disable Nagle's algorithm to improve performance.
   */
  boolean SOCKET_TCP_NO_DELAY = JPPFConfiguration.getProperties().getBoolean("jppf.socket.tcp_no_delay", true);
  /**
   * Size of temporary buffers used in I/O transfers.
   */
  int TEMP_BUFFER_SIZE = ConfigurationHelper.getInt("jppf.temp.buffer.size", 32*1024, 1024, 65536);
  /**
   * A definition of an empty byte array.
   */
  byte[] EMPTY_BYTES = new byte[0];
}
