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
/*
 * @(#)ServerNotifForwarder.java	1.3
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

import java.io.IOException;
import java.security.*;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;

import com.sun.jmx.remote.opt.util.*;

/**
 *
 */
public class ServerNotifForwarder {
  /**
   * 
   */
  private MBeanServer mbeanServer;
  /**
   * 
   */
  private final long connectionTimeout;
  /**
   * 
   */
  private static int listenerCounter = 0;
  /**
   * 
   */
  private final static int[] listenerCounterLock = new int[0];
  /**
   * 
   */
  private NotificationBuffer notifBuffer;
  /**
   * 
   */
  private Set<ListenerInfo> listenerList = new HashSet<>();
  /**
   * 
   */
  private boolean terminated = false;
  /**
   * 
   */
  private final int[] terminationLock = new int[0];
  /**
   * 
   */
  static final String broadcasterClass = NotificationBroadcaster.class.getName();
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.misc", "ServerNotifForwarder");

  /**
   *
   * @param mbeanServer .
   * @param env .
   * @param notifBuffer .
   */
  public ServerNotifForwarder(final MBeanServer mbeanServer, final Map<String, ?> env, final NotificationBuffer notifBuffer) {
    this.mbeanServer = mbeanServer;
    this.notifBuffer = notifBuffer;
    connectionTimeout = EnvHelp.getServerConnectionTimeout(env);
  }

  /**
   *
   * @param name .
   * @param filter .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public Integer addNotificationListener(final ObjectName name, final NotificationFilter filter) throws InstanceNotFoundException, IOException {
    if (logger.traceOn()) logger.trace("addNotificationListener", "Add a listener at " + name);
    checkState();
    // Explicitly check MBeanPermission for addNotificationListener
    checkMBeanPermission(name, "addNotificationListener");
    try {
      Boolean instanceOf = AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
        @Override
        public Boolean run() throws InstanceNotFoundException {
          return new Boolean(mbeanServer.isInstanceOf(name, broadcasterClass));
        }
      });
      if (!instanceOf.booleanValue()) throw new IllegalArgumentException("The specified MBean [" + name + "] is not a " + "NotificationBroadcaster " + "object.");
    } catch (PrivilegedActionException e) {
      throw (InstanceNotFoundException) extractException(e);
    }
    final Integer id = getListenerID();
    synchronized (listenerList) {
      listenerList.add(new ListenerInfo(id, name, filter));
    }
    return id;
  }

  /**
   *
   * @param name .
   * @param listenerIDs .
   * @throws Exception .
   */
  public void removeNotificationListener(final ObjectName name, final Integer[] listenerIDs) throws Exception {
    if (logger.traceOn()) logger.trace("removeNotificationListener", "Remove some listeners from " + name);
    checkState();
    // Explicitly check MBeanPermission for removeNotificationListener
    checkMBeanPermission(name, "removeNotificationListener");
    Exception re = null;
    for (int i = 0; i < listenerIDs.length; i++) {
      try {
        removeNotificationListener(name, listenerIDs[i]);
      } catch (Exception e) {
        // Give back the first exception
        if (re != null) re = e;
      }
    }
    if (re != null) throw re;
  }

  /**
   *
   * @param name .
   * @param listenerID .
   * @throws InstanceNotFoundException .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public void removeNotificationListener(final ObjectName name, final Integer listenerID) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    if (logger.traceOn()) logger.trace("removeNotificationListener", "Remove the listener " + listenerID + " from " + name);
    checkState();
    if (name != null && !name.isPattern()) {
      if (!mbeanServer.isRegistered(name)) throw new InstanceNotFoundException("The MBean " + name + " is not registered.");
    }
    synchronized (listenerList) {
      if (!listenerList.remove(new ListenerInfo(listenerID, name, null))) throw new ListenerNotFoundException("Listener not found!");
    }
  }

  /**
   *
   * @param startSequenceNumber .
   * @param timeout .
   * @param maxNotifications .
   * @return .
   */
  public NotificationResult fetchNotifs(final long startSequenceNumber, final long timeout, final int maxNotifications) {
    if (logger.traceOn()) logger.trace("fetchNotifs", "Fetching notifications, the " + "startSequenceNumber is " + startSequenceNumber + ", the timeout is " + timeout + ", the maxNotifications is " + maxNotifications);
    NotificationResult nr = null;
    final long t = Math.min(connectionTimeout, timeout);
    try {
      nr = notifBuffer.fetchNotifications(listenerList, startSequenceNumber, t, maxNotifications);
    } catch (@SuppressWarnings("unused") InterruptedException ire) {
      nr = new NotificationResult(0L, 0L, new TargetedNotification[0]);
    }
    if (logger.traceOn())logger.trace("fetchNotifs", "Forwarding the notifs: " + nr);
    return nr;
  }

  /**
   *
   */
  public void terminate() {
    if (logger.traceOn()) logger.trace("terminate", "Be called.");
    synchronized (terminationLock) {
      if (terminated) return;
      terminated = true;
      synchronized (listenerList) {
        listenerList.clear();
      }
    }
    if (logger.traceOn()) logger.trace("terminate", "Terminated.");
  }

  /**
   *
   * @throws IOException .
   */
  private void checkState() throws IOException {
    synchronized (terminationLock) {
      if (terminated) throw new IOException("The connection has been terminated.");
    }
  }

  /**
   *
   * @return .
   */
  private Integer getListenerID() {
    synchronized (listenerCounterLock) {
      return new Integer(listenerCounter++);
    }
  }

  /**
   * Explicitly check the MBeanPermission for the current access control context.
   * @param name .
   * @param actions .
   * @throws InstanceNotFoundException .
   * @throws SecurityException .
   */
  private void checkMBeanPermission(final ObjectName name, final String actions) throws InstanceNotFoundException, SecurityException {
    SecurityManager sm = System.getSecurityManager();
    if (sm != null) {
      AccessControlContext acc = AccessController.getContext();
      ObjectInstance oi = null;
      try {
        oi = AccessController.doPrivileged(new PrivilegedExceptionAction<ObjectInstance>() {
          @Override
          public ObjectInstance run() throws InstanceNotFoundException {
            return mbeanServer.getObjectInstance(name);
          }
        });
      } catch (PrivilegedActionException e) {
        throw (InstanceNotFoundException) extractException(e);
      }
      String classname = oi.getClassName();
      MBeanPermission perm = new MBeanPermission(classname, null, name, actions);
      sm.checkPermission(perm, acc);
    }
  }

  /**
   * Iterate until we extract the real exception from a stack of PrivilegedActionExceptions.
   * @param e .
   * @return .
   */
  private static Exception extractException(final Exception e) {
    Exception ex = e;
    while (ex instanceof PrivilegedActionException) ex = ((PrivilegedActionException) e).getException();
    return ex;
  }
}
