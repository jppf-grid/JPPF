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

import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PeerConnectionPoolHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(PeerConnectionPoolHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The connection pools.
   */
  private final List<PeerConnectionPool> pools = new ArrayList<>();

  /**
   * Create a new peer connection pool.
   * @param size the size of this pool.
   * @param peerName name of the peer in the configuration file.
   * @param connectionInfo the peer connection information.
   * @param secure determines whether communication with remote peer servers should be secure.
   * @param fromDiscovery whether this connection and its pool were created by the discovery mechanism.
   * @return the newly created pool.
   */
  public PeerConnectionPool newPool(final String peerName, final int size, final JPPFConnectionInformation connectionInfo, final boolean secure, final boolean fromDiscovery) {
    if (debugEnabled) log.debug(String.format("creating PeerConnectionPool with peerName=%s, size=%d, connectionInfo=%s, secure=%b, fromDiscovery=%b",
      peerName, size, connectionInfo, secure, fromDiscovery));
    final PeerConnectionPool pool = new PeerConnectionPool(peerName, size, connectionInfo, secure, fromDiscovery);
    pools.add(pool);
    return pool;
  }
}
