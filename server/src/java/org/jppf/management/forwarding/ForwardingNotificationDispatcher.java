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

package org.jppf.management.forwarding;

import java.util.*;
import java.util.concurrent.locks.*;

import javax.management.*;

import org.jppf.management.JMXNodeConnectionWrapper;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class receive notifications from a single node and for any number of MBeans in that node,
 * and dispatch them to client notification listeners that registered.
 * <p>The dispatch is performed via an event listener mechanism: for each notification rreceived from the node,
 * an event will be emitted and the listener(s) are in charge of forwarding the notifications.
 * @author Laurent Cohen
 * @exclude
 */
class ForwardingNotificationDispatcher {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ForwardingNotificationDispatcher.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Constant for an empty array of event listeners.
   */
  private static final ForwardingNotificationEventListener[] EMPTY_LISTENERS = new ForwardingNotificationEventListener[0];
  /**
   * Connection to the node.
   */
  private final AbstractNodeContext node;
  /**
   * The node uuid.
   */
  private final String nodeUuid;
  /**
   * Mapping of MBean names to corresponding notification listeners.
   */
  private final Map<String, ForwardingNotificationHandler> handlerMap = new HashMap<>();
  /**
   * The list of registered event listeners.
   */
  private final List<ForwardingNotificationEventListener> listenerList = new ArrayList<>();
  /**
   * A temporary array used to avoid synchronization issues between addition/removal of listeners
   * and firing of events.
   */
  private ForwardingNotificationEventListener[] listenerArray = EMPTY_LISTENERS;
  /**
   * Used to synchronize access to the mapping of MBean names to notification listeners.
   */
  private final Lock lock = new ReentrantLock();

  /**
   * Initialize this dispatcher with the specified node uuid.
   * @param node connection to the node.
   */
  public ForwardingNotificationDispatcher(final AbstractNodeContext node) {
    this.node = node;
    this.nodeUuid = node.getUuid();
  }

  /**
   * Add a listener to the notifications emitted by the specified MBean.
   * @param mBeanName the name of the MBean from which to receive notifications.
   * @return <code>true</code> if the listener was sucessfully added, <code>false</code> otherwise.
   */
  public boolean addNotificationListener(final String mBeanName) {
    lock.lock();
    try {
      if (handlerMap.containsKey(mBeanName)) return false;
      JMXNodeConnectionWrapper jmx = node.getJmxConnection();
      if (jmx != null) {
        ForwardingNotificationHandler handler = new ForwardingNotificationHandler(mBeanName);
        try {
          if (debugEnabled) log.debug("mBeanName={}, handler={}", mBeanName, handler);
          jmx.addNotificationListener(mBeanName, handler);
          handlerMap.put(mBeanName, handler);
          return true;
        } catch (Exception e) {
          String format = "failed to add notification listener for node=%s : exception=%s";
          if (debugEnabled) log.debug(String.format(format, node, ExceptionUtils.getStackTrace(e)));
          else log.info(String.format(format, node, ExceptionUtils.getMessage(e)));
        }
      }
    } finally {
      lock.unlock();
    }
    return false;
  }

  /**
   * Add a listener to the notifications emitted by the specified MBean.
   * @param mBeanName the name of the MBean from which to receive notifications.
   * @return <code>true</code> if the listener was sucessfully added, <code>false</code> otherwise.
   */
  public boolean removeNotificationListener(final String mBeanName) {
    lock.lock();
    try {
      ForwardingNotificationHandler handler = handlerMap.remove(mBeanName);
      if (handler == null) return false;
      JMXNodeConnectionWrapper jmx = node.getJmxConnection();
      if (jmx != null) {
        try {
          jmx.removeNotificationListener(mBeanName, handler);
          return true;
        } catch (Exception e) {
          String message = String.format("error removing notification listener for node=%s, mBeanName=%s", nodeUuid, mBeanName);
          if (debugEnabled) log.debug(message, e);
          else log.info("{} : {}", message, ExceptionUtils.getMessage(e));
        }
      }
    } finally {
      lock.unlock();
    }
    return false;
  }

  /**
   * Determine whether this dispatcher is listening to notifications from any Mbean.
   * @return true if any notification listener is active, false otherwise.
   */
  public boolean hasNotificationListener() {
    lock.lock();
    try {
      return !handlerMap.isEmpty();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Add a listner to the list of listeners.
   * @param listener the listener to add.
   */
  public void addForwardingNotificationEventListener(final ForwardingNotificationEventListener listener) {
    synchronized (listenerList) {
      listenerList.add(listener);
      listenerArray = listenerList.toArray(new ForwardingNotificationEventListener[listenerList.size()]);
    }
  }

  /**
   * Remove a listner from the list of listeners.
   * @param listener the listener to add.
   */
  public void removeForwardingNotificationEventListener(final ForwardingNotificationEventListener listener) {
    synchronized (listenerList) {
      if (listenerList.remove(listener)) listenerArray = listenerList.toArray(new ForwardingNotificationEventListener[listenerList.size()]);
    }
  }

  /**
   * Fire a notification event to all registered listeners.
   * @param mBeanName the name of the MBean form which the notification was received.
   * @param notification the notification to dispatch.
   */
  public void fireNotificationEvent(final String mBeanName, final Notification notification) {
    ForwardingNotificationEvent event = new ForwardingNotificationEvent(nodeUuid, mBeanName, notification);
    ForwardingNotificationEventListener[] tmp;
    synchronized (listenerList) {
      tmp = listenerArray;
    }
    for (ForwardingNotificationEventListener listener : tmp) listener.notificationReceived(event);
  }

  /**
   * 
   */
  private class ForwardingNotificationHandler implements NotificationListener {
    /**
     * The name of the MBean to receive notifications from.
     */
    private final String mBeanName;

    /**
     * Initiialize this listener with the specified MBean name.
     * @param mBeanName the name of the MBean to receive notifications from.
     */
    public ForwardingNotificationHandler(final String mBeanName) {
      this.mBeanName = mBeanName;
    }

    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      try {
        if (debugEnabled) log.debug(String.format("received notification from node=%s, mbean=%s, notification=%s, handback=%s", nodeUuid, mBeanName, notification, handback));
        fireNotificationEvent(mBeanName, notification);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
