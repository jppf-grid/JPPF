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
import java.util.concurrent.LinkedBlockingQueue;

import org.jppf.utils.RegexUtils;
import org.slf4j.*;

/**
 * Common abstact super class for connection strategies that read connection from groups of comma-separated values.
 * @author Laurent Cohen
 * @since 6.0
 */
public abstract class AbstractCsvConnectionStrategy implements DriverConnectionStrategy {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractCsvConnectionStrategy.class);
  /**
   * The queue in which {@code DriverConnectionInfo} objects are stored.
   */
  final Queue<DriverConnectionInfo> queue = new LinkedBlockingQueue<>();
  /**
   * The fallback strategy to use in case the CSV file is not found or none of the driver defintions it contains is valid. 
   */
  final DriverConnectionStrategy fallbackStrategy;

  /**
   * Find and read the CSV data.
   */
  public AbstractCsvConnectionStrategy() {
    readAllConnectionInfo();
    fallbackStrategy = queue.isEmpty() ? new JPPFDefaultConnectionStrategy() : null;
    if (log.isDebugEnabled()) {
      if (queue.isEmpty()) log.debug("no valid driver definition found, falling back to default strategy");
      else {
        final StringBuilder sb = new StringBuilder("driver definitions:");
        for (final DriverConnectionInfo info: queue) sb.append('\n').append(info);
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
    final DriverConnectionInfo info = queue.poll();
    queue.offer(info);
    return info;
  }

  /**
   * Parse the CSV file specified in the configuration and convert each line
   * into a {@link DriverConnectionInfo} which is then added to the queue.
   */
  void readAllConnectionInfo() {
    try {
      final List<String> lines = getConnectionInfoAsLines();
      for (final String line: lines) {
        final DriverConnectionInfo info = parseLine(line.trim());
        if (info != null) {
          if (log.isDebugEnabled()) log.debug("got connection info: {}", info);
          queue.offer(info);
        }
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * @return a list of csv-fromatted lines.
   */
  abstract List<String> getConnectionInfoAsLines();

  /**
   * Parse a CSV line to generate a driver connection info.
   * @param csv the csv line to parse.
   * @return a {@link DriverConnectionInfo} instance, or {@code null} if the CSV is not valid or a comment.
   */
  static DriverConnectionInfo parseLine(final String csv) {
    if (csv.startsWith("#")) return null;
    final String[] tokens = RegexUtils.COMMA_PATTERN.split(csv);
    if ((tokens != null) && (tokens.length == 4)) {
      for (int i=0; i<tokens.length; i++) tokens[i] = tokens[i].trim();
      final boolean secure = "true".equalsIgnoreCase(tokens[0]);
      final String host = tokens[1];
      final int port;
      try {
        port = Integer.valueOf(tokens[2]);
      } catch(@SuppressWarnings("unused") final Exception e) {
        return null;
      }
      int recoveryPort;
      try {
        recoveryPort = Integer.valueOf(tokens[3]);
      } catch(@SuppressWarnings("unused") final Exception e) {
        recoveryPort = -1;
      }
      return new JPPFDriverConnectionInfo(secure, host, port, recoveryPort);
    }
    return null;
  }
}
