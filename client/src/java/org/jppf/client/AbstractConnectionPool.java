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

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * An abstract implementation of the {@link ConnectionPool} interface.
 * @param <E> the type of the connections in the pool.
 * @author Laurent Cohen
 * @since 4.2
 */
public abstract class AbstractConnectionPool<E extends AutoCloseable> implements ConnectionPool<E> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractConnectionPool.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The core size of this pool, that is, the minimum number of connections always present.
   */
  int coreSize;
  /**
   * The max size of this pool.
   */
  int maxSize;
  /**
   * Index of the last used connection in this pool.
   */
  private int lastUsedIndex = 0;
  /**
   * List of connection objects handled by this poool.
   */
  final List<E> connections = new ArrayList<>();
  /**
   * The connection objects in the core set.
   */
  final Set<E> coreConnections = new HashSet<>();

  /**
   * Initialize this pool with the specfiied core size.
   * @param coreSize the minimum number of connections in this pool.
   */
  protected AbstractConnectionPool(final int coreSize) {
    if (coreSize < 1) throw new IllegalArgumentException("the pool size should be >= 1, but it is " + coreSize);
    this.coreSize = coreSize;
    this.maxSize = coreSize;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized boolean add(final E connection) {
    if (debugEnabled) log.debug("adding {} to {}", connection, this);
    if (connectionCount() < coreSize) coreConnections.add(connection);
    return connections.add(connection);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized boolean remove(final E connection) {
    if (debugEnabled) log.debug("removing {} from {}", connection, this);
    if (connections.remove(connection)) {
      if (lastUsedIndex >= connections.size() && lastUsedIndex > 0) lastUsedIndex--;
      return true;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized E nextConnection() {
    if (connections.isEmpty()) return null;
    lastUsedIndex = ++lastUsedIndex % connections.size();
    return connections.get(lastUsedIndex);
  }

  @Override
  public synchronized boolean isEmpty() {
    return connections.isEmpty();
  }

  @Override
  public synchronized int connectionCount() {
    return connections.size();
  }

  @Override
  public int getCoreSize() {
    return coreSize;
  }

  @Override
  public synchronized int getMaxSize() {
    return maxSize;
  }

  @Override
  public synchronized List<E> getConnections() {
    return new ArrayList<>(connections);
  }

  @Override
  public synchronized Iterator<E> iterator() {
    return connections.iterator();
  }

  @Override
  public synchronized void close() {
    List<E> connections = getConnections();
    for (E connection: connections) {
      try {
        connection.close();
      } catch(Exception e) {
        String format = "error while closing connection {} : {}";
        if (debugEnabled) log.debug(format, connection, ExceptionUtils.getMessage(e));
        else log.warn(format, connection, ExceptionUtils.getStackTrace(e));
      }
    }
    this.connections.clear();
    coreConnections.clear();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("coreSize=").append(coreSize);
    sb.append(", maxSize=").append(maxSize);
    sb.append(", connectionCount=").append(connectionCount());
    sb.append(", coreConnections=").append(coreConnections.size());
    sb.append(']');
    return sb.toString();
  }
}
