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

package org.jppf.client;

import java.util.*;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.*;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The jmx port to use on the remote driver.
   */
  private int port = -1;
  /**
   * The associated client connection pool.
   */
  private final JPPFConnectionPool pool;

  /**
   * Initialize this pool witht he psecified core size.
   * @param pool the associated client connection pool.
   * @param coreSize the pool core size.
   */
  public JMXConnectionPool(final JPPFConnectionPool pool, final int coreSize) {
    super(coreSize);
    this.pool = pool;
  }

  @Override
  public synchronized JMXDriverConnectionWrapper getConnection() {
    int count = 0;
    int size = connections.size();
    while (count++ < size) {
      JMXDriverConnectionWrapper jmx = nextConnection();
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
    if (connectedOnly) return getConnections();
    List<JMXDriverConnectionWrapper> list = new ArrayList<>();
    for (JMXDriverConnectionWrapper connection: getConnections()) {
      if (connection.isConnected()) list.add(connection);
    }
    return list;
  }

  @Override
  public synchronized int setMaxSize(final int maxSize) {
    if (debugEnabled) log.debug("requesting new maxSize={}, current maxSize={}", maxSize, this.maxSize);
    if ((maxSize < coreSize) || (maxSize == this.maxSize)) return this.maxSize;
    int diff = maxSize - this.maxSize;
    int size = connectionCount();
    if (diff < 0) {
      int actual = 0;
      int i = size;
      while ((--i >= 0) && (actual < -diff)) {
        JMXDriverConnectionWrapper c = connections.get(i);
        if (!coreConnections.contains(c)) {
          if (debugEnabled) log.debug("removing connection {} from pool {}", c, this);
          try {
            c.close();
          } catch(Exception ignore) {
          }
          remove(c);
          actual++;
        }
      }
      this.maxSize -= actual;
    } else {
      for (int i=0; i<diff; i++) {
        JMXDriverConnectionWrapper c = new JMXDriverConnectionWrapper(pool.getDriverHost(), port, pool.isSslEnabled());
        this.add(c);
        c.connect();
      }
      this.maxSize += diff;
    }
    return this.maxSize;
  }

  /**
   * Callback invoked when the driver host is set on the enclosing {@link JPPFConnectionPool}.
   */
  synchronized void hostSet() {
    if (getPort() >= 0) initializeCoreConnections();
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
      if (pool.getDriverIPAddress() != null) initializeCoreConnections();
    }
  }

  /**
   * Initialize all the core connections.
   */
  private void initializeCoreConnections() {
    int n = 0;
    if ((n = connectionCount()) < coreSize) {
      for (int i=n; i<coreSize; i++) {
        JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper(pool.getDriverIPAddress(), port, pool.isSslEnabled());
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
  List<JMXDriverConnectionWrapper> awaitJMXConnections(final Operator operator, final int nbConnections, final long timeout, final boolean connected) {
    final Operator op = operator == null ? Operator.EQUAL : operator;
    setMaxSize(nbConnections);
    final MutableReference<List<JMXDriverConnectionWrapper>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        return op.evaluate(ref.set(getConnections(connected)).size(), nbConnections);
      }
    }, timeout);
    return ref.get();
  }
}
