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
   * Size of send and receive buffer for socket connections. Defaults to 32768.
   */
  int SOCKET_BUFFER_SIZE = new ConfigurationHelper(JPPFConfiguration.getProperties()).getInt("jppf.socket.buffer.size", 32*1024, 1024, 64 * 1024);
  /**
   * Disable Nagle's algorithm to improve performance. Defaults to true.
   */
  boolean SOCKET_TCP_NODELAY = JPPFConfiguration.getProperties().getBoolean("jppf.socket.tcp_nodelay", true);
  /**
   * Enable / disable keepalive. Defaults to false.
   */
  boolean SOCKET_KEEPALIVE = JPPFConfiguration.getProperties().getBoolean("jppf.socket.keepalive", false);
  /**
   * Size of temporary buffers (including direct buffers) used in I/O transfers. Defaults to 32768.
   */
  int TEMP_BUFFER_SIZE = new ConfigurationHelper(JPPFConfiguration.getProperties()).getInt("jppf.temp.buffer.size", 32*1024, 1024, 65536);
  /**
   * A definition of an empty byte array.
   */
  byte[] EMPTY_BYTES = new byte[0];
}
