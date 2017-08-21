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

package org.jppf.server.peer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.JPPFConnectionInformation;

/**
 *
 * @author Laurent Cohen
 */
public class PeerConnectionPool implements AutoCloseable {
  /**
   * Name of the peer in the configuration file.
   */
  private final String peerName;
  /**
   * Peer connection information.
   */
  private final JPPFConnectionInformation connectionInfo;
  /**
   * Determines whether communication with remote peer servers should be secure.
   */
  private final boolean secure;
  /**
   * Whether this connection and its pool were created by the discovery mechanism.
   */
  private final boolean fromDiscovery;
  /**
   * The size of this pool.
   */
  private int size;
  /**
   * Sequence number for connection numbers in this pool.
   */
  private final AtomicInteger connectionSequence = new AtomicInteger(0);
  /**
   * Holds all the peer connections in this pool.
   */
  private final List<JPPFPeerInitializer> initializers = new ArrayList<>();

  /**
   * Initialize this connection pool.
   * @param size the size of this pool.
   * @param peerName name of the peer in the configuration file.
   * @param connectionInfo the peer connection information.
   * @param secure determines whether communication with remote peer servers should be secure.
   * @param fromDiscovery whether this connection and its pool were created by the discovery mechanism.
   */
  public PeerConnectionPool(final String peerName, final int size, final JPPFConnectionInformation connectionInfo, final boolean secure, final boolean fromDiscovery) {
    this.peerName = peerName;
    this.size = size < 1 ? 1 : size;
    this.connectionInfo = connectionInfo;
    this.secure = secure;
    this.fromDiscovery = fromDiscovery;
    init();
  }

  /**
   * @return the name of the peer in the configuration file.
   */
  public String getPeerName() {
    return peerName;
  }

  /**
   * @return the peer connection information.
   */
  public JPPFConnectionInformation getConnectionInfo() {
    return connectionInfo;
  }

  /**
   * @return whether communication with remote peer servers should be secure.
   */
  public boolean isSecure() {
    return secure;
  }

  /**
   * @return whether this connection and its pool were created by the discovery mechanism.
   */
  public boolean isFromDiscovery() {
    return fromDiscovery;
  }

  /**
   * @return the size of this pool.
   */
  public int getSize() {
    return size;
  }

  /**
   * Set the size of this pool.
   * @param size the pool size to set.
   */
  public void setSize(final int size) {
    this.size = size;
  }

  /**
   * Initialize this pool by starting all connections up to the specified pool size.
   */
  private void init() {
    for (int i=1; i<=size; i++) {
      String name = String.format("%s-%d", peerName, connectionSequence.incrementAndGet());
      JPPFPeerInitializer initializer = new JPPFPeerInitializer(name, connectionInfo, secure, fromDiscovery);
      initializers.add(initializer);
      initializer.start();
    }
  }

  @Override
  public void close() {
    for (JPPFPeerInitializer initializer: initializers) initializer.close();
    initializers.clear();
  }
}
