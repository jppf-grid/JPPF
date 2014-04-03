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
import org.slf4j.*;

/**
 * Instances of this class manage a list of client connections with the same pool name, priority and remote driver.
 * @since 4.1
 */
public class JPPFConnectionPool implements Comparable<JPPFConnectionPool>, Iterable<JPPFClientConnection> {
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
   * Index of the last used connection in this pool.
   */
  private int lastUsedIndex = 0;
  /**
   * List of <code>JPPFClientConnection</code> instances with the same priority.
   */
  private final List<JPPFClientConnection> connections = new ArrayList<>();
  /**
   * The id of this pool.
   */
  private final int id;
  /**
   * The name of this pool.
   */
  private final String name;
  /**
   * The core size of this pool.
   */
  private final int coreSize;
  /**
   * The core size of this pool.
   */
  private int maxSize;
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
   * Initialize this pool with the specified parameters.
   * @param client the JPPF client which holds this pool.
   * @param id the id of this pool.
   * @param name name assigned to this pool.
   * @param priority the priority of the connectios in this pool.
   * @param coreSize the core size of this pool.
   * @param sslEnabled determines whether the pool is for SSL connections.
   * @exclude
   */
  JPPFConnectionPool(final AbstractGenericClient client, final int id, final String name, final int priority, final int coreSize, final boolean sslEnabled) {
    this.client = client;
    this.id = id;
    this.priority = priority;
    this.name = name;
    this.coreSize = coreSize;
    this.maxSize = coreSize;
    this.sslEnabled = sslEnabled;
  }

  /**
   * Get the next client connection.
   * @return a <code>JPPFClientConnection</code> instances.
   * @exclude
   */
  synchronized JPPFClientConnection nextConnection() {
    if (connections.isEmpty()) return null;
    lastUsedIndex = ++lastUsedIndex % connections.size();
    return connections.get(getLastUsedIndex());
  }

  /**
   * Determine whether this pool is empty.
   * @return <code>true</code> if this pool is empty, <code>false</code> otherwise.
   */
  public synchronized boolean isEmpty() {
    return connections.isEmpty();
  }

  /**
   * Get the current size of this pool.
   * @return the size as an int.
   */
  public synchronized int connectionCount() {
    return connections.size();
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
   * Add a driver connection to this pool.
   * @param client the connection too add.
   * @return true if the underlying list of connections changed as a result of calling this method.
   * @exclude
   */
  synchronized boolean add(final JPPFClientConnection client) {
    return connections.add(client);
  }

  /**
   * Remove a driver connection from this pool.
   * @param client the connection too remove.
   * @return true if the underlying list of connections changed as a result of calling this method.
   * @exclude
   */
  synchronized boolean remove(final JPPFClientConnection client) {
    if (connections.remove(client)) {
      if (lastUsedIndex >= connections.size() && lastUsedIndex > 0) lastUsedIndex--;
      return true;
    }
    return false;
  }

  /**
   * Get the index of the last used connection in this pool.
   * @return the last used index as an int.
   * @exclude
   */
  private int getLastUsedIndex() {
    return lastUsedIndex;
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
    return !connections.isEmpty() ? connections.get(0).getDriverUuid() : null;
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

  /**
   * Get the core size of this connection pool.
   * @return the core size as an int value.
   */
  public int getCoreSize() {
    return coreSize;
  }

  /**
   * Get the maximum size of this connection pool.
   * @return the max size as an int.
   */
  public synchronized int getMaxSize() {
    return maxSize;
  }

  /**
   * Set the maximum size of this connection pool, starting or stopping connections as needed.
   * <p>If {@code maxSize} if less than or equal to {@link #getCoreSize()}, then this method has no effect.
   * <p>If any connection to be stopped is currently executing a job, then it will not be stopped.
   * @param maxSize the max size as an int.
   * @return the new maximum pool size.
   */
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
        if (connectionDoesNotMatchStatus(c, EXECUTING)) {
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
      info.host = c.getHost();
      int[] ports = new int[] {c.getPort() };
      if (c.isSSLEnabled()) info.sslServerPorts = ports;
      else info.serverPorts = ports;
      info.uuid = c.getDriverUuid();
      for (int i=0; i<diff; i++) client.submitNewConnection(info, this, sslEnabled);
      this.maxSize += diff;
    }
    return this.maxSize;
  }

  /**
   * Get a list of connections held by this pool. The returned list is independent from this pool,
   * thus changing, adding or removing elements has not effect on the pool.
   * @return a list of {@link JPPFClientConnection} instances.
   */
  public synchronized List<JPPFClientConnection> getConnections() {
    return new ArrayList<>(connections);
  }

  /**
   * Get a list of connections held by this pool whose status is one of the specified statuses.
   * The returned list is independent from this pool, thus changing, adding or removing elements has not effect on the pool.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return a list of {@link JPPFClientConnection} instances.
   */
  public synchronized List<JPPFClientConnection> getConnections(final JPPFClientConnectionStatus...statuses) {
    List<JPPFClientConnection> list = new ArrayList<>(connections.size());
    for (JPPFClientConnection c: connections) {
      if (connectionMatchesStatus(c, statuses)) list.add(c);
    }
    return list;
  }

  @Override
  public synchronized Iterator<JPPFClientConnection> iterator() {
    return connections.iterator();
  }

  /**
   * Check whether the status of the specified connection is one of the specified statuses.
   * @param connection the connection to check.
   * @param statuses an array of {@link JPPFClientConnectionStatus} values to check against.
   * @return {@code true} if the connection status is one of the specified statuses, {@code false} otherwise.
   */
  private boolean connectionMatchesStatus(final JPPFClientConnection connection, final JPPFClientConnectionStatus...statuses) {
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
  private boolean connectionDoesNotMatchStatus(final JPPFClientConnection connection, final JPPFClientConnectionStatus...statuses) {
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
    sb.append(",id =").append(id);
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
}
