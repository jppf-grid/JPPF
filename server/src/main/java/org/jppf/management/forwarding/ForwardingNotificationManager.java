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

package org.jppf.management.forwarding;

import java.util.*;
import java.util.concurrent.locks.*;

import javax.management.*;

import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.event.*;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * This class implements the logic of registering JMX notification listeners to the nodes and dispatching
 * the received notifications to the client-side JMX listeners. The actual notification filtering for the
 * clients is implemented in the class {@link InternalNotificationFilter}.
 * <p>Additionally, this class also handles a dynamic grid topology by registering as a {@link NodeConnectionListener}.
 * This allows it to dynamically adapt when new nodes connect to the server or existing nodes disconnect: node
 * JMX listeners are then automatically registered or unregistered and their notifications forwarded to client-side
 * listeners whose <code>NodeSelector</code> matches the nodes.
 * @author Laurent Cohen
 * @exclude
 */
public class ForwardingNotificationManager implements NodeConnectionListener, ForwardingNotificationEventListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ForwardingNotificationManager.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Reference to the MBean which forwards the notifications.
   */
  private final AbstractNodeForwarding forwarder;
  /**
   * Mapping of node uuids to corresponding notification dispatchers.
   */
  private Map<String, ForwardingNotificationDispatcher> nodeMap = new HashMap<>();
  /**
   * Each node uuid is mapped to a mapping of mbean names to a list of notification listeners.
   */
  private Map<String, CollectionMap<String, NotificationListenerWrapper>> clientMap = new HashMap<>();
  /**
   * Used to synchronized access to the collection in this class.
   */
  private final Lock lock = new ReentrantLock();
  /**
   * Helper for dispatching node notifications to the client-side listeners.
   */
  private NodeForwardingHelper forwardingHelper = NodeForwardingHelper.getInstance();
  /**
   * Provides an API for selecting nodes based on a {@link NodeSelector}.
   */
  final NodeSelectionHelper selectionHelper;
  /**
   * The JPPF driver.
   */
  final JPPFDriver driver;

  /**
   * Initialize this manager.
   * @param forwarder the forwarding mbean instance.
   */
  public ForwardingNotificationManager(final AbstractNodeForwarding forwarder) {
    this.forwarder = forwarder;
    this.selectionHelper = forwarder.getSelectionHelper();
    this.driver = forwarder.driver;
    driver.getInitializer().getNodeConnectionEventHandler().addProvider(this);
  }

  /**
   * Add the specified notification listener to the map of listeners.
   * @param listenerID the id of the listener to add.
   * @param selector the slector which determines from which nodes notification should be forwarded.
   * @param mBeanName the name of the MBean from which to receive notifications from the selected nodes.
   */
  public void addNotificationListener(final String listenerID, final NodeSelector selector, final String mBeanName) {
    addNotificationListener(new NotificationListenerWrapper(listenerID, selector, mBeanName));
  }

  /**
   * Add the specified notification listener to the map of listeners.
   * @param wrapper the listener to add.
   */
  private void addNotificationListener(final NotificationListenerWrapper wrapper) {
    if (debugEnabled) log.debug("adding notification listener {}", wrapper);
    final NodeSelector selector = wrapper.getSelector();
    final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector);
    if (debugEnabled) log.debug("found {} nodes", nodes.size());
    lock.lock();
    try {
      forwardingHelper.setListener(wrapper.getListenerID(), wrapper);
      for (BaseNodeContext node: nodes) addNotificationListener(node, wrapper);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Add the specified listener to the specified node.
   * @param node the node to which to add the listener.
   * @param wrapper the listener to add.
   */
  private void addNotificationListener(final BaseNodeContext node, final NotificationListenerWrapper wrapper) {
    final String uuid = node.getUuid();
    final String mbean = wrapper.getMBeanName();
    if (debugEnabled) log.debug("adding notification listener for node={} : {}", uuid, wrapper);
    ForwardingNotificationDispatcher dispatcher = nodeMap.get(uuid);
    final boolean wasNull = dispatcher == null;
    if (wasNull) dispatcher = new ForwardingNotificationDispatcher(node);
    if (dispatcher.addNotificationListener(mbean)) {
      if (wasNull) {
        nodeMap.put(uuid, dispatcher);
        dispatcher.addForwardingNotificationEventListener(this);
      }
      CollectionMap<String, NotificationListenerWrapper> map = clientMap.get(uuid);
      if (map == null) {
        map = new ArrayListHashMap<>();
        clientMap.put(uuid, map);
      }
      map.putValue(mbean, wrapper);
    }
  }

  /**
   * Remove the specified notification listener from the map of listeners.
   * @param listenerID the id of the listener to remove.
   * @throws ListenerNotFoundException if the listener could not be found.
   */
  public void removeNotificationListener(final String listenerID) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("removing notification listeners for listenerID = {}", listenerID);
    final NotificationListenerWrapper wrapper = forwardingHelper.removeListener(listenerID);
    if (wrapper == null) throw new ListenerNotFoundException("could not find listener with id=" + listenerID);
    removeNotificationListener(wrapper);
  }

  /**
   * Remove the specified notification listener from the map of listeners.
   * @param wrapper the listener to remove.
   * @throws ListenerNotFoundException if the listener could not be found.
   */
  public void removeNotificationListener(final NotificationListenerWrapper wrapper) throws ListenerNotFoundException {
    if (debugEnabled) log.debug("removing notification listeners for {}", wrapper);
    final NodeSelector selector = wrapper.getSelector();
    final Set<BaseNodeContext> nodes = selectionHelper.getChannels(selector);
    final Runnable r = () -> {
      lock.lock();
      try {
        for (BaseNodeContext node: nodes) removeNotificationListener(node, wrapper);
      } finally {
        lock.unlock();
      }
    };
    ThreadUtils.startThread(r, "removeNotificationListener(" + wrapper + ")");
  }

  /**
   * Remove the specified listener from the specified node.
   * @param node the node from which to remove the listener.
   * @param wrapper the listener to rmeove.
   */
  private void removeNotificationListener(final BaseNodeContext node, final NotificationListenerWrapper wrapper) {
    if (debugEnabled) log.debug("removing notification listener {} for node {}", wrapper, node);
    final String mbean = wrapper.getMBeanName();
    final String uuid = node.getUuid();
    final ForwardingNotificationDispatcher dispatcher = nodeMap.get(uuid);
    if (dispatcher == null) return;
    final CollectionMap<String, NotificationListenerWrapper> map = clientMap.get(uuid);
    if (map != null) {
      map.removeValue(mbean, wrapper);
      if (!map.containsKey(mbean)) dispatcher.removeNotificationListener(mbean);
      if (map.isEmpty()) clientMap.remove(uuid);
    }
    if (!dispatcher.hasNotificationListener()) {
      dispatcher.removeForwardingNotificationEventListener(this);
      nodeMap.remove(uuid);
    }
  }

  @Override
  public void nodeConnected(final NodeConnectionEvent event) {
    final JPPFManagementInfo info = event.getNodeInformation();
    if (debugEnabled) log.debug("handling new connected node {},", info);
    if ((info == null) || (info.getPort() < 0) || (info.getHost() == null)) return;
    final String uuid = info.getUuid();
    final BaseNodeContext node = driver.getAsyncNodeNioServer().getConnection(uuid);
    if (debugEnabled) log.debug("new connected node {}", node);
    if (node == null) return;
    lock.lock();
    try {
      for (final NotificationListenerWrapper wrapper: forwardingHelper.allListeners()) {
        if (selectionHelper.isNodeAccepted(node, wrapper.getSelector())) addNotificationListener(node, wrapper);
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void nodeDisconnected(final NodeConnectionEvent event) {
    final JPPFManagementInfo info = event.getNodeInformation();
    if (debugEnabled) log.debug("handling disconnected node {}", info);
    final String uuid = info.getUuid();
    final BaseNodeContext node = driver.getAsyncNodeNioServer().getConnection(uuid);
    if (node == null) return;
    final Runnable r = () -> {
      lock.lock();
      try {
        for (NotificationListenerWrapper wrapper: forwardingHelper.allListeners()) {
          if (selectionHelper.isNodeAccepted(node, wrapper.getSelector())) removeNotificationListener(node, wrapper);
        }
      } finally {
        lock.unlock();
      }
    };
    ThreadUtils.startThread(r, event.toString());
  }

  @Override
  public synchronized void notificationReceived(final ForwardingNotificationEvent event) {
    final Notification notif = event.getNotification();
    if (debugEnabled) log.debug("received notification from node={}, mbean={}, notification={} (sequence={}, timestamp={}), userData = {}",
      event.getNodeUuid(), event.getMBeanName(), notif, notif.getSequenceNumber(), notif.getTimeStamp(), notif.getUserData());
    forwarder.sendNotification(new JPPFNodeForwardingNotification(notif, event.getNodeUuid(), event.getMBeanName()));
  }
}
