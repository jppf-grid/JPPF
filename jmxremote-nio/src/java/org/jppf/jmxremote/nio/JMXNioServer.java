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

import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;
import javax.net.ssl.*;

import org.jppf.jmx.JMXHelper;
import org.jppf.jmxremote.*;
import org.jppf.jmxremote.message.*;
import org.jppf.jmxremote.notification.ServerNotificationHandler;
import org.jppf.nio.*;
import org.jppf.ssl.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * The NIO server that handles client-side and server-side JMX connections.
 * @author Laurent Cohen
 */
public final class JMXNioServer extends NioServer<JMXState, JMXTransition> implements JMXNioServerMBean {
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
  private final ServerNotificationHandler serverNotificationHandler = new ServerNotificationHandler();
  /**
   * The peak number of connections.
   */
  private int peakConnections;

  /**
   * @throws Exception if any error occurs.
   */
  private JMXNioServer() throws Exception {
    super(JPPFIdentifiers.JMX_REMOTE_CHANNEL, false);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    this.selectTimeout = 1L;
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
      if (!key.isValid()) continue;
      ChannelsPair pair = null;
      try {
        pair = (ChannelsPair) key.attachment();
        if (pair.isClosed()) continue;
        int ops = 0;
        boolean readable = key.isReadable(), writable = key.isWritable();
        if (readable) {
          if (pair.isClosing()) continue;
          ops = SelectionKey.OP_READ;
        }
        if (writable) ops |= SelectionKey.OP_WRITE;
        if (ops == 0) continue;
        transitionManager.updateInterestOpsNoWakeup(key, ops, false);
        if (readable) transitionManager.submit(pair.getReadingTask());
        if (writable) transitionManager.submit(pair.getWritingTask());
      } catch (CancelledKeyException e) {
        if (pair != null) {
          if (!pair.isClosing() && !pair.isClosed()) {
            log.error(e.getMessage(), e);
            closeConnection(pair.getConnectionID(), e);
          }
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        if (pair != null) {
          closeConnection(pair.getConnectionID(), e);
        } else if (!(key.channel() instanceof ServerSocketChannel)) {
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
    //e.g. "jppf://192.168.1.12:12001 2135"
    String connectionID = String.format("%s://%s:%d %d", JMXHelper.JPPF_JMX_PROTOCOL, ip, port, connectionIdSequence.incrementAndGet());
    try {
      ChannelsPair pair = createChannelsPair(env, connectionID, port, channel, ssl, false).setServerSide(true);
      synchronized(mapsLock) {
        channelsByConnectionID.put(connectionID, pair);
        int n = channelsByConnectionID.size();
        if (n > peakConnections) peakConnections = n;
        connectionsByServerPort.putValue(port, connectionID);
      }
      ConnectionEventType.OPENED.fireNotification(connectionStatusListeners, new JMXConnectionStatusEvent(connectionID));
      pair.writingChannel().getContext().getMessageHandler().sendMessage(new JMXResponse(JMXMessageHandler.CONNECTION_MESSAGE_ID, JMXMessageType.CONNECT, connectionID));
      return pair.writingChannel();
    } catch (Exception e) {
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
    JMXChannelWrapper readingChannel = createChannel(env, channel, ssl, true);
    JMXChannelWrapper writingChannel = createChannel(env, channel, ssl, false);
    ChannelsPair pair = new ChannelsPair(readingChannel, writingChannel, this);
    JMXMessageHandler handler = new JMXMessageHandler(pair, env);
    MBeanServer mbeanServer = (MBeanServer) env.get(JPPFJMXConnectorServer.MBEAN_SERVER_KEY);
    pair.setConnectionID(connectionID).setMbeanServer(mbeanServer).setServerPort(port);
    readingChannel.context.setMessageHandler(handler).setState(JMXState.RECEIVING_MESSAGE);
    writingChannel.context.setMessageHandler(handler).setState(JMXState.IDLE);
    lock.lock();
    try {
      wakeUpSelectorIfNeeded();
      channel.register(selector, SelectionKey.OP_READ, pair);
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
  private JMXChannelWrapper createChannel(final Map<String, ?> env, final SocketChannel channel, final boolean ssl, final boolean reading) {
    JMXContext context = createNioContext(reading);
    JMXChannelWrapper wrapper = new JMXChannelWrapper(context, channel);
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
    }
    return wrapper;
  }

  /**
   * Configure SSL for the specified channel accepted by the specified server.
   * @param env environment parameters to use for TLS properties.
   * @param channelWrapper the channel to configure.
   * @param channel the associated socket channel.
   * @return a SSLHandler instance.
   * @throws Exception if any error occurs.
   */
  SSLHandler configureSSL(final Map<String, ?> env, final JMXChannelWrapper channelWrapper, final SocketChannel channel) throws Exception {
    SSLHelper2 helper = SSLHelper.getJPPFJMXremoteSSLHelper(env);
    SSLContext sslContext = helper.getSSLContext(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
    SSLParameters params = helper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
    JMXContext jmxContext = channelWrapper.context;
    SSLHandler sslHandler = new SSLHandler(channelWrapper, engine);
    jmxContext.setSSLHandler(sslHandler);
    return sslHandler;
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel) {
  }

  @Override
  public JMXContext createNioContext(final Object...params) {
    return new JMXContext(this, (Boolean) params[0]);
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return false;
  }

  /**
   * Close a connection.
   * @param connectionID the id of the connection to close.
   * @param exception optional exception that caused the connection close.
   * @throws Exception if any error occurs.
   */
  public void closeConnection(final String connectionID, final Exception exception) throws Exception {
    closeConnection(connectionID, exception, false);
  }

  /**
   * Close a connection.
   * @param connectionID the id of the connection to close.
   * @param exception optional exception that caused the connection close.
   * @param clientRequestedClose .
   * @throws Exception if any error occurs.
   */
  public void closeConnection(final String connectionID, final Exception exception, final boolean clientRequestedClose) throws Exception {
    if (debugEnabled) log.debug("closing JMX channels for connectionID = {}", connectionID);
    Exception ex = exception;
    ChannelsPair pair = channelsByConnectionID.get(connectionID);
    try {
      if (pair != null) {
        if (pair.isClosed() || (pair.isClosing() && !clientRequestedClose)) return;
        pair.requestClose();
        JMXChannelWrapper channel = pair.readingChannel();
        JMXContext context = channel.context;
        context.getMessageHandler().close();
        if (pair.isServerSide()) {
          synchronized(mapsLock) {
            channelsByConnectionID.remove(connectionID);
            connectionsByServerPort.removeValue(pair.getServerPort(), connectionID);
          }
        }
        try {
          pair.close();
        } catch (Exception e) {
          if (ex != null) ex = e;
        }
      }
    } catch (Exception e) {
      if (ex == null) ex = e;
    }
    if ((pair != null) && pair.isServerSide()) {
      ConnectionEventType type = (ex != null) ? ConnectionEventType.FAILED : ConnectionEventType.CLOSED;
      try {
        if (ex != null) fireNotification(type, new JMXConnectionStatusEvent(connectionID, ex));
        else fireNotification(type, new JMXConnectionStatusEvent(connectionID));
      } catch (Exception e) {
        log.error(String.format("error firing %s notification for connectionID=%s, exception=%s:%n%s", type, connectionID, ex, ExceptionUtils.getStackTrace(e))); 
      }
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
          closeConnection(connectionID, null);
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
            closeConnection(connectionID, null);
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
      ManagementFactory.getPlatformMBeanServer().registerMBean(server, new ObjectName(JMXNioServerMBean.MBEAN_NAME));
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
        ChannelsPair channels = channelsByConnectionID.get(connectionID);
        if (channels != null) result.put(connectionID, channels.readingChannel().context.getMessageHandler());
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
    StringBuilder sb = new StringBuilder();
    synchronized(mapsLock) {
      sb.append("nbConnections = ").append(channelsByConnectionID.size()).append('\n');
      sb.append("peakConnections = ").append(peakConnections).append('\n');
      sb.append("connectionsByServerPort:");
      for (Map.Entry<Integer, Collection<String>> entry: connectionsByServerPort.entrySet()) {
        sb.append("\n  ").append(entry.getKey()).append(" --> ").append(entry.getValue().size());
      }
    }
    return sb.toString();
  }
}
