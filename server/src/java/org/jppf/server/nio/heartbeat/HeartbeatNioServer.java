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

package org.jppf.server.nio.heartbeat;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import javax.net.ssl.*;

import org.jppf.nio.*;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * The NIO server that handles heartbeat connections.
 * @author Laurent Cohen
 */
public final class HeartbeatNioServer extends NioServer<EmptyEnum, EmptyEnum> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(HeartbeatNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The message handler for this server.
   */
  private final HeartbeatMessageHandler messageHandler;

  /**
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  public HeartbeatNioServer(final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
    messageHandler = new HeartbeatMessageHandler(this);
  }

  @Override
  protected NioServerFactory<EmptyEnum, EmptyEnum> createFactory() {
    return null;
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
    final HeartbeatContext context = (HeartbeatContext) key.attachment();
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

  @Override
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    final Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      final SelectionKey key = it.next();
      it.remove();
      if (!key.isValid()) continue;
      final HeartbeatContext pair = (HeartbeatContext) key.attachment();
      try {
        if (pair.isClosed()) continue;
        final boolean readable = key.isReadable(), writable = key.isWritable();
        if (readable) {
          HeartbeatMessageReader.read(pair);
        }
        if (writable) {
          updateInterestOpsNoWakeup(key, SelectionKey.OP_WRITE, false);
          HeartbeatMessageWriter.write(pair);
        }
      } catch (final CancelledKeyException e) {
        if ((pair != null) && !pair.isClosed()) {
          log.error("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
          pair.handleException(null, e);
        }
      } catch (final EOFException e) {
        if (debugEnabled) log.debug("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
        pair.handleException(null, e);
      } catch (final Exception e) {
        log.error("error on {} :\n{}", pair, ExceptionUtils.getStackTrace(e));
        if (pair != null) pair.handleException(null, e);
      }
    }
  }

  @Override
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object... params) {
    try {
      if (debugEnabled) log.debug("accepting socketChannel = {}", channel);
      final HeartbeatContext context = createContext(channel, ssl);
      registerChannel(context, channel);
      messageHandler.addChannel(context);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Register the specified channel with this server's selectior.
   * @param context the ocntext associated with the channel.
   * @param channel the channel to register.
   * @throws Exception if any error occurs.
   */
  public void registerChannel(final HeartbeatContext context, final SocketChannel channel) throws Exception {
    final int ops = SelectionKey.OP_READ;
    context.setInterestOps(ops);
    sync.wakeUpAndSetOrIncrement();
    try {
      context.setSelectionKey(channel.register(selector, ops, context));
    } finally {
      sync.decrement();
    }
  }

  /**
   * Create a new channel context.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @return a new {@link HeartbeatContext} instance.
   * @throws Exception if any error occurs.
   */
  private HeartbeatContext createContext(final SocketChannel channel, final boolean ssl)
    throws Exception {
    final HeartbeatContext context = createNioContext(channel);
    if (debugEnabled) log.debug(String.format("creating channel wrapper for ssl=%b, context=%s", ssl, context));
    context.setSsl(ssl);
    if (ssl) {
      if (debugEnabled) log.debug("creating SSLEngine for {}", context);
      configureSSL(context);
    }
    return context;
  }

  /**
   * Configure SSL for the specified channel accepted by the specified server.
   * @param context the channel to configure.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  private static void configureSSL(final HeartbeatContext context) throws Exception {
    if (debugEnabled) log.debug(String.format("configuring SSL for %s", context));
    final SocketChannel channel = context.getSocketChannel();
    final SSLContext sslContext = SSLHelper.getSSLContext(JPPFIdentifiers.NODE_HEARTBEAT_CHANNEL);
    final InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
    final SSLEngine engine = sslContext.createSSLEngine(addr.getHostString(), addr.getPort());
    final SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
    if (debugEnabled) log.debug(String.format("created SSLEngine: useClientMode = %b, parameters = %s", engine.getUseClientMode(), engine.getSSLParameters()));
    final SSLHandler sslHandler = new SSLHandlerImpl(channel, engine);
    context.setSSLHandler(sslHandler);
  }

  @Override
  public HeartbeatContext createNioContext(final Object...params) {
    return new HeartbeatContext(this, (SocketChannel) params[0]);
  }


  /**
   * Close the specified channel.
   * @param context the channel to close.
   */
  void closeConnection(final HeartbeatContext context) {
    try {
      messageHandler.removeChannel(context);
      final SelectionKey key = context.getSocketChannel().keyFor(selector);
      key.cancel();
      key.channel().close();
    } catch (final Exception e) {
      log.error("error closing channel {}: {}", context, ExceptionUtils.getStackTrace(e));
    }
  }

  @Override
  public void removeAllConnections() {
    if (!isStopped()) return;
    super.removeAllConnections();
  }

  /**
   * Set the interest ops of a specified selection key.
   * This method is proposed as a convenience, to encapsulate the inner locking mechanism.
   * @param key the key on which to set the interest operations.
   * @param update the operations to update on the key.
   * @param add whether to add the update ({@code true}) or remove it ({@code false}).
   */
  static void updateInterestOpsNoWakeup(final SelectionKey key, final int update, final boolean add) {
    final HeartbeatContext pair = (HeartbeatContext) key.attachment();
    final int ops = pair.getInterestOps();
    final int newOps = add ? ops | update : ops & ~update;
    if (newOps != ops) {
      if (traceEnabled) log.trace(String.format("updating interestOps from %d to %d for %s", ops, newOps, key));
      key.interestOps(newOps);
      pair.setInterestOps(newOps);
    }
  }

  /**
   * @return the message handler for this server.
   */
  public HeartbeatMessageHandler getMessageHandler() {
    return messageHandler;
  }
}
