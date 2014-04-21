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

import java.io.Reader;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

import org.jppf.utils.*;
import org.slf4j.*;

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
 * <p>The file location is read from the configuration property {@code 'jppf.node.connection.strategy.file'}.
 * It will  first be looked up in the file system, then in the classpath if it is not found in the file system.
 * If no file is found at all, the node will fall back to the {@link JPPFDefaultConnectionStrategy JPPF default strategy} and use the configuration to find the driver connection information.
 * <p>The listed drivers will be used as if they were arrayed in a "circle",
 * with the driver selection mechanism rotating one tick each time {@code nextConnectionInfo()} is invoked.
 * @author Laurent Cohen
 * @since 4.1
 */
public class JPPFCsvFileConnectionStrategy implements DriverConnectionStrategy {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFCsvFileConnectionStrategy.class);
  /**
   * The queue in which {@code DriverConnectionInfo} objects are stored.
   */
  private final Queue<DriverConnectionInfo> queue = new LinkedBlockingQueue<>();
  /**
   * The fallback strategy to use in case the CSV file is not found or none of the driver defintions it contains is valid. 
   */
  private final DriverConnectionStrategy fallbackStrategy;

  /**
   * Find and read the CSV file.
   */
  public JPPFCsvFileConnectionStrategy() {
    readCsvFile();
    fallbackStrategy = queue.isEmpty() ? new JPPFDefaultConnectionStrategy() : null;
    if (log.isDebugEnabled()) {
      if (queue.isEmpty()) log.debug("no valid driver definition found, falling back to default strategy");
      else {
        StringBuilder sb = new StringBuilder("driver definitions:");
        for (DriverConnectionInfo info: queue) sb.append('\n').append(info);
        log.debug(sb.toString());
      }
    }
  }

  @Override
  public DriverConnectionInfo nextConnectionInfo(final DriverConnectionInfo currentInfo, final ConnectionContext context) {
    if (fallbackStrategy != null) return fallbackStrategy.nextConnectionInfo(currentInfo, context);
    if (log.isDebugEnabled()) log.debug("new connection request with prevInfo={} and context={}", currentInfo, context);
    if ((currentInfo != null) && (context.getReason() == ConnectionReason.MANAGEMENT_REQUEST)) {
      return currentInfo;
    }
    DriverConnectionInfo info = queue.poll();
    queue.offer(info);
    return info;
  }

  /**
   * Parse the CSV file specified in the configuration and convert each line
   * into a {@link DriverConnectionInfo} which is then added to the queue.
   */
  private void readCsvFile() {
    try {
      String path = JPPFConfiguration.getProperties().getString("jppf.server.connection.strategy.file");
      if ((path != null) && !(path = path.trim()).isEmpty()) {
        Reader reader = FileUtils.getFileReader(path);
        if (reader != null) {
          List<String> lines = FileUtils.textFileAsLines(reader);
          for (String line: lines) {
            DriverConnectionInfo info = parseLine(line.trim());
            if (info != null) queue.offer(info);
          }
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Parse a CSV line to generate a driver connection info.
   * @param csv the csv line to parse.
   * @return a {@link DriverConnectionInfo} instance, or {@code null} if the CSV is not valid or a comment.
   */
  private DriverConnectionInfo parseLine(final String csv) {
    if (csv.startsWith("#")) return null;
    String[] tokens = csv.split(",");
    if ((tokens != null) && (tokens.length == 4)) {
      for (int i=0; i<tokens.length; i++) tokens[i] = tokens[i].trim();
      boolean secure = "true".equalsIgnoreCase(tokens[0]);
      String host = tokens[1];
      int port;
      try {
        port = Integer.valueOf(tokens[2]);
      } catch(Exception e) {
        return null;
      }
      int recoveryPort;
      try {
        recoveryPort = Integer.valueOf(tokens[3]);
      } catch(Exception e) {
        recoveryPort = -1;
      }
      return new JPPFDriverConnectionInfo(secure, host, port, recoveryPort);
    }
    return null;
  }
}
