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
 * @(#)file      ServerIntermediary.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.79
 * @(#)lastedit  07/03/08
 * @(#)build     @BUILD_TAG_PLACEHOLDER@
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

package javax.management.remote.generic;

import java.io.*;

import javax.management.*;
import javax.management.remote.message.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.generic.ClientSynchroMessageConnection;
import com.sun.jmx.remote.opt.util.ClassLogger;

/**
 * @exclude
 */
abstract class AbstractClientIntermediary {
  /** */
  ClientSynchroMessageConnection connection;
  /** */
  GenericConnector client;
  /** */
  ObjectWrapping serialization;
  /** */
  ClassLoader myloader;
  /** */
  GenericClientNotifForwarder notifForwarder;
  /** */
  GenericClientCommunicatorAdmin communicatorAdmin;
  /** */
  long lostNotifCounter = 0;
  /** */
  boolean terminated;
  /** */
  boolean requestTimeoutReconn;
  /** */
  static final ObjectName delegateName;
  static {
    try {
      delegateName = new ObjectName("JMImplementation:type=MBeanServerDelegate");
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e.toString());
    }
  }
  /** */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.generic", "AbstractClientIntermediary");

  /**
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback, final Subject delegationSubject) throws InstanceNotFoundException, IOException {
    logger.trace("addNotificationListener", "name=" + name);
    final Integer listenerID = addListenerWithSubject(name, serialization.wrap(filter), delegationSubject, true);
    notifForwarder.addNotificationListener(listenerID, name, listener, filter, handback, delegationSubject);
  }

  /**
   * @param name .
   * @param wrappedFilter .
   * @param delegationSubject .
   * @param reconnect .
   * @return .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  Integer addListenerWithSubject(final ObjectName name, final Object wrappedFilter, final Subject delegationSubject, final boolean reconnect) throws InstanceNotFoundException, IOException {
    final boolean debug = logger.debugOn();
    if (debug) logger.debug("addListenerWithSubject", "(ObjectName,Object,Subject)");
    final ObjectName[] names = { name };
    final Object[] filters = new Object[] { wrappedFilter };
    final Object[] params = new Object[] { names, filters };
    final int code = MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENERS;
    try {
      Object o = mBeanServerRequest(code, params, delegationSubject, reconnect);
      return (o instanceof Integer) ? (Integer) o : ((Integer[]) o)[0]; // compatible with RI1.0: bug 4948444 
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws IOException .
   */
  public void addNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback, final Subject delegationSubject) throws InstanceNotFoundException, IOException {
    logger.trace("addNotificationListener", "called");
    try {
      mBeanServerRequest(MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENER_OBJECTNAME, new Object[] { name, listener, serialization.wrap(filter), serialization.wrap(handback) }, delegationSubject);
    } catch (InstanceNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener, final Subject delegationSubject) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    logger.trace("removeNotificationListener", "called");
    final Integer[] ids = notifForwarder.removeNotificationListener(name, listener);
    try {
      mBeanServerRequest(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER, new Object[] { name, ids }, delegationSubject);
    } catch (InstanceNotFoundException|ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public void removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback, final Subject delegationSubject)
      throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    logger.trace("removeNotificationListener", "called");
    final Integer ids = notifForwarder.removeNotificationListener(name, listener, filter, handback);
    try {
      mBeanServerRequest(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK, new Object[] { name, ids }, delegationSubject);
    } catch (InstanceNotFoundException|ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public void removeNotificationListener(final ObjectName name, final ObjectName listener, final Subject delegationSubject) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    logger.trace("removeNotificationListener", "called");
    try {
      mBeanServerRequest(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME, new Object[] { name, listener }, delegationSubject);
    } catch (InstanceNotFoundException|ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /**
   * @param name .
   * @param listener .
   * @param filter .
   * @param handback .
   * @param delegationSubject .
   * @throws InstanceNotFoundException .
   * @throws ListenerNotFoundException .
   * @throws IOException .
   */
  public void removeNotificationListener(final ObjectName name, final ObjectName listener, final NotificationFilter filter, final Object handback, final Subject delegationSubject) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    logger.trace("removeNotificationListener", "called");
    try {
      mBeanServerRequest(MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK, new Object[] { name, listener, serialization.wrap(filter), serialization.wrap(handback) }, delegationSubject);
    } catch (InstanceNotFoundException|ListenerNotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw appropriateException(e);
    }
  }

  /** */
  public void terminate() {
    logger.trace("terminate", "Terminated.");
    if (terminated) return;
    terminated = true;
    communicatorAdmin.terminate();
    notifForwarder.terminate();
  }

  /**
   * Used by a GenericConnector
   * @return a {@link GenericClientCommunicatorAdmin} instance.
   */
  public GenericClientCommunicatorAdmin getCommunicatorAdmin() {
    return communicatorAdmin;
  }

  /**
   * @param methodId .
   * @param params .
   * @param delegationSubject .
   * @return .
   * @throws Exception .
   */
  Object mBeanServerRequest(final int methodId, final Object[] params, final Subject delegationSubject) throws Exception {
    return mBeanServerRequest(methodId, params, delegationSubject, true);
  }

  /**
   * @param methodId .
   * @param params .
   * @param delegationSubject .
   * @param reconnect .
   * @return .
   * @throws Exception .
   */
  Object mBeanServerRequest(final int methodId, final Object[] params, final Subject delegationSubject, final boolean reconnect) throws Exception {
    MBeanServerRequestMessage req = new MBeanServerRequestMessage(methodId, params, delegationSubject);
    MBeanServerResponseMessage resp;
    try {
      resp = (MBeanServerResponseMessage) connection.sendWithReturn(req);
    } catch (IOException e) {
      if (terminated || !reconnect || e instanceof InterruptedIOException) throw e;
      communicatorAdmin.gotIOException(e);
      resp = (MBeanServerResponseMessage) connection.sendWithReturn(req);
    }
    Object wrappedResult = resp.getWrappedResult(); // may throw exception
    Object result;
    try {
      result = serialization.unwrap(wrappedResult, myloader);
    } catch (ClassNotFoundException e) {
      throw new IOException(e);
    }
    if (resp.isException()) throw (Exception) result;
    return result;
  }

  /**
   * Throw an exception appropriate for e. This method is called from the final catch (Exception e) clause of methods that have already caught all the exceptions they were expecting,
   * except IOException and RuntimeException. If the exception is one of those it is thrown. Otherwise, it is wrapped in an IOException and that is thrown. This method is declared to
   * return an exception but never does. This simply allows us to write "throw appropriateException(e)" without getting errors about variables not initialized or missing return statements.
   * @param e the exception to convert.
   * @return a new {@code Exception}.
   * @throws IOException if any error occurs while doing the conversion.
   */
  static IOException appropriateException(final Exception e) throws IOException {
    if (e instanceof IOException) throw (IOException) e;
    if (e instanceof RuntimeException) throw (RuntimeException) e;
    throw new IOException("Unexpected exception: " + e.getMessage(), e);
  }
}
