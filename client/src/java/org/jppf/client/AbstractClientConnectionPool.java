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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.discovery.ClientConnectionPoolInfo;
import org.jppf.management.*;
import org.jppf.utils.*;
import org.jppf.utils.Operator;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Instances of this class manage a list of client connections with the same pool name, priority and remote driver.
 * <p>This connection pool also holds a pool of JMX connections to the same remote driver
 * @since 4.1
 */
public abstract class AbstractClientConnectionPool extends AbstractConnectionPool<JPPFClientConnection> implements Comparable<AbstractClientConnectionPool> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractClientConnectionPool.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The priority associated with this pool.
   */
  final int priority;
  /**
   * The id of this pool.
   */
  final int id;
  /**
   * The name of this pool.
   */
  final String name;
  /**
   * The JPPF client which holds this pool.
   */
  final JPPFClient client;
  /**
   * Determines whether the pool is for SSL connections.
   */
  final boolean sslEnabled;
  /**
   * Sequence number for created connections.
   */
  final AtomicInteger sequence = new AtomicInteger(0);
  /**
   * The port to use on the remote driver.
   */
  int driverPort = -1;
  /**
   * The uuid of the remote driver.
   */
  String driverUuid;
  /**
   * Represents the system information.
   */
  JPPFSystemInformation systemInfo;
  /**
   * The pool of JMX connections.
   */
  final JMXConnectionPool jmxPool;
  /**
   * The host and IP address of the driver.
   */
  HostIP hostIP;
  /**
   * Inofrmation on this pool as discovered.
   */
  ClientConnectionPoolInfo discoveryInfo;

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
  AbstractClientConnectionPool(final JPPFClient client, final int id, final String name, final int priority, final int size, final boolean sslEnabled, final int jmxPoolSize) {
    super(size);
    this.client = client;
    this.id = id;
    this.priority = priority;
    this.name = name;
    this.sslEnabled = sslEnabled;
    jmxPool = new JMXConnectionPool(jmxPoolSize, sslEnabled);
  }

  /**
   * Initialize this pool with the specified parameters.
   * @param client the JPPF client which holds this pool.
   * @param id the id of this pool.
   * @param info information needed for the pool's attributes.
   */
  AbstractClientConnectionPool(final JPPFClient client, final int id, final ClientConnectionPoolInfo info) {
    this(client, id, info.getName(), info.getPriority(), info.getPoolSize(), info.isSecure(), info.getJmxPoolSize());
    this.discoveryInfo = info;
  }

  @Override
  public synchronized JPPFClientConnection getConnection() {
    int count = 0;
    final int size = connections.size();
    while (count++ < size) {
      final JPPFClientConnection c = nextConnection();
      if (c.getStatus() == JPPFClientConnectionStatus.ACTIVE) return c;
    }
    return null;
  }

  /**
   * Get the number of connections in this pool that have one of the psecified statuses.
   * <p>Warning: the execution time for this method is in O(n).
   * @param statuses the set of connection statuses to look for.
   * @return the size as an int.
   */
  public int connectionCount(final JPPFClientConnectionStatus...statuses) {
    synchronized(this) {
      if ((statuses == null) || (statuses.length <= 0)) return connections.size();
    }
    int count = 0;
    for (JPPFClientConnection c: getConnections()) {
      if (connectionHasStatus(c, true, statuses)) count++;
    }
    return count;
  }

  /**
   * Get the id of this pool.
   * @return the id as an int value.
   */
  public int getId() {
    return id;
  }

  /**
   * Get the priority associated with this pool.
   * @return the priority as an int.
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Check whether this pool is for SSL connections.
   * @return {@code true} if SSL is enabled, false otherwise.
   */
  public boolean isSslEnabled() {
    return sslEnabled;
  }

  /**
   * Get the name of this pool.
   * @return this connection pool's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the uuid of the driver to which connections in this pool are connected.
   * @return the driver uuid as a string.
   */
  public synchronized String getDriverUuid() {
    return driverUuid;
  }

  /**
   * Set the uuid of the driver to which connections in this pool are connected.
   * @param uuid the driver uuid as a string.
   */
  synchronized void setDriverUuid(final String uuid) {
    if (this.driverUuid == null) this.driverUuid = uuid;
  }

  /**
   * Compares this connection pool with another, based on their respective priorities.
   * <p>This comparison defines an ordering of connection pools by their <b><i>descending</i></b> priority.
   * @param other the other connection pool to compare with.
   * @return -1 if this pool's priority is greater than the other pool's, 0 if the priorities are equal, +1 if this pool's priority is less than the other pool's.
   */
  @Override
  public int compareTo(final AbstractClientConnectionPool other) {
    if (other == null) return 1;
    return priority > other.priority ? -1 : (priority < other.priority ? 1 : 0);
  }

  @Override
  public int hashCode() {
    return 31 + id;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    return id == ((AbstractClientConnectionPool) obj).id;
  }

  /**
   * Get a list of connections held by this pool whose status is one of the specified statuses.
   * The returned list is independent from this pool, thus changing, adding or removing elements has not effect on the pool.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return a list of {@link JPPFClientConnection} instances, possibly empty but never {@code null}.
   */
  public List<JPPFClientConnection> getConnections(final JPPFClientConnectionStatus...statuses) {
    final List<JPPFClientConnection> list = new ArrayList<>(getSize());
    for (final JPPFClientConnection c: getConnections()) {
      if (connectionHasStatus(c, true, statuses)) list.add(c);
    }
    if (log.isTraceEnabled()) log.trace("statuses={}, got connections {}", Arrays.asList(statuses), list);
    return list;
  }

  /**
   * Check whether the status of the specified connection is or isn't one of the specified statuses.
   * @param connection the connection to check.
   * @param has whether to check if the connection has one of the statuses, or if it doesn't.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return {@code true} if the connection status is not one of the specified statuses, {@code false} otherwise.
   */
  boolean connectionHasStatus(final JPPFClientConnection connection, final boolean has, final JPPFClientConnectionStatus...statuses) {
    if (connection == null) return !has;
    if ((statuses == null) || (statuses.length <= 0)) return has;
    final JPPFClientConnectionStatus status = connection.getStatus();
    for (final JPPFClientConnectionStatus s: statuses) {
      if (status == s) return has;
    }
    return !has;
  }

  /**
   * Wait for the specified number of connections to be in one of the specified states, or the specified timeout to expire, whichever happens first.
   * This method will increase or decrease the number of connections in this pool as needed.
   * @param operator the condition on the number of connections to wait for. If {@code null}, it is assumed to be {@link Operator#EQUAL}.
   * @param nbConnections the number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds.
   * @param statuses the possible statuses of the connections to wait for.
   * @return a list of {@link JPPFClientConnection} instances, possibly less than the requested number if the timeout expired first.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitConnections(final ComparisonOperator operator, final int nbConnections, final long timeout, final JPPFClientConnectionStatus...statuses) {
    final ComparisonOperator op = operator == null ? Operator.EQUAL : operator;
    if (debugEnabled) log.debug(String.format("awaiting %d connections with operator=%s and status in %s", nbConnections, op, Arrays.asList(statuses)));
    final MutableReference<List<JPPFClientConnection>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        return op.evaluate(ref.set(getConnections(statuses)).size(), nbConnections);
      }
    }, timeout);
    if (debugEnabled) log.debug("got expected connections: " + ref.get());
    return ref.get();
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("name=").append(name)
      .append(", id=").append(id)
      .append(", size=").append(size)
      .append(", priority=").append(priority)
      .append(", driverHost=").append(hostIP != null ? hostIP.hostName() : null)
      .append(", driverPort=").append(driverPort)
      .append(", sslEnabled=").append(sslEnabled)
      .append(']')
      .toString();
  }

  /**
   * Return the next sequence number.
   * @return the next sequence number.
   */
  int nextSequence() {
    return sequence.incrementAndGet();
  }

  /**
   * Set the host and IP address of the driver.
   * @param hostIP a {@link HostIP} instance.
   */
  synchronized void setDriverHostIP(final HostIP hostIP) {
    this.hostIP = hostIP;
    jmxPool.setDriverHostIP(hostIP);
  }

  /**
   * Get the host name of the remote driver.
   * @return a string representing the host name or ip address.
   * @since 4.2
   */
  public synchronized String getDriverHost() {
    return hostIP != null ? hostIP.hostName() : null;
  }

  /**
   * Get the ip address of the remote driver.
   * @return a string representing the host name or ip address.
   * @since 4.2
   */
  public synchronized String getDriverIPAddress() {
    return hostIP != null ? hostIP.ipAddress() : null;
  }

  /**
   * Get the port to use on the remote driver.
   * @return the port number as an int.
   * @since 4.2
   */
  public synchronized int getDriverPort() {
    return driverPort;
  }

  /**
   * Set the port to use on the remote driver.
   * @param driverPort the port number as an int.
   */
  synchronized void setDriverPort(final int driverPort) {
    if (this.driverPort < 0) this.driverPort = driverPort;
  }

  /**
   * Get the JPPF client which holds this pool.
   * @return a {@link JPPFClient} instance.
   * @since 4.2
   */
  public JPPFClient getClient() {
    return client;
  }

  /**
   * Get the driver's system information.
   * @return an instance of {@link JPPFSystemInformation}.
   * @since 4.2
   */
  public synchronized JPPFSystemInformation getSystemInfo() {
    return systemInfo;
  }

  /**
   * Set the driver's system information.
   * @param systemInfo an instance of {@link JPPFSystemInformation}.
   */
  synchronized void setSystemInfo(final JPPFSystemInformation systemInfo) {
    this.systemInfo = systemInfo;
  }

  /**
   * Get the jmx port to use on the remote driver.
   * @return the jmx port number as an int.
   * @since 4.2
   */
  public int getJmxPort() {
    return jmxPool.getPort();
  }

  /**
   * Set the jmx port to use on the remote driver.
   * @param jmxPort the jmx port number as an int.
   */
  void setJmxPort(final int jmxPort) {
    jmxPool.setPort(jmxPort);
  }

  /**
   * Get a <i>connected</i> JMX connection among those in the JMX pool.
   * @return a {@link JMXDriverConnectionWrapper} instance, or {@code null} if the JMX pool has no connected connection.
   * @since 4.2
   */
  public JMXDriverConnectionWrapper getJmxConnection() {
    return jmxPool.getConnection();
  }

  /**
   * Get a JMX connection among those in the JMX pool.
   * @param connectedOnly specifies whether to get a connection in connected state only or in any state.
   * @return a {@link JMXDriverConnectionWrapper} instance, or {@code null} if the JMX pool has no connection in the specified state.
   * @since 4.2
   */
  public JMXDriverConnectionWrapper getJmxConnection(final boolean connectedOnly) {
    return connectedOnly ? jmxPool.getConnection() : jmxPool.nextConnection();
  }

  /**
   * Get the current maximum size of the associated JMX connection pool.
   * @return the JMX pool maximum size.
   * @since 4.2
   * @deprecated use {@link #getJMXPoolSize()} instead.
   */
  public int getJMXPoolMaxSize() {
    return jmxPool.getSize();
  }

  /**
   * Set a new maximum size for the associated pool of JMX connections, adding new or closing existing connections as needed.
   * @param maxSize the new maxsize to set.
   * @return the actual new maximum size.
   * @since 4.2
   * @deprecated use {@link #setJMXPoolSize(int)} instead.
   */
  public int setJMXPoolMaxSize(final int maxSize) {
    return jmxPool.setSize(maxSize);
  }

  /**
   * Get the current maximum size of the associated JMX connection pool.
   * @return the JMX pool maximum size.
   * @since 6.0
   */
  public int getJMXPoolSize() {
    return jmxPool.getSize();
  }

  /**
   * Set a new maximum size for the associated pool of JMX connections, adding new or closing existing connections as needed.
   * @param maxSize the new maxsize to set.
   * @return the actual new maximum size.
   * @since 6.0
   */
  public int setJMXPoolSize(final int maxSize) {
    return jmxPool.setSize(maxSize);
  }

  /**
   * Get the list of connections currently in the JMX pool.
   * @return a list of {@link JMXDriverConnectionWrapper} instances.
   * @since 4.2
   */
  public List<JMXDriverConnectionWrapper> getJMXConnections() {
    return jmxPool.getConnections();
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}", this);
    super.close();
    jmxPool.close();
  }

  /**
   * @return the discovery information for this pool.
   */
  ClientConnectionPoolInfo getDiscoveryInfo() {
    return discoveryInfo;
  }
}
