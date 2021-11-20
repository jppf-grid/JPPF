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

package org.jppf.jmxremote.notification;

import java.util.*;

import javax.management.*;

import org.jppf.jmxremote.message.*;
import org.jppf.jmxremote.nio.JMXNioServer;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Instances of this class handle the registration and unregistration of remote notification listeners
 * and dispatch local notifications to the registered remote notification listeners.
 * @author Laurent Cohen
 */
class MBeanServerNotificationDispatcher implements NotificationListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MBeanServerNotificationDispatcher.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The MBean server whose notifications to dispatch.
   */
  private final MBeanServer mbeanServer;
  /**
   * Association of server-side listeners to each mbean for which a listener is registered.
   */
  private final CollectionMap<ObjectName, ServerListenerInfo> listenerMap = new ArrayListHashMap<>();
  /**
   * The nio server.
   */
  private final JMXNioServer server;

  /**
   * Initialize with the specified MBean server.
   * @param mbeanServer the MBean server whose notifications to dispatch.
   * @param server the nio server.
   */
  MBeanServerNotificationDispatcher(final MBeanServer mbeanServer, final JMXNioServer server) {
    if (mbeanServer == null) throw new IllegalArgumentException("MBeanServer cannot be null");
    this.mbeanServer = mbeanServer;
    this.server = server;
  }

  /**
   * Add a notification listener to the specified MBean.
   * @param mbeanName name of the mbean to register the listener to.
   * @param filter the filter ot apply, may be {@code null}.
   * @param listenerID the id of the listener to register.
   * @param connectionID the id of the connection tot he client.
   * @throws Exception if any error occurs.
   */
  void addNotificationListener(final ObjectName mbeanName, final NotificationFilter filter, final int listenerID, final String connectionID) throws Exception {
    final ServerListenerInfo info = new ServerListenerInfo(listenerID, filter, connectionID);
    if (debugEnabled) log.debug("adding listenerID = {}, {} to {}", listenerID, info, mbeanServer);
    synchronized(listenerMap) {
      if (!listenerMap.containsKey(mbeanName)) mbeanServer.addNotificationListener(mbeanName, this, null, mbeanName);
      listenerMap.putValue(mbeanName, info);
      if (debugEnabled) log.debug("added listenerID = {}, nbListeners = {}", listenerID, listenerMap.size());
    }
  }

  /**
   * Unregister the specified listener ids from the specified MBean.
   * @param mbeanName name of the MBean from which to rmeove the listeners.
   * @param listenerIDs the ids of the listeners to remove.
   * @throws Exception if any error occurs.
   */
  void removeNotificationListeners(final ObjectName mbeanName, final int[] listenerIDs) throws Exception {
    if (debugEnabled) log.debug("removing listenerIDs = {} from {}", Arrays.toString(listenerIDs), mbeanName);
    final Set<Integer> idSet = new HashSet<>();
    for (final int n: listenerIDs) idSet.add(n);
    final List<ServerListenerInfo> toRemove = new ArrayList<>();
    synchronized(listenerMap) {
      if (listenerMap.containsKey(mbeanName)) {
        for (final ServerListenerInfo info: listenerMap.getValues(mbeanName)) {
          if (idSet.contains(info.getListenerID())) toRemove.add(info);
        }
      }
      if (toRemove.isEmpty()) throw new ListenerNotFoundException("found no listener for " + mbeanName);
      for (final ServerListenerInfo info: toRemove) listenerMap.removeValue(mbeanName, info);
      if (!listenerMap.containsKey(mbeanName)) mbeanServer.removeNotificationListener(mbeanName, this);
    }
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    if (debugEnabled) log.debug("received notification for mbean={} : {}", handback, notification);
    final ObjectName mbeanName = (ObjectName) handback;
    List<ServerListenerInfo> infos = null;
    synchronized(listenerMap) {
      final Collection<ServerListenerInfo> coll = listenerMap.getValues(mbeanName);
      if (coll != null) infos = new ArrayList<>(coll);
    }
    if (infos != null) {
      final CollectionMap<String, Integer> listenersPerConnection = new ArrayListHashMap<>();
      for (final ServerListenerInfo info: infos) {
        if (info.getFilter().isNotificationEnabled(notification)) listenersPerConnection.putValue(info.getConnectionID(), info.getListenerID());
      }
      if (!listenersPerConnection.isEmpty()) {
        final Map<String, JMXMessageHandler> handlersMap = server.getMessageHandlers(listenersPerConnection.keySet());
        if (!handlersMap.isEmpty()) {
          for (final Map.Entry<String, JMXMessageHandler> entry: handlersMap.entrySet()) {
            final Collection<Integer> listenerIDs = listenersPerConnection.getValues(entry.getKey());
            if ((listenerIDs != null) && !listenerIDs.isEmpty()) {
              if (debugEnabled) log.debug("sending notification to listeners {} : via connection {}", listenerIDs, entry.getValue().getChannels().getConnectionID());
              try {
                entry.getValue().sendMessage(new JMXNotification(-1L, notification, listenerIDs.toArray(new Integer[listenerIDs.size()])));
              } catch (final Exception e) {
                log.error(e.getMessage(), e);
              }
            }
          }
        }
      }
    }
  }
}
