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

package org.jppf.management;

import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.management.MBeanServerConnection;
import javax.management.remote.*;

import org.jppf.jmx.*;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Wrapper around a JMX connection, providing a thread-safe way of handling disconnections and recovery.
 * @author Laurent Cohen
 */
public abstract class AbstractJMXConnectionWrapper extends ThreadSynchronization implements JPPFAdminMBean, AutoCloseable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AbstractJMXConnectionWrapper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Prefix for the name given to the connection thread.
   */
  static final String CONNECTION_NAME_PREFIX = "jmx@";
  /**
   * The timeout in millis for JMX connection attempts. A value of 0 or less means no timeout.
   */
  static final long CONNECTION_TIMEOUT = JPPFConfiguration.get(JPPFProperties.MANAGEMENT_CONNECTION_TIMEOUT);
  /**
   * URL of the MBean server, in a JMX-compliant format.
   */
  JMXServiceURL url;
  /**
   * The JMX client.
   */
  JMXConnector jmxc;
  /**
   * A connection to the MBean server.
   */
  AtomicReference<MBeanServerConnection> mbeanConnection = new AtomicReference<>(null);
  /**
   * The host the server is running on.
   */
  String host;
  /**
   * The port used by the server.
   */
  int port;
  /**
   * The connection thread that performs the connection to the management server.
   */
  AtomicReference<JMXConnectionThread> connectionThread = new AtomicReference<>(null);
  /**
   * A string representing this connection, used for logging purposes.
   */
  String idString;
  /**
   * A string representing this connection, used for displaying in the admin conosle.
   */
  String displayName;
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  AtomicBoolean connected = new AtomicBoolean(false);
  /**
   * Determines whether this connection has been closed by a all to the {@link #close()} method.
   */
  AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  boolean local;
  /**
   * JMX properties used for establishing the connection.
   */
  Map<String, Object> env = new HashMap<>();
  /**
   * Determines whether the JMX connection should be secure or not.
   */
  boolean sslEnabled;
  /**
   * Used to synchronize during the connection process.
   */
  final Object connectionLock = new Object();
  /**
   * The time at which connection attempts started.
   */
  long connectionStart;
  /**
   * Whether to try to reconnect upon error.
   */
  boolean reconnectOnError = true;
  /**
   * The JMX remote protocol.
   */
  private final String protocol;
  /**
   * The last exception received when attempting to connect, if any.
   */
  Throwable lastConnectionException;

  /**
   * Initialize a local connection (same JVM) to the MBean server.
   */
  public AbstractJMXConnectionWrapper() {
    local = true;
    idString = displayName = "local";
    host = "local";
    this.protocol = JMXHelper.LOCAL_PROTOCOL;
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param sslEnabled specifies whether the jmx connection should be secure or not.
   */
  public AbstractJMXConnectionWrapper(final String host, final int port, final boolean sslEnabled) {
    this(JPPFConfiguration.get(JPPFProperties.JMX_REMOTE_PROTOCOL), host, port, sslEnabled);
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param protocol the JMX remote protocol to use.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param sslEnabled specifies whether the jmx connection should be secure or not.
   */
  AbstractJMXConnectionWrapper(final String protocol, final String host, final int port, final boolean sslEnabled) {
    this.protocol = protocol;
    try {
      this.host = (NetworkUtils.isIPv6Address(host)) ? "[" + host + "]" : host;
      this.port = port;
      this.sslEnabled = sslEnabled;
      idString = this.host + ':' + this.port;
      this.displayName = this.idString;
      //url = new JMXServiceURL("service:jmx:jmxmp://" + idString);
      url = new JMXServiceURL(protocol, host, port);
      if (sslEnabled) SSLHelper.configureJMXProperties(protocol, env);
      if (JMXHelper.JMXMP_PROTOCOL.equals(protocol)) initJMXMP();
      else initJPPF();
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) cl = getClass().getClassLoader();
      env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER, cl);
      env.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, cl);
      if (debugEnabled) log.debug("created {} with sslEnabled={}, url={}, env={}", getClass().getSimpleName(), this.sslEnabled, url, env);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    local = false;
  }

  /**
   * Initialize the environment for the JMXMP protocol.
   * @throws Exception if any error occcurs.
   */
  private void initJMXMP() throws Exception {
    env.put("jmx.remote.object.wrapping", JMXMPServer.newObjectWrapping());
    env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "com.sun.jmx.remote.protocol");
    env.put("jmx.remote.x.server.max.threads", 1);
    env.put("jmx.remote.x.client.connection.check.period", 0);
    env.put("jmx.remote.x.request.timeout", JPPFConfiguration.get(JPPFProperties.JMX_REMOTE_REQUEST_TIMEOUT));
  }

  /**
   * Initialize the environment for the JPPF JMX remote protocol.
   * @throws Exception if any error occcurs.
   */
  private void initJPPF() throws Exception {
    env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "org.jppf.jmxremote.protocol");
    env.put(JPPFJMXProperties.REQUEST_TIMEOUT.getName(), JPPFConfiguration.get(JPPFJMXProperties.REQUEST_TIMEOUT));
    env.put(JPPFJMXProperties.TLS_ENABLED.getName(), Boolean.valueOf(sslEnabled).toString());
  }

  /**
   * Initialize the connection to the remote MBean server.
   */
  public abstract void connect();

  /**
   * Initiate the connection and wait until the connection is established or the timeout has expired, whichever comes first.
   * @param timeout the maximum time to wait for, a value of zero means no timeout and
   * this method just waits until the connection is established.
   * @return {@code true} if the connection was established in the specified time, {@code false} otherwise.
   */
  public abstract boolean connectAndWait(final long timeout);

  /**
   * Initialize the connection to the remote MBean server.
   * @throws Exception if the connection could not be established.
   */
  void performConnection() throws Exception {
    connected.set(false);
    final long elapsed;
    synchronized(this) {
      elapsed = (System.nanoTime() - connectionStart) / 1_000_000L;
    }
    if ((CONNECTION_TIMEOUT > 0L) && (elapsed >= CONNECTION_TIMEOUT)) {
      fireTimeout();
      close();
      return;
    }
    synchronized(connectionLock) {
      if (jmxc == null) {
        jmxc = JMXConnectorFactory.newJMXConnector(url, env);
        jmxc.addConnectionNotificationListener((notification, handback) -> {
          if (JMXConnectionNotification.FAILED.equals(notification.getType())) reset();
        }, null, null);
      }
      jmxc.connect();
      connectionThread.get().setStopped(true);
      connectionThread.set(null);
    }
    synchronized(this) {
      mbeanConnection.set(jmxc.getMBeanServerConnection());
      try {
        setHost(InetAddress.getByName(host).getHostName());
      } catch (final UnknownHostException e) {
        if (debugEnabled) log.debug("unable to set host", e);
      }
    }
    connected.set(true);
    wakeUp();
    fireConnected();
    if (debugEnabled) log.debug(getId() + " JMX connection successfully established");
  }

  /**
   * Reset the JMX connection and attempt to reconnect.
   */
  void reset() {
    connected.set(false);
    if (jmxc != null) {
      try {
        jmxc.close();
      } catch(final Exception e2) {
        if (debugEnabled) log.debug(e2.getMessage(), e2);
      }
      jmxc = null;
    }
    if (isReconnectOnError()) connect();
  }

  /**
   * Get the host the server is running on.
   * @param host the host as a string.
   */
  private void setHost(final String host) {
    this.host = host;
    this.displayName = this.host + ':' + this.port;
  }

  /**
   * Get a string describing this connection.
   * @return a string in the format {@code host:port}.
   */
  public String getId() {
    return idString;
  }

  /**
   * Get the string representing this connection, used for displaying in the admin conosle.
   * @return the display name as a string.
   */
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public String toString() {
    return  new StringBuilder(getClass().getSimpleName()).append('[').append("url=").append(url).append(", connected=").append(connected)
      .append(", local=").append(local).append(", secure=").append(sslEnabled).append(']').toString();
  }

  /**
   * @return Whether this connection wrapper reconnects on error.
   * @exclude
   */
  public synchronized boolean isReconnectOnError() {
    return reconnectOnError;
  }

  /**
   * Specifiy whether this connection wrapper reconnects on error.
   * @param reconnectOnError {@code true} to reconnect, {@code false} otherwise.
   * @exclude
   */
  public synchronized void setReconnectOnError(final boolean reconnectOnError) {
    this.reconnectOnError = reconnectOnError;
  }

  /**
   * Get the JMX remote protocol used.
   * @return the JMX remote protocol string.
   * @exclude
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * @return the last exception received when attempting to connect, if any.
   * @exclude
   */
  public Throwable getLastConnectionException() {
    return lastConnectionException;
  }

  /**
   * Notify all listeners that the connection was successful.
   */
  abstract void fireConnected();

  /**
   * Notify all listeners that the connection could not be established before reaching the timeout.
   */
  abstract void fireTimeout();
}
