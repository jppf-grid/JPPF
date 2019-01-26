/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.event;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.management.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class handles operations for node connection events, including
 * listener registration, event notifications and initial loading of listeners via SPI.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeConnectionEventHandler extends ServiceProviderHandler<NodeConnectionListener> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeConnectionEventHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Keeps track of the number of connected nodes.
   */
  private final AtomicInteger connectedNodes = new AtomicInteger(0);
  /**
   * Keeps track of the number of connected peers.
   */
  private final AtomicInteger connectedPeers = new AtomicInteger(0);
  /**
   * Keeps track of the number of connected real (not peer) nodes.
   */
  private final AtomicInteger connectedRealNodes = new AtomicInteger(0);
  
  /**
   * Default constructor.
   */
  public NodeConnectionEventHandler() {
    super(NodeConnectionListener.class);
  }

  /**
   * Notify all listeners that a node is connected tot he server.
   * @param info encapsulates the information about the node.
   */
  public void fireNodeConnected(final JPPFManagementInfo info) {
    connectedNodes.incrementAndGet();
    if (info.isPeer()) connectedPeers.incrementAndGet();
    else connectedRealNodes.incrementAndGet();
    final NodeConnectionEvent event = new NodeConnectionEvent(info);
    for (final NodeConnectionListener listener : providers) listener.nodeConnected(event);
    JPPFNodeConnectionNotifier.getInstance().onNodeConnected(info);
  }

  /**
   * Notify all listeners that a node is disconnected from the server.
   * @param info encapsulates the information about the node.
   */
  public void fireNodeDisconnected(final JPPFManagementInfo info) {
    connectedNodes.decrementAndGet();
    if (info.isPeer()) connectedPeers.decrementAndGet();
    else connectedRealNodes.decrementAndGet();
    final NodeConnectionEvent event = new NodeConnectionEvent(info);
    for (final NodeConnectionListener listener : providers) listener.nodeDisconnected(event);
    JPPFNodeConnectionNotifier.getInstance().onNodeDisconnected(info);
  }

  /**
   * Load all listener instances found in the class path via a service definition.
   */
  public void loadListeners() {
    final Iterator<NodeConnectionListener> it = ServiceFinder.lookupProviders(NodeConnectionListener.class);
    final List<NodeConnectionListener> list = new ArrayList<>();
    while (it.hasNext()) {
      final NodeConnectionListener listener = it.next();
      if (listener == null) continue;
      list.add(listener);
      if (debugEnabled) log.debug("successfully added node connection listener " + listener.getClass().getName());
    }
    providers.addAll(list);
  }

  /**
   * @return the number of connected nodes.
   */
  public int getConnectedNodes() {
    return connectedNodes.get();
  }

  /**
   * @return the number of connected peers.
   */
  public int getConnectedPeers() {
    return connectedPeers.get();
  }

  /**
   * @return the number of connected real (not peer) nodes.
   */
  public int getConnectedRealNodes() {
    return connectedRealNodes.get();
  }
}
