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

package org.jppf.nio.acceptor;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.*;

import javax.net.ssl.SSLEngine;

import org.jppf.JPPFException;
import org.jppf.comm.interceptor.InterceptorHandler;
import org.jppf.io.IO;
import org.jppf.nio.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class AcceptorNioServer extends StatelessNioServer<AcceptorContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AcceptorNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The statsistics to update, if any.
   */
  private final JPPFStatistics stats;
  /**
   * The ports this server is listening to.
   */
  protected int[] ports = {};
  /**
   * The SSL ports this server is listening to.
   */
  protected int[] sslPorts = {};
  /**
   * List of opened server socket channels.
   */
  protected final Map<Integer, ServerSocketChannel> servers = new HashMap<>();

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @param sslPorts the SSL ports this socket server is listening to.
   * @param stats the statsistics to update, if any.
   * @param configuration the JPPF configuration to use.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public AcceptorNioServer(final int[] ports, final int[] sslPorts, final JPPFStatistics stats, final TypedProperties configuration) throws Exception {
    super(JPPFIdentifiers.ACCEPTOR_CHANNEL, true, configuration);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    this.stats = stats;
    if (debugEnabled) log.debug("{} initialized", this);
    //if (debugEnabled) log.debug("{} initialized, call stack:\n{}", this, ExceptionUtils.getCallStack());
  }

  /**
   * Initialize the underlying server sockets.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  @Override
  protected void init() throws Exception {
    if ((ports != null) && (ports.length != 0)) init(ports, false);
    if ((sslPorts != null) && (sslPorts.length != 0)) init(sslPorts, true);
    super.init();
  }

  /**
   * Initialize the underlying server sockets for the specified array of ports.
   * @param portsToInit the array of ports to initiialize.
   * @param ssl {@code true} if the server sockets should be initialized with SSL enabled, {@code false} otherwise.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  private void init(final int[] portsToInit, final boolean ssl) throws Exception {
    for (int i=0; i<portsToInit.length; i++) addServer(portsToInit[i], ssl, null, true);
  }

  @Override
  protected void handleRead(final SelectionKey key) throws Exception {
    AcceptorMessageReader.read((AcceptorContext) key.attachment());
  }

  @Override
  protected void handleWrite(final SelectionKey key) throws Exception {
  }

  @Override
  protected void handleSelectionException(final SelectionKey key, final Exception e) {
    boolean logError = true;
    final SelectableChannel channel = key.channel();
    if (!(channel instanceof ServerSocketChannel)) {
      final AcceptorContext context = (AcceptorContext) key.attachment();
      if (e instanceof CancelledKeyException) {
        if (!context.isClosed()) {
          context.setClosed(true);
          try {
            if (!channel.isOpen()) channel.close();
          } catch (final Exception e2) {
            log.error("error trying to close " + channel, e2);
          }
        } else logError = false;
      }
    }
    if (logError) log.error(e.getMessage(), e);
  }

  @Override
  protected void doAccept(final SelectionKey key) {
    final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    if (this.isStopped() || !serverSocketChannel.isOpen()) return;
    @SuppressWarnings("unchecked")
    final Map<String, ?> env = (Map<String, ?>) key.attachment();
    final boolean ssl = (Boolean) env.get("jppf.ssl");
    final SocketChannel channel;
    try {
      channel = serverSocketChannel.accept();
      if (debugEnabled) log.debug("accepted {}", channel);
    } catch (final IOException e) {
      log.error(e.getMessage(), e);
      return;
    }
    if (channel == null) return;
    try {
      if (debugEnabled) log.debug("accepting channel {}, ssl={}", channel, ssl);
      channel.setOption(StandardSocketOptions.SO_RCVBUF, IO.SOCKET_BUFFER_SIZE);
      channel.setOption(StandardSocketOptions.SO_SNDBUF, IO.SOCKET_BUFFER_SIZE);
      channel.setOption(StandardSocketOptions.TCP_NODELAY, IO.SOCKET_TCP_NODELAY);
      channel.setOption(StandardSocketOptions.SO_KEEPALIVE, IO.SOCKET_KEEPALIVE);
      if (!InterceptorHandler.invokeOnAccept(channel, JPPFChannelDescriptor.ACCEPTOR_CHANNEL)) throw new JPPFException("connection denied by interceptor: " + channel);
      channel.configureBlocking(false);
      accept(serverSocketChannel, channel, null, ssl, false, env);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
      StreamUtils.close(channel, log);
    }
  }

  /**
   * Register an incoming connection with this server's selector.
   * The channel is registered with an empty set of initial interest operations,
   * which means a call to the corresponding {@link SelectionKey}'s <code>interestOps()</code> method will return 0.
   * @param serverSocketChannel the server socket channel accepting the connection.
   * @param channel the socket channel representing the connection.
   * @param sslHandler an sslEngine eventually passed on from a different server.
   * @param ssl specifies whether an <code>SSLHandler</code> should be initialized for the channel.
   * @param peer specifiies whether the channel is for a peer driver.
   * @param params optional parameters.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  @Override
  public void accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object...params) throws Exception {
    if (debugEnabled) log.debug("{} performing accept() of channel {}, ssl={}", this, channel, ssl);
    final AcceptorContext context = (AcceptorContext) createNioContext(serverSocketChannel, channel);
    context.setPeer(peer);
    if (sslHandler != null) context.setSSLHandler(sslHandler);
    final SelectionKey selKey = channel.register(selector, 0, context);
    context.setSsl(ssl);
    if (ssl && (sslHandler == null) && (sslContext != null)) {
      if (debugEnabled) log.debug("creating SSLEngine for  {}", context);
      final SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
      configureSSLEngine(engine);
      context.setSSLHandler(new SSLHandlerImpl(channel, engine));
    }
    context.setInterestOps(SelectionKey.OP_READ);
    selKey.interestOps(SelectionKey.OP_READ);
    if (debugEnabled) log.debug("{} channel {} accepted", this, channel);
  }

  @Override
  public NioContext createNioContext(final Object...params) {
    return new AcceptorContext(this, (ServerSocketChannel) params[0], (SocketChannel) params[1], stats);
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean addServer(final int portToInit, final boolean ssl, final Map<String, ?> env, final boolean retryOnException) throws Exception {
    int port = portToInit;
    if (debugEnabled) log.debug("adding server for port={}, ssl={}", port, ssl);
    if (port >= 0) {
      ServerSocketChannel server = null;
      Map<String, Object> map = null;
      synchronized(servers) {
        server = servers.get(port);
        if (server != null) {
          if (debugEnabled) log.debug("port {} already used, not creating nio server", port);
          final SelectionKey key = server.keyFor(selector);
          map = (Map<String, Object>) key.attachment();
          if (env != null) map.putAll(env);
          if (debugEnabled) log.debug("server added for port={}, ssl={}", port, ssl);
          return false;
        }
        final int maxBindTries = retryOnException ? JPPFConfiguration.getProperties().getInt("jppf.acceptor.bind.maxRetries", 3) : 1;
        final long retryDelay = JPPFConfiguration.getProperties().getLong("jppf.acceptor.bind.retryDelay", 3000L);
        server = ServerSocketChannel.open().setOption(StandardSocketOptions.SO_RCVBUF, IO.SOCKET_BUFFER_SIZE);
        final InetAddress bindAddress = NetworkUtils.getInetAddress("jppf.bind.");
        if (debugEnabled) log.debug("bind address: {}", bindAddress);
        final InetSocketAddress addr = (bindAddress == null) ? new InetSocketAddress(port) : new InetSocketAddress(bindAddress, port);
        if (debugEnabled) log.debug("binding server socket channel to address {}", addr);
        final ServerSocketChannel socketServer = server;
        RetryUtils.runWithRetry(maxBindTries, retryDelay, () ->  socketServer.bind(addr));
        if (debugEnabled) log.debug("server socket channel bound to address {}", addr);
        // If the user specified port zero, the operating system should dynamically allocate a port number.
        // we store the actual assigned port number so that it can be broadcast.
        if (port == 0) port = server.socket().getLocalPort();
        server.configureBlocking(false);
        map = new HashMap<>();
        map.put("jppf.ssl", ssl);
        if (env != null) map.putAll(env);
        servers.put(port, server);
        if (debugEnabled) log.debug("added server {} on port {}, servers = {}, this = {}", server, port, servers, this);
      }
      if (debugEnabled) log.debug("about to register server {} with selector", server);
      sync.wakeUpAndSetOrIncrement();
      try {
        if (debugEnabled) log.debug("registering server {} with selector", server);
        server.register(selector, SelectionKey.OP_ACCEPT, map);
        if (debugEnabled) log.debug("server {} registered with selector", server);
      } finally {
        sync.decrement();
      }
    }
    if (debugEnabled) log.debug("server added for port={}, ssl={}", port, ssl);
    return true;
  }

  /**
   * Remove the server identified by the local port it is listening to.
   * @param port the port the sever is listening to.
   * @throws IOException if any error occurs closing the specified server socket channel.
   */
  @Override
  public void removeServer(final int port) throws IOException {
    if (debugEnabled) log.debug("removing server on port {}", port);
    final ServerSocketChannel server;
    synchronized(servers) {
      server = servers.remove(port);
    }
    if (debugEnabled) log.debug("removed server {} on port {}", server, port);
    if (server != null) server.close();
    if (debugEnabled) log.debug("closed server {} on port {}", server, port);
  }

  @Override
  public void removeAllConnections() {
  }

  /**
   * Get the ports this server is listening to.
   * @return an array of int values.
   */
  public int[] getPorts() {
    return ports;
  }

  /**
   * Get the SSL ports this server is listening to.
   * @return an array of int values.
   */
  public int[] getSSLPorts() {
    return sslPorts;
  }

  /**
   * @return the statistics to update, if any.
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
    writeHandler = null;
  }

  @Override
  public void end() {
    if (!stopped.compareAndSet(false, true)) return;
    sync.wakeUpAndSetOrIncrement();
    if (debugEnabled) {
      synchronized(servers) {
        log.debug("ending {}, servers = {}", this, servers);
      }
    }
    Set<SelectionKey> keys = null;
    try {
      try {
        keys = new HashSet<>(selector.keys());
        selector.close();
      } catch (final Exception e) {
        log.error("error closing  selector", e);
      }
      if (keys != null) {
        if (debugEnabled) log.debug("cancelling {} keys", keys.size());
        for (final SelectionKey key: keys) {
          try {
            if (key.isValid()) key.cancel();
            final SelectableChannel channel = key.channel();
            if (channel.isOpen()) channel.close();
          } catch (final Exception e) {
            log.error("error closing  channel {}", key, e);
          }
        }
      }
    } finally {
      sync.decrement();
    }
    synchronized(servers) {
      if (debugEnabled) log.debug("ending {}, servers = {}", this, servers);
      servers.forEach((port, server) -> {
        try {
          if (debugEnabled) log.debug("closing server bound to port {} : {}", port, server);
          if (server.isOpen()) server.close();
        } catch (final Exception e) {
          if (debugEnabled) log.debug("error closing server {} on port {}", server, port, e);
        }
      });
      servers.clear();
    }
  }
}
