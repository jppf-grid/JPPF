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

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.JPPFTask;
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
   * @exclude
   */
  public static String SERIALIZATION_HELPER_IMPL = "org.jppf.utils.SerializationHelperImpl";
  /**
   * Name of the SerializationHelper implementation class for the JCA connector.
   * @exclude
   */
  public static String JCA_SERIALIZATION_HELPER = "org.jppf.jca.serialization.JcaSerializationHelperImpl";
  /**
   * Total count of the tasks submitted by this client.
   * @exclude
   */
  protected int totalTaskCount = 0;
  /**
   * A sequence number used as an id for connection pools.
   */
  protected final AtomicInteger poolSequence = new AtomicInteger(0);
  /**
   * Contains all the connections pools in ascending priority order.
   */
  protected final CollectionMap<Integer, JPPFConnectionPool> pools = new LinkedListSortedMap<>(new DescendingIntegerComparator());
  /**
   * Keeps inactive connection ppols, that is the pools who do not yet have an active connection.
   */
  protected final Set<JPPFConnectionPool> pendingPools = new HashSet<>();
  /**
   * Unique universal identifier for this JPPF client.
   */
  protected String uuid = null;
  /**
   * A list of all the connections not yet active.
   */
  final List<JPPFClientConnection> pendingConnections = new CopyOnWriteArrayList<>();
  /**
   * A list of all the connections initially created.
   */
  private final List<JPPFClientConnection> allConnections = new CopyOnWriteArrayList<>();
  /**
   * List of listeners to this JPPF client.
   */
  private final List<ClientListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Determines whether this JPPF client is closed.
   * @exclude
   */
  protected final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * Determines whether this JPPF client is resetting.
   * @exclude
   */
  protected final AtomicBoolean resetting = new AtomicBoolean(false);
  /**
   * Fully qualified name of the serilaization helper class to use.
   * @exclude
   */
  protected String serializationHelperClassName = JPPFConfiguration.getProperties().getString("jppf.serialization.helper.class", SERIALIZATION_HELPER_IMPL);

  /**
   * Initialize this client with a specified application UUID.
   * @param uuid the unique identifier for this local client.
   */
  protected AbstractJPPFClient(final String uuid) {
    this.uuid = (uuid == null) ? new JPPFUuid().toString() : uuid;
    if (debugEnabled) log.debug("Instantiating JPPF client with uuid=" + this.uuid);
    VersionUtils.logVersionInformation("client", this.uuid);
    SystemUtils.printPidAndUuid("client", this.uuid);
  }

  /**
   * Read all client connection information from the configuration and initialize
   * the connection pools accordingly.
   * @param config The JPPF configuration properties.
   * @exclude
   */
  protected abstract void initPools(final TypedProperties config);

  /**
   * Get all the client connections handled by this JPPFClient.
   * @return a list of <code>JPPFClientConnection</code> instances.
   */
  public List<JPPFClientConnection> getAllConnections() {
    return Collections.unmodifiableList(allConnections);
  }

  /**
   * Get count of all client connections handled by this JPPFClient.
   * @return count of <code>JPPFClientConnection</code> instances.
   */
  public int getAllConnectionsCount()
  {
    return allConnections.size();
  }

  /**
   * Get the names of all the client connections handled by this JPPFClient.
   * @return a list of connection names as strings.
   */
  public List<String> getAllConnectionNames() {
    List<String> names = new LinkedList<>();
    for (JPPFClientConnection c : allConnections) names.add(c.getName());
    return names;
  }

  /**
   * Get a connection given its name.
   * @param name the name of the connection to find.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection(final String name) {
    for (JPPFClientConnection c : allConnections) {
      if (c.getName().equals(name)) return c;
    }
    return null;
  }

  /**
   * Get an available connection with the highest possible priority.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection() {
    return getClientConnection(true, true);
  }

  /**
   * Get an available connection with the highest possible priority.
   * @param oneAttempt determines whether this method should wait until a connection
   * becomes available (ACTIVE status) or fail immediately if no available connection is found.<br>
   * This enables the execution to be performed locally if the client is not connected to a server.
   * @param anyState specifies whether this method should look for an active connection or not care about the connection state.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection(final boolean oneAttempt, final boolean anyState) {
    synchronized(pools) {
      for (Integer priority: pools.keySet()) {
        JPPFClientConnection connection = getClientConnection(priority, oneAttempt, anyState);
        if (oneAttempt || (connection != null)) return connection;
      }
    }
    return null;
  }

  /**
   * Get an available connection for the specified priority.
   * @param priority the priority of the connection to find.
   * @param oneAttempt determines whether this method should wait until a connection
   * becomes available (ACTIVE status) or fail immediately if no available connection is found.<br>
   * This enables the execution to be performed locally if the client is not connected to a server.
   * @param anyState specifies whether this method should look for an active connection or not care about the connection state.
   * @return a <code>JPPFClientConnection</code> with the highest possible priority.
   */
  public JPPFClientConnection getClientConnection(final int priority, final boolean oneAttempt, final boolean anyState) {
    synchronized(pools) {
      JPPFClientConnection connection = null;
      Collection<JPPFConnectionPool> priorityPools = pools.getValues(priority);
      if (priorityPools == null) return null;
      for (JPPFConnectionPool pool: priorityPools) {
        int count = 0;
        while ((connection == null) && (count < pool.connectionCount())) {
          JPPFClientConnection c = pool.nextConnection();
          if (c == null) break;
          switch (c.getStatus()) {
            case ACTIVE:
              connection = c;
              break;
            case FAILED:
              break;
            default:
              if (anyState) connection = c;
              break;
          }
          count++;
        }
        if (connection != null) return connection;
      }
      if (oneAttempt) return connection;
    }
    return null;
  }

  /**
   * Submit a JPPFJob for execution.
   * @param job the job to execute.
   * @return the results of the tasks' execution, as a list of <code>JPPFTask</code> instances for a blocking job, or null if the job is non-blocking.
   * @throws Exception if an error occurs while sending the job for execution.
   * @deprecated use {@link #submitJob(JPPFJob)} instead.
   */
  @Deprecated
  public abstract List<JPPFTask> submit(JPPFJob job) throws Exception;

  /**
   * Submit a JPPFJob for execution.
   * @param job the job to execute.
   * @return the results of the tasks' execution, as a list of <code>JPPFTask</code> instances for a blocking job, or null if the job is non-blocking.
   * @throws Exception if an error occurs while sending the job for execution.
   * @since 4.0
   */
  public abstract List<Task<?>> submitJob(JPPFJob job) throws Exception;

  /**
   * Invoked when the status of a client connection has changed.
   * @param event the event to notify of.
   * @exclude
   */
  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    JPPFClientConnection c = (JPPFClientConnection) event.getClientConnectionStatusHandler();
    if (c.getStatus() == JPPFClientConnectionStatus.FAILED) connectionFailed(c);
  }

  /**
   * Invoked when the status of a connection has changed to <code>JPPFClientConnectionStatus.FAILED</code>.
   * @param c the connection that failed.
   * @exclude
   */
  protected abstract void connectionFailed(final JPPFClientConnection c);

  /**
   * Add a new connection to the set of connections handled by this client.
   * @param connection the connection to add.
   * @exclude
   */
  void addClientConnection(final JPPFClientConnection connection) {
    if (connection == null) throw new IllegalArgumentException("connection is null");
    if (debugEnabled) log.debug("adding connection {}", connection);
    int priority = connection.getPriority();
    JPPFConnectionPool pool = connection.getConnectionPool();
    synchronized (pools) {
      //pool.add(connection);
      if (pendingPools.remove(pool)) pools.putValue(priority, pool);
    }
    allConnections.add(connection);
    pendingConnections.remove(connection);
  }

  /**
   * Remove a connection from the set of connections handled by this client.
   * @param connection the connection to remove.
   * @exclude
   */
  void removeClientConnection(final JPPFClientConnection connection) {
    if (connection == null) throw new IllegalArgumentException("connection is null");
    if (debugEnabled) log.debug("removing connection {}", connection);
    connection.removeClientConnectionStatusListener(this);
    int priority = connection.getPriority();
    JPPFConnectionPool pool = connection.getConnectionPool();
    synchronized (pools) {
      if (pool != null) {
        pool.remove(connection);
        if (pool.isEmpty()) pools.removeValue(priority, pool);
      }
    }
    allConnections.remove(connection);
  }

  /**
   * Close this client and release all the resources it is using.
   */
  public void close() {
    Set<JPPFClientConnection> connectionSet = new HashSet<>(getAllConnections());
    connectionSet.addAll(pendingConnections);
    if (debugEnabled) log.debug("closing all connections: " + connectionSet);
    for (JPPFClientConnection c : connectionSet) {
      try {
        c.close();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Add a listener to the list of listeners to this client.
   * @param listener the listener to add.
   */
  public void addClientListener(final ClientListener listener) {
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners to this client.
   * @param listener the listener to remove.
   */
  public void removeClientListener(final ClientListener listener) {
    listeners.remove(listener);
  }

  /**
   * Notify all listeners to this client that a connection failed.
   * @param c the connection that triggered the event.
   * @exclude
   */
  protected void fireConnectionFailed(final JPPFClientConnection c) {
    ClientEvent event = new ClientEvent(c);
    for (ClientListener listener : listeners) listener.connectionFailed(event);
  }

  /**
   * Notify all listeners to this client that a new connection was added.
   * @param c the connection that was added.
   * @exclude
   */
  protected void fireNewConnection(final JPPFClientConnection c) {
    ClientEvent event = new ClientEvent(c);
    for (ClientListener listener : listeners) listener.newConnection(event);
  }

  /**
   * Notify all listeners that a new connection was created.
   * @param c the connection that was created.
   * @exclude
   */
  public void newConnection(final JPPFClientConnection c) {
    fireNewConnection(c);
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
   * @return <code>true</code> if this client is closed, <code>false</code> otherwise.
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Get the name of the serialization helper implementation class name to use.
   * @return the fully qualified class name of a <code>SerializationHelper</code> implementation.
   * @exclude
   */
  protected String getSerializationHelperClassName() {
    return serializationHelperClassName;
  }

  /**
   * Find the connection pool with the specified priority and id.
   * @param priority the priority of the pool, helps speedup the search.
   * @param poolId the id of the pool to find.
   * @return a {@link JPPFConnectionPool} instance, or {@code null} if no pool witht he specified id could be found.
   * @since 4.1
   */
  public JPPFConnectionPool findConnectionPool(final int priority, final int poolId) {
    JPPFConnectionPool pool = null;
    synchronized (pools) {
      Collection<JPPFConnectionPool> priorityPools = pools.getValues(priority);
      if (priorityPools != null) {
        for (JPPFConnectionPool p: priorityPools) {
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
   * Find the connection pool with the specified name.
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
   * Get a set of existing connection pools with the specified priority.
   * @param priority the priority of the pool, helps speedup the search.
   * @return a list of {@link JPPFConnectionPool} instances, possibly empty but never {@code null}.
   * @since 4.1
   */
  public List<JPPFConnectionPool> getConnectionPools(final int priority) {
    Collection<JPPFConnectionPool> coll;
    synchronized(pools) {
      if ((coll = pools.getValues(priority)) != null) {
        List<JPPFConnectionPool> list = new ArrayList<>(coll.size());
        list.addAll(coll);
        return list;
      }
    }
    return Collections.<JPPFConnectionPool>emptyList();
  }

  /**
   * Get a list of all priorities for the currently existing pools, ordered by descending priority.
   * @return a list of integers represent the priorities.
   * @since 4.1
   */
  public List<Integer> getPoolPriorities() {
    synchronized(pools) {
      return new ArrayList<>(pools.keySet());
    }
  }

  /**
   * Get a list of existing connection pools, ordered by descending priority.
   * @return a list of {@link JPPFConnectionPool} instances.
   * @since 4.1
   */
  public List<JPPFConnectionPool> getConnectionPools() {
    synchronized(pools) {
      return new ArrayList<>(pools.allValues());
    }
  }

  /**
   * This comparator defines a descending value order for integers.
   * @exclude
   */
  public static class DescendingIntegerComparator implements Comparator<Integer> {
    /**
     * Compare two integers. This comparator defines a descending order for integers.
     * @param o1 first integer to compare.
     * @param o2 second integer to compare.
     * @return -1 if o1 > o2, 0 if o1 == o2, 1 if o1 < o2
     */
    @Override
    public int compare(final Integer o1, final Integer o2) {
      if (o1 < o2) return 1;
      if (o1 > o2) return -1;
      return 0;
    }
  }
}
