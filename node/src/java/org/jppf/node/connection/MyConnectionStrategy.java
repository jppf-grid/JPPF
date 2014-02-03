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

package org.jppf.node.connection;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This implementation of {@link DriverConnectionStrategy} reads a list of drivers
 * from a CSV file where each line has the following format:
 * <p><code>secure, host, port, recovery_port</code>
 * <p>where:
 * <ul>
 * <li><i>secure</i> is a boolean value (either 'true' or 'false', case-insenssitive) indicating whether a SSL/TLS connection should be established.
 * any value that is not 'true' will be interpreted as 'false'.</li>
 * <li><i>host</i> is the host name or ip address of the driver to connect to</li>
 * <li><i>port</i> is the port to connect to on the driver host</li>
 * <li><i>recovery_port</i> is a valid port number for the recovery heartbeat mechanism, or a negative value to disable recovery for the node</li>
 * </ul>
 * <p>Additionally, any line starting with a '#' (after trimming) will be considered as a comment and ignored.
 * <p>The file location is read from the configuration property 'jppf.node.connection.strategy.file'.
 * It will  first be looked up in the file system, then in the classpath if it is not found in the file system.
 * If no file is found at all, the node will fall back to the {@link JPPFDefaultConnectionStrategy JPPF default strategy} and use the configuration to find the driver connection information.
 * <p>The listed drivers will be used as if they were arrayed in a "circle",
 * with the driver selection mechanism rotating one tick each time {@code nextConnectionInfo()} is invoked.
 *  * @author Laurent Cohen
 */
public class MyConnectionStrategy implements DriverConnectionStrategy {
  /**
   * The queue in which {@code DriverConnectionInfo} objects are stored.
   */
  private final Queue<DriverConnectionInfo> queue = new LinkedBlockingQueue<>();

  /**
   * Initialize the set of drivers to connect to.
   */
  public MyConnectionStrategy() {
    queue.offer(new JPPFDriverConnectionInfo(false, "192.168.1.11", 11111, -1));
    queue.offer(new JPPFDriverConnectionInfo(false, "192.168.1.12", 11111, -1));
    queue.offer(new JPPFDriverConnectionInfo(true, "192.168.1.13", 11143, -1));
  }

  @Override
  public DriverConnectionInfo nextConnectionInfo(
    final DriverConnectionInfo currentInfo, final ConnectionContext context) {
    if ((currentInfo != null) &&
        (context.getReason() == ConnectionReason.MANAGEMENT_REQUEST)) {
      return currentInfo;
    }
    DriverConnectionInfo info = queue.poll();
    queue.offer(info);
    return info;
  }
}
