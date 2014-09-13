/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
 * @(#)ArrayNotificationBuffer.java	1.3
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
 *
 */

package com.sun.jmx.remote.opt.internal;

import java.security.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import com.sun.jmx.remote.opt.util.*;

/** A circular buffer of notifications received from an MBean server. */
public final class ArrayNotificationBuffer implements NotificationBuffer {
  /** */
  private boolean disposed = false;
  // FACTORY STUFF, INCLUDING SHARING
  /** */
  private static final HashMap<MBeanServer,ArrayNotificationBuffer> mbsToBuffer = new HashMap<>(1);
  /** */
  private final Collection<ArrayNotificationBufferAux.ShareBuffer> sharers = new HashSet<>(1);
  /** */
  private final MBeanServer mBeanServer;
  /** */
  private final ArrayQueue<ArrayNotificationBufferAux.NamedNotification> queue;
  /** */
  private int queueSize;
  /** */
  private long earliestSequenceNumber;
  /** */
  private long nextSequenceNumber;
  /** */
  private Set<ObjectName> createdDuringQuery;
  /** */
  static final String broadcasterClass = NotificationBroadcaster.class.getName();
  /** */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "ArrayNotificationBuffer");
  /** */
  private final NotificationListener bufferListener;
  /** */
  private static final QueryExp broadcasterQuery = new ArrayNotificationBufferAux.BroadcasterQuery();
  /** */
  private static final NotificationFilter creationFilter;
  static {
    NotificationFilterSupport nfs = new NotificationFilterSupport();
    nfs.enableType(MBeanServerNotification.REGISTRATION_NOTIFICATION);
    creationFilter = nfs;
  }
  /** */
  private final NotificationListener creationListener = new NotificationListener() {
    @Override
    public void handleNotification(final Notification notif, final Object handback) {
      logger.debug("creationListener", "handleNotification called");
      createdNotification((MBeanServerNotification) notif);
    }
  };
  /** */
  private static final ObjectName delegateName;
  static {
    try {
      delegateName = ObjectName.getInstance("JMImplementation:" + "type=MBeanServerDelegate");
    } catch (MalformedObjectNameException e) {
      RuntimeException re = new RuntimeException("Can't create delegate name: " + e);
      EnvHelp.initCause(re, e);
      logger.error("<init>", "Can't create delegate name: " + e);
      logger.debug("<init>", e);
      throw re;
    }
  }

  /**
   * @param mbs .
   * @param env .
   * @return .
   */
  public static synchronized NotificationBuffer getNotificationBuffer(final MBeanServer mbs, final Map<String, ?> env) {
    int queueSize = EnvHelp.getNotifBufferSize(env); //Find out queue size
    ArrayNotificationBuffer buf = mbsToBuffer.get(mbs);
    if (buf == null) {
      buf = new ArrayNotificationBuffer(mbs, queueSize);
      mbsToBuffer.put(mbs, buf);
    }
    return new ArrayNotificationBufferAux.ShareBuffer(buf, queueSize);
  }

  /**
   * @param mbs .
   */
  public static synchronized void removeNotificationBuffer(final MBeanServer mbs) {
    mbsToBuffer.remove(mbs);
  }

  /**
   * @param sharer .
   */
  synchronized void addSharer(final ArrayNotificationBufferAux.ShareBuffer sharer) {
    if (sharer.getSize() > queueSize) resize(sharer.getSize());
    sharers.add(sharer);
  }

  /**
   * @param sharer .
   */
  void removeSharer(final ArrayNotificationBufferAux.ShareBuffer sharer) {
    boolean empty;
    synchronized (this) {
      sharers.remove(sharer);
      empty = sharers.isEmpty();
      if (!empty) {
        int max = 0;
        for (ArrayNotificationBufferAux.ShareBuffer buf: sharers) {
          int bufsize = buf.getSize();
          if (bufsize > max) max = bufsize;
        }
        if (max < queueSize) resize(max);
      }
    }
    if (empty) dispose();
  }

  /**
   * @param newSize .
   */
  private void resize(final int newSize) {
    if (newSize == queueSize) return;
    while (queue.size() > newSize) dropNotification();
    queue.resize(newSize);
    queueSize = newSize;
  }

  // ARRAYNOTIFICATIONBUFFER IMPLEMENTATION
  /**
   * @param mbs .
   * @param queueSize .
   */
  private ArrayNotificationBuffer(final MBeanServer mbs, final int queueSize) {
    if (logger.traceOn()) logger.trace("Constructor", "queueSize=" + queueSize);
    if (mbs == null || queueSize < 1) throw new IllegalArgumentException("Bad args");
    this.bufferListener = new ArrayNotificationBufferAux.BufferListener(this);
    this.mBeanServer = mbs;
    this.queueSize = queueSize;
    this.queue = new ArrayQueue<>(ArrayNotificationBufferAux.NamedNotification.class, queueSize);
    this.earliestSequenceNumber = System.currentTimeMillis();
    this.nextSequenceNumber = this.earliestSequenceNumber;
    createListeners();
    logger.trace("Constructor", "ends");
  }

  /**
   * @return .
   */
  private synchronized boolean isDisposed() {
    return disposed;
  }

  @Override
  public void dispose() {
    logger.trace("dispose", "starts");
    synchronized (this) {
      removeNotificationBuffer(mBeanServer);
      disposed = true;
      notifyAll(); //Notify potential waiting fetchNotification call
    }
    destroyListeners();
    logger.trace("dispose", "ends");
  }

  @Override
  public NotificationResult fetchNotifications(final Set<ListenerInfo> listeners, final long startSequenceNumber, final long timeout, final int maxNotif) throws InterruptedException {
    logger.trace("fetchNotifications", "starts");
    int maxNotifications = maxNotif;
    if (startSequenceNumber < 0 || isDisposed()) {
      synchronized (this) {
        return new NotificationResult(earliestSequenceNumber(), nextSequenceNumber(), new TargetedNotification[0]);
      }
    }
    if (listeners == null || startSequenceNumber < 0 || timeout < 0 || maxNotifications < 0) {
      logger.trace("fetchNotifications", "Bad args");
      throw new IllegalArgumentException("Bad args to fetch");
    }
    if (logger.debugOn()) logger.trace("fetchNotifications", "listener-length=" + listeners.size() + "; startSeq=" + startSequenceNumber + "; timeout=" + timeout + "; max=" + maxNotifications);
    if (startSequenceNumber > nextSequenceNumber()) {
      final String msg = "Start sequence number too big: " + startSequenceNumber + " > " + nextSequenceNumber();
      logger.trace("fetchNotifications", msg);
      throw new IllegalArgumentException(msg);
    }
    long endTime = System.currentTimeMillis() + timeout;
    if (endTime < 0) endTime = Long.MAX_VALUE;  // overflow
    if (logger.debugOn()) logger.debug("fetchNotifications", "endTime=" + endTime);
    long earliestSeq = -1; // Set earliestSeq the first time through the loop. If we set it here, notifications could be dropped before we started examining them, so earliestSeq might not correspond to the earliest notification we examined.
    long nextSeq = startSequenceNumber;
    List<TargetedNotification> notifs = new ArrayList<>();
    while (true) { // On exit from this loop, notifs, earliestSeq, and nextSeq must all be correct values for the returned NotificationResult.
      logger.debug("fetchNotifications", "main loop starts");
      ArrayNotificationBufferAux.NamedNotification candidate;
      synchronized (this) { // Get the next available notification regardless of filters, or wait for one to arrive if there is none.
        if (earliestSeq < 0) { // First time through. The current earliestSequenceNumber is the first one we could have examined.
          earliestSeq = earliestSequenceNumber();
          if (logger.debugOn()) logger.debug("fetchNotifications", "earliestSeq=" + earliestSeq);
          if (nextSeq < earliestSeq) {
            nextSeq = earliestSeq;
            logger.debug("fetchNotifications", "nextSeq=earliestSeq");
          }
        } else earliestSeq = earliestSequenceNumber();
        if (nextSeq < earliestSeq) { // If many notifications were dropped since last time through, nextSeq could be earlier than the current earliest. If so, notifications may have been lost and we return now so the caller can see this next time it calls.
          logger.trace("fetchNotifications", "nextSeq=" + nextSeq + " < " + "earliestSeq=" + earliestSeq + " so may have lost notifs");
          break;
        }
        if (nextSeq < nextSequenceNumber()) {
          candidate = notificationAt(nextSeq);
          if (logger.debugOn()) logger.debug("fetchNotifications", "candidate: " + candidate + ", nextSeq now " + nextSeq);
        } else {
          if (notifs.size() > 0) { // nextSeq is the largest sequence number. If we already got notifications, return them now. Otherwise wait for some to arrive, with timeout.
            logger.debug("fetchNotifications", "no more notifs but have some so don't wait");
            break;
          }
          long toWait = endTime - System.currentTimeMillis();
          if (toWait <= 0) {
            logger.debug("fetchNotifications", "timeout");
            break;
          }
          if (isDisposed()) { // dispose called
            if (logger.debugOn()) logger.debug("fetchNotifications", "dispose callled, no wait");
            return new NotificationResult(earliestSequenceNumber(), nextSequenceNumber(), new TargetedNotification[0]);
          }
          if (logger.debugOn()) logger.debug("fetchNotifications", "wait(" + toWait + ")");
          wait(toWait);
          continue;
        }
      }
      List<TargetedNotification> matchedNotifs = matchNotifs(candidate, listeners);
      if (matchedNotifs.size() > 0) {
        if (maxNotifications <= 0) { // We only check the max size now, so that our returned nextSeq is as large as possible, preventing the caller from thinking it missed interesting notifications when we knew they weren't.
          logger.debug("fetchNotifications", "reached maxNotifications");
          break;
        }
        --maxNotifications;
        if (logger.debugOn()) logger.debug("fetchNotifications", "add: " + matchedNotifs);
        notifs.addAll(matchedNotifs);
      }
      ++nextSeq;
    }
    TargetedNotification[] resultNotifs = notifs.toArray(new TargetedNotification[notifs.size()]);
    NotificationResult nr = new NotificationResult(earliestSeq, nextSeq, resultNotifs);
    if (logger.debugOn()) logger.debug("fetchNotifications", nr.toString());
    logger.trace("fetchNotifications", "ends");
    return nr;
  }

  /**
   * We have a candidate notification. See if it matches our filters. We do this outside the synchronized block so we don't hold up everyone accessing the buffer
   * (including notification senders) while we evaluate potentially slow filters.
   * @param candidate .
   * @param listeners .
   * @return .
   */
  private List<TargetedNotification> matchNotifs(final ArrayNotificationBufferAux.NamedNotification candidate, final Set<ListenerInfo> listeners) {
    Notification notif = candidate.getNotification();
    List<TargetedNotification> matchedNotifs = new ArrayList<>();
    logger.debug("fetchNotifications", "applying filters to candidate");
    synchronized (listeners) {
      for (ListenerInfo li: listeners) {
        NotificationFilter filter = li.getNotificationFilter();
        if (logger.debugOn()) logger.debug("fetchNotifications", "pattern=<" + li.getObjectName() + ">; filter=" + filter);
        if (li.getObjectName().apply(candidate.getObjectName())) {
          logger.debug("fetchNotifications", "pattern matches");
          if (filter == null || filter.isNotificationEnabled(notif)) {
            logger.debug("fetchNotifications", "filter matches");
            matchedNotifs.add(new TargetedNotification(notif, li.getListenerID()));
          }
        }
      }
    }
    return matchedNotifs;
  }

  /**
   * @return .
   */
  synchronized long earliestSequenceNumber() {
    return earliestSequenceNumber;
  }

  /**
   * @return .
   */
  synchronized long nextSequenceNumber() {
    return nextSequenceNumber;
  }

  /**
   * @param notif .
   */
  synchronized void addNotification(final ArrayNotificationBufferAux.NamedNotification notif) {
    if (logger.traceOn()) logger.trace("addNotification", notif.toString());
    while (queue.size() >= queueSize) {
      dropNotification();
      if (logger.debugOn()) logger.debug("addNotification", "dropped oldest notif, earliestSeq=" + earliestSequenceNumber);
    }
    queue.add(notif);
    nextSequenceNumber++;
    if (logger.debugOn()) logger.debug("addNotification", "nextSeq=" + nextSequenceNumber);
    notifyAll();
  }

  /** */
  private void dropNotification() {
    queue.remove(0);
    earliestSequenceNumber++;
  }

  /**
   * @param seqNo .
   * @return .
   */
  synchronized ArrayNotificationBufferAux.NamedNotification notificationAt(final long seqNo) {
    long index = seqNo - earliestSequenceNumber;
    if (index < 0 || index > Integer.MAX_VALUE) {
      final String msg = "Bad sequence number: " + seqNo + " (earliest " + earliestSequenceNumber + ")";
      logger.trace("notificationAt", msg);
      throw new IllegalArgumentException(msg);
    }
    return queue.get((int) index);
  }

  /**
   * <p>Add our listener to every NotificationBroadcaster MBean currently in the MBean server and to every NotificationBroadcaster later created.
   * <p>It would be really nice if we could just do mbs.addNotificationListener(new ObjectName("*:*"), ...); Definitely something for the next version of JMX.
   * <p>There is a nasty race condition that we must handle. We first register for MBean-creation notifications so we can add listeners to new
   * MBeans, then we query the existing MBeans to add listeners to them. The problem is that a new MBean could arrive after we register for
   * creations but before the query has completed. Then we could see the MBean both in the query and in an MBean-creation notification, and we
   * would end up registering our listener twice.
   * <p>To solve this problem, we arrange for new MBeans that arrive while the query is being done to be added to the Set createdDuringQuery
   * and we do not add a listener immediately. When the query is done, we atomically turn off the addition of new names to createdDuringQuery
   * and add all the names that were there to the result of the query. Since we are dealing with Sets, the result is the same whether or not
   * the newly-created MBean was included in the query result.
   * <p>It is important not to hold any locks during the operation of adding listeners to MBeans. An MBean's addNotificationListener can be
   * arbitrary user code, and this could deadlock with any locks we hold (see bug 6239400). The corollary is that we must not do any operations
   * in this method or the methods it calls that require locks.
   */
  private void createListeners() {
    logger.debug("createListeners", "starts");
    synchronized (this) {
      createdDuringQuery = new HashSet<>();
    }
    try {
      addNotificationListener(delegateName, creationListener, creationFilter, null);
      logger.debug("createListeners", "added creationListener");
    } catch (Exception e) {
      final String msg = "Can't add listener to MBean server delegate: ";
      RuntimeException re = new IllegalArgumentException(msg + e);
      EnvHelp.initCause(re, e);
      logger.fine("createListeners", msg + e);
      logger.debug("createListeners", e);
      throw re;
    }
    // Spec doesn't say whether Set returned by QueryNames can be modified so we clone it.
    Set<ObjectName> names = queryNames(null, broadcasterQuery);
    names = new HashSet<>(names);
    synchronized (this) {
      names.addAll(createdDuringQuery);
      createdDuringQuery = null;
    }
    for (ObjectName name: names) addBufferListener(name);
    logger.debug("createListeners", "ends");
  }

  /**
   * @param name .
   */
  private void addBufferListener(final ObjectName name) {
    if (logger.debugOn()) logger.debug("addBufferListener", name.toString());
    try {
      addNotificationListener(name, bufferListener, null, name);
    } catch (Exception e) {
      logger.trace("addBufferListener", e);
      // This can happen if the MBean was unregistered just after the query. Or user NotificationBroadcaster might throw unexpected exception.
    }
  }

  /**
   * @param name .
   */
  private void removeBufferListener(final ObjectName name) {
    if (logger.debugOn()) logger.debug("removeBufferListener", name.toString());
    try {
      removeNotificationListener(name, bufferListener);
    } catch (Exception e) {
      logger.trace("removeBufferListener", e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @throws Exception .
   */
  private void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run() throws InstanceNotFoundException {
          mBeanServer.addNotificationListener(name, listener, filter, handback);
          return null;
        }
      });
    } catch (Exception e) {
      throw extractException(e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @throws Exception .
   */
  private void removeNotificationListener(final ObjectName name, final NotificationListener listener) throws Exception {
    try {
      AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run() throws Exception {
          mBeanServer.removeNotificationListener(name, listener);
          return null;
        }
      });
    } catch (Exception e) {
      throw extractException(e);
    }
  }

  /**
   * @param name .
   * @param query .
   * @return .
   */
  private Set<ObjectName> queryNames(final ObjectName name, final QueryExp query) {
    PrivilegedAction<Set<ObjectName>> act = new PrivilegedAction<Set<ObjectName>>() {
      @Override
      public Set<ObjectName> run() {
        return mBeanServer.queryNames(name, query);
      }
    };
    try {
      return AccessController.doPrivileged(act);
    } catch (RuntimeException e) {
      logger.fine("queryNames", "Failed to query names: " + e);
      logger.debug("queryNames", e);
      throw e;
    }
  }

  /**
   * @param mbs .
   * @param name .
   * @param className .
   * @return .
   */
  static boolean isInstanceOf(final MBeanServer mbs, final ObjectName name, final String className) {
    PrivilegedExceptionAction<Boolean> act = new PrivilegedExceptionAction<Boolean>() {
      @Override
      public Boolean run() throws InstanceNotFoundException {
        return new Boolean(mbs.isInstanceOf(name, className));
      }
    };
    try {
      return AccessController.doPrivileged(act).booleanValue();
    } catch (Exception e) {
      logger.fine("isInstanceOf", "failed: " + e);
      logger.debug("isInstanceOf", e);
      return false;
    }
  }

  /**
   * This method must not be synchronized. See the comment on the createListeners method.
   * The notification could arrive after our buffer has been destroyed or even during its destruction. So we always add our listener (without synchronization), then we check if the buffer has been
   * destroyed and if so remove the listener we just added.
   * @param n .
   */
  private void createdNotification(final MBeanServerNotification n) {
    final String shouldEqual = MBeanServerNotification.REGISTRATION_NOTIFICATION;
    if (!n.getType().equals(shouldEqual)) {
      logger.warning("createNotification", "bad type: " + n.getType());
      return;
    }
    ObjectName name = n.getMBeanName();
    if (logger.debugOn()) logger.debug("createdNotification", "for: " + name);
    synchronized (this) {
      if (createdDuringQuery != null) {
        createdDuringQuery.add(name);
        return;
      }
    }
    if (isInstanceOf(mBeanServer, name, broadcasterClass)) {
      addBufferListener(name);
      if (isDisposed()) removeBufferListener(name);
    }
  }

  /** */
  private void destroyListeners() {
    logger.debug("destroyListeners", "starts");
    try {
      removeNotificationListener(delegateName, creationListener);
    } catch (Exception e) {
      logger.warning("remove listener from MBeanServer delegate", e);
    }
    Set<ObjectName> names = queryNames(null, broadcasterQuery);
    for (ObjectName name: names) {
      if (logger.debugOn()) logger.debug("destroyListeners", "remove listener from " + name);
      removeBufferListener(name);
    }
    logger.debug("destroyListeners", "ends");
  }

  /**
   * Iterate until we extract the real exception from a stack of PrivilegedActionExceptions.
   * @param e .
   * @return .
   */
  private static Exception extractException(final Exception e) {
    Exception ex = e;
    while (ex instanceof PrivilegedActionException) {
      ex = ((PrivilegedActionException) ex).getException();
    }
    return ex;
  }
}
