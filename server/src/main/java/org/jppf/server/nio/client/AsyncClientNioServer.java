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

package org.jppf.server.nio.client;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import javax.net.ssl.*;

import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.client.*;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * The NIO server that handles asynchronous client connections, which can handle multiple jobs concurrently.
 * @author Laurent Cohen
 */
public final class AsyncClientNioServer extends StatelessNioServer<AsyncClientContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The message handler for this server.
   */
  private final AsyncClientMessageHandler messageHandler;
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;

  /**
   * @param driver reference to the driver.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  public AsyncClientNioServer(final JPPFDriver driver, final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL, driver.getConfiguration());
    this.driver = driver;
    selectTimeout = 1000L;
    messageHandler = new AsyncClientMessageHandler(driver);
  }

  @Override
  protected void initReaderAndWriter() {
    messageReader = new AsyncClientMessageReader(this);
    messageWriter = new AsyncClientMessageWriter(this);
  }

  @Override
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    if (traceEnabled) {
      int writable = 0, readable = 0, invalid = 0;
      for (final SelectionKey key: selectedKeys) {
        if (!key.isValid()) invalid++;
        else {
          if (key.isReadable()) readable++;
          if (key.isWritable()) writable++;
        }
      }
      log.trace("nb keys = {}, readable = {}, writable = {}, invalid = {}", selectedKeys.size(), readable, writable, invalid);
    }
    super.go(selectedKeys);
  }

  @Override
  protected void handleSelectionException(final SelectionKey key, final Exception e) {
    final AsyncClientContext context = (AsyncClientContext) key.attachment();
    if (e instanceof CancelledKeyException) {
      if ((context != null) && !context.isClosed()) {
        log.error("error on {} :\n{}", context, ExceptionUtils.getStackTrace(e));
        closeConnection(context);
      }
    } else if (e instanceof EOFException) {
      if (debugEnabled) log.debug("error on {} :\n{}", context, ExceptionUtils.getStackTrace(e));
      context.handleException(e);
    } else {
      log.error("error on {} :\n{}", context, ExceptionUtils.getStackTrace(e));
      if (context != null) context.handleException(e);
    }
  }

  @Override
  public void accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl, final boolean peer, final Object... params) {
    try {
      if (debugEnabled) log.debug("accepting socketChannel = {}", channel);
      final AsyncClientContext context = createContext(channel, ssl);
      context.setPeer(peer);
      registerChannel(context, channel);
      if (debugEnabled) log.debug("registered channel = {}, for context = {}", channel, context);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    driver.getStatistics().addValue(JPPFStatisticsHelper.CLIENTS, 1);
  }

  /**
   * Create a new channel context.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @return a new {@link AsyncClientContext} instance.
   * @throws Exception if any error occurs.
   */
  private AsyncClientContext createContext(final SocketChannel channel, final boolean ssl)
    throws Exception {
    final AsyncClientContext context = createNioContext(channel);
    if (debugEnabled) log.debug("creating context for channel={}, ssl={}: {}", channel, ssl, context);
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
  private void configureSSL(final AsyncClientContext context) throws Exception {
    if (debugEnabled) log.debug("configuring SSL for {}", context);
    final SocketChannel channel = context.getSocketChannel();
    final InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
    final SSLEngine engine = sslContext.createSSLEngine(addr.getHostString(), addr.getPort());
    final SSLParameters params = sslHelper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
    if (debugEnabled) log.debug("created SSLEngine: useClientMode = {}, parameters = {}", engine.getUseClientMode(), engine.getSSLParameters());
    final SSLHandler sslHandler = new SSLHandlerImpl(channel, engine);
    context.setSSLHandler(sslHandler);
  }

  @Override
  public AsyncClientContext createNioContext(final Object...params) {
    return new AsyncClientContext(this, (SocketChannel) params[0]);
  }

  /**
   * Close the specified channel.
   * @param context the channel to close.
   */
  public void closeConnection(final AsyncClientContext context) {
    if (debugEnabled) log.debug("closing {}", context);
    try {
      final SelectionKey key = context.getSelectionKey();
      if (key != null) {
        key.cancel();
        key.channel().close();
      }
      final String uuid = context.getUuid();
      if (uuid != null) {
        handleClassLoaderAsync(context, uuid);
      }
    } catch (final Exception e) {
      log.error("error closing channel {}", context, e);
    } finally {
      driver.getStatistics().addValue(JPPFStatisticsHelper.CLIENTS, -1);
    }
  }

  /**
   * @param context the channel to close.
   * @param uuid the uuid of the conenction to close.
   * @throws Exception if any errort occurs.
   */
  private void handleClassLoaderAsync(final AsyncClientContext context, final String uuid) throws Exception {
    final AsyncClientClassNioServer classServer = driver.getAsyncClientClassServer();
    final List<AsyncClientClassContext> list = classServer.getProviderConnections(uuid);
    if (debugEnabled) log.debug("found {} provider connections for clientUuid={}; context={}", (list == null) ? 0 :list.size(), uuid, context);
    if ((list != null) && !list.isEmpty()) {
      for (final AsyncClientClassContext ctx: list) {
        if (ctx.getConnectionUuid().equals(context.getConnectionUuid())) {
          if (debugEnabled) log.debug("found provider connection with connectionUuid={} : {}", context.getConnectionUuid(), ctx);
          try {
            classServer.closeConnection(ctx, false);
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
          break;
        }
      }
    }
  }

  @Override
  public void removeAllConnections() {
    if (!isStopped()) return;
    super.removeAllConnections();
  }

  /**
   * @return the message handler for this server.
   */
  public AsyncClientMessageHandler getMessageHandler() {
    return messageHandler;
  }

  @Override
  protected void initNioHandlers() {
    super.initNioHandlers();
    acceptHandler = null;
  }

  /**
   * @return a reference to the driver.
   */
  JPPFDriver getDriver() {
    return driver;
  }
}
