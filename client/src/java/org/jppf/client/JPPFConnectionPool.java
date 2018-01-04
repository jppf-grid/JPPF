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

import java.util.List;

import org.jppf.discovery.ClientConnectionPoolInfo;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Instances of this class manage a list of client connections with the same pool name, priority and remote driver.
 * <p>This connection pool also holds a pool of JMX connections to the same remote driver
 * @since 4.1
 */
public class JPPFConnectionPool extends AbstractClientConnectionPool {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFConnectionPool.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this pool with the specified parameters.
   * @param client the JPPF client which holds this pool.
   * @param id the id of this pool.
   * @param name name assigned to this pool.
   * @param priority the priority of the connectios in this pool.
   * @param size the core size of this pool.
   * @param sslEnabled determines whether the pool is for SSL connections.
   * @param jmxPoolSize the core size of the JMX connections pool.
   */
  JPPFConnectionPool(final JPPFClient client, final int id, final String name, final int priority, final int size, final boolean sslEnabled, final int jmxPoolSize) {
    super(client, id, name, priority, size, sslEnabled, jmxPoolSize);
  }

  /**
   * Initialize this pool with the specified parameters.
   * @param client the JPPF client which holds this pool.
   * @param id the id of this pool.
   * @param info information needed for the pool's attributes.
   */
  JPPFConnectionPool(final JPPFClient client, final int id, final ClientConnectionPoolInfo info) {
    this(client, id, info.getName(), info.getPriority(), info.getPoolSize(), info.isSecure(), info.getJmxPoolSize());
    this.discoveryInfo = info;
  }

  @Override
  public int setSize(final int newSize) {
    final int currentSize = getSize();
    if (currentSize == newSize) return currentSize;
    if (debugEnabled) log.debug("requesting new size={}, current size={}", newSize, currentSize);
    final int diff = newSize - currentSize;
    final int size = connectionCount();
    if (diff < 0) {
      int actual = 0;
      int i = size;
      while ((--i >= 0) && (actual < -diff)) {
        final JPPFClientConnection c = getConnection(i);
        if (connectionHasStatus(c, false, JPPFClientConnectionStatus.EXECUTING)) {
          if (debugEnabled) log.debug("removing connection {} from pool {}", c, this);
          c.close();
          remove(c);
          actual++;
        }
      }
      synchronized(this) {
        this.size -= actual;
      }
    } else {
      synchronized(this) {
        this.size += diff;
      }
      for (int i=0; i<diff; i++) client.submitNewConnection(this);
    }
    return getSize();
  }

  /**
   * Wait for the specified number of connections to be in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} status.
   * This is a shorthand for {@code awaitConnections(nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE)}.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the expected number of connections to wait for.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances with the desired status.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitActiveConnections(final Operator operator, final int nbConnections) {
    return awaitConnections(operator, nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE);
  }

  /**
   * Wait for the a connection to be in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} status.
   * This is a shorthand for {@code awaitActiveConnections(Operator.AT_LEAST, 1).get(0)}.
   * @return a {@link JPPFClientConnection} instances with the desired status.
   * @since 5.1
   */
  public JPPFClientConnection awaitActiveConnection() {
    return awaitActiveConnections(Operator.AT_LEAST, 1).get(0);
  }

  /**
   * Wait for the specified number of connections to be in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitConnections(nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the number of connections to wait for.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances with the desired status.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitWorkingConnections(final Operator operator, final int nbConnections) {
    return awaitConnections(operator, nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait for a connection to be in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitWorkingConnections(Operator.AT_LEAST, 1).get(0)}.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances with the desired status.
   * @since 5.0
   */
  public JPPFClientConnection awaitWorkingConnection() {
    return awaitWorkingConnections(Operator.AT_LEAST, 1).get(0);
  }

  /**
   * Wait for the specified number of connections to be in one of the specified states.
   * This is a shorthand for {@code awaitConnections(nbConnections, Long.MAX_VALUE, JPPFstatuses)}.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the number of connections to wait for.
   * @param statuses the possible statuses of the connections to wait for.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitConnections(final Operator operator, final int nbConnections, final JPPFClientConnectionStatus...statuses) {
    return awaitConnections(operator, nbConnections, Long.MAX_VALUE, statuses);
  }

  /**
   * Wait for a connection to be in one of the specified states.
   * This is a shorthand for {@code awaitConnections(Operator.AT_LEAST, 1, Long.MAX_VALUE, statuses).get(0)}.
   * @param statuses the possible statuses of the connections to wait for.
   * @return a {@link JPPFClientConnection} instance in one of the specified statuses.
   * @since 5.1
   */
  public JPPFClientConnection awaitConnection(final JPPFClientConnectionStatus...statuses) {
    return awaitConnections(Operator.AT_LEAST, 1, Long.MAX_VALUE, statuses).get(0);
  }

  /**
   * Wait for the specified number of JMX connections to be in the specified state.
   * This is a shorthand for {@code awaitJMXConnections(nbConnections, Long.MAX_VALUE, connectedOnly)}.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the number of connections to wait for.
   * @param connectedOnly specifies whether to get a connection in connected state only or in any state.
   * @return a list of at least {@code nbConnections} {@link JMXDriverConnectionWrapper} instances.
   * @since 5.0
   */
  public List<JMXDriverConnectionWrapper> awaitJMXConnections(final Operator operator, final int nbConnections, final boolean connectedOnly) {
    return jmxPool.awaitJMXConnections(operator, nbConnections, Long.MAX_VALUE, connectedOnly);
  }

  /**
   * Wait for the specified number of JMX connections to be in the specified state, or the specified timeout to expire, whichever happens first.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds.
   * @param connectedOnly specifies whether to get a connection in connected state only or in any state.
   * @return a list of {@link JMXDriverConnectionWrapper} instances, possibly less than the requested number if the timeout expired first.
   * @since 5.0
   */
  public List<JMXDriverConnectionWrapper> awaitJMXConnections(final Operator operator, final int nbConnections, final long timeout, final boolean connectedOnly) {
    return jmxPool.awaitJMXConnections(operator, nbConnections, timeout, connectedOnly);
  }

  /**
   * Wait a JMX connection to be in the specified state. This is a shorthand for {@code awaitJMXConnections(Operator.AT_LEAST, 1, connectedOnly).get(0)}.
   * @param connectedOnly specifies whether to get a connection in connected state only or in any state.
   * @return a {@link JMXDriverConnectionWrapper} instance in the specified connected state.
   * @since 5.1
   */
  public JMXDriverConnectionWrapper awaitJMXConnection(final boolean connectedOnly) {
    return awaitJMXConnections(Operator.AT_LEAST, 1, connectedOnly).get(0);
  }

  /**
   * Wait an established JMX connection to be available. This is a shorthand for {@code awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0)}.
   * @return a connected {@link JMXDriverConnectionWrapper} instance.
   * @since 5.1
   */
  public JMXDriverConnectionWrapper awaitWorkingJMXConnection() {
    return awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
  }
}
