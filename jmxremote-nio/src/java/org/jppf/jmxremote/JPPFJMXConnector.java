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

import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

import javax.management.*;
import javax.management.remote.*;
import javax.security.auth.Subject;

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.*;
import org.jppf.jmxremote.message.JMXMessageHandler;
import org.jppf.jmxremote.nio.*;
import org.jppf.utils.JPPFIdentifiers;
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
   *
   * @param serviceURL the address of this connector.
   * @param environment the environment for this connector.
   */
  public JPPFJMXConnector(final JMXServiceURL serviceURL, final Map<String, ?> environment) {
    this.environment = (environment == null) ? new HashMap<String, Object>() : new HashMap<>(environment);
    this.address = serviceURL;
  }

  @Override
  public void connect() throws IOException {
    connect(null);
  }

  @Override
  public void connect(final Map<String, ?> env) throws IOException {
    if (env != null) environment.putAll(env);
    final Boolean tls = (Boolean) environment.get("jppf.jmx.remote.tls.enabled");
    secure = (tls == null) ? false : tls;
    try {
      init();
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
  }

  @Override
  public void removeConnectionNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
  }

  @Override
  public void removeConnectionNotificationListener(final NotificationListener l, final NotificationFilter f, final Object handback) throws ListenerNotFoundException {
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
   * Initialize this connector.
   * @throws Exception if an error is raised during initialization.
   */
  private synchronized void init() throws Exception {
    final String host = address.getHost();
    final int port = address.getPort();
    @SuppressWarnings("resource")
    final SocketChannelClient socketClient =  new SocketChannelClient(host, port, true);
    if (debugEnabled) log.debug("Attempting connection to remote peer at {}", address);
    final SocketInitializerImpl socketInitializer = new SocketInitializerImpl();
    if (!socketInitializer.initializeSocket(socketClient)) {
      final Exception e = socketInitializer.getLastException();
      throw (e == null) ? new ConnectException("could not connect to remote JMX server " + address) : e;
    }
    if (!InterceptorHandler.invokeOnConnect(socketClient.getChannel())) throw new JPPFException("peer connection denied by interceptor");
    if (debugEnabled) log.debug("Connected to JMX server {}, sending channel identifier", address);
    socketClient.writeInt(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    if (debugEnabled) log.debug("Reconnected to JMX server {}", address);
    final JMXNioServer server = JMXNioServerPool.getServer();
    final ChannelsPair pair = server.createChannelsPair(environment, "", port, socketClient.getChannel(), secure, true);
    final JMXMessageHandler messageHandler = pair.getMessageHandler();
    mbsc = new JPPFMBeanServerConnection(messageHandler);
    pair.setMbeanServerConnection(mbsc);
    if (debugEnabled) log.debug("sending connection request");
    connectionID = mbsc.receiveConnectionID();
    pair.setConnectionID(connectionID);
  }
}
