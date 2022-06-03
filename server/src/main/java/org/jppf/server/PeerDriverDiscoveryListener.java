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

package org.jppf.server;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.discovery.*;

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
   * Reference tot he JPPF driver.
   */
  private final JPPFDriver driver;

  /**
   * Initialize this listener.
   * @param driver a reference tot he JPPF driver.
   */
  PeerDriverDiscoveryListener(final JPPFDriver driver) {
    this.driver = driver;
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
      final JPPFConnectionInformation connectionInfo = DriverDiscoveryHandler.toJPPFConnectionInformation(info);
      driver.getInitializer().getPeerConnectionPoolHandler().newPool(info.getName(), info.getPoolSize(), connectionInfo, info.isSecure(), false);
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
