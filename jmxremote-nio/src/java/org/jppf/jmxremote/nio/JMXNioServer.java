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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.*;

import org.jppf.jmxremote.*;
import org.jppf.jmxremote.utils.JPPFJMXHelper;
import org.jppf.nio.*;
import org.jppf.ssl.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class JMXNioServer extends NioServer<JMXState, JMXTransition> {
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
   *
   */
  private final AtomicLong connectionIdSequence = new AtomicLong(0L);
  /**
   * Mapping of connections to their connection ID.
   */
  private final Map<String, ChannelsPair> channelsByConnectionID = new ConcurrentHashMap<>();
  /**
   * Mapping of connectionIDs tot he server port on which the connection was established.
   */
  private final CollectionMap<Integer, String> connectionsByServerPort = new ArrayListHashMap<>();
  /**
   * Lock to synchronize on the {@link #channelsByConnectionID} and {@link #connectionsByServerPort} maps.
   */
  private final Object mapsLock = new Object();
  /**
   * 
   */
  private final List<JMXConnectionStatusListener> connectionStatusListeners = new CopyOnWriteArrayList<>();

  /**
   * @throws Exception if any error occurs.
   */
  private JMXNioServer() throws Exception {
    super(JPPFIdentifiers.JMX_REMOTE_CHANNEL, false);
    NioHelper.putServer(JPPFIdentifiers.JMX_REMOTE_CHANNEL, this);
    start();
  }

  @Override
  protected NioServerFactory<JMXState, JMXTransition> createFactory() {
    return new JMXNioServerFactory(this);
  }

  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object... params) {
    // client-side connection
    if (serverSocketChannel == null) {
      ChannelWrapper<?>  channelWrapper = super.accept(null, channel, sslHandler, ssl, peer, params);
      ((JMXContext) channelWrapper.getContext()).setClient(true);
      return channelWrapper;
    }
    // server-side connection
    NioServer<?, ?> acceptor = NioHelper.getServer(JPPFIdentifiers.ACCEPTOR_CHANNEL);
    @SuppressWarnings("unchecked")
    Map<String, ?> env = (Map<String, ?>) serverSocketChannel.keyFor(acceptor.getSelector()).attachment();
    ServerSocket serverSocket = serverSocketChannel.socket();
    int port = serverSocket.getLocalPort();
    InetAddress addr = serverSocket.getInetAddress();
    String ip = addr.getHostAddress();
    if (addr instanceof Inet6Address) ip =  "[" + ip + "]";
    //String connectionID = "jppf://192.168.1.1:12001 2135";
    String connectionID = String.format("%s://%s:%d %d", JPPFJMXHelper.PROTOCOL, ip, port, connectionIdSequence.incrementAndGet());
    ChannelWrapper<?> readingChannel = createChannel(env, channel, ssl, true);
    ((JMXContext) readingChannel.getContext()).setConnectionID(connectionID).setClient(false).setServerPort(port);
    ChannelWrapper<?> writingChannel = createChannel(env, channel, ssl, false);
    ((JMXContext) writingChannel.getContext()).setConnectionID(connectionID).setClient(false).setServerPort(port);
    synchronized(mapsLock) {
      channelsByConnectionID.put(connectionID, new ChannelsPair(readingChannel, writingChannel));
      connectionsByServerPort.putValue(port, connectionID);
    }
    ConnectionEventType.OPENED.fireNotification(connectionStatusListeners, new JMXConnectionStatusEvent(connectionID));
    return writingChannel;
  }

  /**
   * Create a new channel wrapper.
   * @param env environement parmateers to use for SL properties.
   * @param channelWrapper the channel to configure.
   * @param channel the associated socket channel.
   * @param reading {@code true} to create a reading channel, {@code false} to create a writing channel.
   * @return a new {@link ChannelWrapper} instance.
   */
  public ChannelWrapper<?> createChannel(final Map<String, ?> env, final SocketChannel channel, final boolean ssl, final boolean reading) {
    NioContext<?> context = createNioContext(reading);
    context.setPeer(false);
    SelectionKeyWrapper wrapper = null;
    lock.lock();
    try {
      SelectionKey selKey = channel.register(selector.wakeup(), 0, context);
      wrapper = new SelectionKeyWrapper(selKey);
      context.setChannel(wrapper);
      context.setSsl(ssl);
      if (ssl) {
        if (debugEnabled) log.debug("creating SSLEngine for {}", wrapper);
        configureSSL(env, wrapper, channel);
      }
      postAccept(wrapper);
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
   * @param env environement parmateers to use for SL properties.
   * @param channelWrapper the channel to configure.
   * @param channel the associated socket channel.
   * @return a SSLHandler instance.
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
    try {
      JMXContext context = (JMXContext) channel.getContext();
      transitionManager.transitionChannel(channel, context.isReading() ? JMXTransition.TO_RECEIVING_MESSAGE : JMXTransition.TO_SENDING_MESSAGE);
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      try {
        closeConnection(channel);
      } catch (Exception e2) {
        log.error(e2.getMessage(), e2);
      }
    }
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
   * Relmove and close all connections for the server witht he specified port.
   * @param port .
   */
  public void removeAllConnections(final int port) {
    Collection<String> connectionIDs = null;
    synchronized(mapsLock) {
      connectionIDs = connectionsByServerPort.getValues(port);
      if (connectionIDs != null) {
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

  @Override
  public void configureSSL(final ChannelWrapper<?> channel) throws Exception {
    super.configureSSL(channel);
  }

  @Override
  protected void createSSLContext() throws Exception {
    super.createSSLContext();
  }

  @Override
  protected void configureSSLEngine(final SSLEngine engine) throws Exception {
    super.configureSSLEngine(engine);
  }

  /**
   * Add a listener to the connection status events.
   * @param listener the luistener to add.
   */
  public void addConnectionStatusListener(JMXConnectionStatusListener listener) {
    if (listener != null) connectionStatusListeners.add(listener);
  }

  /**
   * Remove a listener from the connection status events.
   * @param listener the luistener to remove.
   */
  public void removeConnectionStatusListener(JMXConnectionStatusListener listener) {
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
   * Enumeration of the possible types of connection status event 
   */
  private enum ConnectionEventType {
    OPENED {
      @Override
      void fireNotification(List<JMXConnectionStatusListener> listeners, JMXConnectionStatusEvent event) {
        for (JMXConnectionStatusListener listener: listeners) listener.connectionOpened(event);
      }
    },
    CLOSED {
      @Override
      void fireNotification(List<JMXConnectionStatusListener> listeners, JMXConnectionStatusEvent event) {
        for (JMXConnectionStatusListener listener: listeners) listener.connectionClosed(event);
      }
    },
    FAILED {
      @Override
      void fireNotification(List<JMXConnectionStatusListener> listeners, JMXConnectionStatusEvent event) {
        for (JMXConnectionStatusListener listener: listeners) listener.connectionFailed(event);
      }
    };

    /**
     * Notify all listeners of the event.
     * @param listeners the listeners to notify.
     * @param event the event to notify of.
     */
    abstract void fireNotification(List<JMXConnectionStatusListener> listeners, final JMXConnectionStatusEvent event);
  }
}
