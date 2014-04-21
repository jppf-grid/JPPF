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

package org.jppf.example.initializationhook;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jppf.node.NodeRunner;
import org.jppf.node.initialization.InitializationHook;
import org.jppf.utils.*;

/**
 * A node initialization hook that implements a simple failover scheme for the connection to a driver.
 * It uses a list of servers, configured in a node configuration property, which it will browse in
 * round-robin fashion to discover the next server to connect to.
 * @author Laurent Cohen
 */
public class DiscoveryHook implements InitializationHook
{
  /**
   * A queue containing an ordered set of servers to connect/fallback to.
   */
  private Queue<String> servers = populateServers();
  /**
   * The currently configured server.
   */
  private String currentServer = null;
  /**
   * The next server, to which the node will fall back if the current server fails.
   */
  private String nextServer = null;

  /**
   * This method is called at node startup and each time the connection to the driver fails,
   * but before the new connection is established.
   * @param initialConfiguration the initial configuration, such as read from the config file or configuration input source.
   */
  @Override
  public void initializing(final UnmodifiableTypedProperties initialConfiguration)
  {
    // fetch the server to configure and put it back to the tail of the queue
    currentServer = servers.poll();
    servers.offer(currentServer);
    nextServer = servers.peek();
    configureServer(currentServer);
    // save the current and next server so they can be used by other plugins
    NodeRunner.setPersistentData("current.server", currentServer);
    NodeRunner.setPersistentData("next.server", nextServer);
  }

  /**
   * Read the servers from the configuration.
   * @return a {@link Queue} of <i<>host:port</i> strings.
   */
  private Queue<String> populateServers()
  {
    Queue<String> queue = (Queue<String>) NodeRunner.getPersistentData("jppf.servers");
    if (queue == null)
    {
      queue = new ConcurrentLinkedQueue<>();
      // servers are configured via the property "jppf.drivers.discovery" in the node's configuration
      String s = JPPFConfiguration.getProperties().getString("jppf.drivers.discovery", "").trim();
      if (!"".equals(s))
      {
        // servers are defined as a space-separated list of host:port strings
        // this defines both the servers and the order in which the node will try
        // to connect to them.
        String[] ids = s.split("\\s");
        System.out.println("*** found " + ids.length + " servers ***");
        for (String id: ids)
        {
          queue.offer(id);
          System.out.println("  registered server " + id);
        }
      }
      NodeRunner.setPersistentData("jppf.servers", queue);
    }
    return queue;
  }

  /**
   * Set the specified server config in the current JPPF configuration.
   * @param server the server to configure.
   */
  private void configureServer(final String server)
  {
    TypedProperties jppfConfig = JPPFConfiguration.getProperties();
    String[] tokens = server.split(":");
    // modify the node configuration so it will ocnnect to the specified server
    jppfConfig.setProperty("jppf.server.host", tokens[0]);
    jppfConfig.setProperty("jppf.server.port", tokens[1]);
  }
}
