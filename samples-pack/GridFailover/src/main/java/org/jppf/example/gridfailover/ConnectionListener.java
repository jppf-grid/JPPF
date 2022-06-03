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

package org.jppf.example.gridfailover;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFClientConnection;
import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.client.event.ConnectionPoolEvent;
import org.jppf.client.event.ConnectionPoolListenerAdapter;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.NodeSelector;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A connection listener which forces the nodes to reconnect to the driver with the highest priority, when it comes back online.
 * @author Laurent Cohen
 */
public class ConnectionListener extends ConnectionPoolListenerAdapter implements ClientConnectionStatusListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ConnectionListener.class);
  /**
   * Mapping of connection priorities to the count of connections with the corresponding priority.
   */
  private final SortedMap<Integer, AtomicInteger> activePriorities = new TreeMap<>();
  /**
   * To avoid reconnecting the nodes when this client starts.
   */
  private final Set<JPPFClientConnection> firstTime = new HashSet<>();

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try (final JPPFClient client = new JPPFClient(new ConnectionListener())) {
      // wait until the JVM is terminated or the Enter key is pressed
      StreamUtils.waitKeyPressed("press [Enter] to exit");
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  // implementation of the ConnectionPoolListener interface

  @Override
  public void connectionAdded(final ConnectionPoolEvent event) {
    event.getConnection().addClientConnectionStatusListener(this);
  }

  @Override
  public void connectionRemoved(final ConnectionPoolEvent event) {
    event.getConnection().removeClientConnectionStatusListener(this);
  }

  // implementation of the ClientConnectionStatusListener interface

  @Override
  public void statusChanged(final ClientConnectionStatusEvent event) {
    final JPPFClientConnection connection = event.getClientConnection();
    // client local executor is not relevant here
    if (connection.isLocal())
      return;
    final JPPFConnectionPool pool = connection.getConnectionPool();
    final StatusChange change = getStatusChange(event);
    // when the client just (re)connected to a driver
    if (change == StatusChange.CONNECTED) {
      log.info("connection of {}", connection);
      // do not reconnect the nodes on first driver connection
      synchronized(firstTime) {
        if (!firstTime.contains(connection)) {
          firstTime.add(connection);
          return;
        }
      }
      final List<JPPFClientConnection> list = pool.getConnections(JPPFClientConnectionStatus.workingStatuses());
      if (list.size() == 1) {
        log.info("pool has 1 active connection");
        final int priority = pool.getPriority();
        // the highest priority among th current connections
        final int highestPriority;
        synchronized(activePriorities) {
          highestPriority = activePriorities.isEmpty() ? Integer.MIN_VALUE : activePriorities.lastKey();
          // increment the count of pools with this pool's priority
          AtomicInteger count = activePriorities.get(priority);
          if (count == null) {
            count = new AtomicInteger(1);
            activePriorities.put(priority, count);
          } else
            count.incrementAndGet();
        }
        if (priority > highestPriority) {
          // if the priority of the connection for the event is the highest,
          // force-reconnect the nodes attached to all other drivers,
          // to force them to reconnect to the one with the highest priority
          log.info("reconnecting the nodes"); 
          final List<JPPFConnectionPool> pools = pool.getClient().findConnectionPools(JPPFClientConnectionStatus.workingStatuses());
          for (final JPPFConnectionPool p: pools) {
            // skip the connection pool for which we just got an event
            if (p == pool)
              continue;

            // send the management request in a new thread, to avoid blocking this listener for too long
            new Thread(() -> {
              try {
                // obtain a JMX connection to the driver
                final JMXDriverConnectionWrapper jmx = p.awaitWorkingJMXConnection();
                // request that all the nodes attached to the driver attempt a reconnection,
                // once they have finished executing their remaining tasks
                jmx.getForwarder().reconnect(NodeSelector.ALL_NODES, false);
              } catch (final Exception e) {
                log.error("error attempting to reconnect the nodes", e);
              }
            }).start();
          }
        }
      }
    } else if (change == StatusChange.DISCONNECTED) {
      // when the client just disconnected from a driver
      log.info("disconnection of {}", connection); 
      final List<JPPFClientConnection> list = pool.getConnections(JPPFClientConnectionStatus.workingStatuses());
      // if the pool no longer has any wotking connection
      if (list.isEmpty()) {
        // decrement the count of pools with this pool's priority
        log.info("pool has no active connection"); 
        final int priority = pool.getPriority();
        synchronized(activePriorities) {
          final AtomicInteger count = activePriorities.get(priority);
          if (count == null)
            return;
          final int n = count.decrementAndGet();
          if (n <= 0)
            activePriorities.remove(priority);
        }
      }
    }
  }

  /**
   * Determine whether the connection status changed from not connected to connected or vice-versa.
   * @param event the status change event to check.
   * @return a {@link StatusChange} element.
   */
  private static StatusChange getStatusChange(final ClientConnectionStatusEvent event) {
    final JPPFClientConnectionStatus oldStatus = event.getOldStatus();
    final JPPFClientConnectionStatus newStatus = event.getClientConnection().getStatus();
    // whether the previous status was a working status, either ACTIVE or EXECUTING
    final boolean before = (oldStatus != null) && oldStatus.isWorkingStatus();
    // whether the current status is a working status, either ACTIVE or EXECUTING
    final boolean after = (newStatus != null) && newStatus.isWorkingStatus();
    // if status changed from not connected to connected
    if (!before && after)
      return StatusChange.CONNECTED;
    // else if status changed from connected to not connected
    else if (before && !after)
      return StatusChange.DISCONNECTED;
    return StatusChange.NONE;
  }

  /**
   * Categories of connection status change.
   */
  private enum StatusChange {
    /**
     * A connection to a driver was just established.
     */
    CONNECTED,
    /**
     * A connection to a driver was just closed or broken.
     */
    DISCONNECTED,
    /**
     * No change in the connection status.
     */
    NONE;
  }
}
