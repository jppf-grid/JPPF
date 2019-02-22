/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.jmxremote.nio;

import java.io.EOFException;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import javax.management.remote.*;
import javax.net.ssl.*;

import org.jppf.jmx.*;
import org.jppf.jmxremote.*;
import org.jppf.jmxremote.message.JMXMessageHandler;
import org.jppf.jmxremote.notification.ServerNotificationHandler;
import org.jppf.management.ObjectNameCache;
import org.jppf.nio.*;
import org.jppf.nio.acceptor.AcceptorNioServer;
import org.jppf.ssl.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * The NIO server that handles client-side and server-side JMX connections.
 * @author Laurent Cohen
 */
public final class JMXNioServer extends StatelessNioServer<JMXContext> implements JMXNioServerMBean {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JMXNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Sequence number for the instances of this class.
   */
  private static final AtomicInteger instanceCount = new AtomicInteger(0);
  /**
   * Sequence number used to generate connection IDs.
   */
  private static final AtomicLong connectionIdSequence = new AtomicLong(0L);
  /**
   * Mapping of connections to their connection ID.
   */
  private final Map<String, ChannelsPair> channelsByConnectionID = new HashMap<>();
  /**
   * Mapping of connectionIDs to the server port on which the connection was established.
   */
  private final CollectionMap<Integer, String> connectionsByServerPort = new ArrayListHashMap<>();
  /**
   * Lock to synchronize on the {@link #channelsByConnectionID} and {@link #connectionsByServerPort} maps.
   */
  private final Object mapsLock = new Object();
  /**
   * Connection status listeners.
   */
  private final List<JMXConnectionStatusListener> connectionStatusListeners = new CopyOnWriteArrayList<>();
  /**
   * Handles server-side notification listeners.
   */
  private final ServerNotificationHandler serverNotificationHandler;
  /**
   * The peak number of connections.
   */
  private int peakConnections;
  /**
   * The statsistics to update, if any.
   */
  private final JPPFStatistics stats;

  /**
   * @throws Exception if any error occurs.
   */
  JMXNioServer() throws Exception {
    super(JPPFIdentifiers.serverName(JPPFIdentifiers.JMX_REMOTE_CHANNEL) + "-" + instanceCount.incrementAndGet(), JPPFIdentifiers.JMX_REMOTE_CHANNEL, false);
    serverNotificationHandler = new ServerNotificationHandler(this);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    registerMBean();
    final AcceptorNioServer acceptor = (AcceptorNioServer) NioHelper.getServer(JPPFIdentifiers.ACCEPTOR_CHANNEL);
    this.stats = (acceptor != null) ? acceptor.getStats() : null;
    if (debugEnabled) log.debug("initialized {}, stats = {}", this, (stats == null ? "null" : stats.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(stats))));
  }

  @Override
  protected void handleRead(final SelectionKey key) throws Exception {
    final ChannelsPair pair = (ChannelsPair) key.attachment();
    if (pair.isClosing()) return;
    JMXMessageReader.read(pair.readingContext());
  }

  @Override
  protected void handleWrite(final SelectionKey key) throws Exception {
    final ChannelsPair pair = (ChannelsPair) key.attachment();
    updateInterestOpsNoWakeup(key, SelectionKey.OP_WRITE, false);
    final JMXTransitionTask task = pair.getNonSelectingWritingTask();
    if (!task.incrementCountIfNeeded()) task.run();
  }

  @Override
  protected void handleSelectionException(final SelectionKey key, final Exception e) {
    final ChannelsPair pair = (ChannelsPair) key.attachment();
    if (pair != null) {
      if (e instanceof CancelledKeyException) {
        if (!pair.isClosing() && !pair.isClosed()) {
          log.error("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
          closeConnection(pair, e, false);
        }
      } else if (e instanceof EOFException) {
        if (debugEnabled) log.debug("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
        closeConnection(pair, e, false);
      } else {
        log.error("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
        closeConnection(pair, e, false);
      }
    } else {
      log.error("error on {} :\n{}", toString(key), ExceptionUtils.getStackTrace(e));
    }
  }

  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object... params) {
    try {
      // server-side connection
      if (debugEnabled) log.debug("accepting socketChannel = {}", channel);
      final NioServer<?, ?> acceptor = NioHelper.getServer(JPPFIdentifiers.ACCEPTOR_CHANNEL);
      @SuppressWarnings("unchecked")
      final Map<String, ?> env = (Map<String, ?>) serverSocketChannel.keyFor(acceptor.getSelector()).attachment();
      final InetSocketAddress saddr = (InetSocketAddress) serverSocketChannel.getLocalAddress();
      final int port = saddr.getPort();
      final InetAddress addr = saddr.getAddress();
      String ip = addr.getHostAddress();
      if (addr instanceof Inet6Address) ip =  "[" + ip + "]";
      //e.g. "jppf://192.168.1.12:12001 2135"
      final String connectionID = String.format("%s://%s:%d %d", JMXHelper.JPPF_JMX_PROTOCOL, ip, port, connectionIdSequence.incrementAndGet());
      final ChannelsPair pair = createChannelsPair(env, connectionID, port, channel, ssl, false);
      pair.setServerSide(true);
      synchronized(mapsLock) {
        channelsByConnectionID.put(connectionID, pair);
        final int n = channelsByConnectionID.size();
        if (n > peakConnections) peakConnections = n;
        connectionsByServerPort.putValue(port, connectionID);
      }
      ConnectionEventType.OPENED.fireNotification(connectionStatusListeners, new JMXConnectionStatusEvent(connectionID));
      registerChannel(pair, channel);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Create a pair of channels that can be used concurrently for the specified socket channel.
   * @param env environment parameters to use for connector properties.
   * @param connectionID the server-sde connection ID.
   * @param port the server port that accepted the connection.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @param client whether the created channels are client-side (true) or server-side (false).
   * @return a {@link ChannelsPair} instance.
   * @throws Exception if any error occurs.
   */
  public ChannelsPair createChannelsPair(final Map<String, ?> env, final String connectionID, final int port, final SocketChannel channel, final boolean ssl, final boolean client) throws Exception {
    if (debugEnabled) log.debug("connectionID={}, port={}, ssl={}, client={}, channel={}, env={}", connectionID, port, ssl, client, channel, env);
    if (channel.isBlocking()) channel.configureBlocking(false);
    final JMXContext readingChannel = createContext(env, channel, ssl, true, null, client);
    final JMXContext writingChannel = createContext(env, channel, ssl, false, readingChannel.getSSLHandler(), client);
    final JMXAuthenticator authenticator = client ? null : (JMXAuthenticator) env.get(JMXConnectorServer.AUTHENTICATOR);
    final ChannelsPair pair = new ChannelsPair(readingChannel, writingChannel, this, authenticator);
    if (!client) pair.setAuhtorizationChecker(env.get(JPPFJMXConnectorServer.AUTHORIZATION_CHECKER));
    final JMXMessageHandler handler = new JMXMessageHandler(pair, env);
    final JPPFJMXConnectorServer connectorServer = (JPPFJMXConnectorServer) env.get(JPPFJMXConnectorServer.CONNECTOR_SERVER_KEY);
    pair.setConnectionID(connectionID);
    pair.setJxmConnectorServer(connectorServer);
    pair.setServerPort(port);
    readingChannel.setMessageHandler(handler);
    writingChannel.setMessageHandler(handler);
    if (debugEnabled) log.debug("created {}, env = {}", pair, env);
    return pair;
  }

  /**
   * Create a new channel context.
   * @param env environment parameters to use for TLS properties.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @param reading {@code true} to create a reading channel, {@code false} to create a writing channel.
   * @param sslHandler .
   * @param client whether the created channels are client-side (true) or server-side (false).
   * @return a new {@link ChannelWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private JMXContext createContext(final Map<String, ?> env, final SocketChannel channel, final boolean ssl, final boolean reading, final SSLHandler sslHandler, final boolean client)
    throws Exception {
    final JMXContext context = createNioContext(reading, channel);
    if (debugEnabled) log.debug("creating channel wrapper for ssl={}, reading={}, sslHandler={}, context={}, env={}", ssl, reading, sslHandler, context, env);
    context.setSsl(ssl);
    if (ssl) {
      if (sslHandler == null) {
        if (debugEnabled) log.debug("creating SSLEngine for {}", context);
        configureSSL(env, context, client);
      } else context.setSSLHandler(sslHandler);
    }
    return context;
  }

  /**
   * Configure SSL for the specified channel accepted by the specified server.
   * @param env environment parameters to use for TLS properties.
   * @param context the channel to configure.
   * @param client whether the created channels are client-side (true) or server-side (false).
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  private static void configureSSL(final Map<String, ?> env, final JMXContext context, final boolean client) throws Exception {
    if (debugEnabled) log.debug("configuring {}-side SSL for {}, env = {}", (client ? "client" : "server"), context, env);
    ((Map<String, Object>) env).put(JPPFJMXProperties.TLS_ENABLED.getName(), "true");
    ((Map<String, Object>) env).put("jppf.ssl", true);
    final SSLHelper2 helper = SSLHelper.getJPPFJMXremoteSSLHelper(env);
    final SocketChannel channel = context.getSocketChannel();
    final SSLContext sslContext = helper.getSSLContext(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    final InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
    final SSLEngine engine = sslContext.createSSLEngine(addr.getHostString(), addr.getPort());
    final SSLParameters params = helper.getSSLParameters();
    engine.setUseClientMode(client);
    engine.setSSLParameters(params);
    if (debugEnabled) log.debug("created SSLEngine: useClientMode = {}, parameters = {}", engine.getUseClientMode(), engine.getSSLParameters());
    final SSLHandler sslHandler = new SSLHandlerImpl(channel, engine);
    context.setSSLHandler(sslHandler);
  }

  @Override
  public JMXContext createNioContext(final Object...params) {
    return new JMXContext(this, (Boolean) params[0], (SocketChannel) params[1]);
  }

  /**
   * Close a connection.
   * @param pair .
   * @param exception optional exception that caused the connection close.
   * @param clientRequestedClose .
   * @throws Exception if any error occurs.
   */
  public void closeConnection(final ChannelsPair pair, final Exception exception, final boolean clientRequestedClose) {
    final String connectionID = pair.getConnectionID();
    if (debugEnabled) log.debug("closing JMX channels for {}-side connectionID = {}", pair.isServerSide() ? "server" : "client", connectionID);
    Exception ex = exception;
    try {
      if (pair.isClosed() || (pair.isClosing() && !clientRequestedClose)) return;
      pair.requestClose();
      pair.close(exception);
      pair.getMessageHandler().close();
    } catch (final Exception e) {
      if (ex == null) ex = e;
    } finally {
      if (pair.isServerSide()) { 
        synchronized(mapsLock) {
          channelsByConnectionID.remove(connectionID);
          connectionsByServerPort.removeValue(pair.getServerPort(), connectionID);
        }
      }
    }
    if (pair.isServerSide()) {
      final ConnectionEventType type = (ex != null) ? ConnectionEventType.FAILED : ConnectionEventType.CLOSED;
      try {
        if (ex != null) fireNotification(type, new JMXConnectionStatusEvent(connectionID, ex));
        else fireNotification(type, new JMXConnectionStatusEvent(connectionID));
      } catch (final Exception e) {
        log.error("error firing {} notification for connectionID={}, exception={}:\n{}", type, connectionID, ex, ExceptionUtils.getStackTrace(e));
      }
    }
  }

  @Override
  public void removeAllConnections() {
    if (!isStopped()) return;
    Map<String, ChannelsPair> connectionMap = null;
    synchronized(mapsLock) {
      connectionMap = new HashMap<>(channelsByConnectionID);
      for (final Map.Entry<String, ChannelsPair> entry: connectionMap.entrySet()) {
        try {
          closeConnection(entry.getValue(), null, false);
        } catch (final Exception e) {
          log.error("error closing connectionID {} : {}", entry.getKey(), ExceptionUtils.getStackTrace(e));
        }
      }
      channelsByConnectionID.clear();
      connectionsByServerPort.clear();
    }
    super.removeAllConnections();
  }

  /**
   * Remove and close all connections for the server with the specified port.
   * @param port the port to which the connection was established.
   */
  public void removeAllConnections(final int port) {
    synchronized(mapsLock) {
      final Collection<String> coll = connectionsByServerPort.getValues(port);
      if (coll != null) {
        final  List<String> connectionIDs = new ArrayList<>(coll);
        for (final String connectionID: connectionIDs) {
          try {
            final ChannelsPair pair = channelsByConnectionID.get(connectionID);
            if (pair != null) closeConnection(pair, null, false);
          } catch (final Exception e) {
            log.error("error closing connectionID " + connectionID, e);
          }
        }
      }
    }
  }

  /**
   * Register this server as an MBean that collects statistics.
   */
  private void registerMBean() {
    try {
      ManagementFactory.getPlatformMBeanServer().registerMBean(this, ObjectNameCache.getObjectName(JMXNioServerMBean.MBEAN_NAME + "-" + getName()));
    } catch (final Exception e) {
      log.error("error creating JPPF JMX remote server", e);
    }
  }

  /**
   * Add a listener to the connection status events.
   * @param listener the listener to add.
   */
  public void addConnectionStatusListener(final JMXConnectionStatusListener listener) {
    if (listener != null) connectionStatusListeners.add(listener);
  }

  /**
   * Remove a listener from the connection status events.
   * @param listener the listener to remove.
   */
  public void removeConnectionStatusListener(final JMXConnectionStatusListener listener) {
    if (listener != null) connectionStatusListeners.remove(listener);
  }

  /**
   * Fire the specified notification type.
   * @param type the type of notification.
   * @param event the notificaztion to send to the listeners.
   */
  private void fireNotification(final ConnectionEventType type, final JMXConnectionStatusEvent event) {
    type.fireNotification(connectionStatusListeners, event);
  }

  /**
   * Get a list of message handlers for the specified connection ids.
   * @param connectionIDs the ids of the connections for which to get a message handler.
   * @return a map of connection ids to their corresponding {@link JMXMessageHandler}, possibly empty.
   */
  public Map<String, JMXMessageHandler> getMessageHandlers(final Collection<String> connectionIDs) {
    final Map<String, JMXMessageHandler> result = new HashMap<>(connectionIDs.size());
    synchronized(mapsLock) {
      for (final String connectionID: connectionIDs) {
        final ChannelsPair channels = channelsByConnectionID.get(connectionID);
        if (channels != null) result.put(connectionID, channels.getMessageHandler());
      }
    }
    return result;
  }

  /**
   * Get the object that handles server-side notification listeners.
   * @return a {@link ServerNotificationHandler} instance.
   */
  ServerNotificationHandler getServerNotificationHandler() {
    return serverNotificationHandler;
  }

  @Override
  public String stats() {
    final StringBuilder sb = new StringBuilder();
    synchronized(mapsLock) {
      sb.append("nbConnections = ").append(channelsByConnectionID.size()).append('\n');
      sb.append("peakConnections = ").append(peakConnections).append('\n');
      sb.append("connectionsByServerPort:");
      for (final Map.Entry<Integer, Collection<String>> entry: connectionsByServerPort.entrySet()) sb.append("\n  ").append(entry.getKey()).append(" --> ").append(entry.getValue().size());
    }
    return sb.toString();
  }

  /**
   * @return the statsistics to update, if any.
   */
  public JPPFStatistics getStats() {
    return stats;
  }

  @Override
  protected void initReaderAndWriter() {
  }

  @Override
  protected void initNioHandlers() {
    super.initNioHandlers();
    acceptHandler = null;
  }
}
