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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.discovery.*;

/**
 * Listens to new connectionr equests from {@link DriverDiscovery} instances.
 * @author Laurent Cohen
 */
class ClientDriverDiscoveryListener implements DriverDiscoveryListener<ClientConnectionPoolInfo> {
  /**
   * The client that handles new connection notifications.
   */
  private final AbstractGenericClient client;
  /**
   * The set of discovered pools. Used to determine whether a connection pool request has already bee made.
   */
  private final Set<DriverConnectionInfo> discoveredPools = new HashSet<>();
  /**
   * Whether this listener was close.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Initialize this listener with the specified client.
   * @param client the client that handles new connection notifications.
   */
  ClientDriverDiscoveryListener(final AbstractGenericClient client) {
    this.client = client;
  }

  @Override
  public boolean onNewConnection(final ClientConnectionPoolInfo info) {
    if (closed.get()) return false;
    boolean hasPool = false;
    synchronized(discoveredPools) {
      if (!(hasPool = discoveredPools.contains(info))) discoveredPools.add(info);
    }
    if (!hasPool) client.newConnectionPool(info);
    return !hasPool;
  }

  /**
   * Called when a pool is removed from the client's set of pools.
   * @param info information on the connection pool that was removed.
   * @return {@code true} if this listener contained the specified info, {@code false} otherwise.
   */
  boolean onPoolRemoved(final ClientConnectionPoolInfo info) {
    return discoveredPools.remove(info);
  }

  /**
   * Opens this listener.
   * @return this listener.
   */
  ClientDriverDiscoveryListener open() {
    closed.set(false);
    return this;
  }

  /**
   * Close this listener.
   * @return this listener.
   */
  ClientDriverDiscoveryListener close() {
    if (closed.compareAndSet(true, false)) {
      synchronized(discoveredPools) {
        discoveredPools.clear();
      }
    }
    return this;
  }
}
