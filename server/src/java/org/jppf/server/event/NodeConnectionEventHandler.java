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

package org.jppf.server.event;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.management.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class handles operations for node connection events, including
 * listener registration, event notifications and initial loading of listeners via SPI.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeConnectionEventHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeConnectionEventHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The list of node connection listeners.
   */
  private final List<NodeConnectionListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Add a listener to the list of listeners.
   * @param listener a {@link NodeConnectionListener} instance.
   */
  public void addNodeConnectionListener(final NodeConnectionListener listener) {
    if (listener == null) return;
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener a {@link NodeConnectionListener} instance.
   */
  public void removeNodeConnectionListener(final NodeConnectionListener listener) {
    if (listener == null) return;
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that a node is connected tot he server.
   * @param info encapsulates the information about the node.
   */
  public void fireNodeConnected(final JPPFManagementInfo info) {
    NodeConnectionEvent event = new NodeConnectionEvent(info);
    for (NodeConnectionListener listener : listeners) listener.nodeConnected(event);
    JPPFNodeConnectionNotifier.getInstance().onNodeConnected(info);
  }

  /**
   * Notify all listeners that a node is disconnected from the server.
   * @param info encapsulates the information about the node.
   */
  public void fireNodeDisconnected(final JPPFManagementInfo info) {
    NodeConnectionEvent event = new NodeConnectionEvent(info);
    for (NodeConnectionListener listener : listeners) listener.nodeDisconnected(event);
    JPPFNodeConnectionNotifier.getInstance().onNodeDisconnected(info);
  }

  /**
   * Load all listener instances found in the class path via a service definition.
   */
  public void loadListeners() {
    Iterator<NodeConnectionListener> it = ServiceFinder.lookupProviders(NodeConnectionListener.class);
    List<NodeConnectionListener> list = new ArrayList<>();
    while (it.hasNext()) {
      NodeConnectionListener listener = it.next();
      if (listener == null) continue;
      list.add(listener);
      if (debugEnabled) log.debug("successfully added node connection listener " + listener.getClass().getName());
    }
    listeners.addAll(list);
  }
}
