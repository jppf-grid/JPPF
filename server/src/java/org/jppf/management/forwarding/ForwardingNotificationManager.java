/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import javax.management.ListenerNotFoundException;

import org.jppf.management.NodeSelector;
import org.jppf.server.event.*;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.collections.*;
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
public class ForwardingNotificationManager implements NodeConnectionListener, ForwardingNotificationEventListener
{
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
  private final JPPFNodeForwarding forwarder;
  /**
   * Mapping of node uuids to corresponding notification dispatchers.
   */
  private Map<String, ForwardingNotificationDispatcher> nodeMap = new HashMap<String, ForwardingNotificationDispatcher>();
  /**
   * Each node uuid is mapped to a mapping of mbean names to a list of notification listeners.
   */
  private Map<String, CollectionMap<String, NotificationListenerWrapper>> clientMap = new HashMap<String, CollectionMap<String, NotificationListenerWrapper>>();
  /**
   * Used to synchronized access to the collection in this class.
   */
  private final Lock lock = new ReentrantLock();
  /**
   * Helper for dispatching node notifications to the client-side listeners.
   */
  private NodeForwardingHelper helper = NodeForwardingHelper.getInstance();

  /**
   * Initialize this manager.
   * @param forwarder the forwarding mbean instance.
   */
  public ForwardingNotificationManager(final JPPFNodeForwarding forwarder)
  {
    this.forwarder = forwarder;
    forwarder.driver.getInitializer().getNodeConnectionEventHandler().addNodeConnectionListener(this);
  }

  /**
   * Add the specified notification listener to the map of listeners.
   * @param listenerID the id of the listener to add.
   * @param selector the slector which determines from which nodes notification should be forwarded.
   * @param mBeanName the name of the MBean from which to receive notifications from the selected nodes.
   */
  public void addNotificationListener(final String listenerID, final NodeSelector selector, final String mBeanName)
  {
    addNotificationListener(new NotificationListenerWrapper(listenerID, selector, mBeanName));
  }

  /**
   * Add the specified notification listener to the map of listeners.
   * @param wrapper the listener to add.
   */
  private void addNotificationListener(final NotificationListenerWrapper wrapper)
  {
    NodeSelector selector = wrapper.getSelector();
    Set<AbstractNodeContext> nodes = forwarder.getSelectionHelper().getChannels(selector);
    lock.lock();
    try
    {
      helper.setListener(wrapper.getListenerID(), wrapper);
      for (AbstractNodeContext node: nodes) addNotificationListener(node, wrapper);
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Add the psecified listener to the specified node.
   * @param node the node to which to add the listener.
   * @param wrapper the listener to add.
   */
  private void addNotificationListener(final AbstractNodeContext node, final NotificationListenerWrapper wrapper)
  {
    String uuid = node.getUuid();
    String mbean = wrapper.getMBeanName();
    if (debugEnabled) log.debug("add notification listener for node=" + uuid + ", mbean='" + mbean + "'");
    ForwardingNotificationDispatcher dispatcher = nodeMap.get(uuid);
    boolean wasNull = dispatcher == null;
    if (wasNull) dispatcher = new ForwardingNotificationDispatcher(node);
    if (dispatcher.addNotificationListener(mbean))
    {
      if (wasNull)
      {
        nodeMap.put(uuid, dispatcher);
        dispatcher.addForwardingNotificationEventListener(this);
      }
      CollectionMap<String, NotificationListenerWrapper> map = clientMap.get(uuid);
      if (map == null)
      {
        map = new ArrayListHashMap<String, NotificationListenerWrapper>();
        map.putValue(mbean, wrapper);
      }
      clientMap.put(uuid, map);
    }
  }

  /**
   * Remove the specified notification listener from the map of listeners.
   * @param listenerID the id of the listener to remove.
   * @throws ListenerNotFoundException if the listener could not be found.
   */
  public void removeNotificationListener(final String listenerID) throws ListenerNotFoundException
  {
    NotificationListenerWrapper wrapper = helper.removeListener(listenerID);
    if (wrapper == null) throw new ListenerNotFoundException("could not find listener with id=" + listenerID);
    removeNotificationListener(wrapper);
  }

  /**
   * Remove the specified notification listener from the map of listeners.
   * @param wrapper the listener to remove.
   * @throws ListenerNotFoundException if the listener could not be found.
   */
  public void removeNotificationListener(final NotificationListenerWrapper wrapper) throws ListenerNotFoundException
  {
    NodeSelector selector = wrapper.getSelector();
    Set<AbstractNodeContext> nodes = forwarder.getSelectionHelper().getChannels(selector);
    lock.lock();
    try
    {
      for (AbstractNodeContext node: nodes) removeNotificationListener(node, wrapper);
    }
    finally
    {
      lock.unlock();
    }
  }

  /**
   * Remove the psecified listener from the specified node.
   * @param node the node from which to remove the listener.
   * @param wrapper the listener to rmeove.
   */
  private void removeNotificationListener(final AbstractNodeContext node, final NotificationListenerWrapper wrapper)
  {
    String mbean = wrapper.getMBeanName();
    String uuid = node.getUuid();
    ForwardingNotificationDispatcher dispatcher = nodeMap.get(uuid);
    if (dispatcher == null) return;
    CollectionMap<String, NotificationListenerWrapper> map = clientMap.get(uuid);
    map.removeValue(mbean, wrapper);
    if (!map.containsKey(mbean)) dispatcher.removeNotificationListener(mbean);
    if (map.isEmpty()) clientMap.remove(uuid);
    if (!dispatcher.hasNotificationListener())
    {
      dispatcher.removeForwardingNotificationEventListener(this);
      nodeMap.remove(uuid);
    }
  }

  @Override
  public void nodeConnected(final NodeConnectionEvent event)
  {
    String uuid = event.getNodeInformation().getUuid();
    AbstractNodeContext node = forwarder.driver.getNodeNioServer().getConnection(uuid);
    if (node == null) return;
    lock.lock();
    try
    {
      for (NotificationListenerWrapper wrapper: helper.allListeners())
      {
        if (forwarder.getSelectionHelper().isNodeAccepted(node, wrapper.getSelector())) addNotificationListener(node, wrapper);
      }
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public void nodeDisconnected(final NodeConnectionEvent event)
  {
    String uuid = event.getNodeInformation().getUuid();
    AbstractNodeContext node = forwarder.driver.getNodeNioServer().getConnection(uuid);
    if (node == null) return;
    lock.lock();
    try
    {
      for (NotificationListenerWrapper wrapper: helper.allListeners())
      {
        if (forwarder.getSelectionHelper().isNodeAccepted(node, wrapper.getSelector())) removeNotificationListener(node, wrapper);
      }
    }
    finally
    {
      lock.unlock();
    }
  }

  @Override
  public synchronized void notificationReceived(final ForwardingNotificationEvent event)
  {
    if (debugEnabled) log.debug("received notification from node=" +  event.getNodeUuid() + ", mbean='" + event.getMBeanName() + "' : " + event.getNotification());
    forwarder.sendNotification(new JPPFNodeForwardingNotification(event.getNotification(), event.getNodeUuid(), event.getMBeanName()));
  }
}
