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

package com.sun.jmx.remote.opt.internal;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * 
 * @author Laurent Cohen
 */
class NotifFetcher implements Runnable {
  /** */
  private static final ClassLogger logger = new ClassLogger("com.sun.jmx.remote.opt.internal", "NotifFetcher");
  /** */
  private final ClientNotifForwarder forwarder;

  /**
   * @param forwarder .
   */
  NotifFetcher(final ClientNotifForwarder forwarder) {
    this.forwarder = forwarder;
  }

  @Override
  public void run() {
    synchronized (forwarder) {
      forwarder.currentFetchThread = Thread.currentThread();
      if (forwarder.state == ClientNotifForwarder.STARTING) forwarder.setState(ClientNotifForwarder.STARTED);
    }
    if (forwarder.defaultClassLoader != null) {
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
          Thread.currentThread().setContextClassLoader(forwarder.defaultClassLoader);
          return null;
        }
      });
    }
    while (!shouldStop()) {
      NotificationResult nr = fetchNotifs();
      if (nr == null) break; // nr == null means got exception
      final TargetedNotification[] notifs = nr.getTargetedNotifications();
      final int len = notifs.length;
      final Map<Integer, ListenerInfo> listeners;
      final Integer myListenerID;
      long missed = 0;
      synchronized (forwarder) {
        if (forwarder.clientSequenceNumber >= 0) missed = nr.getEarliestSequenceNumber() - forwarder.clientSequenceNumber; // check sequence number
        forwarder.clientSequenceNumber = nr.getNextSequenceNumber();
        final int size = forwarder.infoList.size();
        listeners = new HashMap<>(((size > len) ? len : size));
        for (int i = 0; i < len; i++) {
          final TargetedNotification tn = notifs[i];
          final Integer listenerID = tn.getListenerID();
          if (!listenerID.equals(forwarder.mbeanRemovedNotifID)) { // check if an mbean unregistration notif
            final ListenerInfo li = forwarder.infoList.get(listenerID);
            if (li != null) listeners.put(listenerID, li);
            continue;
          }
          final Notification notif = tn.getNotification();
          final String unreg = MBeanServerNotification.UNREGISTRATION_NOTIFICATION;
          if (notif instanceof MBeanServerNotification && notif.getType().equals(unreg)) {
            MBeanServerNotification mbsn = (MBeanServerNotification) notif;
            ObjectName name = mbsn.getMBeanName();
            forwarder.removeNotificationListener(name);
          }
        }
        myListenerID = forwarder.mbeanRemovedNotifID;
      }
      if (missed > 0) {
        final String msg = "May have lost up to " + missed + " notification" + (missed == 1 ? "" : "s");
        forwarder.lostNotifs(msg, missed);
        logger.trace("NotifFetcher.run", msg);
      }
      for (int i = 0; i < len; i++) { // forward
        final TargetedNotification tn = notifs[i];
        dispatchNotification(tn, myListenerID, listeners);
      }
    }
    forwarder.setState(ClientNotifForwarder.STOPPED); // tell that the thread is REALLY stopped
  }

  /**
   * @param tn .
   * @param myListenerID .
   * @param listeners .
   */
  void dispatchNotification(final TargetedNotification tn, final Integer myListenerID, final Map<Integer, ListenerInfo> listeners) {
    final Notification notif = tn.getNotification();
    final Integer listenerID = tn.getListenerID();
    if (listenerID.equals(myListenerID)) return;
    final ListenerInfo li = listeners.get(listenerID);
    if (li == null) {
      logger.trace("NotifFetcher.dispatch", "Listener ID not in map");
      return;
    }
    NotificationListener l = li.getListener();
    Object h = li.getHandback();
    try {
      l.handleNotification(notif, h);
    } catch (RuntimeException e) {
      final String msg = "Failed to forward a notification " + "to a listener";
      logger.trace("NotifFetcher-run", msg, e);
    }
  }

  /**
   * @return .
   */
  private NotificationResult fetchNotifs() {
    try {
      NotificationResult nr = forwarder.fetchNotifs(forwarder.clientSequenceNumber, forwarder.maxNotifications, forwarder.timeout);
      if (logger.traceOn()) logger.trace("NotifFetcher-run", "Got notifications from the server: " + nr);
      return nr;
    } catch (ClassNotFoundException|NotSerializableException e) {
      logger.trace("NotifFetcher.fetchNotifs", e);
      return fetchOneNotif();
    } catch (IOException ioe) {
      if (!shouldStop()) logger.debug("NotifFetcher-run", ioe);
      return null; // no more fetching
    }
  }

  /**
   * Fetch one notification when we suspect that it might be a notification that we can't deserialize (because of a missing class). First we ask for 0 notifications with 0 timeout.
   * This allows us to skip sequence numbers for notifications that don't match our filters. Then we ask for one notification. If that produces a ClassNotFoundException or a
   * NotSerializableException, we increase our sequence number and ask again. Eventually we will either get a successful notification, or a return with 0 notifications.
   * In either case we can return a NotificationResult. This algorithm works (albeit less well) even if the server implementation doesn't optimize a request for 0 notifications
   * to skip sequence numbers for notifications that don't match our filters. If we had at least one ClassNotFoundException, then we must emit a JMXConnectionNotification.LOST_NOTIFS.
   * @return .
   */
  private NotificationResult fetchOneNotif() {
    long startSequenceNumber = forwarder.clientSequenceNumber;
    int notFoundCount = 0;
    NotificationResult result = null;
    while (result == null && !shouldStop()) {
      NotificationResult nr;
      try {
        // 0 notifs to update startSequenceNumber
        nr = forwarder.fetchNotifs(startSequenceNumber, 0, 0L);
      } catch (ClassNotFoundException e) {
        logger.warning("NotifFetcher.fetchOneNotif", "Impossible exception: " + e);
        logger.debug("NotifFetcher.fetchOneNotif", e);
        return null;
      } catch (IOException e) {
        if (!shouldStop()) logger.trace("NotifFetcher.fetchOneNotif", e);
        return null;
      }
      if (shouldStop()) return null;
      startSequenceNumber = nr.getNextSequenceNumber();
      try {
        // 1 notif to skip possible missing class
        result = forwarder.fetchNotifs(startSequenceNumber, 1, 0L);
      } catch (Exception e) {
        if (e instanceof ClassNotFoundException || e instanceof NotSerializableException) {
          logger.warning("NotifFetcher.fetchOneNotif", "Failed to deserialize a notification: " + e.toString());
          if (logger.traceOn()) logger.trace("NotifFetcher.fetchOneNotif", "Failed to deserialize a notification.", e);
          notFoundCount++;
          startSequenceNumber++;
        } else {
          if (!shouldStop()) logger.trace("NotifFetcher.fetchOneNotif", e);
          return null;
        }
      }
    }
    if (notFoundCount > 0) {
      final String msg = "Dropped " + notFoundCount + " notification" + (notFoundCount == 1 ? "" : "s") + " because classes were missing locally";
      forwarder.lostNotifs(msg, notFoundCount);
    }
    return result;
  }

  /**
   * @return .
   */
  private boolean shouldStop() {
    synchronized (forwarder) {
      if (forwarder.state != ClientNotifForwarder.STARTED) return true;
      else if (forwarder.infoList.size() == 0) { // no more listener, stop fetching
        forwarder.setState(ClientNotifForwarder.STOPPING);
        return true;
      }
      return false;
    }
  }
}
