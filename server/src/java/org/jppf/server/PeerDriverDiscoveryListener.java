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

package org.jppf.server;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.discovery.*;
import org.jppf.server.peer.JPPFPeerInitializer;

/**
 * Listens to new connectionr equests from {@link DriverDiscovery} instances.
 * @author Laurent Cohen
 */
class PeerDriverDiscoveryListener implements DriverDiscoveryListener<DriverConnectionInfo> {
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
   */
  PeerDriverDiscoveryListener() {
  }

  @Override
  public boolean onNewConnection(final DriverConnectionInfo info) {
    if (closed.get()) return false;
    boolean hasPool = false;
    synchronized(discoveredPools) {
      hasPool = discoveredPools.contains(info);
      if (!hasPool) discoveredPools.add(info);
    }
    if (!hasPool) {
      JPPFConnectionInformation connectionInfo = DriverDiscoveryHandler.toJPPFConnectionInformation(info);
      new JPPFPeerInitializer(info.getName(), connectionInfo, info.isSecure()).start();
    }
    return !hasPool;
  }

  /**
   * Opens this listener.
   * @return this listener.
   */
  PeerDriverDiscoveryListener open() {
    closed.set(false);
    return this;
  }

  /**
   * Close this listener.
   * @return this listener.
   */
  PeerDriverDiscoveryListener close() {
    if (closed.compareAndSet(true, false)) {
      synchronized(discoveredPools) {
        discoveredPools.clear();
      }
    }
    return this;
  }

  /**
   * Get the set of discovered pools.
   * @return a set of {@link DriverConnectionInfo} instances.
   */
  Set<DriverConnectionInfo> getDiscoveredPools() {
    synchronized(discoveredPools) {
      return new HashSet<>(discoveredPools);
    }
  }
}
