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

package org.jppf.classloader;

import java.io.IOException;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.comm.socket.*;
import org.jppf.node.connection.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Concrete implementation of {@link ClassLoaderConnection} for connecting to a remote driver.
 * @author Laurent Cohen
 * @exclude
 */
public class RemoteClassLoaderConnection extends AbstractClassLoaderConnection<SocketWrapper> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(RemoteClassLoaderConnection.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to synchronize access to the underlying socket from multiple threads.
   */
  private SocketInitializer socketInitializer = SocketInitializer.Factory.newInstance();
  /**
   * Determines whether SSL is enabled.
   */
  private boolean sslEnabled;
  /**
   * The object used to serialize and deserialize resources.
   */
  private ObjectSerializer serializer;
  /**
   * The server conenction information.
   */
  private final DriverConnectionInfo connectionInfo;
  /**
   * The node configuration.
   */
  private final TypedProperties config;

  /**
   * Initialize with the required information to connect to the server.
   * @param uuid this node's uuid.
   * @param connectionInfo he server conenction information.
   * @param config the node configuration
   */
  public RemoteClassLoaderConnection(final String uuid, final DriverConnectionInfo connectionInfo, final TypedProperties config) {
    super(uuid);
    this.connectionInfo = connectionInfo;
    this.config = config;
  }

  @Override
  public void init() throws Exception {
    lock.lock();
    try {
      if (initializing.compareAndSet(false, true)) {
        try {
          if (debugEnabled) log.debug("initializing connection");
          initChannel();
          String message = "Attempting connection to the class server at " + channel.getHost() + ':' + channel.getPort();
          System.out.println(message);
          log.info(message);
          if (!socketInitializer.initialize(channel)) {
            channel = null;
            throw new JPPFNodeReconnectionNotification("the JPPF class loader could not reconnect to the server", socketInitializer.getLastException(), ConnectionReason.CLASSLOADER_INIT_ERROR);
          }
          if (debugEnabled) log.debug("channel initialized: {}", channel);
          if (!InterceptorHandler.invokeOnConnect(channel, JPPFChannelDescriptor.NODE_CLASSLOADER_CHANNEL))
            throw new JPPFNodeReconnectionNotification("connection denied by interceptor", null, ConnectionReason.CLASSLOADER_INIT_ERROR);
          performHandshake();
          message = getClass().getSimpleName() + ": Reconnected to the class server";
          System.out.println(message);
          log.info(message);
        } finally {
          initializing.set(false);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Create the ssl connection over an established plain connection.
   */
  private void createSSLConnection() {
    try {
      channel = new SSLHelper(config).createSSLClientConnection(channel);
    } catch(final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Perform the handshake with the server. The handshake consists in:
   * <ol>
   * <li>sending a channel identifier {@link JPPFIdentifiers#NODE_CLASSLOADER_CHANNEL} to the server</li>
   * <li>calling {@link #performCommonHandshake(ResourceRequestRunner) performCommonHandshake()} on the superclass</li>
   * </ol>
   */
  private void performHandshake() {
    try {
      if (debugEnabled) log.debug("sending channel identifier");
      channel.writeInt(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
      channel.flush();
      if (debugEnabled) log.debug("channel identifier sent successfully");
      if (sslEnabled) createSSLConnection();
      final ResourceRequestRunner rr = new RemoteResourceRequest(getSerializer(), channel);
      performCommonHandshake(rr);
      if (debugEnabled) log.debug("performed common handshake successfully");
    } catch (final IOException e) {
      throw new JPPFNodeReconnectionNotification("Error during driver handshake", e, ConnectionReason.CLASSLOADER_INIT_ERROR);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Initialize the underlying socket connection.
   */
  private void initChannel() {
    if (debugEnabled) log.debug("initializing socket connection");
    sslEnabled = connectionInfo.isSecure();
    channel = new BootstrapSocketClient();
    channel.setHost(connectionInfo.getHost());
    channel.setPort(connectionInfo.getPort());
    if (debugEnabled) log.debug("initialized socket connection: {}", channel);
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}", this);
    lock.lock();
    try {
      if (requestHandler != null) {
        final ResourceRequestRunner requestRunner = requestHandler.close();
        requestHandler = null;
        sendCloseChannelCommand(requestRunner);
      }
      if (socketInitializer != null) socketInitializer.close();
      if (channel != null) {
        try {
          channel.close();
        } catch(final Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
        }
        channel = null;
      }
    } finally {
      initializing.set(false);
      lock.unlock();
    }
  }

  /**
   * Get the object used to serialize and deserialize resources.
   * @return an {@link ObjectSerializer} instance.
   * @throws Exception if any error occurs.
   * @exclude
   */
  private ObjectSerializer getSerializer() throws Exception {
    if (serializer == null) serializer = new BootstrapObjectSerializer();
    return serializer;
  }
}
