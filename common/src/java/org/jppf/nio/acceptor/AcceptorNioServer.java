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

package org.jppf.nio.acceptor;

import java.io.IOException;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.Callable;

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
public class AcceptorNioServer extends NioServer<AcceptorState, AcceptorTransition> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AcceptorNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The statsistics to update, if any.
   */
  private final JPPFStatistics stats;
  /**
   * 
   */
  private final NioState<AcceptorTransition> identifyingState;

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @param sslPorts the SSL ports this socket server is listening to.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public AcceptorNioServer(final int[] ports, final int[] sslPorts) throws Exception {
    this(ports, sslPorts, null);
    if (debugEnabled) log.debug("{} initialized", getClass().getSimpleName());
  }

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @param sslPorts the SSL ports this socket server is listening to.
   * @param stats the statsistics to update, if any.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public AcceptorNioServer(final int[] ports, final int[] sslPorts, final JPPFStatistics stats) throws Exception {
    super(ports, sslPorts, JPPFIdentifiers.ACCEPTOR_CHANNEL);
    //this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    this.selectTimeout = 1L;
    this.stats = stats;
    identifyingState = factory.getState(AcceptorState.IDENTIFYING_PEER);
    if (debugEnabled) log.debug("{} initialized", getClass().getSimpleName());
  }

  @Override
  public void run() {
    try {
      final boolean hasTimeout = selectTimeout > 0L;
      int n = 0;
      while (!isStopped() && !externalStopCondition()) {
        n = hasTimeout ? selector.select(selectTimeout) : selector.select();
        if (n > 0) go(selector.selectedKeys());
      }
    } catch (final Throwable t) {
      log.error("error in selector loop for {} : {}", getClass().getSimpleName(), ExceptionUtils.getStackTrace(t));
    } finally {
      end();
    }
  }

  /**
   * Process the keys selected by the selector for IO operations.
   * @param selectedKeys the set of keys that were selected by the latest <code>select()</code> invocation.
   * @throws Exception if an error is raised while processing the keys.
   */
  @Override
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    final Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      final SelectionKey key = it.next();
      it.remove();
      if (!key.isValid()) continue;
      AcceptorContext context = null;
      try {
        if (key.isAcceptable()) {
          doAccept(key);
        } else if (key.isReadable()) {
          context = (AcceptorContext) key.attachment();
          identifyingState.performTransition(context.getChannel());
        }
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
        if (context != null) context.handleException(context.getChannel(), e);
        if (!(key.channel() instanceof ServerSocketChannel)) {
          try {
            key.channel().close();
          } catch (final Exception e2) {
            log.error(e2.getMessage(), e2);
          }
        }
      }
    }
  }

  /**
   * accept the incoming connection.
   * It accept and put it in a state to define what type of peer is.
   * @param key the selection key that represents the channel's registration with the selector.
   */
  @Override
  protected void doAccept(final SelectionKey key) {
    final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
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
      if (!InterceptorHandler.invokeOnAccept(channel)) throw new JPPFException("connection denied by interceptor: " + channel);
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
   * @return a wrapper for the newly registered channel.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object...params) throws Exception {
    if (debugEnabled) log.debug("{} performing accept() of channel {}, ssl={}", this, channel, ssl);
    final AcceptorContext context = (AcceptorContext) createNioContext(serverSocketChannel);
    context.setPeer(peer);
    context.setState(AcceptorState.IDENTIFYING_PEER);
    if (sslHandler != null) context.setSSLHandler(sslHandler);
    final SelectionKey selKey = channel.register(selector, 0, context);
    final SelectionKeyWrapper wrapper = new SelectionKeyWrapper(selKey);
    context.setChannel(wrapper);
    context.setSsl(ssl);
    if (ssl && (sslHandler == null) && (sslContext != null)) {
      if (debugEnabled) log.debug("creating SSLEngine for  {}", wrapper);
      final SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
      configureSSLEngine(engine);
      context.setSSLHandler(new SSLHandlerImpl(wrapper, engine));
    }
    context.setInterestOps(SelectionKey.OP_READ);
    selKey.interestOps(SelectionKey.OP_READ);
    if (debugEnabled) log.debug("{} channel {} accepted", this, channel);
    return wrapper;
  }

  @Override
  protected NioServerFactory<AcceptorState, AcceptorTransition> createFactory() {
    return new AcceptorServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel) {
  }

  @Override
  public NioContext<AcceptorState> createNioContext(final Object...params) {
    return new AcceptorContext(this, (ServerSocketChannel) params[0], stats);
  }

  /**
   * Close a connection to a node.
   * @param channel a <code>SocketChannel</code> that encapsulates the connection.
   */
  public void closeChannel(final ChannelWrapper<?> channel) {
    try {
      channel.close();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return false;
  }

  @Override
  public void addServer(final int portToInit, final boolean ssl, final Map<String, ?> env, final boolean retryOnException) throws Exception {
    int port = portToInit;
    if (debugEnabled) log.debug("adding server for port={}, ssl={}", port, ssl);
    if (port >= 0) {
      synchronized(servers) {
        final ServerSocketChannel server = servers.get(port);
        if (server != null) {
          if (debugEnabled) log.debug("port {} already used, not creating nio server", port);
          final SelectionKey key = server.keyFor(selector);
          @SuppressWarnings("unchecked")
          final Map<String, Object> map = (Map<String, Object>) key.attachment();
          if (env != null) map.putAll(env);
          if (debugEnabled) log.debug("server added for port={}, ssl={}", port, ssl);
          return;
        }
      }
      final int maxBindRetries = retryOnException ? JPPFConfiguration.getProperties().getInt("jppf.acceptor.bind.maxRetries", 3) : 1;
      final long retryDelay = JPPFConfiguration.getProperties().getLong("jppf.acceptor.bind.retryDelay", 3000L);
      final ServerSocketChannel server = ServerSocketChannel.open().setOption(StandardSocketOptions.SO_RCVBUF, IO.SOCKET_BUFFER_SIZE);
      final InetSocketAddress addr = new InetSocketAddress(port);
      if (debugEnabled) log.debug("binding server socket channel to address {}", addr);
      RetryUtils.runWithRetry(maxBindRetries, retryDelay, new Callable<ServerSocketChannel>() {
        @Override
        public ServerSocketChannel call() throws Exception {
          return server.bind(addr);
        }
      });
      if (debugEnabled) log.debug("server socket channel bound to address {}", addr);
      // If the user specified port zero, the operating system should dynamically allocate a port number.
      // we store the actual assigned port number so that it can be broadcast.
      if (port == 0) port = server.socket().getLocalPort();
      server.configureBlocking(false);
      final Map<String, Object> map = new HashMap<>();
      map.put("jppf.ssl", ssl);
      if (env != null) map.putAll(env);
      if (debugEnabled) log.debug("adding server {} on port {}", server, port);
      synchronized(servers) {
        servers.put(port, server);
      }
      if (debugEnabled) log.debug("about to register server {} with selector", server);
      sync.wakeUpAndSetOrIncrement();
      if (debugEnabled) log.debug("registering server {} with selector", server);
      try {
        server.register(selector, SelectionKey.OP_ACCEPT, map);
      } finally {
        sync.decrement();
      }
      if (debugEnabled) log.debug("server {} registered with selector", server);
    }
    if (debugEnabled) log.debug("server added for port={}, ssl={}", port, ssl);
  }

  /**
   * Remove the server identified by the local port it is listneing to.
   * @param port the port the sever is listening to.
   * @throws IOException if any error occurs closing the specified server socket channel.
   */
  @Override
  public void removeServer(final int port) throws IOException {
    if (debugEnabled) log.debug("removing server on port={}", port);
    final ServerSocketChannel server;
    synchronized(servers) {
      server = servers.remove(port);
    }
    if (debugEnabled) log.debug("removed server={} on port={}", server, port);
    if (server != null) server.close();
  }

  /**
   * @return the statsistics to update, if any.
   */
  public JPPFStatistics getStats() {
    return stats;
  }
}
