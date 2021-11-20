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

package org.jppf.comm.recovery;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.*;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Client-side connection for the recovery mechanism.
 * @author Laurent Cohen
 */
public class HeartbeatConnection extends AbstractHeartbeatConnection {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  private SocketInitializer socketInitializer;
  /**
   * The list of listeners to this object's events.
   */
  private final List<HeartbeatConnectionListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The host to connect to for echanging heartbeat mesages.
   */
  private String host;
  /**
   * The port to connect to.
   */
  private int port = -1;
  /**
   * Whether secure connectivity is enabled.
   */
  private final boolean sslEnabled;
  /**
   * Identifies this channel with the server.
   */
  private final int channelIdentifier;
  /**
   * Contains the location or source of the SSL configuration.
   */
  private final TypedProperties config;

  /**
   * Initialize this client connection with the specified uuid.
   * @param uuid the JPPF node or client uuid.
   * @param host the host ot which to connect.
   * @param port the port number to connect to on the host.
   * @param sslEnabled whether secure connectivity is enabled.
   * @param channelIdentifier identifies this channel with the server.
   * @param config contains the location or source of the SSL configuration.
   */
  public HeartbeatConnection(final int channelIdentifier, final String uuid, final String host, final int port, final boolean sslEnabled, final TypedProperties config) {
    this.channelIdentifier = channelIdentifier;
    this.uuid = uuid;
    this.host = host;
    this.port = port;
    this.sslEnabled = sslEnabled;
    this.config = config;
  }

  @Override
  public void run() {
    runThread = Thread.currentThread();
    try {
      configure();
      if (debugEnabled) log.debug("initializing heartbeat connection {}", socketWrapper);
      socketInitializer = SocketInitializer.Factory.newInstance();
      if (!socketInitializer.initialize(socketWrapper)) {
        log.error("Could not initialize heartbeat connection: {}", socketWrapper);
        close();
        return;
      }
      if (!InterceptorHandler.invokeOnConnect(socketWrapper, JPPFIdentifiers.descriptorFor(channelIdentifier))) {
        log.error("heartbeat client connection denied by interceptor: {}", socketWrapper);
        close();
        return;
      }
      if (debugEnabled) log.debug("sending channel identifier {}", JPPFIdentifiers.asString(channelIdentifier));
      socketWrapper.writeInt(channelIdentifier);
      socketWrapper.flush();
      if (sslEnabled) socketWrapper = new SSLHelper(config).createSSLClientConnection(socketWrapper);
      while (!isStopped()) {
        final HeartbeatMessage message = receiveMessage(maxRetries, socketReadTimeout);
        if (debugEnabled) log.debug("received {}", message);
        final HeartbeatMessage response = new HeartbeatMessage(message.getMessageID());
        if (!isInitialized()) {
          setInitialized(true);
          final int timeout = message.getProperties().get(JPPFProperties.RECOVERY_READ_TIMEOUT);
          maxRetries = message.getProperties().get(JPPFProperties.RECOVERY_MAX_RETRIES);
          socketReadTimeout = timeout;
          response.setUuid(uuid);
          if (debugEnabled) log.debug("initialized with timeout={}, maxRetries={}, socketReadTimeout={}, response={}", timeout, maxRetries, socketReadTimeout, response);
        }
        if (debugEnabled) log.debug("sending {}", response);
        sendMessage(response);
      }
    } catch (final InterruptedException e) {
      if (debugEnabled) log.debug("thread " + Thread.currentThread().getName() + "interrupted, stopping", e);
      close();
    } catch (final Exception e) {
      if (isInitialized()) {
        log.error(e.getMessage(), e);
        fireClientConnectionEvent();
      } else log.info("heartbeat server handshake failed, possibly because heartbeat is disabled in the server, exception:\n{}", ExceptionUtils.getStackTrace(e));
      close();
    } finally {
      if (debugEnabled) log.debug(Thread.currentThread().getName() + " stopping");
    }
  }

  /**
   * Configure this client connection from the JPPF properties.
   */
  private void configure() {
    if (debugEnabled) log.debug("configuring connection");
    final TypedProperties config = JPPFConfiguration.getProperties();
    if (host == null) host = config.get(JPPFProperties.SERVER_HOST);
    if (port < 0) port = config.get(JPPFProperties.SERVER_PORT);
    maxRetries = 1;
    socketReadTimeout = 0;
    socketWrapper = new BootstrapSocketClient();
    socketWrapper.setHost(host);
    socketWrapper.setPort(port);
  }

  @Override
  public void close() {
    setStopped(true);
    if (runThread != null) runThread.interrupt();
    try {
      if (debugEnabled) log.debug("closing connection");
      final SocketWrapper tmp = socketWrapper;
      socketWrapper = null;
      if (tmp != null) tmp.close();
      if (socketInitializer != null) socketInitializer.close();
      socketInitializer = null;
      listeners.clear();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add.
   */
  public void addClientConnectionListener(final HeartbeatConnectionListener listener) {
    if (listener == null) return;
    listeners.add(listener);
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove.
   */
  public void removeClientConnectionListener(final HeartbeatConnectionListener listener) {
    if (listener == null) return;
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that an event has occurred.
   */
  private void fireClientConnectionEvent() {
    final HeartbeatConnectionEvent event = new HeartbeatConnectionEvent(this);
    for (HeartbeatConnectionListener listener : listeners) listener.heartbeatConnectionFailed(event);
  }
}
