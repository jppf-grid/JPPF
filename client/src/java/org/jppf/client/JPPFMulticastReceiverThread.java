/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.discovery.*;
import org.jppf.utils.ThreadSynchronization;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * This class listens to information broadcast by JPPF servers on the network and uses it
 * to establish a connection with one or more servers.
 * @exclude
 */
class JPPFMulticastReceiverThread extends ThreadSynchronization implements Runnable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFMulticastReceiverThread.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Contains the set of retrieved connection information objects.
   */
  private final CollectionMap<String, JPPFConnectionInformation> infoMap = new SetHashMap<>();
  /**
   * Count of distinct retrieved connection information objects.
   */
  private final AtomicInteger count = new AtomicInteger(0);
  /**
   * Determines whether we keep the addresses of all discovered network interfaces for the same driver,
   * or if we only use the first one that is discovered.
   */
  private final boolean acceptMultipleInterfaces;
  /**
   * Defines a callback for objects wishing to be notified of discovery events.
   */
  private final ConnectionHandler connectionHandler;
  /**
   * Holds a set of filters to include or exclude sets of IP addresses in the discovery process.
   */
  private final IPFilter ipFilter;
  /**
   * The thread that executes the <code>run()</code> method.
   */
  private Thread runningThread = null;

  /**
   * Initialize this discovery thread with the specified JPPF client.
   * @param connectionHandler handler for adding new connection
   * @param ipFilter for accepted IP addresses
   * @param acceptMultipleInterfaces accept all discovered interfaces for same driver
   */
  JPPFMulticastReceiverThread(final ConnectionHandler connectionHandler, final IPFilter ipFilter, final boolean acceptMultipleInterfaces) {
    if (connectionHandler == null) throw new IllegalArgumentException("connectionHandler is null");
    this.connectionHandler = connectionHandler;
    this.ipFilter = ipFilter;
    this.acceptMultipleInterfaces = acceptMultipleInterfaces;
  }

  /**
   * Lookup server configurations from UDP multicasts.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    runningThread = Thread.currentThread();
    JPPFMulticastReceiver receiver = null;
    try {
      receiver = new JPPFMulticastReceiver(ipFilter);
      while (!isStopped()) {
        JPPFConnectionInformation info = receiver.receive();
        if (isStopped()) break;
        synchronized(this) {
          if ((info != null) && !hasConnectionInformation(info)) {
            if (debugEnabled) log.debug("Found connection information: " + info);
            addConnectionInformation(info);
            onNewConnection(AbstractGenericClient.VALUE_JPPF_DISCOVERY + '-' + count.incrementAndGet(), info);
          }
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
  private boolean hasConnectionInformation(final JPPFConnectionInformation info) {
    if (acceptMultipleInterfaces) {
      if (infoMap.containsValue(info.uuid, info)) return true;
    }
    if (infoMap.containsKey(info.uuid)) return true;
    for (JPPFConnectionInformation tmp: infoMap) {
      if (info.isSame(tmp, false)) return true;
    }
    return false;
  }

  /**
   * Add the specified connection information to discovered map.
   * @param info a {@link JPPFConnectionInformation} instance.
   */
  public synchronized void addConnectionInformation(final JPPFConnectionInformation info) {
    infoMap.putValue(info.uuid, info);
  }

  /**
   * Remove a disconnected connection.
   * @param uuid uuid of the driver for the connections to remove.
   * @return whether connection was successfully removed
   */
  public synchronized boolean removeConnectionInformation(final String uuid) {
    return infoMap.removeKey(uuid) != null;
  }

  /**
   * Remove a disconnected connection.
   * @param info connection info of the peer to remove
   * @return whether connection was successfully removed
   */
  public synchronized boolean removeConnectionInformation(final JPPFConnectionInformation info) {
    if (acceptMultipleInterfaces) return infoMap.removeValue(info.uuid, info);
    return infoMap.removeKey(info.uuid) != null;
  }

  /**
   * Close this multicast receiver and interrupt the thread that runs it.
   */
  public void close() {
    setStopped(true);
    if (runningThread != null) {
      try {
        runningThread.interrupt();
      } catch (Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
      }
      runningThread = null;
      synchronized(this) {
        infoMap.clear();
      }
    }
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
}
