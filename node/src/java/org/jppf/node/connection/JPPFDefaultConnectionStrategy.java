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

package org.jppf.node.connection;

import org.jppf.comm.discovery.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This implementation of {@link DriverConnectionStrategy} is the JPPF default
 * and produces DriverConnectionInfo instances based solely on the JPPF configuration.
 * @author Laurent Cohen
 * @since 4.1
 */
public class JPPFDefaultConnectionStrategy implements DriverConnectionStrategy {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFDefaultConnectionStrategy.class);

  @Override
  public DriverConnectionInfo nextConnectionInfo(final DriverConnectionInfo currentInfo, final ConnectionContext context) {
    return JPPFConfiguration.get(JPPFProperties.DISCOVERY_ENABLED) ? discoverDriver() : connectionFromManualConfiguration();
  }

  /**
   * Automatically discover the server connection information using a datagram multicast.
   * Upon receiving the connection information, the JPPF configuration is modified to take into
   * account the discovered information. If no information could be received, the node relies on
   * the static information in the configuration file.
   * @return the discovered connection information.
   */
  private DriverConnectionInfo discoverDriver() {
    TypedProperties config = JPPFConfiguration.getProperties();
    JPPFMulticastReceiver receiver = new JPPFMulticastReceiver(new IPFilter(config));
    JPPFConnectionInformation info = receiver.receive();
    receiver.setStopped(true);
    if (info == null) {
      if (log.isDebugEnabled()) log.debug("Could not auto-discover the driver connection information");
      return connectionFromManualConfiguration();
    }
    if (log.isDebugEnabled()) log.debug("Discovered driver: " + info);
    boolean ssl = config.get(JPPFProperties.SSL_ENABLED);
    boolean recovery = config.get(JPPFProperties.RECOVERY_ENABLED) && (info.recoveryPort >= 0);
    return JPPFDriverConnectionInfo.fromJPPFConnectionInformation(info, ssl, recovery);
  }

  /**
   * Determine the connection information specified manually in the configuration.
   * @return the configured connection information.
   */
  private DriverConnectionInfo connectionFromManualConfiguration() {
    TypedProperties config = JPPFConfiguration.getProperties();
    boolean ssl = config.get(JPPFProperties.SSL_ENABLED);
    String host = config.get(JPPFProperties.SERVER_HOST);
    int port = config.get(ssl ? JPPFProperties.SERVER_SSL_PORT_NODE : JPPFProperties.SERVER_PORT);
    int recoveryPort  = config.get(JPPFProperties.RECOVERY_ENABLED) ? config.get(JPPFProperties.RECOVERY_SERVER_PORT) : -1; 
    return new JPPFDriverConnectionInfo(ssl, host, port, recoveryPort);
  }
}
