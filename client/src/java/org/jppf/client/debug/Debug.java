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

package org.jppf.client.debug;

import java.lang.management.ManagementFactory;
import java.util.*;

import javax.management.*;

import org.jppf.client.*;

/**
 *
 * @author Laurent Cohen
 */
public class Debug implements DebugMBean {
  /**
   * The client to monitor.
   */
  private final JPPFClient client;
  /**
   *
   */
  private final Map<String, String> parameters = new TreeMap<>();

  /**
   * Initialize this MBean instance.
   * @param client the client to monitor.
   */
  public Debug(final JPPFClient client) {
    this.client = client;
  }

  @Override
  public String allConnections() {
    final List<JPPFConnectionPool> all = client.getConnectionPools();
    final StringBuilder sb = new StringBuilder();
    int count = 0;
    for (final JPPFConnectionPool pool: all) {
      for (final JPPFClientConnection c: pool.getConnections()) {
        if (count > 0) sb.append('\n');
        sb.append(c.toString());
        count++;
      }
    }
    return sb.toString();
  }

  /**
   * Register a debug mbean for the specified JPPF client.
   * @param client the client for which to register a debug mbean.
   */
  public static void register(final JPPFClient client) {
    try {
      final Debug debug = new Debug(client);
      final StandardMBean mbean = new StandardMBean(debug, DebugMBean.class);
      final ObjectName name = new ObjectName(DebugMBean.MBEAN_NAME_PREFIX + client.getUuid());
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      server.registerMBean(mbean, name);
      //System.out.println("registered client debug mbean: " + name);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Unregister a debug mbean for the specified JPPF client.
   * @param client the client for which to unregister a debug mbean.
   */
  public static void unregister(final JPPFClient client) {
    try {
      final ObjectName name = new ObjectName(DebugMBean.MBEAN_NAME_PREFIX + client.getUuid());
      final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      if (server.isRegistered(name)) server.unregisterMBean(name);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getParameter(final String key) {
    return parameters.get(key);
  }

  @Override
  public void setParameter(final String key, final String value) {
    parameters.put(key, value);
  }

  @Override
  public void removeParameter(final String key) {
    parameters.remove(key);
  }

  @Override
  public String allParameters() {
    final StringBuilder sb = new StringBuilder();
    int count = 0;
    for (final Map.Entry<String, String> param: parameters.entrySet()) {
      if (count++ > 0) sb.append('\n');
      sb.append(param.getKey()).append(" = ").append(param.getValue());
    }
    return sb.toString();
  }
}
