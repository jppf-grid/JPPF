/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.example.initializationhook;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.node.initialization.InitializationHook;
import org.jppf.utils.TypedProperties;

/**
 * A node initialization hook that implements a simple failover scheme for the connection to a driver.
 * It uses a list of servers, configured in a node configuration property, which it will browse in
 * round-robin fashion to discover the next server to connect to.
 * @author Laurent Cohen
 */
public class DiscoveryHook implements InitializationHook {
  /**
   * A queue containing an ordered set of servers to connect/fallback to.
   */
  private static Queue<String> serversQueue;
  /**
   * The currently configured server.
   */
  private String currentServer;
  /**
   * The JPPF configuration passed on to this initialization hook.
   */
  private TypedProperties jppfConfig;

  /**
   * This method is called at node startup and each time the connection to the driver fails,
   * but before the new connection is established.
   * @param initialConfiguration the initial configuration, such as read from the config file or configuration input source.
   */
  @Override
  public void initializing(final TypedProperties initialConfiguration) {
    jppfConfig = initialConfiguration;
    populateServers();
    // fetch the server to configure and put it back to the tail of the queue
    currentServer = serversQueue.poll();
    serversQueue.offer(currentServer);
    configureServer(currentServer);
  }

  /**
   * Read the servers from the configuration.
   * @return a {@link Queue} of <i<>host:port</i> strings.
   */
  private Queue<String> populateServers() {
    synchronized(getClass()) {
      if (serversQueue == null) {
        serversQueue = new ConcurrentLinkedQueue<>();
        // servers are configured via the property "jppf.drivers.discovery" in the node's configuration
        final String s = jppfConfig.getString("jppf.drivers.discovery", "").trim();
        if (!s.isEmpty()) {
          // servers are defined as a space-separated list of host:port strings
          // this defines both the servers and the order in which the node will try
          // to connect to them.
          final String[] ids = s.split("\\s");
          System.out.println("*** found " + ids.length + " servers ***");
          for (final String id : ids) {
            serversQueue.offer(id);
            System.out.println("  registered server " + id);
          }
        }
      }
    }
    return serversQueue;
  }

  /**
   * Set the specified server config in the current JPPF configuration.
   * @param server the server to configure.
   */
  private void configureServer(final String server) {
    final String[] tokens = server.split(":");
    // modify the node configuration so it will ocnnect to the specified server
    jppfConfig.setProperty("jppf.server.host", tokens[0]);
    jppfConfig.setProperty("jppf.server.port", tokens[1]);
  }
}
