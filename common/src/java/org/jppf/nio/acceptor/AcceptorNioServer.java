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
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

import javax.net.ssl.SSLEngine;

import org.jppf.io.IO;
import org.jppf.nio.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class AcceptorNioServer extends NioServer<AcceptorState, AcceptorTransition> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AcceptorNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The statsistics to update, if any.
   */
  private final JPPFStatistics stats;
  /**
   * 
   */
  private final BlockingQueue<ChannelRegistration<AcceptorContext>> registrationQueue = new LinkedBlockingQueue<>();

  /**
   * Initialize this server with the specified port numbers.
   * @param ports the ports this socket server is listening to.
   * @param sslPorts the SSL ports this socket server is listening to.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public AcceptorNioServer(final int[] ports, final int[] sslPorts) throws Exception {
    this(ports, sslPorts, null);
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
  }

  @Override
  public void run() {
    try {
      final boolean hasTimeout = selectTimeout > 0L;
      int n = 0;
      ChannelRegistration<AcceptorContext> reg;
      while (!isStopped() && !externalStopCondition()) {
        while ((reg = registrationQueue.poll()) != null) {
          synchronized(reg) {
            reg.key = reg.channel.register(selector, reg.interestOps, reg.attachment);
            reg.notify();
          }
        }
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
        if (key.isAcceptable()) doAccept(key);
        else if (key.isReadable()) {
          context = (AcceptorContext) key.attachment();
          updateInterestOpsNoWakeup(key, SelectionKey.OP_READ, false);
          transitionManager.submit(new AcceptorTransitionTask(context.getChannel(), AcceptorState.IDENTIFYING_PEER, this));
        }
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
        if (context != null) context.handleException(context.getChannel(), e);
        if (!(key.channel() instanceof ServerSocketChannel)) try {
          key.channel().close();
        } catch (final Exception e2) {
          log.error(e2.getMessage(), e2);
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
    final Map<String, ?> map = (Map<String, ?>) key.attachment();
    final boolean ssl = (Boolean) map.get("jppf.ssl");
    final SocketChannel channel;
    try {
      channel = serverSocketChannel.accept();
      if (debugEnabled) log.debug("accepted {}", channel);
    } catch (final IOException e) {
      log.error(e.getMessage(), e);
      return;
    }
    if (channel == null) return;
    transitionManager.submit(new AcceptorAcceptTask(this, serverSocketChannel, channel, ssl));
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
  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object...params) throws Exception {
    if (debugEnabled) log.debug("{} performing accept() of channel {}, ssl={}", new Object[] {this, channel, ssl});
    final AcceptorContext context = (AcceptorContext) createNioContext(serverSocketChannel);
    context.setPeer(peer);
    context.setState(AcceptorState.IDENTIFYING_PEER);
    if (sslHandler != null) context.setSSLHandler(sslHandler);
    final ChannelRegistration<AcceptorContext> reg = new ChannelRegistration<>(channel, 0, context);
    registrationQueue.offer(reg);
    synchronized(reg) {
      while (reg.key == null) reg.wait();
    }
    final SelectionKey selKey = reg.key;
    final SelectionKeyWrapper wrapper = new SelectionKeyWrapper(selKey);
    context.setChannel(wrapper);
    context.setSsl(ssl);
    if (ssl && (sslHandler == null) && (sslContext != null)) {
      if (debugEnabled) log.debug("creating SSLEngine for  {}", wrapper);
      final SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
      configureSSLEngine(engine);
      context.setSSLHandler(new SSLHandler(wrapper, engine));
    }
    updateInterestOps(selKey, SelectionKey.OP_READ, true);
    if (debugEnabled) log.debug("{} channel {} accepted", this, channel);
    return wrapper;
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
    final AcceptorContext context = (AcceptorContext) key.attachment();
    final int ops = context.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
      context.setInterestOps(newOps);
      sync.wakeUpAndSetOrIncrement();
      try {
        key.interestOps(newOps);
      } finally {
        sync.decrement();
      }
    }
  }

  /**
   * Set the interest ops of a specified selection key.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param update the operations to update on the key.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   */
  private static void updateInterestOpsNoWakeup(final SelectionKey key, final int update, final boolean add) {
    final AcceptorContext context = (AcceptorContext) key.attachment();
    final int ops = context.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
      key.interestOps(newOps);
      context.setInterestOps(newOps);
    }
  }

  @Override
  protected void createSSLContext() throws Exception {
  }

  @Override
  protected void configureSSLEngine(final SSLEngine engine) throws Exception {
  }

  @Override
  protected NioServerFactory<AcceptorState, AcceptorTransition> createFactory() {
    return new AcceptorServerFactory(this);
  }

  /*
  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl, final boolean peer,
    final Object... params) throws Exception {
    return super.accept(serverSocketChannel, channel, sslHandler, ssl, peer, serverSocketChannel);
  }

  @Override
  public void postAccept(final ChannelWrapper<?> channel) {
    try {
      transitionManager.transitionChannel(channel, AcceptorTransition.TO_IDENTIFYING_PEER);
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeChannel(channel);
    }
  }
   */

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

  /**
   * Initialize the underlying server sockets for the spcified array of ports.
   * @param portToInit the array of ports to initiialize.
   * @param ssl <code>true</code> if the server sockets should be initialized with SSL enabled, <code>false</code> otherwise.
   * @param env optional map of parameters to associate with the server socket channel.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  @Override
  public void addServer(final int portToInit, final boolean ssl, final Map<String, ?> env) throws Exception {
    int port = portToInit;
    if (debugEnabled) log.debug("adding server for port={}, ssl={}", port, ssl);
    if (port > 0) {
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
      final ServerSocketChannel server = ServerSocketChannel.open();
      server.socket().setReceiveBufferSize(IO.SOCKET_BUFFER_SIZE);
      final InetSocketAddress addr = new InetSocketAddress(port);
      if (debugEnabled) log.debug("binding server socket channel to address {}", addr);
      server.socket().bind(addr);
      if (debugEnabled) log.debug("server socket channel bound to address {}", addr);
      // If the user specified port zero, the operating system should dynamically allocate a port number.
      // we store the actual assigned port number so that it can be broadcast.
      if (port == 0) port = server.socket().getLocalPort();
      server.configureBlocking(false);
      final Map<String, Object> map = new HashMap<>();
      map.put("jppf.ssl", ssl);
      if (env != null) map.putAll(env);
      synchronized(servers) {
        sync.wakeUpAndSetOrIncrement();
        try {
          server.register(selector, SelectionKey.OP_ACCEPT, map);
        } finally {
          sync.decrement();
        }
        servers.put(portToInit, server);
      }
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
    ServerSocketChannel server = null;
    synchronized(servers) {
      server = servers.remove(port);
    }
    if (server != null) server.close();
  }
}
