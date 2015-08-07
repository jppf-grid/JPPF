/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import java.security.*;
import java.util.*;

import javax.management.*;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.*;
import javax.management.remote.message.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.generic.*;
import com.sun.jmx.remote.opt.internal.*;
import com.sun.jmx.remote.opt.util.*;

/**
 *
 */
class ServerIntermediary extends AbstractServerIntermediary {
  /** */
  private final RequestHandler requestHandler = new RequestHandler();
  /** */
  private final ClassLoaderWithRepository clr;

  /**
   * Note: it is necessary to pass the defaultClassLoader - because the defaultClassLoader is determined from the Map + context
   * class loader of the thread that calls GenericConnectorServer.start()
   * @param mbeanServer .
   * @param myServer .
   * @param connection .
   * @param wrapper .
   * @param subject .
   * @param defaultClassLoader .
   * @param env .
   */
  public ServerIntermediary(final MBeanServer mbeanServer, final GenericConnectorServer myServer, final ServerSynchroMessageConnection connection, final ObjectWrapping wrapper, final Subject subject,
      final ClassLoader defaultClassLoader, final Map<String, ?> env) {
    super(mbeanServer, myServer, connection, wrapper, subject, defaultClassLoader, env);
    final ClassLoader dcl = defaultClassLoader;
    this.clr = AccessController.doPrivileged(new PrivilegedAction<ClassLoaderWithRepository>() {
      @Override
      public ClassLoaderWithRepository run() {
        return new ClassLoaderWithRepository(getClassLoaderRepository(), dcl);
      }
    });
    final long timeout = EnvHelp.getServerConnectionTimeout(this.env);
    serverCommunicatorAdmin = new GenericServerCommunicatorAdmin(timeout);
  }

  /**
   * @return .
   */
  private synchronized ServerNotifForwarder getServerNotifFwd() {
    // Lazily created when first use. Mainly when addNotificationListener is first called.
    if (serverNotifForwarder == null) serverNotifForwarder = new ServerNotifForwarder(mbeanServer, env, myServer.getNotifBuffer());
    return serverNotifForwarder;
  }

  /**
   * @param req .
   * @return .
   * @throws Exception .
   */
  public Object handleRequest(final MBeanServerRequestMessage req) throws Exception {
    if (logger.traceOn())logger.trace("handleRequest", "Handle a request: " + req);
    if (req == null) return null;
    final Object[] params = req.getParams();
    switch (req.getMethodId()) {
      case MBeanServerRequestMessage.CREATE_MBEAN: if (logger.traceOn()) logger.trace("handleRequest", "Handle a CREATE_MBEAN request.");
        return mbeanServer.createMBean((String) params[0], (ObjectName) params[1]);
      case MBeanServerRequestMessage.CREATE_MBEAN_LOADER: if (logger.traceOn()) logger.trace("handleRequest", "Handle a CREATE_MBEAN_LOADER request.");
        return mbeanServer.createMBean((String) params[0], (ObjectName) params[1], (ObjectName) params[2]);
      case MBeanServerRequestMessage.CREATE_MBEAN_PARAMS: if (logger.traceOn()) logger.trace("handleRequest", "Handle a CREATE_MBEAN_PARAMS request.");
        return mbeanServer.createMBean((String) params[0], (ObjectName) params[1], (Object[]) serialization.unwrap(params[2], clr), (String[]) params[3]);
      case MBeanServerRequestMessage.CREATE_MBEAN_LOADER_PARAMS: if (logger.traceOn()) logger.trace("handleRequest", "Handle a CREATE_MBEAN_LOADER_PARAMS request.");
        return mbeanServer.createMBean((String) params[0], (ObjectName) params[1], (ObjectName) params[2], (Object[]) unwrapWithDefault(params[3], getClassLoader((ObjectName) params[2])),(String[]) params[4]);
      case MBeanServerRequestMessage.GET_ATTRIBUTE: if (logger.traceOn()) logger.trace("handleRequest", "Handle a GET_ATTRIBUTE request.");
        return mbeanServer.getAttribute((ObjectName) params[0], (String) params[1]);
      case MBeanServerRequestMessage.GET_ATTRIBUTES: if (logger.traceOn())logger.trace("handleRequest", "Handle a GET_ATTRIBUTES request.");
        return mbeanServer.getAttributes((ObjectName) params[0], (String[]) params[1]);
      case MBeanServerRequestMessage.GET_DEFAULT_DOMAIN: if (logger.traceOn()) logger.trace("handleRequest", "Handle a GET_DEFAULT_DOMAIN request.");
        return mbeanServer.getDefaultDomain();
      case MBeanServerRequestMessage.GET_DOMAINS: if (logger.traceOn()) logger.trace("handleRequest", "Handle a GET_DOMAINS request.");
        return mbeanServer.getDomains();
      case MBeanServerRequestMessage.GET_MBEAN_COUNT: if (logger.traceOn()) logger.trace("handleRequest", "Handle a GET_MBEAN_COUNT request.");
        return mbeanServer.getMBeanCount();
      case MBeanServerRequestMessage.GET_MBEAN_INFO: if (logger.traceOn()) logger.trace("handleRequest", "Handle a GET_MBEAN_INFO request.");
        return mbeanServer.getMBeanInfo((ObjectName) params[0]);
      case MBeanServerRequestMessage.GET_OBJECT_INSTANCE: if (logger.traceOn()) logger.trace("handleRequest", "Handle a GET_OBJECT_INSTANCE request.");
        return mbeanServer.getObjectInstance((ObjectName) params[0]);
      case MBeanServerRequestMessage.INVOKE: if (logger.traceOn()) logger.trace("handleRequest", "Handle a INVOKE request.");
        return mbeanServer.invoke((ObjectName) params[0], (String) params[1], (Object[]) unwrapWithDefault(params[2], getClassLoaderFor((ObjectName) params[0])), (String[]) params[3]);
      case MBeanServerRequestMessage.IS_INSTANCE_OF: if (logger.traceOn()) logger.trace("handleRequest", "Handle a IS_INSTANCE_OF request.");
        return mbeanServer.isInstanceOf((ObjectName) params[0], (String) params[1]) ? Boolean.TRUE : Boolean.FALSE;
      case MBeanServerRequestMessage.IS_REGISTERED:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a IS_REGISTERED request.");
        return mbeanServer.isRegistered((ObjectName) params[0]) ? Boolean.TRUE : Boolean.FALSE;
      case MBeanServerRequestMessage.QUERY_MBEANS:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a QUERY_MBEANS request.");
        return mbeanServer.queryMBeans((ObjectName) params[0], (QueryExp) serialization.unwrap(params[1], defaultClassLoader));
      case MBeanServerRequestMessage.QUERY_NAMES:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a QUERY_NAMES request.");
        return mbeanServer.queryNames((ObjectName) params[0], (QueryExp) serialization.unwrap(params[1], defaultClassLoader));
      case MBeanServerRequestMessage.SET_ATTRIBUTE:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a SET_ATTRIBUTE request.");
        mbeanServer.setAttribute((ObjectName) params[0], (Attribute) unwrapWithDefault(params[1], getClassLoaderFor((ObjectName) params[0])));
        return null;
      case MBeanServerRequestMessage.SET_ATTRIBUTES:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a SET_ATTRIBUTES request.");
        return mbeanServer.setAttributes((ObjectName) params[0], (AttributeList) unwrapWithDefault(params[1], getClassLoaderFor((ObjectName) params[0])));
      case MBeanServerRequestMessage.UNREGISTER_MBEAN: 
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a UNREGISTER_MBEAN request.");
        mbeanServer.unregisterMBean((ObjectName) params[0]);
        return null;
      case MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENER_OBJECTNAME:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a ADD_NOTIFICATION_LISTENER_OBJECTNAME request.");
        final ClassLoader cl1 = getClassLoaderFor((ObjectName) params[0]);
        mbeanServer.addNotificationListener((ObjectName) params[0], (ObjectName) params[1], (NotificationFilter) unwrapWithDefault(params[2], cl1), unwrapWithDefault(params[3], cl1));
        return null;
      case MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a REMOVE_NOTIFICATION_LISTENER_OBJECTNAME request.");
        mbeanServer.removeNotificationListener((ObjectName) params[0], (ObjectName) params[1]);
        return null;
      case MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK request.");
        final ClassLoader cl2 = getClassLoaderFor((ObjectName) params[0]);
        mbeanServer.removeNotificationListener((ObjectName) params[0], (ObjectName) params[1], (NotificationFilter) unwrapWithDefault(params[2], cl2), unwrapWithDefault(params[3], cl2));
        return null;
      case MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENERS:
        return handleAddNotificationListener(params);
      case MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK request.");
        getServerNotifFwd().removeNotificationListener((ObjectName) params[0], new Integer[] { (Integer) params[1] });
        return null;
      case MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER:
        if (logger.traceOn()) logger.trace("handleRequest", "Handle a REMOVE_NOTIFICATION_LISTENER request.");
        getServerNotifFwd().removeNotificationListener((ObjectName) params[0], (Integer[]) params[1]);
        return null;
      default: 
        logger.info("handleRequest", "Unknown request id: " + req.getMethodId());
        throw new IllegalArgumentException("The specified method is not " + "found [MethodId=" + req.getMethodId() + "]");
    }
  }

  /**
   * Handle a request to add a notification listener.
   * @param params the parmaters of the request.
   * @return an arry of ids, one for each mbean to which the listener is added.
   * @throws Exception if any error occurs.
   */
  private Object handleAddNotificationListener(final Object[] params) throws Exception {
    if (logger.traceOn()) logger.trace("handleRequest", "Handle a ADD_NOTIFICATION_LISTENERS request.");
    if (isRI10) { // compatible to RI 10: bug 4948444
      final ObjectName name = ((ObjectName[]) params[0])[0];
      final ClassLoader cl3 = getClassLoaderFor(name);
      final Object wrappedFilter = ((Object[]) params[1])[0];
      return getServerNotifFwd().addNotificationListener(name, (NotificationFilter) unwrapWithDefault(wrappedFilter, cl3));
    }
    if (params[0] == null || params[1] == null) throw new IllegalArgumentException("Got null arguments.");
    ObjectName[] names = (ObjectName[]) params[0];
    Object[] wrappedFilters = (Object[]) params[1];
    if (names.length != wrappedFilters.length) throw new IllegalArgumentException("The value lengths of 2 parameters are not same.");
    for (int i = 0; i < names.length; i++) {
      if (names[i] == null) throw new IllegalArgumentException("Null Object name.");
    }
    int i = 0;
    Integer[] ids = new Integer[names.length];
    final boolean debug = logger.debugOn();
    try {
      for (; i < names.length; i++) {
        ClassLoader targetCl = getClassLoaderFor(names[i]);
        if (debug) logger.debug("addNotificationListener" + "(ObjectName,NotificationFilter)", "connectionId=" + clientId + " unwrapping filter with target extended ClassLoader.");
        NotificationFilter filterValue = (NotificationFilter) unwrapWithDefault(wrappedFilters[i], targetCl);
        if (debug) logger.debug("addNotificationListener" + "(ObjectName,NotificationFilter)", "connectionId=" + clientId + ", name=" + names[i] + ", filter=" + filterValue);
        ids[i] = getServerNotifFwd().addNotificationListener(names[i], filterValue);
      }
      return ids;
    } catch (Exception e) {
      for (int j = 0; j < i; j++) { // remove all registered listeners
        try {
          getServerNotifFwd().removeNotificationListener(names[j], ids[j]);
        } catch (Exception eee) {
          logger.warning("handleRequest-addNotificationListener", "Failed to remove a listener from the MBean " + names[j] + ". " + eee.toString());
        }
      }
      if (e instanceof PrivilegedActionException) e = extractException(e);
      throw e;
    }
  }

  /**
   *
   */
  public void terminate() {
    terminate(false, "The server is stopped.");
  }

  /**
   * @param local .
   * @param msg .
   */
  public void terminate(final boolean local, final String msg) {
    if (logger.traceOn()) logger.trace("terminate", "Terminating....");
    synchronized (stateLock) {
      if (state == TERMINATED) return;
      state = TERMINATED;
    }
    // inform the remote side of termination
    if (!local) {
      if (logger.traceOn()) logger.trace("terminate", "Send a CloseMessage to the client.");
      try {
        synchronized (connection) {
          connection.sendOneWay(new CloseMessage(msg));
        }
      } catch (UnsupportedOperationException uoe) {
        if (logger.traceOn()) logger.trace("terminate", "The transport level does not support " + "the method sendOneWay: " + uoe);
      } catch (IOException ioe) {
        // We haven't been able to inform the client that we're closing.
        logger.warning("terminate", "Failed to inform the client: " + ioe);
        logger.debug("terminate", ioe);
      }
    }
    if (serverNotifForwarder != null) serverNotifForwarder.terminate(); // stopping listening
    try { // close the transport protocol
      connection.close();
    } catch (Exception ce) { // OK. We are closing, so ignore it.
    }
    if (serverCommunicatorAdmin != null) serverCommunicatorAdmin.terminate();
    myServer.clientClosing(this, clientId, "The method terminate is called.", null); // inform the server of termination
    if (logger.traceOn()) logger.trace("terminate", "Terminated.");
  }

  /**
   * @return .
   */
  ServerSynchroMessageConnection getTransport() {
    return connection;
  }

  /**
   * Read a message and distribute to a thread for execution
   */
  class RequestHandler implements SynchroCallback {
    @Override
    public Message execute(final Message msg) {
      final boolean terminated = serverCommunicatorAdmin.reqIncoming();
      if (logger.traceOn()) logger.trace("RequestHandler.execute", "terminated=" + terminated);
      try {
        if (terminated) throw new ConnectionClosedException("The connection is being terminated by the server");
        if (logger.traceOn()) logger.trace("RequestHandler.execute", "Execute the request: " + msg);
        if (msg instanceof CloseMessage) return handleCloseMessage((CloseMessage) msg);
        else if (msg instanceof NotificationRequestMessage) return handleNotifReqMessage((NotificationRequestMessage) msg);
        else if (msg instanceof MBeanServerRequestMessage) return handleMBSReqMessage((MBeanServerRequestMessage) msg);
        else {
          // We can recover from this, so this should rather be a warning...
          logger.warning("RequestHandler.execute", "Got unknown message: " + msg);
          myServer.failedConnectionNotif(clientId, "Got unknown message: " + msg, msg);
          ServerIntermediary.this.terminate(false, "Got unknown message: " + msg);
        }
      } catch (IOException cce) { // Do nothing here, the connection should be closed soon.
        logger.trace("RequestHandler.execute", cce);
      } finally {
        serverCommunicatorAdmin.rspOutgoing();
      }
      return null;
    }

    /**
     * @param msg .
     * @return .
     */
    private Message handleCloseMessage(final CloseMessage msg) {
      if (logger.traceOn()) logger.trace("RequestHandler-execute", "Receive a CloseMessage.");
      ServerIntermediary.this.terminate(true, null);
      return null;
    }

    /**
     * @param nr .
     * @return .
     * @throws IOException .
     */
    private Message handleNotifReqMessage(final NotificationRequestMessage nr) throws IOException {
      if (logger.traceOn()) logger.trace("RequestHandler.handleNotifReqMessage", "Receive a NotificationRequestMessage.");
      NotificationResult result = getServerNotifFwd().fetchNotifs(nr.getClientSequenceNumber(), nr.getTimeout(), nr.getMaxNotifications());
      Object wrapped;
      try {
        wrapped = serialization.wrap(result);
      } catch (NotSerializableException e) {
        // This presumably means that at least one of the notifications is unserializable, for example because it contains an unserializable object in its userData.
        // This should not happen often (Notification itself is serializable).
        wrapped = serialization.wrap(purgeUnserializable(result));
        // If even this call to serialization.wrap gets an exception, then the ObjectWrapping is broken and we can't reasonably recover.
        // So propagate the exception. We are inside a catch(IOException) that will swallow the IOException, but at least it will trace it if asked.
      }
      return new NotificationResponseMessage(wrapped);
    }

    /**
     * @param req .
     * @return .
     * @throws IOException .
     */
    private Message handleMBSReqMessage(final MBeanServerRequestMessage req) throws IOException {
      if (logger.traceOn()) logger.trace("RequestHandler.handleMBSReqMessage", "Receive a MBeanServerRequestMessage.");
      try {
        final AccessControlContext reqACC;
        final Subject delegationSubject = req.getDelegationSubject();
        if (delegationSubject == null) reqACC = acc;
        else {
          if (subject == null) throw new SecurityException("Subject delegation cannot be enabled " + "unless an authenticated subject " + "is put in place");
          reqACC = subjectDelegator.delegatedContext(acc, delegationSubject);
        }
        Object result = AccessController.doPrivileged(new PrivilegedRequestJob(req), reqACC);
        return new MBeanServerResponseMessage(req.getMessageId(), result, false);
      } catch (Exception e) {
        e = extractException(e);
        if (logger.traceOn()) logger.trace("RequestHandler.handleMBSReqMessage", "Got an exception: " + e, e);
        return new MBeanServerResponseMessage(req.getMessageId(), wrapException(e), true);
      } catch (Error r) {
        if (logger.traceOn()) logger.trace("RequestHandler.handleMBSReqMessage", "Got an error: " + r, r);
        final JMXServerErrorException see = new JMXServerErrorException(r.toString(), r);
        return new MBeanServerResponseMessage(req.getMessageId(), wrapException(see), true);
      }
    }

    /**
     * Handle the case where the exception to be returned to the client is not serializable.
     * This is pretty unlikely, but if it does happen then we'll end up returning nothing at all to the client so it will get out of sync in the message exchange.
     * The ObjectWrapping had better be able to serialize a NotSerializableException.
     * @param e .
     * @return .
     * @throws IOException .
     */
    private Object wrapException(final Exception e) throws IOException {
      try {
        return serialization.wrap(e);
      } catch (NotSerializableException nse) {
        return serialization.wrap(nse);
      }
    }

    @Override
    public void connectionException(final Exception e) {
      synchronized (stateLock) {
        if (state == RUNNING) state = FAILED;
        else return;
      }
      // This is the usual case when the client exits abruptly or forgets to close the connection before exiting, so log a more comprehensible warning. (5003051)
      if (e instanceof EOFException) logger.warning("RequestHandler.connectionException", "JMX connector client exited without " + "closing connection");
      else logger.warning("RequestHandler.connectionException", "JMX connector transport got exception " + "when reading input message: " + e);
      logger.finer("RequestHandler.connectionException", e);
      String msg = null;
      if (e instanceof ClassNotFoundException) msg = "The client " + clientId + "got an unknown message: " + e;
      else msg = "The client " + clientId + " has failed: " + e;
      myServer.failedConnectionNotif(clientId, msg, e);
      ServerIntermediary.this.terminate(true, null);
    }
  }

  /*
   * The following stuff handles notifications that are not serializable. This can happen, for example, if the userData of a notification is an unserializable object.
   * This is probably uncommon as a developer error, but when it happens we don't want the whole notification subsystem to collapse.
   * We also don't want to lose other notifications that are in the same NotificationResult and that are serializable.
   * Ideally we would have some way to signal unambiguously to the remote client that unserializable notifications have been lost.
   * However, that turns out to be difficult. Here are some possible approaches:
   * - Replace the unserializable notifications by instances of a class that is known only to a private class loader. Then the client will see them as instances of an unknown class, and the logic it
   * should already have for dealing with that should flag lost notifications (though it won't indicate why they were lost). The problem is that we can't expect an arbitrary ObjectWrapping to be able
   * to serialize this private class.
   * - A special Notification subclass that indicates there were unserializable notifications. Unfortunately the spec doesn't define one. If we invent an implementation-specific one, then we again
   * can't expect an arbitrary ObjectWrapping to be able to handle it.
   * - Tweak the returned NotificationResult so that notifications are seen to have been lost. If we had specified a way to do this explicitly (e.g., if elements of the TargetedNotification[] are null
   * or are of a special class) then this would have been straightforward. Since we did not, the only possibility would be to return an earliestSequenceNumber that is greater than the starting
   * sequence number given by the client. This would mean that the earliestSequenceNumber would subsequently go backwards. We don't know what the client does with earliestSequenceNumber so even though
   * our implementation would not be adversely affected it seems dangerous to impose this on other clients.
   * What we do in fact is to return a JMXConnectionNotification.NOTIFS_LOST in place of each unserializable notification. This is ambiguous (there could have been a real notification of this kind)
   * but we are making a best effort to recover from a programming error in user code, so it is acceptable. It is still not guaranteed that an arbitrary ObjectWrapping can serialize this notification
   * (which is not usually sent by user MBeans) so if serialization of the NotificationResult still fails then we simply drop the offending notification.
   */

  /**
   * Replace unserializable notifications in the NotificationResult by JMXConnectionNotification.NOTIFS_LOST.
   * @param nr .
   * @return .
   */
  private NotificationResult purgeUnserializable(final NotificationResult nr) {
    List<TargetedNotification> tnList = new ArrayList<>();
    TargetedNotification[] tns = nr.getTargetedNotifications();
    // For each TargetedNotification (TN), try putting it on its own inside a NotificationResult (NR) and see if we can serialize. If so, assume the TN can also be serialized when combined with other
    // TNs. If not, replace it with a NOTIFS_LOST. We serialize a whole NR rather than just a single TN, because an arbitrary serializer might not know how to serialize a single TN (for example, it's
    // an XML serializer that can serialize an NR containing a TN but not a TN on its own).
    for (int i = 0; i < tns.length; i++) {
      TargetedNotification tn = tns[i];
      NotificationResult trialnr = new NotificationResult(0, 0, new TargetedNotification[] { tn });
      try {
        serialization.wrap(trialnr);
        tnList.add(tn);
      } catch (IOException e) {
        logger.warning("purgeUnserializable", "cannot serialize notif: " + tn);
        logger.fine("purgeUnserializable", e);
        final Integer listenerID = tn.getListenerID();
        final Notification badNotif = tn.getNotification();
        final String notifType = JMXConnectionNotification.NOTIFS_LOST;
        final String notifMessage = "Not serializable: " + badNotif;
        Notification goodNotif = new JMXConnectionNotification(notifType, badNotif.getSource(), clientId, badNotif.getSequenceNumber(), notifMessage, ONE_LONG);
        // Our implementation has the convention that a NOTIFS_LOST has a userData that says how many notifs were lost.
        tn = new TargetedNotification(goodNotif, listenerID);
        trialnr = new NotificationResult(0, 0, new TargetedNotification[] { tn });
        try {
          serialization.wrap(trialnr);
          tnList.add(tn);
        } catch (IOException e1) { // OK. too bad, at least we have logged it
        }
      }
    }
    tns = tnList.toArray(new TargetedNotification[0]);
    return new NotificationResult(nr.getEarliestSequenceNumber(), nr.getNextSequenceNumber(), tns);
  }

  /** */
  class GenericServerCommunicatorAdmin extends ServerCommunicatorAdmin {
    /**
     * @param timeout .
     */
    public GenericServerCommunicatorAdmin(final long timeout) {
      super(timeout);
    }

    @Override protected void doStop() {
      ServerIntermediary.this.terminate();
    }
  }

  /** */
  class PrivilegedRequestJob implements PrivilegedExceptionAction<Object> {
    /** */
    private MBeanServerRequestMessage request;

    /**
     * @param request .
     */
    public PrivilegedRequestJob(final MBeanServerRequestMessage request) {
      this.request = request;
    }

    @Override public Object run() throws Exception {
      return serialization.wrap(handleRequest(request));
    }
  }

  /**
   * called by GenericConnectorServer, make sure that this object will be started only when we finish its creation.
   */
  void start() {
    this.connection.setCallback(requestHandler);
  }

  /**
   * @return .
   */
  private ClassLoaderRepository getClassLoaderRepository() {
    return AccessController.doPrivileged(new PrivilegedAction<ClassLoaderRepository>() {
      @Override public ClassLoaderRepository run() {
        return mbeanServer.getClassLoaderRepository();
      }
    });
  }

  /**
   * @param name .
   * @return .
   * @throws InstanceNotFoundException .
   */
  private ClassLoader getClassLoader(final ObjectName name) throws InstanceNotFoundException {
    try {
      return  AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
        @Override public ClassLoader run() throws InstanceNotFoundException {
          return mbeanServer.getClassLoader(name);
        }
      });
    } catch (PrivilegedActionException pe) {
      throw (InstanceNotFoundException) extractException(pe);
    }
  }

  /**
   * @param name .
   * @return .
   * @throws InstanceNotFoundException .
   */
  private ClassLoader getClassLoaderFor(final ObjectName name) throws InstanceNotFoundException {
    try {
      return AccessController.doPrivileged(new PrivilegedExceptionAction<ClassLoader>() {
        @Override public ClassLoader run() throws InstanceNotFoundException {
          return mbeanServer.getClassLoaderFor(name);
        }
      });
    } catch (PrivilegedActionException pe) {
      throw (InstanceNotFoundException) extractException(pe);
    }
  }
}
