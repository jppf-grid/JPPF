/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
/*
 * @(#)ClientNotifForwarder.java	1.3
 *
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms of either the GNU General
 * Public License Version 2 only ("GPL") or the Common Development and
 * Distribution License("CDDL")(collectively, the "License"). You may not use
 * this file except in compliance with the License. You can obtain a copy of the
 * License at http://opendmk.dev.java.net/legal_notices/licenses.txt or in the
 * LEGAL_NOTICES folder that accompanied this code. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file found at
 *     http://opendmk.dev.java.net/legal_notices/licenses.txt
 * or in the LEGAL_NOTICES folder that accompanied this code.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.
 *
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 *
 *       "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding
 *
 *       "[Contributor] elects to include this software in this distribution
 *        under the [CDDL or GPL Version 2] license."
 *
 * If you don't indicate a single choice of license, a recipient has the option
 * to distribute your version of this file under either the CDDL or the GPL
 * Version 2, or to extend the choice of license to its licensees as provided
 * above. However, if you add GPL Version 2 code and therefore, elected the
 * GPL Version 2 license, then the option applies only if the new code is made
 * subject to such option by the copyright holder.
 *
 */

package com.sun.jmx.remote.opt.internal;

import java.io.*;
import java.security.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.opt.util.*;

/**
 *
 */
public abstract class ClientNotifForwarder {
  /**
   */
  private final ClassLoader defaultClassLoader;
  /**
   */
  private final Map<Integer, ListenerInfo> infoList = new HashMap<>();
  /**
   */
  private long clientSequenceNumber = -1;
  /**
   */
  private final int maxNotifications;
  /**
   */
  private final long timeout;
  /**
   */
  private NotifFetcher notifFetcher;
  /**
   */
  private Integer mbeanRemovedNotifID = null;
  /**
   */
  private Thread currentFetchThread;
  /**
   */
  private boolean inited = false;
  /**
   * This state means that a thread is being created for fetching and forwarding notifications.
   */
  private static final int STARTING = 0;
  /**
   * This state tells that a thread has been started for fetching and forwarding notifications.
   */
  private static final int STARTED = 1;
  /**
   * This state means that the fetching thread is informed to stop.
   */
  private static final int STOPPING = 2;
  /**
   * This state means that the fetching thread is already stopped.
   */
  private static final int STOPPED = 3;
  /**
   * This state means that this object is terminated and no more thread will be created for fetching notifications.
   */
  private static final int TERMINATED = 4;
  /**
   */
  private int state = STOPPED;
  /**
   * This variable is used to tell whether a connector (RMIConnector or ClientIntermediary) is doing reconnection. This variable will be set to true by the method <code>preReconnection</code>, and set
   * fase by <code>postReconnection</code>. When beingReconnected == true, no thread will be created for fetching notifications.
   */
  private boolean beingReconnected = false;
  /**
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "ClientNotifForwarder");

  /**
   * @param env .
   */
  public ClientNotifForwarder(final Map<String, ?> env) {
    this(null, env);
  }

  /**
   * @param defaultClassLoader .
   * @param env .
   */
  public ClientNotifForwarder(final ClassLoader defaultClassLoader, final Map<String, ?> env) {
    maxNotifications = EnvHelp.getMaxFetchNotifNumber(env);
    timeout = EnvHelp.getFetchTimeout(env);
    this.defaultClassLoader = defaultClassLoader;
  }

  /**
   * Called to to fetch notifications from a server.
   * @param clientSequenceNumber .
   * @param maxNotifications .
   * @param timeout .
   * @return .
   * @throws IOException .
   * @throws ClassNotFoundException .
   */
  abstract protected NotificationResult fetchNotifs(long clientSequenceNumber, int maxNotifications, long timeout) throws IOException, ClassNotFoundException;

  /**
   * @return .
   * @throws IOException .
   * @throws InstanceNotFoundException .
   */
  abstract protected Integer addListenerForMBeanRemovedNotif() throws IOException, InstanceNotFoundException;

  /**
   * @param id .
   * @throws IOException .
   * @throws InstanceNotFoundException .
   * @throws ListenerNotFoundException .
   */
  abstract protected void removeListenerForMBeanRemovedNotif(Integer id) throws IOException, InstanceNotFoundException, ListenerNotFoundException;

  /**
   * Used to send out a notification about lost notifs
   * @param message .
   * @param number .
   */
  abstract protected void lostNotifs(String message, long number);

  /**
   * @param listenerID .
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @param delegationSubject .
   * @throws IOException .
   * @throws InstanceNotFoundException .
   */
  public synchronized void addNotificationListener(final Integer listenerID, final ObjectName name, final NotificationListener listener,
      final NotificationFilter filter, final Object handback, final Subject delegationSubject) throws IOException, InstanceNotFoundException {
    if (logger.traceOn()) logger.trace("addNotificationListener", "Add the listener " + listener + " at " + name);
    infoList.put(listenerID, new ClientListenerInfo(listenerID, name, listener, filter, handback, delegationSubject));
    init(false);
  }

  /**
   * @param name .
   * @param listener .
   * @return .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public synchronized Integer[] removeNotificationListener(final ObjectName name, final NotificationListener listener) throws ListenerNotFoundException, IOException {
    beforeRemove();
    if (logger.traceOn()) logger.trace("removeNotificationListener", "Remove the listener " + listener + " from " + name);
    ArrayList<Integer> ids = new ArrayList<>();
    List<ListenerInfo> values = new ArrayList<>(infoList.values());
    for (int i = values.size() - 1; i >= 0; i--) {
      ClientListenerInfo li = (ClientListenerInfo) values.get(i);
      if (li.sameAs(name, listener)) {
        ids.add(li.getListenerID());
        infoList.remove(li.getListenerID());
      }
    }
    if (ids.isEmpty()) throw new ListenerNotFoundException("Listener not found");
    return ids.toArray(new Integer[0]);
  }

  /**
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @return .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public synchronized Integer removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException, IOException {
    if (logger.traceOn()) logger.trace("removeNotificationListener", "Remove the listener " + listener + " from " + name);
    beforeRemove();
    Integer id = null;
    List<ListenerInfo> values = new ArrayList<>(infoList.values());
    for (int i = values.size() - 1; i >= 0; i--) {
      ClientListenerInfo li = (ClientListenerInfo) values.get(i);
      if (li.sameAs(name, listener, filter, handback)) {
        id = li.getListenerID();
        infoList.remove(id);
        break;
      }
    }
    if (id == null) throw new ListenerNotFoundException("Listener not found");
    return id;
  }

  /**
   * @param name .
   * @return .
   */
  public synchronized Integer[] removeNotificationListener(final ObjectName name) {
    if (logger.traceOn()) logger.trace("removeNotificationListener", "Remove all listeners registered at " + name);
    List<Integer> ids = new ArrayList<>();
    List<ListenerInfo> values = new ArrayList<>(infoList.values());
    for (int i = values.size() - 1; i >= 0; i--) {
      ClientListenerInfo li = (ClientListenerInfo) values.get(i);
      if (li.sameAs(name)) {
        ids.add(li.getListenerID());
        infoList.remove(li.getListenerID());
      }
    }
    return ids.toArray(new Integer[ids.size()]);
  }

  /**
   * @return .
   */
  public synchronized ListenerInfo[] getListenerInfo() {
    return infoList.values().toArray(new ListenerInfo[infoList.size()]);
  }

  /**
   * Called when a connector is doing reconnection. Like <code>postReconnection</code>, this method is intended to be called only by a client connetor: <code>RMIConnector</code> and
   * <code>ClientIntermediary</code>. Call this method will set the flag beingReconnection to <code>true</code>, and the thread used to fetch notifis will be stopped, a new thread can be created only
   * after the method <code>postReconnection</code> is called. It is caller's responsiblity to not re-call this method before calling <code>postReconnection.
   * @return .
   * @throws IOException .
   */
  public synchronized ClientListenerInfo[] preReconnection() throws IOException {
    if (state == TERMINATED || beingReconnected) throw new IOException("Illegal state."); // should never
    final ClientListenerInfo[] tmp = infoList.values().toArray(new ClientListenerInfo[0]);
    beingReconnected = true;
    infoList.clear();
    if (currentFetchThread == Thread.currentThread()) {
      // we do not need to stop the fetching thread, because this thread is used to do restarting and it will not be used to do fetching during the re-registering the listeners.
      return tmp;
    }
    while (state == STARTING) {
      try {
        wait();
      } catch (InterruptedException ire) {
        IOException ioe = new IOException(ire.toString());
        EnvHelp.initCause(ioe, ire);
        throw ioe;
      }
    }
    if (state == STARTED) setState(STOPPING);
    return tmp;
  }

  /**
   * Called after reconnection is finished. This method is intended to be called only by a client connetor: <code>RMIConnector</code/> and <code/>ClientIntermediary</code>.
   * @param listenerInfos .
   * @throws IOException .
   */
  public synchronized void postReconnection(final ClientListenerInfo[] listenerInfos) throws IOException {
    if (state == TERMINATED) return;
    while (state == STOPPING) {
      try {
        wait();
      } catch (InterruptedException ire) {
        throw new IOException(ire);
      }
    }
    final boolean trace = logger.traceOn();
    final int len = listenerInfos.length;
    for (int i = 0; i < len; i++) {
      if (trace) logger.trace("addNotificationListeners", "Add a listener at " + listenerInfos[i].getListenerID());
      infoList.put(listenerInfos[i].getListenerID(), listenerInfos[i]);
    }
    beingReconnected = false;
    notifyAll();
    if (currentFetchThread == Thread.currentThread()) {
      try { // no need to init, simply get the id
        mbeanRemovedNotifID = addListenerForMBeanRemovedNotif();
      } catch (Exception e) {
        final String msg = "Failed to register a listener to the mbean " + "server: the client will not do clean when an MBean " + "is unregistered";
        if (logger.traceOn()) logger.trace("init", msg, e);
      }
    } else if (listenerInfos.length > 0) init(true); // old listeners re-registered
    else if (infoList.size() > 0) init(false); // but new listeners registered during reconnection
  }

  /**
   * 
   */
  public synchronized void terminate() {
    if (state == TERMINATED) return;
    if (logger.traceOn()) logger.trace("terminate", "Terminating...");
    if (state == STARTED) infoList.clear();
    setState(TERMINATED);
  }

  /**
   * 
   */
  private class NotifFetcher implements Runnable {
    @Override
    public void run() {
      synchronized (ClientNotifForwarder.this) {
        currentFetchThread = Thread.currentThread();
        if (state == STARTING) setState(STARTED);
      }
      if (defaultClassLoader != null) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
          @Override
          public Object run() {
            Thread.currentThread().setContextClassLoader(defaultClassLoader);
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
        synchronized (ClientNotifForwarder.this) {
          if (clientSequenceNumber >= 0) missed = nr.getEarliestSequenceNumber() - clientSequenceNumber; // check sequence number
          clientSequenceNumber = nr.getNextSequenceNumber();
          final int size = infoList.size();
          listeners = new HashMap<>(((size > len) ? len : size));
          for (int i = 0; i < len; i++) {
            final TargetedNotification tn = notifs[i];
            final Integer listenerID = tn.getListenerID();
            if (!listenerID.equals(mbeanRemovedNotifID)) { // check if an mbean unregistration notif
              final ListenerInfo li = infoList.get(listenerID);
              if (li != null) listeners.put(listenerID, li);
              continue;
            }
            final Notification notif = tn.getNotification();
            final String unreg = MBeanServerNotification.UNREGISTRATION_NOTIFICATION;
            if (notif instanceof MBeanServerNotification && notif.getType().equals(unreg)) {
              MBeanServerNotification mbsn = (MBeanServerNotification) notif;
              ObjectName name = mbsn.getMBeanName();
              removeNotificationListener(name);
            }
          }
          myListenerID = mbeanRemovedNotifID;
        }
        if (missed > 0) {
          final String msg = "May have lost up to " + missed + " notification" + (missed == 1 ? "" : "s");
          lostNotifs(msg, missed);
          logger.trace("NotifFetcher.run", msg);
        }
        for (int i = 0; i < len; i++) { // forward
          final TargetedNotification tn = notifs[i];
          dispatchNotification(tn, myListenerID, listeners);
        }
      }
      setState(STOPPED); // tell that the thread is REALLY stopped
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
        NotificationResult nr = ClientNotifForwarder.this.fetchNotifs(clientSequenceNumber, maxNotifications, timeout);
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
     * Fetch one notification when we suspect that it might be a notification that we can't deserialize (because of a missing class). First we ask for 0 notifications with 0 timeout. This allows us to
     * skip sequence numbers for notifications that don't match our filters. Then we ask for one notification. If that produces a ClassNotFoundException or a NotSerializableException, we increase our
     * sequence number and ask again. Eventually we will either get a successful notification, or a return with 0 notifications. In either case we can return a NotificationResult. This algorithm works
     * (albeit less well) even if the server implementation doesn't optimize a request for 0 notifications to skip sequence numbers for notifications that don't match our filters.
     * If we had at least one ClassNotFoundException, then we must emit a JMXConnectionNotification.LOST_NOTIFS.
     * @return .
     */
    private NotificationResult fetchOneNotif() {
      ClientNotifForwarder cnf = ClientNotifForwarder.this;
      long startSequenceNumber = clientSequenceNumber;
      int notFoundCount = 0;
      NotificationResult result = null;
      while (result == null && !shouldStop()) {
        NotificationResult nr;
        try {
          // 0 notifs to update startSequenceNumber
          nr = cnf.fetchNotifs(startSequenceNumber, 0, 0L);
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
          result = cnf.fetchNotifs(startSequenceNumber, 1, 0L);
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
        lostNotifs(msg, notFoundCount);
      }
      return result;
    }

    /**
     * @return .
     */
    private boolean shouldStop() {
      synchronized (ClientNotifForwarder.this) {
        if (state != STARTED) return true;
        else if (infoList.size() == 0) { // no more listener, stop fetching
          setState(STOPPING);
          return true;
        }
        return false;
      }
    }
  }

  /**
   * @param newState .
   */
  private synchronized void setState(final int newState) {
    if (state == TERMINATED) return;
    state = newState;
    this.notifyAll();
  }

  /**
   * Called to decide whether need to start a thread for fetching notifs. <P>The parameter reconnected will decide whether to initilize the clientSequenceNumber, initilaizing the clientSequenceNumber
   * means to ignore all notifications arrived before. If it is reconnected, we will not initialize in order to get all notifications arrived during the reconnection. It may cause the newly registered
   * listeners to receive some notifications arrived before its registray.
   * @param reconnected .
   * @throws IOException .
   */
  private synchronized void init(final boolean reconnected) throws IOException {
    switch (state) {
      case STARTED: return;
      case STARTING: return;
      case TERMINATED: throw new IOException("The ClientNotifForwarder has been terminated.");
      case STOPPING:
        if (beingReconnected == true) return; // wait for another thread to do, which is doing reconnection
        while (state == STOPPING) { // make sure only one fetching thread.
          try {
            wait();
          } catch (InterruptedException ire) {
            throw new IOException(ire);
          }
        }
        init(reconnected); // re-call this method to check the state again, the state can be other value like TERMINATED.
        return;
      case STOPPED:
        if (beingReconnected == true) return; // wait for another thread to do, which is doing reconnection
        if (logger.traceOn()) logger.trace("init", "Initializing...");
        if (!reconnected) { // init the clientSequenceNumber if not reconnected
          try {
            NotificationResult nr = fetchNotifs(-1, 0, 0);
            clientSequenceNumber = nr.getNextSequenceNumber();
          } catch (ClassNotFoundException e) {
            logger.warning("init", "Impossible exception: " + e);
            logger.debug("init", e);
          }
        }
        try { // for cleaning
          mbeanRemovedNotifID = addListenerForMBeanRemovedNotif();
        } catch (Exception e) {
          final String msg = "Failed to register a listener to the mbean " + "server: the client will not do clean when an MBean " + "is unregistered";
          if (logger.traceOn()) logger.trace("init", msg, e);
        }
        setState(STARTING); // start fetching
        notifFetcher = new NotifFetcher();
        Thread t = new Thread(notifFetcher);
        t.setDaemon(true);
        t.start();
        return;
      default: throw new IOException("Unknown state."); // should not
    }
  }

  /**
   * Import: should not remove a listener dureing reconnection, the reconnection needs to change the listener list and that will possibly make removal fail.
   * @throws IOException .
   */
  private synchronized void beforeRemove() throws IOException {
    while (beingReconnected) {
      if (state == TERMINATED) throw new IOException("Terminated.");
      try {
        wait();
      } catch (InterruptedException ire) {
        throw new IOException(ire);
      }
    }
    if (state == TERMINATED) throw new IOException("Terminated.");
  }
}
