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

package org.jppf.jmxremote.notification;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;

import org.jppf.jmxremote.nio.JMXNioServer;

/**
 * Server-side interface for registering and unregistering notifications listeners, as well as dispatching local notifications to remote listeners.
 * @author Laurent Cohen
 */
public class ServerNotificationHandler {
  /**
   * Sequence number used to generate listener IDs.
   */
  private final AtomicInteger listenerSequence = new AtomicInteger(0);
  /**
   * Association of dispatchers to each mbean server.
   */
  private final Map<MBeanServer, MBeanServerNotificationDispatcher> mbeanServerMap = new HashMap<>();
  /**
   * The nio server.
   */
  private final JMXNioServer server;

  /**
   * Initialize with the specified nio server.
   * @param server the nio server.
   */
  public ServerNotificationHandler(final JMXNioServer server) {
    this.server = server;
  }

  /**
   * Add a notification listener.
   * @param mbeanServer the mbeanServer on which to to register the listener.
   * @param mbeanName the name of the MBean for which to subscribe to notifications.
   * @param filter the filter associated to the listener, may be null.
   * @param connectionID the id of the connection to which notifications will be forwarded.
   * @return a unique integer uniquely identifying the new listener.
   * @throws Exception if any error occurs.
   */
  public int addNotificationListener(final MBeanServer mbeanServer, final String connectionID, final ObjectName mbeanName, final NotificationFilter filter) throws Exception {
    MBeanServerNotificationDispatcher dispatcher = null;
    synchronized(mbeanServerMap) {
      dispatcher = mbeanServerMap.get(mbeanServer);
      if (dispatcher == null) {
        dispatcher = new MBeanServerNotificationDispatcher(mbeanServer, server);
        mbeanServerMap.put(mbeanServer, dispatcher);
      }
    }
    final int listenerID = listenerSequence.incrementAndGet();
    dispatcher.addNotificationListener(mbeanName, filter, listenerID, connectionID);
    return listenerID;
  }

  /**
   * Unregister the specified listener ids from the specified MBean.
   * @param mbeanServer the mbeanServer on which to to register the listener.
   * @param mbeanName name of the MBean from which to rmeove the listeners.
   * @param listenerIDs the ids of the listeners to remove.
   * @throws Exception if any error occurs.
   */
  public void removeNotificationListeners(final MBeanServer mbeanServer, final ObjectName mbeanName, final int[] listenerIDs) throws Exception {
    MBeanServerNotificationDispatcher dispatcher = null;
    synchronized(mbeanServerMap) {
      dispatcher = mbeanServerMap.get(mbeanServer);
    }
    if (dispatcher == null) throw new ListenerNotFoundException("found no listener for " + mbeanName);
    dispatcher.removeNotificationListeners(mbeanName, listenerIDs);
  }
}
