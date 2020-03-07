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
package org.jppf.example.gridfailover;

import java.util.Arrays;

import org.jppf.JPPFRuntimeException;
import org.jppf.node.connection.ConnectionContext;
import org.jppf.node.connection.DriverConnectionInfo;
import org.jppf.node.connection.DriverConnectionStrategy;
import org.jppf.node.connection.JPPFDriverConnectionInfo;
import org.jppf.utils.TypedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node connection strategy which handles an ordered list of drivers to connect to as follows:
 * <ul>
 * <li>when initially connecting or reconnecting due to a mangement request, the node attemps to connect
 * to the first (highest priority) driver in the list, then goes down the list if the driver is not online</li>
 * <li>when reconnecting due to a disconnection from the driver, the node goes down the list of drivers,
 * rolling back to the top when the end of the list is reached</li>
 * </ul>
 * @author Laurent Cohen
 */
public class NodeSideDiscovery implements DriverConnectionStrategy {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeSideDiscovery.class);
  /**
   * An array which contains the connection information for all the drivers enrolled in the failover strategy.
   */
  final JPPFDriverConnectionInfo[] infos;
  /**
   * Current index in the array of {@link JPPFDriverConnectionInfo} instances.
   */
  private int index;

  /**
   * Initiialize this connection strategy by reading a driver definitions file in yaml format.
   * @param configuration the configuration of the node.
   */
  public NodeSideDiscovery(final TypedProperties configuration) {
    try {
      // locate the drivers definition file in YAML fomrat
      final String yamlPath = configuration.getString("drivers.definition.file", "drivers.yaml");
      // read and parse the drivers definition file
      this.infos = Utils.parseYaml(yamlPath, JPPFDriverConnectionInfo[].class);
      System.out.println("read infos:\n" + Arrays.toString(infos));
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new JPPFRuntimeException(e);
    }
  }

  @Override
  public DriverConnectionInfo nextConnectionInfo(final DriverConnectionInfo currentInfo, final ConnectionContext context) {
    log.info("currentInfo = {}, reason = {}", currentInfo, context.getReason());
    System.out.printf("currentInfo = %s, reason = %s\n", currentInfo, context.getReason());
    switch(context.getReason()) {
      // when initialiy connecting or reconnecting due to a
      // management request, start at the first driver in the list
      case INITIAL_CONNECTION_REQUEST:
      case MANAGEMENT_REQUEST:
        index = 0;
        break;

      // in all other cases connect to the nextdriver in the list,
      // and rollover when reaching the end of the list
      default:
        index = (index + 1) % infos.length;
        break;
    }
    return infos[index];
  }
}
