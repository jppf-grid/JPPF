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

package org.jppf.jmxremote.nio;

import org.jppf.JPPFError;
import org.jppf.utils.JPPFConfiguration;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXNioServerPool {
  /**
   * The size of this pool.
   */
  private static final int POOL_SIZE = JPPFConfiguration.getProperties().getInt("jppf.jmxremote.nio.servers.pool.size", 1);
  /**
   * The servers in this pool.
   */
  private static JMXNioServer[] servers = initialize();
  /**
   * The current index in the array of servers.
   */
  private static int currentIndex;
  /**
   * Whether this pool and all servers in it are closed.
   */
  private static boolean closed;

  /**
   * @return .
   * @throws Exception if any error occurs.
   */
  private static JMXNioServer[]  initialize() {
    final JMXNioServer[] servers = new JMXNioServer[POOL_SIZE];
    try {
      for (int i=0; i<POOL_SIZE; i++) {
        servers[i] = new JMXNioServer();
        servers[i].start();
      }
    } catch (final Exception e) {
      throw new JPPFError("can't initialize JMX server pool", e);
    }
    return servers;
  }

  /**
   * Get a server from the pool.
   * @return a {@link JMXNioServer} instance.
   * @throws Exception if any error occurs.
   */
  public static JMXNioServer getServer() throws Exception {
    final JMXNioServer server;
    synchronized(servers) {
      server = servers[currentIndex];
      currentIndex = (currentIndex + 1) % POOL_SIZE;
    }
    return server;
  }

  /**
   * CLose this pool and all the servers in it.
   */
  public static void close() {
    if (!closed) {
      closed = true;
      if (servers != null) for (JMXNioServer server: servers) server.end();
    }
  }

  /**
   * @return the servers.
   * @throws Exception if any error occurs.
   */
  public static JMXNioServer[] getServers() throws Exception {
    return servers;
  }
}
