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

package org.jppf.node.connection;

import java.util.*;

import org.jppf.utils.JPPFConfiguration;
import org.slf4j.*;

/**
 * This implementation of {@link DriverConnectionStrategy} reads a list of drivers from a configuration property with the following format:
 * <p><code>jppf.server.connection.strategy.definitions = secure1, host1, port1, recovery_port1 | ... | secureN, hostN, portN, recovery_portN</code>
 * <p>where:
 * <ul>
 *   <li>each connection definition is represented as a group of comma-separated values</li>
 *   <li>csv groups are separated with the '|' (pipe) character</li>
 *   <li>in each csv group:</li>
 *   <ul>
 *     <li><i>secure<sub>i</sub></i> is a boolean value (either 'true' or 'false', case-insenssitive) indicating whether a SSL/TLS connection should be established.
 *         any value that is not 'true' will be interpreted as 'false'.</li>
 *     <li><i>host<sub>i</sub></i> is the host name or ip address of the driver to connect to</li>
 *     <li><i>port<sub>i</sub></i> is the port to connect to on the driver host</li>
 *     <li><i>recovery_port<sub>i</sub></i> is a valid port number for the recovery heartbeat mechanism, or a negative value to disable recovery for the node</li>
 *   </ul>
 * </ul>
 * <p>Additionally, any group of comma-separated values starting with a '#' (after trimming) will be considered a comment and ignored.
 * This allows writing the value of the property on multiple lines, with comments, as follows:
 * <pre>jppf.server.connection.strategy.definitions = \
 *   # definition for server 1 |\
 *   false, my.host1.org, 11111, -1 |\
 *   # definition for server 2 |\
 *   true, my.host2.org, 11443, 2222
 * </pre>
 * <p>The listed drivers will be used as if they were arrayed in a "circle",
 * with the driver selection mechanism rotating one tick each time {@code nextConnectionInfo()} is invoked.
 * @author Laurent Cohen
 * @since 6.0
 */
public class JPPFCsvPropertyConnectionStrategy extends AbstractCsvConnectionStrategy {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFCsvPropertyConnectionStrategy.class);

  /**
   * Find and read the connection information.
   */
  public JPPFCsvPropertyConnectionStrategy() {
    super();
  }

  @Override
  List<String> getConnectionInfoAsLines() {
    try {
      String content = JPPFConfiguration.getProperties().getString("jppf.server.connection.strategy.definitions");
      if ((content != null) && !(content = content.trim()).isEmpty()) {
        final String[] lines = content.split("\\|");
        if ((lines != null) && (lines.length > 0)) {
          final List<String> result = Arrays.asList(lines);
          if (log.isDebugEnabled()) log.debug("found connection definitions in the configuration: {}", result);
          return result;
        }
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return Collections.emptyList();
  }
}
