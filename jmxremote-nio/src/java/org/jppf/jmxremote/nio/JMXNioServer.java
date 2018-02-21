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

import java.io.EOFException;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

import javax.management.MBeanServer;
import javax.net.ssl.*;

import org.jppf.jmx.JMXHelper;
import org.jppf.jmxremote.*;
import org.jppf.jmxremote.message.*;
import org.jppf.jmxremote.notification.ServerNotificationHandler;
import org.jppf.management.ObjectNameCache;
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
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
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
   * @throws Exception if any error occurs.
   */
  JMXNioServer() throws Exception {
    super(JPPFIdentifiers.serverName(JPPFIdentifiers.JMX_REMOTE_CHANNEL) + "-" + instanceCount.incrementAndGet(), JPPFIdentifiers.JMX_REMOTE_CHANNEL, false);
    serverNotificationHandler = new ServerNotificationHandler(this);
    //this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    this.selectTimeout = 1L;
    registerMBean();
  }

  @Override
  protected NioServerFactory<JMXState, JMXTransition> createFactory() {
    return new JMXNioServerFactory(this);
  }

  @Override
  public void run() {
    try {
      final boolean hasTimeout = selectTimeout > 0L;
      int n = 0;
      while (!isStopped() && !externalStopCondition()) {
        sync.waitForZeroAndSetToMinusOne();
        try {
          n = hasTimeout ? selector.select(selectTimeout) : selector.select();
        } finally {
          sync.setToZeroIfNegative();
        }
        if (n > 0) go(selector.selectedKeys());
      }
    } catch (final Throwable t) {
      log.error("error in selector loop for {} : {}", getClass().getSimpleName(), ExceptionUtils.getStackTrace(t));
    } finally {
      end();
    }
  }

  /**
   * Set the interest ops of a specified selection key, ensuring no blocking occurs while doing so.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param update the operations to update on the key.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   * @throws Exception if any error occurs.
   */
  public void updateInterestOps(final SelectionKey key, final int update, final boolean add) throws Exception {
    final ChannelsPair pair = (ChannelsPair) key.attachment();
    final int ops = pair.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
      pair.setInterestOps(newOps);
      sync.wakeUpAndSetOrIncrement();
      try {
        key.interestOps(newOps);
      } finally {
        sync.decrement();
      }
    }
  }

  @Override
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    final Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      final SelectionKey key = it.next();
      it.remove();
      if (!key.isValid()) continue;
      final ChannelsPair pair = (ChannelsPair) key.attachment();
      try {
        if (pair.isClosed()) continue;
        final boolean readable = key.isReadable(), writable = key.isWritable();
        if (readable) {
          if (pair.isClosing()) continue;
          JMXMessageReader.read(pair.readingChannel().getContext());
        }
        if (writable) {
          updateInterestOpsNoWakeup(key, SelectionKey.OP_WRITE, false);
          final JMXTransitionTask task = pair.getNonSelectingWritingTask();
          if (!task.incrementCountIfNeeded()) task.run();
        }
      } catch (final CancelledKeyException e) {
        if ((pair != null) && !pair.isClosing() && !pair.isClosed()) {
          log.error("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
          closeConnection(pair, e, false);
        }
      } catch (final EOFException e) {
        if (debugEnabled) log.debug("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
        closeConnection(pair, e, false);
      } catch (final Exception e) {
        log.error("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
        if (pair != null) closeConnection(pair, e, false);
      }
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
      /*
      final JMXResponse response = new JMXResponse(JMXMessageHandler.CONNECTION_MESSAGE_ID, JMXMessageType.CONNECT, connectionID);
      pair.getMessageHandler().sendMessage(response);
      if (debugEnabled) log.debug("submitted response {}", response);
      */
      return pair.writingChannel();
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
    if (channel.isBlocking()) channel.configureBlocking(false);
    final JMXChannelWrapper readingChannel = createChannel(env, channel, ssl, true, null, client);
    final JMXChannelWrapper writingChannel = createChannel(env, channel, ssl, false, readingChannel.context.getSSLHandler(), client);
    final ChannelsPair pair = new ChannelsPair(readingChannel, writingChannel, this);
    final JMXMessageHandler handler = new JMXMessageHandler(pair, env);
    final MBeanServer mbeanServer = (MBeanServer) env.get(JPPFJMXConnectorServer.MBEAN_SERVER_KEY);
    pair.setConnectionID(connectionID);
    pair.setMbeanServer(mbeanServer);
    pair.setServerPort(port);
    JMXContext context = readingChannel.context;
    context.setMessageHandler(handler);
    context.setState(JMXState.RECEIVING_MESSAGE);
    context = writingChannel.context;
    context.setMessageHandler(handler);
    context.setState(JMXState.SENDING_MESSAGE);
    final int ops = SelectionKey.OP_READ;
    pair.setInterestOps(ops);
    sync.wakeUpAndSetOrIncrement();
    try {
      pair.setSelectionKey(channel.register(selector, ops, pair));
    } finally {
      sync.decrement();
    }
    return pair;
  }

  /**
   * Create a new channel wrapper.
   * @param env environment parameters to use for TLS properties.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @param reading {@code true} to create a reading channel, {@code false} to create a writing channel.
   * @param sslHandler .
   * @param client whether the created channels are client-side (true) or server-side (false).
   * @return a new {@link ChannelWrapper} instance.
   * @throws Exception if any error occurs.
   */
  private JMXChannelWrapper createChannel(final Map<String, ?> env, final SocketChannel channel, final boolean ssl, final boolean reading, final SSLHandler sslHandler, final boolean client)
    throws Exception {
    final JMXContext context = createNioContext(reading);
    final JMXChannelWrapper wrapper = new JMXChannelWrapper(context, channel);
    context.setChannel(wrapper);
    context.setSsl(ssl);
    if (ssl) {
      if (sslHandler == null) {
        if (debugEnabled) log.debug("creating SSLEngine for {}", wrapper);
        configureSSL(env, wrapper, client);
      } else context.setSSLHandler(sslHandler);
    }
    return wrapper;
  }

  /**
   * Configure SSL for the specified channel accepted by the specified server.
   * @param env environment parameters to use for TLS properties.
   * @param channelWrapper the channel to configure.
   * @param client whether the created channels are client-side (true) or server-side (false).
   * @throws Exception if any error occurs.
   */
  private static void configureSSL(final Map<String, ?> env, final JMXChannelWrapper channelWrapper, final boolean client) throws Exception {
    if (debugEnabled) log.debug(String.format("configuring %s-side SSL for %s, env = %s", (client ? "client" : "server"), channelWrapper, env));
    final SSLHelper2 helper = SSLHelper.getJPPFJMXremoteSSLHelper(env);
    final SocketChannel channel = channelWrapper.getSocketChannel();
    final SSLContext sslContext = helper.getSSLContext(JPPFIdentifiers.JMX_REMOTE_CHANNEL);
    final InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
    final SSLEngine engine = sslContext.createSSLEngine(addr.getHostString(), addr.getPort());
    final SSLParameters params = helper.getSSLParameters();
    engine.setUseClientMode(client);
    engine.setSSLParameters(params);
    if (debugEnabled) log.debug(String.format("created SSLEngine: useClientMode = %b, parameters = %s", engine.getUseClientMode(), engine.getSSLParameters()));
    final SSLHandler sslHandler = new SSLHandlerImpl(channel, engine);
    channelWrapper.context.setSSLHandler(sslHandler);
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
    List<String> connectionIDs = null;
    synchronized(mapsLock) {
      final Collection<String> coll = connectionsByServerPort.getValues(port);
      if (coll != null) {
        connectionIDs = new ArrayList<>(coll);
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
   * Set the interest ops of a specified selection key.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param update the operations to update on the key.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   */
  static void updateInterestOpsNoWakeup(final SelectionKey key, final int update, final boolean add) {
    final ChannelsPair pair = (ChannelsPair) key.attachment();
    final int ops = pair.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
      key.interestOps(newOps);
      pair.setInterestOps(newOps);
    }
  }
}
