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
 * @(#)file      GenericConnector.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   1.85
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

import java.io.IOException;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;
import javax.management.remote.message.*;
import javax.security.auth.Subject;

import com.sun.jmx.remote.generic.*;
import com.sun.jmx.remote.opt.util.*;

/**
 * <p>A client connection to a remote JMX API server. This class can use a {@link MessageConnection} object to specify the transport for communicating with the server.
 * <p>User code does not usually instantiate this class. Instead, a {@link JMXConnectorProvider} should be added to
 * the {@link JMXConnectorFactory} so that users can implicitly instantiate the GenericConnector (or a subclass of it)
 * through the {@link JMXServiceURL} provided when connecting.
 * <p>The specific connector protocol to be used by an instance of this class is specified by attributes in the <code>Map</code>
 * passed to the constructor or the {@link #connect(Map) connect} method. The attribute {@link #MESSAGE_CONNECTION} is the standard
 * way to define the transport. An implementation can recognize other attributes to define the transport differently.
 */
public class GenericConnector implements JMXConnector {
  // -------------------------------------------------------------------
  // WARNING - WARNING - WARNING - WARNING - WARNING - WARNING - WARNING
  // SERIALIZATION ISSUES
  // All private variables must be defined transient.
  // Do not put any initialization here. If a specific initialization is needed, put it in the empty default constructor.
  // -------------------------------------------------------------------
  /**
   * 
   */
  private transient ClientSynchroMessageConnection connection;
  /**
   * 
   */
  private transient ObjectWrapping objectWrapping;
  /**
   * 
   */
  private transient Map<String, ?> env;
  /**
   * 
   */
  private transient ClientIntermediary clientMBeanServer;
  /**
   * 
   */
  private transient WeakHashMap<Subject, RemoteMBeanServerConnection> rmbscMap;
  /**
   * 
   */
  private transient String connectionId;
  /**
   * 
   */
  private transient RequestHandler requestHandler;
  /**
   * 
   */
  private transient final NotificationBroadcasterSupport connectionBroadcaster;
  // state
  /**
   * 
   */
  private static final int CREATED = 1;
  /**
   * 
   */
  private static final int CONNECTED = 2;
  /**
   * 
   */
  private static final int CLOSED = 3;
  /**
   * default value is 0.
   */
  private transient int state;
  /**
   * 
   */
  private transient int[] lock;
  /**
   * 
   */
  private transient long clientNotifID = 0;
  /**
   * 
   */
  private static final ClassLogger logger = new ClassLogger("javax.management.remote.generic", "GenericConnector");
  /**
   * Name of the attribute that specifies the object wrapping for parameters whose deserialization requires special treatment.
   * The value associated with this attribute, if any, must be an object that implements the interface {@link ObjectWrapping}.
   */
  public static final String OBJECT_WRAPPING = GenericConnectorServer.OBJECT_WRAPPING;
  /**
   * Name of the attribute that specifies how this connector sends messages to its connector server.
   * The value associated with this attribute, if any, must be an object that implements the interface {@link MessageConnection}.
   */
  public static final String MESSAGE_CONNECTION = "jmx.remote.message.connection";

  /**
   * Default no-arg constructor.
   */
  public GenericConnector() {
    //  WARNING - WARNING - WARNING - WARNING - WARNING - WARNING
    // This constructor is needed in order for subclasses to be serializable.
    this((Map<String, ?>) null);
  }

  /**
   * Constructor specifying connection attributes.
   * @param env the attributes of the connection.
   **/
  public GenericConnector(final Map<String, ?> env) {
    //  WARNING - WARNING - WARNING - WARNING - WARNING - WARNING
    // Initialize transient variables. All transient variables that need a specific initialization must be initialized here.
    rmbscMap = new WeakHashMap<>();
    lock = new int[0];
    state = CREATED;
    if (env == null) this.env = Collections.emptyMap();
    else {
      EnvHelp.checkAttributes(env);
      this.env = Collections.unmodifiableMap(env);
    }
    connectionBroadcaster = new NotificationBroadcasterSupport();
  }

  @Override
  public void connect() throws IOException {
    connect(null);
  }

  @Override
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void connect(final Map<String, ?> env) throws IOException {
    final boolean tracing = logger.traceOn();
    final String idstr = (tracing ? "[" + this.toString() + "]" : null);
    synchronized (lock) {
      switch (state) {
        case CREATED:
          break;
        case CONNECTED:
          if (tracing) logger.trace("connect", idstr + " already connected.");
          return;
        case CLOSED:
          if (tracing) logger.trace("connect", idstr + " already closed.");
          throw new IOException("Connector already closed.");
        default:
          // should never happen
          if (tracing) logger.trace("connect", idstr + " unknown state: " + state);
          throw new IOException("Invalid state (" + state + ")");
      }
      Map<String, Object> tmpEnv = null;
      if (this.env == null) tmpEnv = Collections.emptyMap();
      else tmpEnv = new HashMap<>(this.env);
      if (env != null) {
        EnvHelp.checkAttributes(env);
        tmpEnv.putAll(env);
      }
      MessageConnection conn = (MessageConnection) tmpEnv.get(MESSAGE_CONNECTION);
      if (conn == null) {
        connection = DefaultConfig.getClientSynchroMessageConnection(tmpEnv);
        if (connection == null) {
          if (tracing) logger.trace("connect", idstr + " No MessageConnection");
          throw new IllegalArgumentException("No MessageConnection");
        }
        if (tracing) logger.trace("connect", "The connection uses a user " + "specific Synchronous message connection.");
      } else {
        requestHandler = new RequestHandler();
        connection = new ClientSynchroMessageConnectionImpl(conn, requestHandler, tmpEnv);
        if (tracing) logger.trace("connect", "The connection uses a user " + "specific Asynchronous message connection.");
      }
      connection.connect(tmpEnv);
      connectionId = connection.getConnectionId();
      objectWrapping = (ObjectWrapping) tmpEnv.get(OBJECT_WRAPPING);
      if (objectWrapping == null) objectWrapping = new ObjectWrappingImpl();
      clientMBeanServer = new ClientIntermediary(connection, objectWrapping, this, tmpEnv);
      this.env = tmpEnv;
      state = CONNECTED;
      if (tracing) logger.trace("connect", idstr + " " + connectionId + " Connected.");
    }
    sendNotification(new JMXConnectionNotification(JMXConnectionNotification.OPENED, this, connectionId, clientNotifID++, null, null));
  }

  @Override
  public String getConnectionId() throws IOException {
    checkState();
    return connection.getConnectionId();
  }

  // implements client interface here
  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return getMBeanServerConnection(null);
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(final Subject delegationSubject) throws IOException {
    checkState();
    if (rmbscMap.containsKey(delegationSubject)) return rmbscMap.get(delegationSubject);
    else {
      RemoteMBeanServerConnection rmbsc = new RemoteMBeanServerConnection(clientMBeanServer, delegationSubject);
      rmbscMap.put(delegationSubject, rmbsc);
      return rmbsc;
    }
  }

  @Override
  public void close() throws IOException {
    close(false, "The connection is closed by a user.");
  }

  /**
   * 
   * @param local .
   * @param msg .
   * @throws IOException .
   */
  private void close(final boolean local, final String msg) throws IOException {
    final boolean tracing = logger.traceOn();
    final boolean debug = logger.debugOn();
    final String idstr = (tracing ? "[" + this.toString() + "]" : null);
    Exception closeException;
    boolean createdState;
    synchronized (lock) {
      if (state == CLOSED) {
        if (tracing) logger.trace("close", idstr + " already closed.");
        return;
      }
      createdState = (state == CREATED);
      state = CLOSED;
      closeException = null;
      if (tracing) logger.trace("close", idstr + " closing.");
      if (!createdState) {
        if (!local) { // inform the remote side of termination
          try {
            synchronized (connection) {
              connection.sendOneWay(new CloseMessage(msg));
              Thread.sleep(100);
            }
          } catch (InterruptedException ire) { // OK
          } catch (Exception e1) {
            closeException = e1; // error trace
            if (tracing) logger.trace("close", idstr + " failed to send close message: " + e1);
            if (debug) logger.debug("close", e1);
          }
        }
        try { // close the transport protocol.
          connection.close();
        } catch (Exception e1) {
          closeException = e1;
          if (tracing) logger.trace("close", idstr + " failed to close MessageConnection: " + e1);
          if (debug) logger.debug("close", e1);
        }
      }
      if (clientMBeanServer != null) clientMBeanServer.terminate();
      // Clean up MBeanServerConnection table
      rmbscMap.clear();
    }
    // if not connected, no need to send closed notif
    if (!createdState) sendNotification(new JMXConnectionNotification(JMXConnectionNotification.CLOSED, this, connectionId, clientNotifID++, "The client has been closed.", null));
    if (closeException != null) {
      if (closeException instanceof RuntimeException) throw (RuntimeException) closeException;
      if (closeException instanceof IOException) throw (IOException) closeException;
      final IOException x = new IOException("Failed to close: " + closeException);
      throw (IOException) EnvHelp.initCause(x, closeException);

    }
    if (tracing) logger.trace("close", idstr + " closed.");
  }

  @Override
  public void addConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    if (listener == null) throw new NullPointerException("listener");
    connectionBroadcaster.addNotificationListener(listener, filter, handback);
  }

  @Override
  public void removeConnectionNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    if (listener == null) throw new NullPointerException("listener");
    connectionBroadcaster.removeNotificationListener(listener);
  }

  @Override
  public void removeConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    if (listener == null) throw new NullPointerException("listener");
    connectionBroadcaster.removeNotificationListener(listener, filter, handback);
  }

  /**
   * <p> Send a notification to the connection listeners. The notification will be sent to
   * every listener added with {@link #addConnectionNotificationListener addConnectionNotificationListener}
   * that was not subsequently removed by a <code>removeConnectionNotificationListener</code>, provided the
   * corresponding {@link NotificationFilter} matches.
   * @param n the notification to send. This will usually be a {@link JMXConnectionNotification}, but an implementation can send other notifications as well.
   */
  protected void sendNotification(final Notification n) {
    Runnable job = new Runnable() {
      @Override
      public void run() {
        try {
          connectionBroadcaster.sendNotification(n);
        } catch (Exception e) { // OK. should never
        }
      }
    };
    ThreadService.getShared().handoff(job);
  }

  /**
   * 
   */
  private class RequestHandler implements SynchroCallback {
    @Override
    public Message execute(final Message msg) {
      if (msg instanceof CloseMessage) {
        if (logger.traceOn()) logger.trace("RequestHandler.execute", "got Message REMOTE_TERMINATION");
        try { // try to re-connect anyway
          com.sun.jmx.remote.opt.internal.ClientCommunicatorAdmin admin = clientMBeanServer.getCommunicatorAdmin();
          admin.gotIOException(new IOException(""));
          return null;
        } catch (IOException ioe) { // OK. the server has been closed.
        }
        try {
          GenericConnector.this.close(true, null);
        } catch (IOException ie) { // OK never
        }
      } else {
        logger.warning("RequestHandler.execute", ((msg == null) ? "null" : msg.getClass().getName()) + ": Bad message type.");
        try {
          logger.warning("RequestHandler.execute", "Closing connector");
          GenericConnector.this.close(false, null);
        } catch (IOException ie) {
          logger.info("RequestHandler.execute", ie);
        }
      }
      return null;
    }

    @Override
    public void connectionException(final Exception e) {
      synchronized (lock) {
        if (state != CONNECTED) return;
      }
      logger.warning("RequestHandler-connectionException", e);
      if (e instanceof IOException) {
        try {
          com.sun.jmx.remote.opt.internal.ClientCommunicatorAdmin admin = clientMBeanServer.getCommunicatorAdmin();
          admin.gotIOException((IOException) e);
          return;
        } catch (IOException ioe) { // OK. closing at the following steps
        }
      }
      synchronized (lock) {
        if (state == CONNECTED) {
          logger.warning("RequestHandler-connectionException", "Got connection exception: " + e.toString());
          logger.debug("RequestHandler-connectionException", "Got connection exception: " + e.toString(), e);
          try {
            GenericConnector.this.close(true, null);
          } catch (IOException ie) {
            logger.info("RequestHandler-execute", ie);
          }
        }
      }
    }
  }

  /**
   * 
   */
  private static class ResponseMsgWrapper {
    /**
     * 
     */
    public boolean got = false;
    /**
     * 
     */
    public Message msg = null;

    /**
     * 
     */
    public ResponseMsgWrapper() {
    }

    /**
     * 
     * @param msg .
     */
    public void setMsg(final Message msg) {
      got = true;
      this.msg = msg;
    }
  }

  /**
   * Called by a ClientIntermediary to reconnect the transport because the server has been closed after its timeout.
   * @return .
   * @throws IOException .
   */
  ClientSynchroMessageConnection reconnect() throws IOException {
    synchronized (lock) {
      if (state != CONNECTED) throw new IOException("The connector is not at the connection state.");
    }
    sendNotification(new JMXConnectionNotification(JMXConnectionNotification.FAILED, this, connectionId, clientNotifID++, "The client has got connection exception.", null));
    connection.connect(env);
    connectionId = connection.getConnectionId();
    sendNotification(new JMXConnectionNotification(JMXConnectionNotification.OPENED, this, connectionId, clientNotifID++, "The client has succesfully reconnected to the server.", null));
    return connection;
  }

  /**
   * 
   * @throws IOException .
   */
  private void checkState() throws IOException {
    synchronized (lock) {
      if (state == CREATED) throw new IOException("The client has not been connected.");
      else if (state == CLOSED) throw new IOException("The client has been closed.");
    }
  }
}
