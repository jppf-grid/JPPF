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

package org.jppf.client;

import java.util.*;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Implementation of a pool of {@link JMXDriverConnectionWrapper} instances.
 * @author Laurent Cohen
 * @since 4.2
 * @exclude
 */
class JMXConnectionPool extends AbstractConnectionPool<JMXDriverConnectionWrapper> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXConnectionPool.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The host and IP address of the driver.
   */
  private HostIP hostIP;
  /**
   * The jmx port to use on the remote driver.
   */
  private int port = -1;
  /**
   * Determines whether the pool is for SSL connections.
   */
  private final boolean sslEnabled;

  /**
   * Initialize this pool witht he psecified core size.
   * @param size the pool core size.
   * @param sslEnabled whether the pool is for SSL connections.
   */
  public JMXConnectionPool(final int size, final boolean sslEnabled) {
    super(size);
    this.sslEnabled = sslEnabled;
  }

  @Override
  public JMXDriverConnectionWrapper getConnection() {
    int count = 0;
    final int size = connectionCount();
    while (count++ < size) {
      final JMXDriverConnectionWrapper jmx = nextConnection();
      if (jmx.isConnected()) return jmx;
    }
    return null;
  }

  /**
   * Get the list of connections in the specified state.
   * @param connectedOnly if {@code true} then only lookup connections in the connected state, otherwise return all connections.
   * @return a list of {@link JMXDriverConnectionWrapper} objects, possibly empty but never {@code null}.
   * @since 5.0
   */
  synchronized List<JMXDriverConnectionWrapper> getConnections(final boolean connectedOnly) {
    if (!connectedOnly) return getConnections();
    final List<JMXDriverConnectionWrapper> list = new ArrayList<>();
    for (final JMXDriverConnectionWrapper connection: getConnections()) {
      if (connection.isConnected()) list.add(connection);
    }
    return list;
  }

  @Override
  public synchronized int setSize(final int maxSize) {
    if (debugEnabled) log.debug("requesting new maxSize={}, current maxSize={}", maxSize, this.size);
    if (maxSize == this.size) return this.size;
    final int diff = maxSize - this.size;
    final int size = connectionCount();
    if (diff < 0) {
      int actual = 0;
      int i = size;
      while ((--i >= 0) && (actual < -diff)) {
        final JMXDriverConnectionWrapper c = connections.get(i);
        if (debugEnabled) log.debug("removing connection {} from pool {}", c, this);
        try {
          c.close();
        } catch(@SuppressWarnings("unused") final Exception ignore) {
        }
        remove(c);
        actual++;
      }
      this.size -= actual;
    } else {
      for (int i=0; i<diff; i++) {
        final JMXDriverConnectionWrapper c = new JMXDriverConnectionWrapper(hostIP.ipAddress(), port, sslEnabled);
        this.add(c);
        c.connect();
      }
      this.size += diff;
    }
    return this.size;
  }

  /**
   * Get the JMX port to use on the remote driver.
   * @return the port as an int.
   */
  synchronized int getPort() {
    return port;
  }

  /**
   * Set the JMX port to use on the remote driver.
   * @param port the port as an int.
   */
  synchronized void setPort(final int port) {
    if ((this.port < 0) && (port >= 0)) {
      this.port = port;
      if (hostIP != null) initializeConnections();
    }
  }

  /**
   * Set the host and IP address of the driver.
   * @param hostIP a {@link HostIP} instance.
   */
  synchronized void setDriverHostIP(final HostIP hostIP) {
    if ((this.hostIP == null) && (hostIP != null)) {
      this.hostIP = hostIP;
      if (port >= 0) initializeConnections();
    }
  }

  /**
   * Initialize all the core connections.
   */
  private void initializeConnections() {
    int n = 0;
    if ((n = connectionCount()) < size) {
      for (int i=n; i<size; i++) {
        final JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper(hostIP.ipAddress(), port, sslEnabled);
        this.add(jmx);
        jmx.connect();
      }
    }
  }

  /**
   * Wait for the specified number of connections to be in one of the specified state, or the
   * specified timeout to expire, whichever happens first.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds.
   * @param connected the possible statuses of the connections to wait for.
   * @return a list of {@link JMXDriverConnectionWrapper} instances, possibly less than the requested number if the timeout expired first.
   * @since 5.0
   */
  List<JMXDriverConnectionWrapper> awaitJMXConnections(final ComparisonOperator operator, final int nbConnections, final long timeout, final boolean connected) {
    final ComparisonOperator op = operator == null ? Operator.EQUAL : operator;
    final MutableReference<List<JMXDriverConnectionWrapper>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(() -> op.evaluate(ref.set(getConnections(connected)).size(), nbConnections), timeout, 10L, false);
    return ref.get();
  }
}
