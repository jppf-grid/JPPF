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

package org.jppf.client;

import static org.jppf.utils.configuration.JPPFProperties.*;

import java.util.*;

import org.jppf.comm.discovery.*;
import org.jppf.discovery.*;
import org.jppf.utils.TypedProperties;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * Built-in driver discovery which creates connections pools based on the client configuration.
 * @see org.jppf.discovery.ClientDriverDiscovery
 * @author Laurent Cohen
 */
public class ClientConfigDriverDiscovery extends ClientDriverDiscovery {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientConfigDriverDiscovery.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The configuration to use.
   */
  private final TypedProperties config;
  /**
   * Emits notifications based on data received via UDP multicast.
   */
  private JPPFMulticastReceiverThread receiverThread;

  /**
   * Initialize this discovery with the specified client configuration.
   * @param config the configuration from which to discover JPPF drivers.
   */
  public ClientConfigDriverDiscovery(final TypedProperties config) {
    this.config = config;
  }

  @Override
  public void discover() throws InterruptedException {
    try {
      boolean initPeers;
      if (config.get(DISCOVERY_ENABLED)) {
        final int priority = config.get(DISCOVERY_PRIORITY);
        final boolean acceptMultipleInterfaces = config.get(DISCOVERY_ACCEPT_MULTIPLE_INTERFACES);
        final boolean heartbeatEnabled = config.get(RECOVERY_ENABLED);
        if (debugEnabled) log.debug("initializing connections from discovery with priority = {} and acceptMultipleInterfaces = {}", priority, acceptMultipleInterfaces);
        final boolean ssl = config.get(SSL_ENABLED);
        receiverThread = new JPPFMulticastReceiverThread(new JPPFMulticastReceiverThread.ConnectionHandler() {
          @Override
          public void onNewConnection(final String name, final JPPFConnectionInformation info) {
            if (info.hasValidPort(ssl)) {
              final int poolSize = config.get(POOL_SIZE);
              final int jmxPoolSize = config.get(JMX_POOL_SIZE);
              newConnection(new ClientConnectionPoolInfo(name, ssl, info.host, info.getValidPort(ssl), priority, poolSize, jmxPoolSize, heartbeatEnabled));
            } else {
              final String type = ssl ? "secure" : "plain";
              log.warn("cannot fulfill a {} connection request to {}:{} because the host does not expose this port as a {} port", type, info.host, info.getValidPort(ssl), type);
            }
          }
        }, new IPFilter(config), acceptMultipleInterfaces);
        ThreadUtils.startDaemonThread(receiverThread, "ReceiverThread");
        initPeers = false;
      } else {
        receiverThread = null;
        initPeers = true;
      }
      if (debugEnabled) log.debug("looking for peers in the configuration");
      final String[] names = config.get(DRIVERS);
      if (debugEnabled) log.debug("list of drivers: {}", Arrays.asList(names));
      for (final String name : names) initPeers |= AbstractGenericClient.VALUE_JPPF_DISCOVERY.equals(name);
      if (debugEnabled) log.debug("initPeers = {}", initPeers);
      if (initPeers) {
        final List<ClientConnectionPoolInfo> infoList = new ArrayList<>(names.length);
        for (final String name : names) {
          if (!AbstractGenericClient.VALUE_JPPF_DISCOVERY.equals(name)) {
            final boolean ssl = config.get(PARAM_SERVER_SSL_ENABLED, name);
            final String host =  config.get(PARAM_SERVER_HOST, name);
            final int port = config.get(PARAM_SERVER_PORT, name);
            final int priority = config.get(PARAM_PRIORITY, name);
            final int poolSize = config.get(PARAM_POOL_SIZE, name);
            final int jmxPoolSize = config.get(PARAM_JMX_POOL_SIZE, name);
            final boolean heartbeatEnabled = config.get(PARAM_RECOVERY_ENABLED, name);
            final ClientConnectionPoolInfo ccpi = new ClientConnectionPoolInfo(name, ssl, host, port, priority, poolSize, jmxPoolSize, heartbeatEnabled);
            if (debugEnabled) log.debug("found pool definition in the configuration: {}", ccpi);
            infoList.add(ccpi);
          }
        }
        if (debugEnabled) log.debug("found {} pool definitions in the configuration", infoList.size());
        // order by decreasing priority before calling newConnection(), to ensure any submitted job is submitted to the connection pool with highest priority
        Collections.sort(infoList, new Comparator<ClientConnectionPoolInfo>() {
          @Override
          public int compare(final ClientConnectionPoolInfo o1, final ClientConnectionPoolInfo o2) {
            final int p1 = o1.getPriority(), p2 = o2.getPriority();
            return p1 > p2 ? -1 : (p1 < p2 ? 1 : 0);
          }
        });
        for (final ClientConnectionPoolInfo poolInfo: infoList) newConnection(poolInfo);
      }
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public void shutdown() {
    if (receiverThread != null) {
      receiverThread.close();
      receiverThread = null;
    }
  }
}
