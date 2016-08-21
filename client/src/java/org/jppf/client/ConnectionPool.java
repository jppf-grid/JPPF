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

import java.util.List;

/**
 * Interface for pools of connections with a maximum size which can be dynamically updated.
 * @param <E> the type of the connections in the pool.
 * @author Laurent Cohen
 * @since 4.2
 */
public interface ConnectionPool<E extends AutoCloseable> extends Iterable<E>, AutoCloseable {

  /**
   * Add a connection to this pool.
   * @param connection the connection too add.
   * @return true if the underlying list of connections changed as a result of calling this method.
   * @exclude
   */
  boolean add(E connection);

  /**
   * Remove a connection from this pool.
   * @param connection the connection too remove.
   * @return true if the underlying list of connections changed as a result of calling this method.
   * @exclude
   */
  boolean remove(E connection);

  /**
   * Get the next connection.
   * @return a connection.
   * @exclude
   */
  E nextConnection();

  /**
   * Get the next connection that is connected and available.
   * @return a connection object if one is found in the desired state, or {@code null} otherwise.
   */
  E getConnection();

    /**
   * Determine whether this pool is empty.
   * @return <code>true</code> if this pool is empty, <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Get the current size of this pool.
   * @return the size as an int.
   */
  int connectionCount();

  /**
   * Get the maximum size of this connection pool.
   * @return the max size as an int.
   */
  int getSize();

  /**
   * Set the size of this connection pool, starting or stopping connections as needed.
   * <p>If any connection to be stopped is currently executing a job, then it will not be stopped.
   * @param maxSize the max size as an int.
   * @return the new maximum pool size.
   */
  int setSize(int maxSize);

  /**
   * Get a list of connections held by this pool. The returned list is independent from this pool,
   * thus changing, adding or removing elements has not effect on the pool.
   * @return a list of {@link JPPFClientConnection} instances.
   */
  List<E> getConnections();
}
