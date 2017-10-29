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

package org.jppf.management;

import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import javax.management.MBeanServerConnection;
import javax.management.remote.*;
import javax.management.remote.generic.GenericConnector;

import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Wrapper around a JMX connection, providing a thread-safe way of handling disconnections and recovery.
 * @author Laurent Cohen
 */
public abstract class AbstractJMXConnectionWrapper extends ThreadSynchronization implements JPPFAdminMBean, AutoCloseable {
  /** Logger for this class. */
  private static Logger log = LoggerFactory.getLogger(AbstractJMXConnectionWrapper.class);
  /** Determines whether debug log statements are enabled. */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /** Prefix for the name given to the connection thread. */
  public static String CONNECTION_NAME_PREFIX = "jmx@";
  /** The timeout in millis for JMX connection attempts. A value of 0 or less means no timeout. */
  static final long CONNECTION_TIMEOUT = JPPFConfiguration.get(JPPFProperties.MANAGEMENT_CONNECTION_TIMEOUT);
  /** URL of the MBean server, in a JMX-compliant format. */
  protected JMXServiceURL url;
  /** The JMX client. */
  protected JMXConnector jmxc;
  /** A connection to the MBean server. */
  protected AtomicReference<MBeanServerConnection> mbeanConnection = new AtomicReference<>(null);
  /** The host the server is running on. */
  protected String host;
  /** The RMI port used by the server. */
  protected int port;
  /** The connection thread that performs the connection to the management server. */
  protected AtomicReference<JMXConnectionThread> connectionThread = new AtomicReference<>(null);
  /** A string representing this connection, used for logging purposes. */
  protected String idString;
  /** A string representing this connection, used for displaying in the admin conosle. */
  protected String displayName;
  /** Determines whether the connection to the JMX server has been established. */
  protected AtomicBoolean connected = new AtomicBoolean(false);
  /** Determines whether this connection has been closed by a all to the {@link #close()} method. */
  protected AtomicBoolean closed = new AtomicBoolean(false);
  /** Determines whether the connection to the JMX server has been established. */
  protected boolean local;
  /** JMX properties used for establishing the connection. */
  protected Map<String, Object> env = new HashMap<>();
  /** Determines whether the JMX connection should be secure or not. */
  protected boolean sslEnabled;
  /**  */
  final Object connectionLock = new Object();
  /** The list of listeners to this connection wrapper. */
  final List<JMXWrapperListener> listeners = new CopyOnWriteArrayList<>();
  /** The time at which connection attempts started. */
  long connectionStart;
  /** */
  boolean reconnectOnError = true;

  /**
   * Initialize a local connection (same JVM) to the MBean server.
   */
  public AbstractJMXConnectionWrapper() {
    local = true;
    idString = displayName = "local";
    host = "local";
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param sslEnabled specifies whether the jmx connection should be secure or not.
   */
  public AbstractJMXConnectionWrapper(final String host, final int port, final boolean sslEnabled) {
    try {
      this.host = (NetworkUtils.isIPv6Address(host)) ? "[" + host + "]" : host;
      this.port = port;
      this.sslEnabled = sslEnabled;
      idString = this.host + ':' + this.port;
      this.displayName = this.idString;
      url = new JMXServiceURL("service:jmx:jmxmp://" + idString);
      if (sslEnabled) SSLHelper.configureJMXProperties(env);
      env.put(GenericConnector.OBJECT_WRAPPING, JMXMPServer.newObjectWrapping());
      env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "com.sun.jmx.remote.protocol");
      env.put(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER, getClass().getClassLoader());
      env.put(JMXConnectorFactory.DEFAULT_CLASS_LOADER, getClass().getClassLoader());
      env.put("jmx.remote.x.server.max.threads", 1);
      env.put("jmx.remote.x.client.connection.check.period", 0);
      env.put("jmx.remote.x.request.timeout", JPPFConfiguration.get(JPPFProperties.JMX_REQUEST_TIMEOUT));
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    local = false;
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
    long elapsed;
    synchronized(this) {
      elapsed = (System.nanoTime() - connectionStart) / 1_000_000L;
    }
    if ((CONNECTION_TIMEOUT > 0L) && (elapsed >= CONNECTION_TIMEOUT)) {
      fireTimeout();
      close();
      return;
    }
    synchronized(connectionLock) {
      if (jmxc == null) jmxc = JMXConnectorFactory.newJMXConnector(url, env);
      jmxc.connect();
      connectionThread.get().close();
      connectionThread.set(null);
    }
    synchronized(this) {
      mbeanConnection.set(jmxc.getMBeanServerConnection());
      try {
        setHost(InetAddress.getByName(host).getHostName());
      } catch (@SuppressWarnings("unused") UnknownHostException e) {
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
      } catch(Exception e2) {
        if (debugEnabled) log.debug(e2.getMessage(), e2);
      }
      jmxc = null;
    }
    if (isReconnectOnError()) connect();
  }

  /**
   * Get the host the server is running on.
   * @return the host as a string.
   */
  public String getHost() {
    return host;
  }

  /**
   * Get the host the server is running on.
   * @param host the host as a string.
   */
  public void setHost(final String host) {
    this.host = host;
    this.displayName = this.host + ':' + this.port;
  }

  /**
   * Get a string describing this connection.
   * @return a string in the format host:port.
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
   * Add a listener to this connection wrapper
   * @param listener the listener to add.
   */
  public void addJMXWrapperListener(final JMXWrapperListener listener) {
    listeners.add(listener);
  }

  /**
   * Remove a listener from this connection wrapper
   * @param listener the listener to add.
   */
  public void removeJMXWrapperListener(final JMXWrapperListener listener) {
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the connection was successful.
   */
  protected void fireConnected() {
    final JMXWrapperEvent event = new JMXWrapperEvent(this);
    Runnable r = new Runnable() {
      @Override
      public void run() {
        for (JMXWrapperListener listener: listeners) listener.jmxWrapperConnected(event);
      }
    };
    new Thread(r, getDisplayName() + " connection notifier").start();
  }

  /**
   * Notify all listeners that the connection could not be established before reaching the timeout.
   */
  protected void fireTimeout() {
    JMXWrapperEvent event = new JMXWrapperEvent(this);
    for (JMXWrapperListener listener: listeners) listener.jmxWrapperTimeout(event);
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
}
