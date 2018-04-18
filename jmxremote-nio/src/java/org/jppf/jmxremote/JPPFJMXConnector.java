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

package org.jppf.jmxremote;

import static org.jppf.jmxremote.message.JMXMessageType.*;

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.*;
import org.jppf.jmx.*;
import org.jppf.jmxremote.message.*;
import org.jppf.jmxremote.nio.*;
import org.jppf.jmxremote.nio.ChannelsPair.CloseCallback;
import org.jppf.jmxremote.notification.ClientListenerInfo;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Implementation of the {@link JMXConnector} interface for the JPPF JMX remote connector.
 * @author Laurent Cohen
 */
public class JPPFJMXConnector implements JMXConnector {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFJMXConnector.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The environment for this connector.
   */
  private final Map<String, Object> environment;
  /**
   * The address of this connector.
   */
  private final JMXServiceURL address;
  /**
   * Whether the connection is secured through TLS.
   */
  private boolean secure = false;
  /**
   * The mbean server connection.
   */
  private JPPFMBeanServerConnection mbsc;
  /**
   * The onnectin ID.
   */
  private String connectionID;
  /**
   * The list of connection notification listeners registered with this connector.
   */
  private final List<ConnectionListenerInfo> connectionListeners = new CopyOnWriteArrayList<>();
  /**
   * A sequence number for connection notifications.
   */
  private final AtomicInteger notificationSequence = new AtomicInteger(0);
  /**
   * Mapping of notification listener ids to actual listeners.
   */
  private final Map<Integer, ClientListenerInfo> notificationListenerMap = new HashMap<>();
  /**
   * The message handler.
   */
  private JMXMessageHandler messageHandler;

  /**
   *
   * @param serviceURL the address of this connector.
   * @param environment the environment for this connector.
   */
  public JPPFJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) {
    this.environment = (environment == null) ? new HashMap<String, Object>() : new HashMap<>(environment);
    this.address = serviceURL;
    if (debugEnabled) log.debug("initialized JPPFJMXConnector with serviceURL = {} and environment = {}", address, this.environment);
  }

  @Override
  public void connect() throws IOException {
    connect(null);
  }

  @Override
  public void connect(final Map<String, ?> env) throws IOException {
    if (debugEnabled) log.debug("env = {}, this.environment={}", env, this.environment);
    if (env != null) environment.putAll(env);
    final String s = JMXEnvHelper.getString(JPPFJMXProperties.TLS_ENABLED, environment, null);
    if (debugEnabled) log.debug("secure='{}'", s);
    final Boolean tls = Boolean.valueOf(s);
    secure = (tls == null) ? false : tls;
    try {
      init();
      fireConnectionNotification(false, null);
    } catch (final IOException e) {
      throw e;
    } catch (final Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    return mbsc;
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(final Subject delegationSubject) throws IOException {
    return mbsc;
  }

  @Override
  public void close() throws IOException {
    mbsc.close();
  }

  @Override
  public void addConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
    if (listener == null) return;
    connectionListeners.add(new ConnectionListenerInfo(listener, filter, handback));
  }

  @Override
  public void removeConnectionNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    if (listener == null) return;
    final List<ConnectionListenerInfo> toRemove = new ArrayList<>(connectionListeners.size());
    for (ConnectionListenerInfo info: connectionListeners)
      if (info.listener == listener) toRemove.add(info);
    if (toRemove.isEmpty()) throw new ListenerNotFoundException("could not find any matching listener");
    connectionListeners.removeAll(toRemove);
  }

  @Override
  public void removeConnectionNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    if (listener == null) return;
    ConnectionListenerInfo toRemove = null;
    for (ConnectionListenerInfo info: connectionListeners)
      if ((info.listener == listener) && (info.filter == filter) && (info.handback == handback)) {
        toRemove = info;
        break;
      }
    if (toRemove == null) throw new ListenerNotFoundException("could not find any matching listener");
    connectionListeners.remove(toRemove);
  }

  /**
   * Send a connection notification to all registered listeners.
   * @param isClose whether this is for a connection closing ({@code true}) or opening ({@code false}).
   * @param exception an optional exception that may have cause the connection to close.
   */
  private void fireConnectionNotification(final boolean isClose, final Exception exception) {
    if (debugEnabled) log.debug("isClose={}, exception={}", isClose, exception);
    final String type;
    if (isClose) type = (exception == null) ? JMXConnectionNotification.CLOSED : JMXConnectionNotification.FAILED;
    else type = JMXConnectionNotification.OPENED;
    if (debugEnabled) log.debug(String.format("firing notif with type=%s, exception=%s, connectionID=%s", type, ExceptionUtils.getMessage(exception), connectionID));
    final JMXConnectionNotification notif = new JMXConnectionNotification(type, this, connectionID, notificationSequence.incrementAndGet(), null, null);
    for (final ConnectionListenerInfo info: connectionListeners) {
      if ((info.filter == null) || info.filter.isNotificationEnabled(notif)) info.listener.handleNotification(notif, info.handback);
    }
  }

  @Override
  public String getConnectionId() throws IOException {
    return connectionID;
  }

  /**
   * @return the environment for this connector.
   */
  public Map<String, ?> getEnvironment() {
    return environment;
  }

  /**
   * @return the address of this connector.
   */
  public JMXServiceURL getAddress() {
    return address;
  }

  /**
   * @return the message handler.
   */
  JMXMessageHandler getMessageHandler() {
    return messageHandler;
  }

  /**
   * Initialize this connector.
   * @throws Exception if an error is raised during initialization.
   */
  private void init() throws Exception {
    @SuppressWarnings("resource")
    final SocketChannelClient socketClient =  new SocketChannelClient(address.getHost(), address.getPort(), true);
    if (debugEnabled) log.debug("Attempting connection to remote peer at {}", address);
    final SocketInitializer socketInitializer = new QueuingSocketInitializer();
    if (!socketInitializer.initialize(socketClient)) {
      final Exception e = socketInitializer.getLastException();
      throw (e == null) ? new ConnectException("could not connect to remote JMX server " + address) : e;
    }
    if (!InterceptorHandler.invokeOnConnect(socketClient.getChannel())) throw new JPPFException("connection denied by interceptor");
    if (debugEnabled) log.debug("Connected to JMX server {}, sending channel identifier {}", address, JPPFIdentifiers.serverName(JPPFIdentifiers.JMX_REMOTE_CHANNEL));
    socketClient.writeInt(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    if (debugEnabled) log.debug("Reconnected to JMX server {}, secure={}", address, secure);
    final JMXNioServer server = JMXNioServerPool.getServer();
    final ChannelsPair pair = server.createChannelsPair(environment, "", -1, socketClient.getChannel(), secure, true);
    pair.addCloseCallback(new CloseCallback() {
      @Override
      public void onClose(final Exception exception) {
        fireConnectionNotification(true, exception);
      }
    });
    messageHandler = pair.getMessageHandler();
    if (debugEnabled) log.debug("registering channel");
    server.registerChannel(pair, socketClient.getChannel());
    if (debugEnabled) log.debug("getting connection id");
    connectionID = messageHandler.receiveConnectionID(address);
    pair.setConnectionID(connectionID);
    if (debugEnabled) log.debug("received connectionId = {}", connectionID);
    mbsc = new JPPFMBeanServerConnection(this);
    pair.setJMXConnector(this);
  }

  /**
   * Handle a new received notification.
   * @param jmxNotification the notification message to process.
   * @throws Exception if any error occurs.
   */
  public void handleNotification(final JMXNotification jmxNotification) throws Exception {
    if (debugEnabled) log.debug("received notification {}", jmxNotification);
    final List<ClientListenerInfo> infos = new ArrayList<>(jmxNotification.getListenerIDs().length);
    synchronized(notificationListenerMap) {
      for (final Integer listenerID: jmxNotification.getListenerIDs()) {
        final ClientListenerInfo info = notificationListenerMap.get(listenerID);
        if (info != null) infos.add(info);
      }
    }
    for  (final ClientListenerInfo info: infos) info.getListener().handleNotification(jmxNotification.getNotification(), info.getHandback());
  }

  /**
   * Add a notification listener.
   * @param name the name of the MBean on which the listener should be added.
   * @param listener the listener object which will handle the notifications emitted by the registered MBean.
   * @param filter the filter object. If filter is null, no filtering will be performed before handling notifications.
   * @param handback the context to be sent to the listener when a notification is emitted.
   * @throws Exception if any error occurs.
   */
  void addNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    final int listenerID = (Integer) messageHandler.sendRequestWithResponse(ADD_NOTIFICATION_LISTENER, name, filter);
    synchronized(notificationListenerMap) {
      notificationListenerMap.put(listenerID, new ClientListenerInfo(listenerID, name, listener, filter, handback));
    }
  }

  /**
   * Removes a listener from a registered MBean.
   * @param name the name of the MBean on which the listener should be removed.
   * @param listener the listener to remove.
   * @throws Exception if any error occurs.
   */
  void removeNotificationListener(final ObjectName name, final NotificationListener listener) throws Exception {
    final List<ClientListenerInfo> toRemove = new ArrayList<>();
    synchronized(notificationListenerMap) {
      for (final Map.Entry<Integer, ClientListenerInfo> entry: notificationListenerMap.entrySet()) {
        final ClientListenerInfo info = entry.getValue();
        if (info.getMbeanName().equals(name) && (info.getListener() == listener)) toRemove.add(info);
      }
      if (toRemove.isEmpty()) throw new ListenerNotFoundException("no matching listener");
      final int[] ids = new int[toRemove.size()];
      for (int i=0; i<ids.length; i++) ids[i] = toRemove.get(i).getListenerID();
      messageHandler.sendRequestWithResponse(REMOVE_NOTIFICATION_LISTENER, name, ids);
      for (final int id: ids) notificationListenerMap.remove(id);
    }
  }

  /**
   * Removes a listener from a registered MBean.
   * @param name the name of the MBean on which the listener should be removed.
   * @param listener the listener to remove.
   * @param filter the filter that was specified when the listener was added.
   * @param handback the handback that was specified when the listener was added.
   * @throws Exception if any error occurs.
   */
  void removeNotificationListener(final ObjectName name, final NotificationListener listener, final NotificationFilter filter, final Object handback) throws Exception {
    ClientListenerInfo toRemove = null;
    synchronized(notificationListenerMap) {
      for (Map.Entry<Integer, ClientListenerInfo> entry: notificationListenerMap.entrySet()) {
        final ClientListenerInfo info = entry.getValue();
        if (info.getMbeanName().equals(name) && (info.getListener() == listener) && (info.getFilter() == filter) && (info.getHandback() == handback)) {
          toRemove = info;
          break;
        }
      }
      if (toRemove == null) throw new ListenerNotFoundException("no matching listener");
      messageHandler.sendRequestWithResponse(REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK, name, toRemove.getListenerID());
      notificationListenerMap.remove(toRemove.getListenerID());
    }
  }

  /**
   * Information for all registered connection listeners.
   */
  private final static class ConnectionListenerInfo {
    /**
     * The notification listener.
     */
    private final NotificationListener listener;
    /**
     * The notification filter.
     */
    private final NotificationFilter filter;
    /**
     * The handback object.
     */
    private final Object handback;

    /**
     * Initialize with the specified parameters.
     * @param listener the notification listener.
     * @param filter the notification filter.
     * @param handback the handback object.
     */
    private ConnectionListenerInfo(final NotificationListener listener, final NotificationFilter filter, final Object handback) {
      super();
      this.listener = listener;
      this.filter = filter;
      this.handback = handback;
    }
  }
}
