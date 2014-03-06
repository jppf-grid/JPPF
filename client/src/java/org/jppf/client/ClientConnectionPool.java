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

/**
 * Instances of this class manage a list of client connections with the same priority and remote driver.
 */
public class ClientConnectionPool implements Comparable<ClientConnectionPool> {
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
  private final List<JPPFClientConnection> clientList = new ArrayList<>();
  /**
   * The id of this pool.
   */
  private final int id;

  /**
   * Initialize this pool with the specified connection.
   * @param connection the first connection added to this pool.
   */
  ClientConnectionPool(final JPPFClientConnection connection) {
    this.id = connection.getPoolId();
    this.priority = connection.getPriority();
    clientList.add(connection);
  }

  /**
   * Get the next client connection.
   * @return a <code>JPPFClientConnection</code> instances.
   */
  synchronized JPPFClientConnection nextClient() {
    if (clientList.isEmpty()) return null;
    lastUsedIndex = ++lastUsedIndex % clientList.size();
    return clientList.get(getLastUsedIndex());
  }

  /**
   * Determine whether this pool is empty.
   * @return <code>true</code> if this pool is empty, <code>false</code> otherwise.
   */
  public synchronized boolean isEmpty() {
    return clientList.isEmpty();
  }

  /**
   * Get the current size of this pool.
   * @return the size as an int.
   */
  public synchronized int size() {
    return clientList.size();
  }

  /**
   * Add a driver connection to this pool.
   * @param client the connection too add.
   * @return true if the underlying list of connections changed as a result of calling this method.
   */
  synchronized boolean add(final JPPFClientConnection client) {
    return clientList.add(client);
  }

  /**
   * Remove a driver connection from this pool.
   * @param client the connection too remove.
   * @return true if the underlying list of connections changed as a result of calling this method.
   */
  synchronized boolean remove(final JPPFClientConnection client) {
    if (clientList.remove(client)) {
      if (lastUsedIndex >= clientList.size() && lastUsedIndex > 0) lastUsedIndex--;
      return true;
    }
    return false;
  }

  /**
   * Get the priority associated with this pool.
   * @return the priority as an int.
   */
  public synchronized int getPriority() {
    return priority;
  }

  /**
   * Get the index of the last used connection in this pool.
   * @return the last used index as an int.
   */
  private int getLastUsedIndex() {
    return lastUsedIndex;
  }

  /**
   * Get the uuid of the driver to which connections in this pool are connected.
   * @return the driver uuid as a string.
   */
  public synchronized String getDriverUuid() {
    return !clientList.isEmpty() ? clientList.get(0).getDriverUuid() : null;
  }

  @Override
  public int compareTo(final ClientConnectionPool o) {
    if (o == null) return -1;
    return priority > o.priority ? -1 : (priority < o.priority ? 1 : 0);
  }

  /**
   * Get the id of this pool.
   * @return the id as an int value.
   */
  public int getId() {
    return id;
  }
}
