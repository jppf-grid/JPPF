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

import java.io.Closeable;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.ConfigurationHelper;
import org.jppf.utils.pooling.AbstractBoundedObjectPoolQueue;

/**
 * Super interface for all input source and output destination implementations.
 * @author Laurent Cohen
 */
public interface IO extends Closeable {
  /**
   * Size of send and receive buffer for socket connections. Defaults to 32768.
   */
  int SOCKET_BUFFER_SIZE = new ConfigurationHelper(JPPFConfiguration.getProperties()).getInt("jppf.socket.buffer.size", 32*1024, 1024, 1024 * 1024);
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
   * Size of temporary buffer pool. Defaults to 10.
   */
  int TEMP_BUFFER_POOL_SIZE = new ConfigurationHelper(JPPFConfiguration.getProperties()).getInt("jppf.temp.buffer.pool.size", 10, 1, 2*1024);
  /**
   * Size of temporary buffer pool. Defaults to 100.
   */
  int LENGTH_BUFFER_POOL_SIZE = new ConfigurationHelper(JPPFConfiguration.getProperties()).getInt("jppf.length.buffer.pool.size", 100, 1, 2*1024);
  /**
   * A definition of an empty byte array.
   */
  byte[] EMPTY_BYTES = new byte[0];
  /**
   * A bounded pool of temporary buffers.
   */
  AbstractBoundedObjectPoolQueue<byte[]> TEMP_BUFFER_POOL = new BoundedByteArrayPool(TEMP_BUFFER_POOL_SIZE, TEMP_BUFFER_SIZE);
  /**
   * A bounded pool of temporary buffers for reading/writing lengths, geenrally from/to a stream.
   */
  AbstractBoundedObjectPoolQueue<byte[]> LENGTH_BUFFER_POOL = new BoundedByteArrayPool(LENGTH_BUFFER_POOL_SIZE, 4);
  /**
   * Ratio of free memory / requested allocation size threshold that triggers disk overflow.
   */
  double FREE_MEM_TO_SIZE_RATIO = JPPFConfiguration.getProperties().getDouble("jppf.disk.overflow.threshold", 2.0d);
  /**
   * Whether to trigger a garbage collection whenever disk overflow is triggered.
   */
  boolean GC_ON_DISK_OVERFLOW = JPPFConfiguration.getProperties().getBoolean("jppf.gc.on.disk.overflow", true);
  /**
   * The available heap threshold above which it is unlikely that memory fragmentation will cause object allocations to fail,
   * i.e. when there is enough free memory but not enough <i><b>contiguous</b></i> free memory. Default value is 32 MB.  
   */
  long LOW_MEMORY_THRESHOLD = JPPFConfiguration.getProperties().getLong("jppf.low.memory.threshold", 32L) * 1024L * 1024L;
}
