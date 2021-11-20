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


import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.*;
import org.jppf.management.diagnostics.DiagnosticsMBean;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * Wrapper around a JMX connection, providing a thread-safe way of handling disconnections and recovery.
 * @author Laurent Cohen
 */
public class JMXConnectionWrapper extends AbstractJMXConnectionWrapper {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXConnectionWrapper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The list of listeners to this connection wrapper.
   */
  final List<JMXConnectionWrapperListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Initialize a local connection (same JVM) to the MBean server.
   */
  public JMXConnectionWrapper() {
    super();
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param sslEnabled specifies whether the jmx connection should be secure or not.
   */
  public JMXConnectionWrapper(final String host, final int port, final boolean sslEnabled) {
    super(host, port, sslEnabled);
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param protocol the JMX remote protocol to use.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param sslEnabled specifies whether the jmx connection should be secure or not.
   */
  JMXConnectionWrapper(final String protocol, final String host, final int port, final boolean sslEnabled) {
    super(protocol, host, port, sslEnabled);
  }

  /**
   * Initialize the connection to the remote MBean server.
   */
  @Override
  public void connect() {
    if (isConnected()) return;
    if (local) {
      mbeanConnection.compareAndSet(null, ManagementFactory.getPlatformMBeanServer());
      connected.set(true);
      fireConnected();
    } else {
      JMXConnectionThread jct = null;
      synchronized(this) {
        if ((jct = connectionThread.get()) == null) {
          jct = new JMXConnectionThread(this);
          connectionThread.set(jct);
          connectionStart = System.nanoTime();
          ThreadUtils.startDaemonThread(jct, CONNECTION_NAME_PREFIX + getId());
        }
      }
    }
  }

  /**
   * Initiate the connection and wait until the connection is established or the timeout has expired, whichever comes first.
   * @param timeout the maximum time to wait for, a value of zero means no timeout and
   * this method just waits until the connection is established.
   * @return {@code true} if the connection was established in the specified time, {@code false} otherwise. 
   */
  @Override
  public boolean connectAndWait(final long timeout) {
    if (isConnected()) return true;
    final long start = System.nanoTime();
    final long max = timeout > 0 ? timeout : Long.MAX_VALUE;
    connect();
    long elapsed;
    while (!isConnected() && ((elapsed = (System.nanoTime() - start) / 1_000_000L) < max)) goToSleep(Math.min(10L, max - elapsed));
    return isConnected();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      connected.compareAndSet(true, false);
      listeners.clear();
      final JMXConnectionThread jct = connectionThread.get();
      if (jct != null) {
        jct.close();
        connectionThread.set(null);
      }
      if (jmxc != null) {
        final JMXConnector connector = jmxc;
        jmxc = null;
        final Runnable r = () -> {
          try {
            connector.close();
          } catch (final Exception e) {
            if (debugEnabled) log.debug(e.getMessage(), e);
          }
        };
        ThreadUtils.startThread(r, getDisplayName() + " closing");
      }
    }
  }

  /**
   * Invoke a method on the specified MBean.
   * @param mbeanName the name of the MBean.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return an object or null.
   * @throws Exception if the invocation failed.
   */
  public Object invoke(final String mbeanName, final String methodName, final Object[] params, final String[] signature) throws Exception {
    if (!isConnected()) {
      log.warn("invoking mbean '{}' method '{}({})' while not connected", mbeanName, methodName, (signature == null ? "" : StringUtils.arrayToString(signature)));
      return null;
    }
    Object result = null;
    try {
      final ObjectName objectName = ObjectNameCache.getObjectName(mbeanName);
      result = getMbeanConnection().invoke(objectName, methodName, params, signature);
    } catch(final IOException e) {
      final String msg = String.format("error invoking mbean '%s' method '%s(%s)' while not connected%n%s", mbeanName, methodName, StringUtils.arrayToString(signature), ExceptionUtils.getStackTrace(e));
      if (debugEnabled) log.debug(msg);
      reset();
    }
    return result;
  }

  /**
   * Invoke a method on the specified MBean.
   * This is a convenience method to be used when invoking a remote MBean method with no parameters.<br/>
   * This is equivalent to calling <code>invoke(name, methodName, (Object[]) null, (String[]) null)</code>.
   * @param mbeanName the name of the MBean.
   * @param methodName the name of the method to invoke.
   * @return an object or null.
   * @throws Exception if the invocation failed.
   */
  public Object invoke(final String mbeanName, final String methodName) throws Exception {
    return invoke(mbeanName, methodName, (Object[]) null, (String[]) null);
  }

  /**
   * Get the value of an attribute of the specified MBean.
   * @param mbeanName the name of the MBean.
   * @param attribute the name of the attribute to read.
   * @return an object or null.
   * @throws Exception if the invocation failed.
   */
  public Object getAttribute(final String mbeanName, final String attribute) throws Exception {
    if (!isConnected()) {
      log.warn("getting mbean '{}' attribute '{}' while not connected", mbeanName, attribute);
      return null;
    }
    Object result = null;
    try {
      final ObjectName objectName = ObjectNameCache.getObjectName(mbeanName);
      result = getMbeanConnection().getAttribute(objectName, attribute);
    } catch(final IOException e) {
      if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
      reset();
      throw e;
    }
    return result;
  }

  /**
   * Set the value of an attribute of the specified MBean.
   * @param mbeanName the name of the MBean.
   * @param attribute the name of the attribute to write.
   * @param value the value to set on the attribute.
   * @throws Exception if the invocation failed.
   */
  public void setAttribute(final String mbeanName, final String attribute, final Object value) throws Exception {
    if (!isConnected()) {
      log.warn("setting mbean '{}' attribute '{}' while not connected", mbeanName, attribute);
      return;
    }
    try {
      final ObjectName objectnName = ObjectNameCache.getObjectName(mbeanName);
      getMbeanConnection().setAttribute(objectnName, new Attribute(attribute, value));
    } catch(final IOException e) {
      if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
      reset();
      throw e;
    }
  }

  /**
   * Get the host the server is running on.
   * @return the host as a string.
   */
  public String getHost() {
    return host;
  }

  /**
   * Get the JMX remote port used by the server.
   * @return the port as an int.
   */
  public int getPort() {
    return port;
  }

  /**
   * Get the service URL of the MBean server.
   * @return a {@link JMXServiceURL} instance, or {@code null} if this object represents a local MBean server connection.
   */
  public JMXServiceURL getURL() {
    return url;
  }

  /**
   * Get the connection to the MBean server.
   * @return a <code>MBeanServerConnection</code> instance.
   */
  public MBeanServerConnection getMbeanConnection() {
    return mbeanConnection.get();
  }

  /**
   * Determines whether the connection to the JMX server has been established.
   * @return true if the connection is established, false otherwise.
   */
  public boolean isConnected() {
    return connected.get();
  }

  /**
   * Obtain a proxy to the specified remote MBean.
   * @param <T> the type of the MBean (must be an interface).
   * @param name the name of the mbean to retrieve.
   * @param inf the class of the MBean interface.
   * @return an instance of the specified proxy interface.
   * @throws Exception if any error occurs.
   */
  public <T> T getProxy(final String name, final Class<T> inf) throws Exception {
    return getProxy(ObjectNameCache.getObjectName(name), inf);
  }

  /**
   * Obtain a proxy to the specified remote MBean.
   * @param <T> the type of the MBean (must be an interface).
   * @param objectName the name of the mbean to retrieve.
   * @param inf the class of the MBean interface.
   * @return an instance of the specified proxy interface.
   * @throws Exception if any error occurs.
   */
  public <T> T getProxy(final ObjectName objectName, final Class<T> inf) throws Exception {
    if (!isConnected()) connect();
    if (isConnected()) {
      final MBeanServerConnection mbsc = getMbeanConnection();
      return MBeanInvocationHandler.newMBeanProxy(inf, mbsc, objectName);
    }
    return null;
  }

  /**
   * Get a proxy to the diagnostics/JVM health MBean.
   * @return a DiagnosticsMBean instance.
   * @throws Exception if any error occurs.
   */
  public DiagnosticsMBean getDiagnosticsProxy() throws Exception {
    throw new JPPFUnsupportedOperationException("this method can only be invoked on a subclass of " + getClass().getName());
  }

  /**
   * Adds a listener to the specified MBean.
   * @param mBeanName the name of the MBean on which the listener should be added.
   * @param listener the listener to add.
   * @throws Exception if any error occurs.
   */
  public void addNotificationListener(final String mBeanName, final NotificationListener listener) throws Exception {
    mbeanConnection.get().addNotificationListener(ObjectNameCache.getObjectName(mBeanName), listener, null, null);
  }

  /**
   * Adds a listener to the specified MBean.
   * @param mBeanName the name of the MBean on which the listener should be added.
   * @param listener the listener to add.
   * @param filter the filter object.
   * @param handback the handback object to use.
   * @throws Exception if any error occurs.
   */
  public void addNotificationListener(final String mBeanName, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    mbeanConnection.get().addNotificationListener(ObjectNameCache.getObjectName(mBeanName), listener, filter, handback);
  }

  /**
   * Remove a listener from the specified MBean.
   * @param mBeanName the name of the MBean from which the listener should be removed.
   * @param listener the listener to remove.
   * @throws Exception if any error occurs.
   */
  public void removeNotificationListener(final String mBeanName, final NotificationListener listener) throws Exception {
    mbeanConnection.get().removeNotificationListener(ObjectNameCache.getObjectName(mBeanName), listener, null, null);
  }

  /**
   * Remove a listener from the specified MBean.
   * @param mBeanName the name of the MBean from which the listener should be removed.
   * @param listener the listener to remove.
   * @param filter the filter object.
   * @param handback the handback object used.
   * @throws Exception if any error occurs.
   */
  public void removeNotificationListener(final String mBeanName, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    mbeanConnection.get().removeNotificationListener(ObjectNameCache.getObjectName(mBeanName), listener, filter, handback);
  }

  /**
   * Get the {@link MBeanNotificationInfo} descriptors for the specified MBean.
   * @param mBeanName the name of the MBean.
   * @return a an array of {@link MBeanNotificationInfo}, which is empty if the MBean does not implement {@link NotificationBroadcaster}.
   * @throws Exception if any error occurs.
   */
  public MBeanNotificationInfo[] getNotificationInfo(final String mBeanName) throws Exception {
    return mbeanConnection.get().getMBeanInfo(ObjectNameCache.getObjectName(mBeanName)).getNotifications();
  }

  @Override
  public JPPFSystemInformation systemInformation() throws Exception {
    throw new JPPFException("this method is not implemented");
  }

  /**
   * Determine whether the JMX connection is secure or not.
   * @return <code>true</code> if this connection is secure, <code>false</code> otherwise.
   */
  public boolean isSecure() {
    return sslEnabled;
  }

  /**
   * The JMX client connector.
   * @return a {@link JMXConnector} instance.
   */
  public JMXConnector getJmxconnector() {
    return jmxc;
  }

  /**
   * Add a listener to this connection wrapper.
   * @param listener the listener to add.
   */
  public void addJMXConnectionWrapperListener(final JMXConnectionWrapperListener listener) {
    listeners.add(listener);
  }

  /**
   * Remove a listener from this connection wrapper.
   * @param listener the listener to add.
   */
  public void removeJMXConnectionWrapperListener(final JMXConnectionWrapperListener listener) {
    listeners.remove(listener);
  }

  @Override
  void fireConnected() {
    final JMXConnectionWrapperEvent event = new JMXConnectionWrapperEvent(this);
    for (final JMXConnectionWrapperListener listener: listeners) listener.onConnected(event);
  }

  @Override
  void fireTimeout() {
    final JMXConnectionWrapperEvent event = new JMXConnectionWrapperEvent(this);
    for (final JMXConnectionWrapperListener listener: listeners) listener.onConnectionTimeout(event);
  }
}
