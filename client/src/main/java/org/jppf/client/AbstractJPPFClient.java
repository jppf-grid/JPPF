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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;
import java.util.regex.Pattern;

import org.jppf.client.event.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClient implements ClientConnectionStatusListener, AutoCloseable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClient.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Name of the default SerializationHelper implementation class.
   */
  static String SERIALIZATION_HELPER_IMPL = "org.jppf.utils.SerializationHelperImpl";
  /**
   * Name of the SerializationHelper implementation class for the JCA connector.
   * @exclude
   */
  protected static String JCA_SERIALIZATION_HELPER = "org.jppf.jca.serialization.JcaSerializationHelperImpl";
  /**
   * A sequence number used as an id for connection pools.
   */
  final AtomicInteger poolSequence = new AtomicInteger(0);
  /**
   * Contains all the connections pools in ascending priority order.
   */
  final CollectionMap<Integer, JPPFConnectionPool> pools = new LinkedListSortedMap<>(new DescendingIntegerComparator());
  /**
   * Unique universal identifier for this JPPF client.
   */
  private String uuid = null;
  /**
   * List of listeners to this JPPF client.
   */
  private final List<ConnectionPoolListener> connectionPoolListeners = new CopyOnWriteArrayList<>();
  /**
   * Determines whether this JPPF client is closed.
   */
  final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * Determines whether this JPPF client is resetting.
   */
  final AtomicBoolean resetting = new AtomicBoolean(false);
  /**
   * Fully qualified name of the serilaization helper class to use.
   */
  private String serializationHelperClassName;
  /**
   * The JPPF configuration properties.
   */
  protected TypedProperties config;

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   */
  protected AbstractJPPFClient(final String uuid) {
    this.uuid = (uuid == null) ? JPPFUuid.normalUUID() : uuid;
    if (debugEnabled) log.debug("Instantiating JPPF client with uuid=" + this.uuid);
    VersionUtils.logVersionInformation("client", this.uuid);
    SystemUtils.printPidAndUuid("client", this.uuid);
  }

  /**
   * Get JPPF configuration properties. These properties are unmodifiable.
   * @return {@code TypedProperties} instance. With JPPF configuration.
   */
  public TypedProperties getConfig() {
    return config;
  }

  /**
   * Read all client connection information from the configuration and initialize
   * the connection pools accordingly.
   * @param config the JPPF configuration properties.
   */
  abstract void initPools(final TypedProperties config);

  /**
   * Get count of all client connections handled by this JPPFClient.
   * @return count of {@link JPPFClientConnection} instances.
   */
  public int getAllConnectionsCount() {
    int count = 0;
    for (JPPFConnectionPool pool: pools) count += pool.connectionCount();
    return count;
  }

  /**
   * Get a connection with the specified priority that matches one of the specified statuses.
   * @param priority the priority of the connetion to find.
   * @param statuses a set of statuses, one of which must match the status of the connection to find.
   * @return a {@link JPPFClientConnection} that matches one of the specified statuses, or {@code null} if none could be found.
   * @since 4.2
   */
  public JPPFClientConnection getClientConnection(final int priority, final JPPFClientConnectionStatus...statuses) {
    synchronized(pools) {
      final Collection<JPPFConnectionPool> pls = pools.getValues(priority);
      if (pls == null) return null;
      for (final JPPFConnectionPool pool: pls) {
        final List<JPPFClientConnection> list = pool.getConnections(statuses);
        if (!list.isEmpty()) return list.get(0);
      }
    }
    return null;
  }

  /**
   * Invoked when the status of a client connection has changed.
   * @param event the event to notify of.
   * @exclude
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    final JPPFClientConnection c = event.getClientConnection();
    if (c.getStatus().isTerminatedStatus() && !event.getOldStatus().isTerminatedStatus()) connectionFailed(c);
  }

  /**
   * Invoked when the status of a connection has changed to {@link JPPFClientConnectionStatus#FAILED}.
   * @param c the connection that failed.
   */
  abstract void connectionFailed(final JPPFClientConnection c);

  /**
   * Remove a connection from the set of connections handled by this client.
   * @param connection the connection to remove.
   * @return {@code true} if the pool holding the connection became empty and was also removed, {@code false} otherwise.
   */
  boolean removeClientConnection(final JPPFClientConnection connection) {
    if (connection == null) throw new IllegalArgumentException("connection is null");
    if (debugEnabled) log.debug("removing connection {}", connection);
    connection.removeClientConnectionStatusListener(this);
    final JPPFConnectionPool pool = connection.getConnectionPool();
    boolean poolRemoved = false;
    if (pool != null) {
      pool.remove(connection);
      if (pool.isEmpty()) {
        synchronized (pools) {
          pools.removeValue(pool.getPriority(), pool);
        }
        poolRemoved = true;
      }
    }
    return poolRemoved;
  }

  /**
   * Close this client and release all the resources it is using.
   */
  @Override
  public void close() {
    for (JPPFConnectionPool pool: getConnectionPools()) pool.close();
    this.pools.clear();
  }

  /**
   * Add a listener to the list of listeners to this client.
   * @param listener the listener to add.
   */
  public void addConnectionPoolListener(final ConnectionPoolListener listener) {
    connectionPoolListeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners to this client.
   * @param listener the listener to remove.
   */
  public void removeConnectionPoolListener(final ConnectionPoolListener listener) {
    connectionPoolListeners.remove(listener);
  }

  /**
   * Notify all listeners to this client that a connection failed.
   * @param c the connection that triggered the event.
   */
  void fireConnectionRemoved(final JPPFClientConnection c) {
    final ConnectionPoolEvent event = new ConnectionPoolEvent(c.getConnectionPool(), c);
    for (final ConnectionPoolListener listener : connectionPoolListeners) listener.connectionRemoved(event);
  }

  /**
   * Notify all listeners to this client that a new connection was added.
   * @param c the connection that was added.
   */
  void fireConnectionAdded(final JPPFClientConnection c) {
    final ConnectionPoolEvent event = new ConnectionPoolEvent(c.getConnectionPool(), c);
    for (final ConnectionPoolListener listener : connectionPoolListeners) listener.connectionAdded(event);
  }

  /**
   * Notify all listeners to this client that a connection pool was removed.
   * @param pool the connection pool that triggered the event.
   */
  void fireConnectionPoolRemoved(final JPPFConnectionPool pool) {
    final ConnectionPoolEvent event = new ConnectionPoolEvent(pool);
    for (final ConnectionPoolListener listener : connectionPoolListeners) listener.connectionPoolRemoved(event);
  }

  /**
   * Notify all listeners to this client that a new connection pool was added.
   * @param pool the connection pool that was added.
   */
  void fireConnectionPoolAdded(final JPPFConnectionPool pool) {
    final ConnectionPoolEvent event = new ConnectionPoolEvent(pool);
    for (final ConnectionPoolListener listener : connectionPoolListeners) listener.connectionPoolAdded(event);
  }

  /**
   * Notify all listeners that a new connection was created.
   * @param c the connection that was created.
   * @exclude
   */
  void newConnection(final JPPFClientConnectionImpl c) {
    fireConnectionAdded(c);
  }

  /**
   * Get the unique universal identifier for this JPPF client.
   * @return the uuid as a string.
   */
  public String getUuid() {
    return uuid;
  }

  /**
   * Determine whether this JPPF client is closed.
   * @return {@code true} if this client is closed, {@code false} otherwise.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Get the name of the serialization helper implementation class name to use.
   * @return the fully qualified class name of a {@code SerializationHelper} implementation.
   * @exclude
   */
  protected String getSerializationHelperClassName() {
    if (serializationHelperClassName == null)
      serializationHelperClassName = getConfig().getString("jppf.serialization.helper.class", SERIALIZATION_HELPER_IMPL);
    return serializationHelperClassName;
  }

  /**
   * Find the connection pool with the specified priority and id.
   * @param priority the priority of the pool, helps speedup the search.
   * @param poolId the id of the pool to find.
   * @return a {@link JPPFConnectionPool} instance, or {@code null} if no pool witht he specified id could be found.
   * @since 4.1
   */
  public ConnectionPool<?> findConnectionPool(final int priority, final int poolId) {
    ConnectionPool<?> pool = null;
    synchronized (pools) {
      final Collection<JPPFConnectionPool> priorityPools = pools.getValues(priority);
      if (priorityPools != null) {
        for (final JPPFConnectionPool p: priorityPools) {
          if (p.getId() == poolId) {
            pool = p;
            break;
          }
        }
      }
    }
    return pool;
  }

  /**
   * Find the connection pool with the specified id.
   * @param poolId the id of the pool to find.
   * @return a {@link JPPFConnectionPool} instance, or {@code null} if no pool with the specified id could be found.
   * @since 4.1
   */
  public JPPFConnectionPool findConnectionPool(final int poolId) {
    synchronized (pools) {
      for (JPPFConnectionPool pool: pools) {
        if (pool.getId() == poolId) return pool;
      }
    }
    return null;
  }

  /**
   * Find the connection pool with the specified id.
   * @param name the name of the pool to find.
   * @return a {@link JPPFConnectionPool} instance, or {@code null} if no pool with the specified name could be found.
   * @since 4.1
   */
  public JPPFConnectionPool findConnectionPool(final String name) {
    synchronized (pools) {
      for (JPPFConnectionPool pool: pools) {
        if (pool.getName().equals(name)) return pool;
      }
    }
    return null;
  }

  /**
   * Find the connection pools that have a least one connection matching one of the specified statuses.
   * @param statuses a set of statuses of which at least one must be matched by at least one connection in any of the returned pools.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty if none were found matching the specified statuses.
   * The pools in the list are ordered by descending priority.
   * @since 4.2
   */
  public List<JPPFConnectionPool> findConnectionPools(final JPPFClientConnectionStatus...statuses) {
    final List<JPPFConnectionPool> list = new ArrayList<>();
    synchronized (pools) {
      for (final JPPFConnectionPool pool: pools) {
        if (!pool.getConnections(statuses).isEmpty()) list.add(pool);
      }
    }
    return list;
  }

  /**
   * Find the connection pools that pass the specified filter.
   * @param filter an implementation of the {@link ConnectionPoolFilter} interface. A {@code null} value is interpreted as no filter (all pools are accepted).
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty if none passed the specified filter.
   * The pools in the list are ordered by descending priority.
   * @since 4.2
   */
  public List<JPPFConnectionPool> findConnectionPools(final ConnectionPoolFilter<JPPFConnectionPool> filter) {
    final List<JPPFConnectionPool> list = new ArrayList<>();
    synchronized (pools) {
      for (final JPPFConnectionPool pool: pools) {
        if ((filter == null) || filter.accepts(pool)) list.add(pool);
      }
    }
    return list;
  }

  /**
   * Find the connection pools whose name matches the specified {@link Pattern regular expression}.
   * @param pattern the regular expression to match against.
   * @return a list of {@link JPPFConnectionPool} instances whose name match the input pattern, possibly empty but never {@code null}.
   * @since 4.2
   */
  public List<JPPFConnectionPool> findConnectionPools(final String pattern) {
    final Pattern p = Pattern.compile(pattern);
    final List<JPPFConnectionPool> result = new ArrayList<>();
    synchronized (pools) {
      for (final JPPFConnectionPool pool: pools) {
        if (p.matcher(pool.getName()).matches()) result.add(pool);
      }
    }
    return result;
  }

  /**
   * Get a pool with at least one {@link JPPFClientConnectionStatus#ACTIVE active} connection and with the highest possible priority.
   * @return a {@link JPPFConnectionPool} instance, or {@code null} if no pool was found with an active connection.
   * @since 4.2
   */
  public JPPFConnectionPool getConnectionPool() {
    final List<JPPFConnectionPool> list = findConnectionPools(JPPFClientConnectionStatus.ACTIVE);
    return list.isEmpty() ? null : list.get(0);
  }

  /**
   * Get a set of existing connection pools with the specified priority.
   * @param priority the priority of the pool, helps speedup the search.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never {@code null}.
   * @since 4.1
   */
  public List<JPPFConnectionPool> getConnectionPools(final int priority) {
    final Collection<JPPFConnectionPool> coll;
    synchronized(pools) {
      if ((coll = pools.getValues(priority)) != null) {
        final List<JPPFConnectionPool> list = new ArrayList<>(coll.size());
        list.addAll(coll);
        return list;
      }
    }
    return Collections.<JPPFConnectionPool>emptyList();
  }

  /**
   * Get a list of all priorities for the currently existing pools, ordered by descending priority.
   * @return a list of integers represent the priorities, possibly empty but never {@code null}.
   * @since 4.1
   */
  public List<Integer> getPoolPriorities() {
    synchronized(pools) {
      return new ArrayList<>(pools.keySet());
    }
  }

  /**
   * Get a list of existing connection pools, ordered by descending priority.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never {@code null}.
   * @since 4.1
   */
  public List<JPPFConnectionPool> getConnectionPools() {
    synchronized(pools) {
      return new ArrayList<>(pools.allValues());
    }
  }

  /**
   * Determine whether this client is resetting. 
   * @return {@code true} if this client is resetting, {@code false} otherwise.
   * @exclude
   */
  public boolean isResetting() {
    return resetting.get();
  }
}
