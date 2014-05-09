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

package org.jppf.test.setup;

import java.util.*;

import org.jppf.client.*;
import org.jppf.management.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JMXHandler {
  /**
   * Cache of <i>distinct</i> driver JMX connections. There is one entry per driver to which the client is connected.
   */
  private final Map<String, JMXDriverConnectionWrapper> wrapperMap = new HashMap<>();
  /**
   * The JPPF client from which fetch the JMX connections.
   */
  private final AbstractGenericClient client;

  /**
   * Initialize this jmx handler with the specified JPPF client.
   * @param client the JPPF client from which fetch the JMX connections.
   */
  public JMXHandler(final AbstractGenericClient client) {
    this.client = client;
  }

  /**
   * Check that the driver and all nodes have been started and are accessible.
   * @param nbDrivers the number of drivers that were started.
   * @param nbNodes the number of nodes that were started.
   * @throws Exception if any error occurs.
   */
  public void checkDriverAndNodesInitialized(final int nbDrivers, final int nbNodes) throws Exception {
    if (client == null) throw new IllegalArgumentException("client cannot be null");
    Map<Integer, JPPFClientConnection> connectionMap = new HashMap<>();
    boolean allConnected = false;
    while (!allConnected) {
      List<JPPFClientConnection> list = client.getAllConnections();
      if (list != null) {
        for (JPPFClientConnection c: list) {
          // since all the drivers are local to the same host, we can differentiate them with their port number
          if (!connectionMap.containsKey(c.getPort())) connectionMap.put(c.getPort(), c);
        }
      }
      if (connectionMap.size() < nbDrivers) Thread.sleep(10L);
      else allConnected = true;
    }
    for (Map.Entry<Integer, JPPFClientConnection> entry: connectionMap.entrySet()) {
      JMXDriverConnectionWrapper wrapper = entry.getValue().getConnectionPool().getJmxConnection();
      String url = wrapper.getURL().toString();
      if (!wrapperMap.containsKey(url)) {
        while (!wrapper.isConnected()) wrapper.connectAndWait(10L);
        wrapperMap.put(url, wrapper);
      }
    }
    int sum = 0;
    while (sum < nbNodes) {
      sum = 0;
      for (Map.Entry<String, JMXDriverConnectionWrapper> entry: wrapperMap.entrySet()) {
        Integer n = entry.getValue().nbNodes();
        if (n != null) sum += n;
        else break;
      }
    }
  }

  /**
   * Close the JMX connections to all the drivers and clear the map.
   */
  public void clearWrapperMap() {
    for (Map.Entry<String, JMXDriverConnectionWrapper> entry: wrapperMap.entrySet())
    {
      JMXDriverConnectionWrapper wrapper = entry.getValue();
      try {
        if (wrapper.isConnected()) wrapper.close();
      } catch (Exception ingore) {
      }
    }
    wrapperMap.clear();
  }

  /**
   * Perform JMX-based operations on the drivers and nodes accesible from the client.
   * @param <D> The type of results returned by the operations performed on the drivers.
   * @param <N> The type of results returned by the operations performed on the nodes.
   * @param driverOp the opeariton to perform on each driver.
   * @param nodeOp the opeariton to perform on each node.
   * @return A mapping of driver results to a list of its attached nodes' results.
   * @throws Exception if any error occurs.
   */
  public <D, N> Map<JMXResult<D>, List<JMXResult<N>>> performJmxOperations(final JmxAwareCallable<D> driverOp, final JmxAwareCallable<N> nodeOp) throws Exception {
    Map<JMXResult<D>, List<JMXResult<N>>> map = new HashMap<>();
    for (Map.Entry<String, JMXDriverConnectionWrapper> entry: wrapperMap.entrySet()) {
      List<JMXResult<N>> list = new ArrayList<>();
      driverOp.setJmx(entry.getValue());
      JMXResult<D> t = driverOp.call();
      map.put(t, list);
      Collection<JPPFManagementInfo> coll = entry.getValue().nodesInformation();
      for (JPPFManagementInfo info: coll) {
        if (info.isPeer()) continue; // skip peer driver
        JMXNodeConnectionWrapper node = null;
        try {
          node = new JMXNodeConnectionWrapper(info.getHost(), info.getPort(), info.isSecure());
          node.connect();
          while (!node.isConnected()) Thread.sleep(10L);
          nodeOp.setJmx(node);
          list.add(nodeOp.call());
        } finally {
          if ((node != null) && node.isConnected()) node.close();
        }
      }
    }
    return map;
  }
}
