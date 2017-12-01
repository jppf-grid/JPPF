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

import org.jppf.comm.discovery.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Instances of this class discover peer drivers over the network.
 * @author Laurent Cohen
 */
public class PeerDiscoveryThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PeerDiscoveryThread.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Interval for removal cleanup.
   */
  private static final long REMOVAL_CLEANUP_INTERVAL = JPPFConfiguration.get(JPPFProperties.PEER_DISCOVERY_REMOVAL_CLEANUP_INTERVAL);
  /**
   * Contains the set of retrieved connection information objects.
   */
  private final Set<JPPFConnectionInformation> infoSet = new HashSet<>();
  /**
   * Count of distinct retrieved connection information objects.
   */
  private final AtomicInteger count = new AtomicInteger(0);
  /**
   * Connection information for this JPPF driver.
   */
  private final JPPFConnectionInformation localInfo;
  /**
   * Defines a callback for objects wishing to be notified of discovery events.
   */
  private final ConnectionHandler connectionHandler;
  /**
   * Holds a set of filters to include or exclude sets of IP addresses in the discovery process.
   */
  private final IPFilter ipFilter;
  /**
   * Holds removed entries for a limited time.
   */
  private final Map<String, Long> removalMap = new HashMap<>();
  /**
   * Last time a cleanup was performed.
   */
  private long lastCleanupTime = 0L;

  /**
   * Default constructor.
   * @param connectionHandler handler for adding new connection
   * @param ipFilter for accepted IP addresses
   * @param localInfo Connection information for this JPPF driver.
   */
  public PeerDiscoveryThread(final ConnectionHandler connectionHandler, final IPFilter ipFilter, final JPPFConnectionInformation localInfo) {
    if (localInfo == null) throw new IllegalArgumentException("localInfo is null");
    if (connectionHandler == null) throw new IllegalArgumentException("connectionHandler is null");

    this.connectionHandler = connectionHandler;
    this.ipFilter = ipFilter;
    this.localInfo = localInfo;
  }

  /**
   * Lookup server configurations from UDP multicasts.
   */
  @Override
  public void run() {
    JPPFMulticastReceiver receiver = null;
    try {
      receiver = new JPPFMulticastReceiver(ipFilter);
      while (!isStopped()) {
        JPPFConnectionInformation info = receiver.receive();
        synchronized(this) {
          if (lastCleanupTime + REMOVAL_CLEANUP_INTERVAL >= System.currentTimeMillis()) cleanRemovals();
        }
        if ((info != null) && !hasConnectionInformation(info) && !wasRecentlyRemoved(info)) {
          if (debugEnabled) log.debug("Found peer connection information: " + info + ", infoSet=" + infoSet);
          addConnectionInformation(info);
          onNewConnection("Peer-" + count.incrementAndGet(), info);
        }
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      if (receiver != null) receiver.setStopped(true);
    }
  }

  /**
   * Add a newly found connection.
   * @param name for the connection
   * @param info the peer's connection information.
   */
  protected synchronized void onNewConnection(final String name, final JPPFConnectionInformation info) {
    connectionHandler.onNewConnection(name, info);
  }

  /**
   * Determine whether a connection information object is already discovered.
   * @param info the connection information to lookup.
   * @return true if the connection information is in the map, false otherwise.
   */
  protected synchronized boolean hasConnectionInformation(final JPPFConnectionInformation info) {
    //return infoSet.contains(info) || info.equals(localInfo) || isSelf(info);
    return infoSet.contains(info) || isSelf(info);
  }

  /**
   * Add the specified connection information to discovered map.
   * @param info a {@link JPPFConnectionInformation} instance.
   */
  public synchronized void addConnectionInformation(final JPPFConnectionInformation info) {
    infoSet.add(info);
  }

  /**
   * Remove a disconnected connection.
   * @param info connection info of the peer to remove
   * @return whether connection was successfully removed
   */
  public synchronized boolean removeConnectionInformation(final JPPFConnectionInformation info) {
    removalMap.put(info.uuid, System.currentTimeMillis());
    return infoSet.remove(info);
  }

  /**
   * Determine whether the specified connection information refers to this driver.
   * This situation may arise if the host has multiple network interfaces, each with its own IP address.
   * Making this distinction is important to prevent a driver from connecting to itself.
   * @param info the peer's connection information.
   * @return true if the host/port combination in the connection information can be resolved
   * as the configuration for this driver.
   */
  private boolean isSelf(final JPPFConnectionInformation info) {
    /*
    List<InetAddress> ipAddresses = NetworkUtils.getIPV4Addresses();
    ipAddresses.addAll(NetworkUtils.getIPV6Addresses());
    for (InetAddress addr: ipAddresses) {
      String ip = addr.getHostAddress();
      if (info.host.equals(ip) && Arrays.equals(info.serverPorts, localInfo.serverPorts)) return true;
    }
    return false;
    */
    return info.uuid.equals(localInfo.uuid);
  }

  /**
   * Perform cleanup of entries to remove.
   */
  private synchronized void cleanRemovals() {
    long now = System.currentTimeMillis();
    List<String> toRemove = new ArrayList<>();
    for (Map.Entry<String, Long> entry: removalMap.entrySet()) {
      if (entry.getValue() + REMOVAL_CLEANUP_INTERVAL <= now) toRemove.add(entry.getKey());
    }
    for (String uuid: toRemove) removalMap.remove(uuid);
  }

  /**
   * Determine whether the specified information was recently removed.
   * @param info the peer's connection information.
   * @return {@code true} if the information was recently removed, {@code false} otherwise.
   */
  private synchronized boolean wasRecentlyRemoved(final JPPFConnectionInformation info) {
    return (info.uuid == null) || removalMap.containsKey(info.uuid);
  }

  /**
   * Defines a callback for objects wishing to be notified of discovery events.
   */
  public interface ConnectionHandler {
    /**
     * Called when a new connection is discovered.
     * @param name the name assigned to the connection.
     * @param info the information required to connect to the driver.
     */
    void onNewConnection(final String name, final JPPFConnectionInformation info);
  }

  /**
   * Contains the set of retrieved connection information objects.
   * @return the list of discovered connection information.
   * @exclude 
   */
  public Set<JPPFConnectionInformation> getInfoSet() {
    return infoSet;
  }
}
