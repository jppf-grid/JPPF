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

package org.jppf.jmxremote.nio;

import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.net.ssl.*;

import org.jppf.jmxremote.*;
import org.jppf.jmxremote.message.JMXMessageHandler;
import org.jppf.jmxremote.notification.ServerNotificationHandler;
import org.jppf.jmxremote.utils.JPPFJMXHelper;
import org.jppf.nio.*;
import org.jppf.ssl.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * The NIO server that handles client-side and server-side JMX connections.
 * @author Laurent Cohen
 */
public final class JMXNioServer extends NioServer<JMXState, JMXTransition> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Singleton instance of this server.
   */
  private static final JMXNioServer INSTANCE = createInstance();
  /**
   * Sequence number used to generate connection IDs. 
   */
  private final AtomicLong connectionIdSequence = new AtomicLong(0L);
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
  private final ServerNotificationHandler serverNotificationHandler = new ServerNotificationHandler();

  /**
   * @throws Exception if any error occurs.
   */
  private JMXNioServer() throws Exception {
    super(JPPFIdentifiers.JMX_REMOTE_CHANNEL, false);
  }

  @Override
  protected NioServerFactory<JMXState, JMXTransition> createFactory() {
    return new JMXNioServerFactory(this);
  }

  @Override
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      SelectionKey key = it.next();
      it.remove();
      ChannelsPair pair = null;
      try {
        if (!key.isValid()) continue;
        if (key.isAcceptable()) doAccept(key);
        else {
          SocketChannel socketChannel = (SocketChannel) key.channel();
          pair = (ChannelsPair) key.attachment();
          if (key.isReadable()) {
            transitionManager.updateInterestOps(socketChannel, SelectionKey.OP_READ, false);
            transitionManager.submitTransition(pair.readingChannel());
          }
          if (key.isWritable()) {
            transitionManager.updateInterestOps(socketChannel, SelectionKey.OP_WRITE, false);
            transitionManager.submitTransition(pair.writingChannel());
          }
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        if (pair != null) {
          JMXContext context = (JMXContext) pair.readingChannel().getContext();
          context.handleException(context.getChannel(), e);
        }
        if (!(key.channel() instanceof ServerSocketChannel)) {
          try {
            key.channel().close();
          } catch (Exception e2) {
            log.error(e2.getMessage(), e2);
          }
        }
      }
    }
  }

  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object... params) {
    // server-side connection
    if (debugEnabled) log.debug("accepting socketChannel = {}", channel);
    NioServer<?, ?> acceptor = NioHelper.getServer(JPPFIdentifiers.ACCEPTOR_CHANNEL);
    @SuppressWarnings("unchecked")
    Map<String, ?> env = (Map<String, ?>) serverSocketChannel.keyFor(acceptor.getSelector()).attachment();
    ServerSocket serverSocket = serverSocketChannel.socket();
    int port = serverSocket.getLocalPort();
    InetAddress addr = serverSocket.getInetAddress();
    String ip = addr.getHostAddress();
    if (addr instanceof Inet6Address) ip =  "[" + ip + "]";
    //e.g. "jppf://192.168.1.1:12001 2135"
    String connectionID = String.format("%s://%s:%d %d", JPPFJMXHelper.PROTOCOL, ip, port, connectionIdSequence.incrementAndGet());
    try {
      ChannelsPair pair = createChannelsPair(env, connectionID, port, channel, ssl, false);
      synchronized(mapsLock) {
        channelsByConnectionID.put(connectionID, pair);
        connectionsByServerPort.putValue(port, connectionID);
      }
      ConnectionEventType.OPENED.fireNotification(connectionStatusListeners, new JMXConnectionStatusEvent(connectionID));
      return pair.writingChannel();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Create a pair of channels that can be used concurrently for the specified socket channel.
   * @param env environment parameters to use for TLS properties.
   * @param connectionID the server-sde connection ID.
   * @param port the server port that accepted the connection.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @param client whether the created channels are client-side (true) or server-side false.
   * @return a {@link ChannelsPair} instance.
   * @throws Exception if any error occurs.
   */
  public ChannelsPair createChannelsPair(final Map<String, ?> env, final String connectionID, final int port, final SocketChannel channel, final boolean ssl, final boolean client) throws Exception {
    ChannelWrapper<?> readingChannel = createChannel(env, channel, ssl, true);
    ChannelWrapper<?> writingChannel = createChannel(env, channel, ssl, false);
    ChannelsPair pair = new ChannelsPair(readingChannel, writingChannel);
    JMXMessageHandler handler = new JMXMessageHandler(pair);
    MBeanServer mbeanServer = (MBeanServer) env.get(JPPFJMXConnectorServer.MBEAN_SERVER_KEY);
    ((JMXContext) readingChannel.getContext()).setConnectionID(connectionID).setServerPort(port)
      .setMessageHandler(handler).setMbeanServer(mbeanServer).setState(JMXState.RECEIVING_MESSAGE);
    ((JMXContext) writingChannel.getContext()).setConnectionID(connectionID).setServerPort(port)
      .setMessageHandler(handler).setMbeanServer(mbeanServer).setState(JMXState.IDLE);
    lock.lock();
    try {
      channel.register(selector.wakeup(), SelectionKey.OP_READ, pair);
    } finally {
      lock.unlock();
    }
    return pair;
  }

  /**
   * Create a new channel wrapper.
   * @param env environment parameters to use for TLS properties.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @param reading {@code true} to create a reading channel, {@code false} to create a writing channel.
   * @return a new {@link ChannelWrapper} instance.
   */
  private ChannelWrapper<?> createChannel(final Map<String, ?> env, final SocketChannel channel, final boolean ssl, final boolean reading) {
    JMXContext context = (JMXContext) createNioContext(reading);
    context.setReading(reading);
    context.setPeer(false);
    JMXChannelWrapper wrapper = new JMXChannelWrapper(context, channel);
    lock.lock();
    try {
      context.setChannel(wrapper);
      context.setSsl(ssl);
      if (ssl) {
        if (debugEnabled) log.debug("creating SSLEngine for {}", wrapper);
        configureSSL(env, wrapper, channel);
      }
    } catch (Exception e) {
      wrapper = null;
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
    return wrapper;
  }

  /**
   * Configure SSL for th specified channel accepted by the specified server.
   * @param env environment parameters to use for TLS properties.
   * @param channelWrapper the channel to configure.
   * @param channel the associated socket channel.
   * @return a SSLHandler instance.
   * @throws Exception if any error occurs.
   */
  SSLHandler configureSSL(final Map<String, ?> env, final ChannelWrapper<?> channelWrapper, final SocketChannel channel) throws Exception {
    SSLHelper2 helper = SSLHelper.getJPPFJMXremoteSSLHelper(env);
    SSLContext sslContext = helper.getSSLContext(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
    SSLParameters params = helper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
    JMXContext jmxContext = (JMXContext) channelWrapper.getContext();
    SSLHandler sslHandler = new SSLHandler(channelWrapper, engine);
    jmxContext.setSSLHandler(sslHandler);
    return sslHandler;
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel) {
  }

  @Override
  public NioContext<JMXState> createNioContext(final Object...params) {
    JMXContext context = new JMXContext(this);
    if ((params != null) && (params.length > 0)) context.setReading((Boolean) params[0]);
    return context;
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return false;
  }

  /**
   * Close a connection.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   * @throws Exception if any error occurs.
   */
  public void closeConnection(final ChannelWrapper<?> channel) throws Exception {
    JMXContext context = (JMXContext) channel.getContext();
    if (debugEnabled) log.debug("closing JMX {} channel {}", context.isReading() ? "reader" : "writer", channel);
    closeConnection(context.getConnectionID());
  }

  /**
   * Close a connection.
   * @param connectionID the id of the connection to close.
   * @throws Exception if any error occurs.
   */
  public void closeConnection(final String connectionID) throws Exception {
    if (debugEnabled) log.debug("closing JMX channels for connectionID = {}", connectionID);
    try {
      ChannelsPair pair = channelsByConnectionID.get(connectionID);
      if (pair != null) {
        ChannelWrapper<?> channel = pair.readingChannel();
        JMXContext context = (JMXContext) channel.getContext();
        synchronized(mapsLock) {
          channelsByConnectionID.remove(connectionID);
          connectionsByServerPort.removeValue(context.getServerPort(), connectionID);
        }
        channel.close();
      }
      fireNotification(ConnectionEventType.CLOSED, new JMXConnectionStatusEvent(connectionID));
    } catch (Exception e) {
      fireNotification(ConnectionEventType.FAILED, new JMXConnectionStatusEvent(connectionID, e));
      throw e;
    }
  }

  @Override
  public void removeAllConnections() {
    if (!isStopped()) return;
    Map<String, ChannelsPair> connectionMap = null;
    synchronized(mapsLock) {
      connectionMap = new HashMap<>(channelsByConnectionID);
      for (Map.Entry<String, ChannelsPair> entry: connectionMap.entrySet()) {
        String connectionID = entry.getKey();
        try {
          closeConnection(connectionID);
        } catch (Exception e) {
          log.error("error closing connectionID {} : {}", connectionID, ExceptionUtils.getStackTrace(e));
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
    List<String> connectionIDs = null;
    synchronized(mapsLock) {
      Collection<String> coll = connectionsByServerPort.getValues(port);
      if (coll != null) {
        connectionIDs = new ArrayList<>(coll);
        for (String connectionID: connectionIDs) {
          try {
            closeConnection(connectionID);
          } catch (Exception e) {
            log.error("error closing connectionID " + connectionID, e);
          }
        }
      }
    }
  }

  /**
   * @return the singleton instance of this server.
   */
  private static JMXNioServer createInstance() {
    try {
      JMXNioServer server = new JMXNioServer();
      NioHelper.putServer(JPPFIdentifiers.JMX_REMOTE_CHANNEL, server);
      server.start();
      return server;
    } catch (Exception e) {
      log.error("error creating JPPF JMX remote server", e);
    }
    return null;
  }

  /**
   * @return the singleton instance of this server.
   */
  public static JMXNioServer getInstance() {
    return INSTANCE;
  }

  /**
   * Add a listener to the connection status events.
   * @param listener the luistener to add.
   */
  public void addConnectionStatusListener(final JMXConnectionStatusListener listener) {
    if (listener != null) connectionStatusListeners.add(listener);
  }

  /**
   * Remove a listener from the connection status events.
   * @param listener the luistener to remove.
   */
  public void removeConnectionStatusListener(final JMXConnectionStatusListener listener) {
    if (listener != null) connectionStatusListeners.remove(listener);
  }

  /**
   * Fire the speificed notification type.
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
    Map<String, JMXMessageHandler> result = new HashMap<>(connectionIDs.size());
    synchronized(mapsLock) {
      for (String connectionID: connectionIDs) {
        ChannelsPair channels = this.channelsByConnectionID.get(connectionID);
        if (channels != null) result.put(connectionID, ((JMXContext) channels.readingChannel().getContext()).getMessageHandler());
      }
    }
    return result;
  }

  /**
   * Get the object that handles server-side notification listeners.
   * @return a {@link ServerNotificationHandler} instance.
   */
  public ServerNotificationHandler getServerNotificationHandler() {
    return serverNotificationHandler;
  }
}
