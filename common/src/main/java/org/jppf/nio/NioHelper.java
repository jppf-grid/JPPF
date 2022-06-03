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

package org.jppf.nio;

import java.lang.reflect.Method;
import java.util.*;

import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.utils.JPPFIdentifiers;
import org.slf4j.*;

/**
 * Utility methods to help with managing nio servers.
 * @author Laurent Cohen
 */
public class NioHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NioHelper.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of NIO servers to their identifier.
   */
  private final Map<Integer, NioServer> identifiedServers = new HashMap<>();
  /**
   * 
   */
  private final List<Integer> ports = new ArrayList<>();
  /**
   * 
   */
  private static Method getJMXServerMethod;
  /**
   * 
   */
  private static final Map<Integer, NioHelper> portToHelperMap = new HashMap<>();
  /**
   * 
   */
  private static AcceptorNioServer acceptor;

  /**
   * Get the {@link NioHelper} instance mapped to the specified port.
   * @param port the port number to lookup.
   * @return the {@link NioHelper} the port is mapped to, or {@code null} if the port is not mapped.
   */
  public static NioHelper getNioHelper(final int port) {
    synchronized(portToHelperMap) {
      return portToHelperMap.get(port);
    }
  }

  /**
   * Map a new port to the specified {@link NioHelper} instance.
   * @param port the port number to map.
   * @param nioHelper the {@link NioHelper} to map the port to.
   */
  public static void putNioHelper(final int port, final NioHelper nioHelper) {
    synchronized(portToHelperMap) {
      portToHelperMap.putIfAbsent(port, nioHelper);
      nioHelper.addPort(port);
    }
  }

  /**
   * Remove a port mùapping.
   * @param port the port number to remove.
   */
  public static void removeNioHelper(final int port) {
    synchronized(portToHelperMap) {
      final NioHelper nioHelper = portToHelperMap.remove(port);
      if (nioHelper != null) nioHelper.removePort(port);
    }
  }

  /**
   * Map the specified server tot he specified identifier.
   * @param identifier the JPPF identifier to mao to.
   * @param server the server to map.
   */
  public void putServer(final int identifier, final NioServer server) {
    synchronized(identifiedServers) {
      identifiedServers.put(identifier, server);
    }
  }

  /**
   * Add the specified port to the list of ports.
   * @param port the port to add.
   */
  public void addPort(final int port) {
    synchronized(identifiedServers) {
      ports.add(port);
    }
  }

  /**
   * Remove the specified port from the list of ports.
   * @param port the port to remove.
   */
  public void removePort(final int port) {
    synchronized(identifiedServers) {
      ports.remove((Integer) port);
    }
  }

  /**
   * Get the list of ports.
   * @return a copy of the list of ports.
   */
  public List<Integer> getPorts() {
    synchronized(identifiedServers) {
      return new ArrayList<>(ports);
    }
  }

  /**
   * Map the specified server tot he specified identifier.
   * @param identifier the JPPF identifier to mao to.
   * @return the server that was removed, or {@code null} if there was no server for the specified identifier.
   */
  public NioServer removeServer(final int identifier) {
    synchronized(identifiedServers) {
      return identifiedServers.remove(identifier);
    }
  }

  /**
   * Get the server mapped to the specified identifier.
   * @param identifier the JPPF identifier to lookup.
   * @return a {@link NioServer} instance.
   * @throws Exception if any error occurs.
   */
  public NioServer getServer(final int identifier) throws Exception {
    synchronized(identifiedServers) {
      if (identifier == JPPFIdentifiers.JMX_REMOTE_CHANNEL) {
        if (getJMXServerMethod == null) initializeJMXServerPool();
        return (NioServer) getJMXServerMethod.invoke(null); 
      }
      return identifiedServers.get(identifier);
    }
  }

  /**
   * Get the acceptor server.
   * @param create whether to create a new acceptor if none exists.
   * @return a {@link AcceptorNioServer} instance.
   * @throws Exception if any error occurs.
   */
  public synchronized static AcceptorNioServer getAcceptorServer(final boolean create) throws Exception {
    if ((acceptor == null) && create) {
      if (debugEnabled) log.debug("starting acceptor");
      acceptor = new AcceptorNioServer(null, null, null, null);
      acceptor.start();
      if (debugEnabled) log.debug("acceptor started");
    }
    return acceptor;
  }

  /**
   * Get the acceptor server.
   * @param acceptorServer the acceptor server to set.
   * @throws Exception if any error occurs.
   */
  public synchronized static void setAcceptorServer(final AcceptorNioServer acceptorServer) throws Exception {
    if (acceptor == null) {
      if (debugEnabled) log.debug("setting acceptor");
      acceptor = acceptorServer;
    }
  }

  /**
   * Initialize the JMX server pool.
   * @throws Exception if any error occurs.
   */
  private static void initializeJMXServerPool() throws Exception {
    final String className = "org.jppf.jmxremote.nio.JMXNioServerPool";
    final Class<?> clazz = Class.forName(className);
    getJMXServerMethod = clazz.getDeclaredMethod("getServer");
  }
}
