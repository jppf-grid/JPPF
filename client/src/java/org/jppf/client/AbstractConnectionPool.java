/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.utils.*;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The max size of this pool.
   */
  int size;
  /**
   * Index of the last used connection in this pool.
   */
  private int lastUsedIndex = 0;
  /**
   * List of connection objects handled by this poool.
   */
  final List<E> connections = new ArrayList<>();

  /**
   * Initialize this pool with the specfiied core size.
   * @param size the minimum number of connections in this pool.
   */
  protected AbstractConnectionPool(final int size) {
    if (size < 1) throw new IllegalArgumentException("the pool size should be >= 1, but it is " + size);
    this.size = size;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public synchronized boolean add(final E connection) {
    if (debugEnabled) log.debug("adding {} to {}", connection, this);
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
  public synchronized List<E> getConnections() {
    return new ArrayList<>(connections);
  }

  @Override
  public synchronized Iterator<E> iterator() {
    return connections.iterator();
  }

  /**
   * Get the connection at the specified index.
   * @param i the index to look at.
   * @return a connection object.
   * @since 5.1
   */
  synchronized E getConnection(final int i) {
    return connections.get(i);
  }

  @Override
  public synchronized int getSize() {
    return size;
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
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append(", maxSize=").append(size);
    sb.append(", connectionCount=").append(connectionCount());
    sb.append(']');
    return sb.toString();
  }
}
