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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.EXECUTING;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.management.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class manage a list of client connections with the same pool name, priority and remote driver.
 * <p>This connection pool also holds a pool of JMX connections to the same remote driver
 * @since 4.1
 */
public class JPPFConnectionPool extends AbstractConnectionPool<JPPFClientConnection> implements Comparable<JPPFConnectionPool> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFConnectionPool.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The priority associated with this pool.
   */
  private final int priority;
  /**
   * The id of this pool.
   */
  private final int id;
  /**
   * The name of this pool.
   */
  private final String name;
  /**
   * The JPPF client which holds this pool.
   */
  private final AbstractGenericClient client;
  /**
   * Determines whether the pool is for SSL connections.
   */
  private final boolean sslEnabled;
  /**
   * Sequence number for created connections
   */
  private final AtomicInteger sequence = new AtomicInteger(0);
  /**
   * The host name or address of the remote driver.
   * @since 4.2
   */
  private String driverHost;
  /**
   * The host name or address of the remote driver.
   * @since 4.2
   */
  private String driverIPAddress;
  /**
   * The port to use on the remote driver.
   * @since 4.2
   */
  private int driverPort = -1;
  /**
   * The uuid of the remote driver.
   * @since 4.2
   */
  private String driverUuid;
  /**
   * Represents the system information.
   * @since 4.2
   */
  private JPPFSystemInformation systemInfo;
  /**
   * The pool of JMX connections.
   * @since 4.2
   */
  private final JMXConnectionPool jmxPool;

  /**
   * Initialize this pool with the specified parameters.
   * @param client the JPPF client which holds this pool.
   * @param id the id of this pool.
   * @param name name assigned to this pool.
   * @param priority the priority of the connectios in this pool.
   * @param coreSize the core size of this pool.
   * @param sslEnabled determines whether the pool is for SSL connections.
   * @param jmxCoreSize the core size of the JMX connections pool.
   * @exclude
   */
  JPPFConnectionPool(final AbstractGenericClient client, final int id, final String name, final int priority, final int coreSize, final boolean sslEnabled, final int jmxCoreSize) {
    super(coreSize);
    this.client = client;
    this.id = id;
    this.priority = priority;
    this.name = name;
    this.sslEnabled = sslEnabled;
    jmxPool = new JMXConnectionPool(this, jmxCoreSize);
  }

  @Override
  public synchronized JPPFClientConnection getConnection() {
    int count = 0;
    int size = connections.size();
    while (count++ < size) {
      JPPFClientConnection c = nextConnection();
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
  public synchronized int connectionCount(final JPPFClientConnectionStatus...statuses) {
    if ((statuses == null) || (statuses.length <= 0)) return connections.size();
    int count = 0;
    for (JPPFClientConnection c: connections) {
      if (connectionMatchesStatus(c, statuses)) count++;
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
  public synchronized int getPriority() {
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
   * @since 4.2
   * @exclude
   */
  public synchronized void setDriverUuid(final String uuid) {
    if (this.driverUuid == null) this.driverUuid = uuid;
  }

  /**
   * Compares this connection pool with another, based on their respective priorities.
   * <p>This comparison defines an ordering of connection pools by their <b><i>descending</i></b> priority.
   * @param other the other connection pool to compare with.
   * @return -1 if this pool's priority is greater than the other pool's, 0 if the priorities are equal,
   * +1 if this pool's priority is less than the other pool's.
   */
  @Override
  public int compareTo(final JPPFConnectionPool other) {
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
    return id == ((JPPFConnectionPool) obj).id;
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
        JPPFClientConnection c = connections.get(i);
        if (!coreConnections.contains(c) && connectionDoesNotMatchStatus(c, EXECUTING)) {
          if (debugEnabled) log.debug("removing connection {} from pool {}", c, this);
          c.close();
          remove(c);
          actual++;
        }
      }
      this.maxSize -= actual;
    } else {
      JPPFClientConnection c = connections.get(0);
      JPPFConnectionInformation info = new JPPFConnectionInformation();
      info.host = driverHost;
      int[] ports = new int[] {driverPort };
      if (sslEnabled) info.sslServerPorts = ports;
      else info.serverPorts = ports;
      info.uuid = driverUuid;
      for (int i=0; i<diff; i++) client.submitNewConnection(info, this);
      this.maxSize += diff;
    }
    return this.maxSize;
  }

  /**
   * Get a list of connections held by this pool whose status is one of the specified statuses.
   * The returned list is independent from this pool, thus changing, adding or removing elements has not effect on the pool.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return a list of {@link JPPFClientConnection} instances, possibly empty but never {@code null}.
   */
  public synchronized List<JPPFClientConnection> getConnections(final JPPFClientConnectionStatus...statuses) {
    List<JPPFClientConnection> list = new ArrayList<>(connections.size());
    for (JPPFClientConnection c: connections) {
      if (connectionMatchesStatus(c, statuses)) list.add(c);
    }
    return list;
  }

  /**
   * Check whether the status of the specified connection is one of the specified statuses.
   * @param connection the connection to check.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return {@code true} if the connection status is one of the specified statuses, {@code false} otherwise.
   */
  boolean connectionMatchesStatus(final JPPFClientConnection connection, final JPPFClientConnectionStatus...statuses) {
    if ((connection == null) || (statuses == null)) return false;
    JPPFClientConnectionStatus status = connection.getStatus();
    for (JPPFClientConnectionStatus s: statuses) {
      if (status == s) return true;
    }
    return false;
  }

  /**
   * Check whether the status of the specified connection is none of the specified statuses.
   * @param connection the connection to check.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return {@code true} if the connection status is not one of the specified statuses, {@code false} otherwise.
   */
  boolean connectionDoesNotMatchStatus(final JPPFClientConnection connection, final JPPFClientConnectionStatus...statuses) {
    if ((connection == null) || (statuses == null)) return true;
    JPPFClientConnectionStatus status = connection.getStatus();
    for (JPPFClientConnectionStatus s: statuses) {
      if (status == s) return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("name=").append(name);
    sb.append(", id=").append(id);
    sb.append(", coreSize=").append(coreSize);
    sb.append(", maxSize=").append(maxSize);
    sb.append(", priority=").append(priority);
    sb.append(", sslEnabled=").append(sslEnabled);
    sb.append(", client=").append(client);
    sb.append(']');
    return sb.toString();
  }

  /**
   * Return the next sequence number.
   * @return the next sequence number.
   */
  int nextSequence() {
    return sequence.incrementAndGet();
  }

  /**
   * Get the host name of the remote driver.
   * @return a string representing the host name or ip address.
   * @since 4.2
   */
  public synchronized String getDriverHost() {
    return driverHost;
  }

  /**
   * Set the host name of the remote driver.
   * @param driverHost a string representing the host name or ip address.
   * @since 4.2
   */
  synchronized void setDriverHost(final String driverHost) {
    if ((this.driverHost == null) && (driverHost != null)) {
      this.driverHost = driverHost;
      jmxPool.hostSet();
    }
  }

  /**
   * Get the ip address of the remote driver.
   * @return a string representing the host name or ip address.
   * @since 4.2
   */
  public synchronized String getDriverIPAddress() {
    return driverIPAddress;
  }

  /**
   * Set the ip address of the remote driver.
   * @param ipAddress a string representing the host name or ip address.
   * @since 4.2
   */
  synchronized void setDriverIPAddress(final String ipAddress) {
    if ((this.driverIPAddress == null) && (ipAddress != null)) {
      this.driverIPAddress = ipAddress;
      jmxPool.hostSet();
    }
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
   * @since 4.2
   */
  synchronized void setDriverPort(final int driverPort) {
    if (this.driverPort < 0) this.driverPort = driverPort;
  }

  /**
   * Get the JPPF client which holds this pool.
   * @return a {@link AbstractGenericClient} instance.
   * @since 4.2
   */
  public AbstractGenericClient getClient() {
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
   * @since 4.2
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
   * @since 4.2
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
   * Get the core size of the associated JMX connection pool.
   * @return the JMX pool core size.
   * @since 4.2
   */
  public int getJMXPoolCoreSize() {
    return jmxPool.getCoreSize();
  }

  /**
   * Get the current maximum size of the associated JMX connection pool.
   * @return the JMX pool maximum size.
   * @since 4.2
   */
  public int getJMXPoolMaxSize() {
    return maxSize;
  }

  /**
   * Set a new maximum size for the associated pool of JMX connections, adding new or closing existing connections as needed.
   * @param maxSize the new maxsize to set.
   * @return the actual new maximum size.
   * @since 4.2
   */
  public int setJMXPoolMaxSize(final int maxSize) {
    return jmxPool.setMaxSize(maxSize);
  }

  /**
   * Get the list of connections currently in the JMX pool.
   * @return a list of {@link JMXDriverConnectionWrapper} instances.
   * @since 4.2
   */
  public synchronized List<JMXDriverConnectionWrapper> getJMXConnections() {
    return jmxPool.getConnections();
  }

  @Override
  public synchronized void close() {
    super.close();
    jmxPool.close();
  }

  /**
   * Wait for the specified number of connections to be in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} status.
   * This is a shorthand for {@code awaitConnections(nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE)}.
   * This method will create or close connections as needed to reach the desired number of connections.
   * @param nbConnections the number of connections to wait for.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances with the desired status.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitActiveConnections(final int nbConnections) {
    return awaitConnections(nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE);
  }

  /**
   * Wait for the specified number of connections to be in the {@link JPPFClientConnectionStatus#ACTIVE ACTIVE} or {@link JPPFClientConnectionStatus#EXECUTING EXECUTING} status.
   * This is a shorthand for {@code awaitConnections(nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING)}.
   * This method will create or close connections as needed to reach the desired number of connections.
   * @param nbConnections the number of connections to wait for.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances with the desired status.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitWorkingConnections(final int nbConnections) {
    return awaitConnections(nbConnections, Long.MAX_VALUE, JPPFClientConnectionStatus.ACTIVE, JPPFClientConnectionStatus.EXECUTING);
  }

  /**
   * Wait for the specified number of connections to be in one of the specified states.
   * This is a shorthand for {@code awaitConnections(nbConnections, Long.MAX_VALUE, JPPFstatuses)}.
   * This method will create or close connections as needed to reach the desired number of connections.
   * @param nbConnections the number of connections to wait for.
   * @param statuses the possible statuses of the connections to wait for.
   * @return a list of {@code nbConnections} {@link JPPFClientConnection} instances.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitConnections(final int nbConnections, final JPPFClientConnectionStatus...statuses) {
    return awaitConnections(nbConnections, Long.MAX_VALUE, statuses);
  }

  /**
   * Wait for the specified number of connections to be in one of the specified states, or the specified timeout to expire, whichever happens first.
   * This method will increase or decrease the number of connections in this pool as needed.
   * This method will create or close connections as needed to reach the desired number of connections.
   * @param nbConnections the number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds.
   * @param statuses the possible statuses of the connections to wait for.
   * @return a list of {@link JPPFClientConnection} instances, possibly less than the requested number if the timeout expired first.
   * @since 5.0
   */
  public List<JPPFClientConnection> awaitConnections(final int nbConnections, final long timeout, final JPPFClientConnectionStatus...statuses) {
    setMaxSize(nbConnections);
    final MutableReference<List<JPPFClientConnection>> ref = new MutableReference<>();
    ConcurrentUtils.awaitCondition(new ConcurrentUtils.Condition() {
      @Override public boolean evaluate() {
        return ref.set(getConnections(statuses)).size() == nbConnections;
      }
    }, timeout);
    return ref.get();
  }

  /**
   * Wait for the specified number of JMX connections to be in the specified state.
   * This is a shorthand for {@code awaitJMXConnections(nbConnections, Long.MAX_VALUE, connectedOnly)}.
   * This method will create or close JMX connections as needed to reach the desired number of connections.
   * @param nbConnections the number of connections to wait for.
   * @param connectedOnly .
   * @return a list of at least {@code nbConnections} {@link JPPFClientConnection} instances.
   * @since 5.0
   */
  public List<JMXDriverConnectionWrapper> awaitJMXConnections(final int nbConnections, final boolean connectedOnly) {
    return jmxPool.awaitJMXConnections(nbConnections, Long.MAX_VALUE, connectedOnly);
  }

  /**
   * Wait for the specified number of JMX connections to be in the specified state, or the specified timeout to expire, whichever happens first.
   * This method will create or close JMX connections as needed to reach the desired number of connections.
   * @param nbConnections the number of connections to wait for.
   * @param timeout the maximum time to wait, in milliseconds.
   * @param connectedOnly the possible statuses of the connections to wait for.
   * @return a list of {@link JPPFClientConnection} instances, possibly less than the requested number if the timeout expired first.
   * @since 5.0
   */
  public List<JMXDriverConnectionWrapper> awaitJMXConnections(final int nbConnections, final long timeout, final boolean connectedOnly) {
    return jmxPool.awaitJMXConnections(nbConnections, timeout, connectedOnly);
  }
}
