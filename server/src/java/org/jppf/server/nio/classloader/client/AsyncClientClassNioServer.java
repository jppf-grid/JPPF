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

package org.jppf.server.nio.classloader.client;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;

import javax.net.ssl.*;

import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.classloader.ClassCache;
import org.jppf.server.nio.client.AsyncClientNioServer;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * The NIO server that handles asynchronous client connections, which can handle multiple jobs concurrently.
 * @author Laurent Cohen
 */
public final class AsyncClientClassNioServer extends StatelessNioServer<AsyncClientClassContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncClientClassNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The message handler for this server.
   */
  private final AsyncClientClassMessageHandler messageHandler;
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;
  /**
   * A mapping of the remote resource provider connections handled by this socket server, to their unique uuid.<br>
   * Provider connections represent connections form the clients only. The mapping to a uuid is required to determine in
   * which application classpath to look for the requested resources.
   */
  private final CollectionMap<String, AsyncClientClassContext> providerConnections = new VectorHashtable<>();
  /**
   * The cache of class definitions.
   */
  final ClassCache classCache;

  /**
   * @param driver reference to the driver.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  public AsyncClientClassNioServer(final JPPFDriver driver, final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
    this.driver = driver;
    selectTimeout = 1000L;
    messageHandler = new AsyncClientClassMessageHandler(this);
    this.classCache = driver.getInitializer().getClassCache();
  }

  @Override
  protected void initReaderAndWriter() {
    messageReader = new AsyncClientClassMessageReader(this);
    messageWriter = new AsyncClientClassMessageWriter(this);
  }

  @Override
  protected void handleSelectionException(final SelectionKey key, final Exception e) {
    final AsyncClientClassContext context = (AsyncClientClassContext) key.attachment();
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
      final AsyncClientClassContext context = createContext(channel, ssl);
      context.setPeer(peer);
      registerChannel(context, channel);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    driver.getStatistics().addValue(JPPFStatisticsHelper.CLIENTS, 1);
  }

  /**
   * Create a new channel context.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @return a new {@link AsyncClientClassContext} instance.
   * @throws Exception if any error occurs.
   */
  private AsyncClientClassContext createContext(final SocketChannel channel, final boolean ssl)
    throws Exception {
    final AsyncClientClassContext context = createNioContext(channel);
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
  private static void configureSSL(final AsyncClientClassContext context) throws Exception {
    if (debugEnabled) log.debug("configuring SSL for {}", context);
    final SocketChannel channel = context.getSocketChannel();
    final SSLContext sslContext = SSLHelper.getSSLContext(JPPFIdentifiers.CLIENT_CLASSLOADER_CHANNEL);
    final InetSocketAddress addr = (InetSocketAddress) channel.getRemoteAddress();
    final SSLEngine engine = sslContext.createSSLEngine(addr.getHostString(), addr.getPort());
    final SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
    if (debugEnabled) log.debug("created SSLEngine: useClientMode = {}, parameters = {}", engine.getUseClientMode(), engine.getSSLParameters());
    final SSLHandler sslHandler = new SSLHandlerImpl(channel, engine);
    context.setSSLHandler(sslHandler);
  }

  @Override
  public AsyncClientClassContext createNioContext(final Object...params) {
    return new AsyncClientClassContext(this, (SocketChannel) params[0]);
  }

  /**
   * Close the specified connection.
   * @param channel the channel representing the connection.
   */
  public void closeConnection(final AsyncClientClassContext channel) {
    closeConnection(channel, true);
  }

  /**
   * Close the specified connection.
   * @param context the channel representing the connection.
   * @param removeJobConnection {@code true} to remove the corresponding job connection as well, {@code false} otherwise.
   */
  public void closeConnection(final AsyncClientClassContext context, final boolean removeJobConnection) {
    if (context == null) {
      log.warn("attempt to close null channel - skipping this step");
      return;
    }
    if (debugEnabled) log.debug("closing {}", context);
    final String uuid = context.getUuid();
    if (uuid != null) removeProviderConnection(uuid, context);
    else if (debugEnabled) log.debug("null uuid for {}", context);
    if (removeJobConnection) {
      final String connectionUuid = context.getConnectionUuid();
      final AsyncClientNioServer clientJobServer = getDriver().getAsyncClientNioServer();
      clientJobServer.performContextAction(ctx -> connectionUuid.equals(ctx.getConnectionUuid()), clientJobServer::closeConnection);
    }
  }

  /**
   * Add a provider connection to the map of existing available providers.
   * @param uuid the provider uuid as a string.
   * @param context the provider's communication channel.
   */
  public void addProviderConnection(final String uuid, final AsyncClientClassContext context) {
    if (debugEnabled) log.debug("adding provider connection: uuid={}, context={}", uuid, context);
    providerConnections.putValue(uuid, context);
  }

  /**
   * Add a provider connection to the map of existing available providers.
   * @param uuid the provider uuid as a string.
   * @param context the provider's communication channel.
   */
  public void removeProviderConnection(final String uuid, final AsyncClientClassContext context) {
    if (debugEnabled) log.debug("removing provider connection: uuid={}, context={}", uuid, context);
    providerConnections.removeValue(uuid, context);
  }

  /**
   * Get all the provider connections for the specified client uuid.
   * @param uuid the uuid of the client for which to get connections.
   * @return a list of connection channels.
   */
  public List<AsyncClientClassContext> getProviderConnections(final String uuid) {
    final Collection<AsyncClientClassContext> channels = providerConnections.getValues(uuid);
    return channels == null ? null : new ArrayList<>(channels);
  }

  /**
   * Remove all the provider connections for the specified client uuid.
   * @param uuid the uuid of the client for which to remove connections.
   */
  public void removeProviderConnections(final String uuid) {
    final Collection<AsyncClientClassContext> channels = providerConnections.removeKey(uuid);
    if (channels != null) {
      for (final AsyncClientClassContext channel: channels) {
        try {
          closeConnection(channel);
        } catch (final Exception e) {
          log.error("error closing channel {} : {}", channel, ExceptionUtils.getStackTrace(e));
        }
      }
    }
  }

  /**
   * Get all the provider connections handled by this server.
   * @return a list of connection channels.
   */
  public List<AsyncClientClassContext> getAllProviderConnections() {
    final Collection<AsyncClientClassContext> channels = providerConnections.allValues();
    return channels == null ? null : new ArrayList<>(channels);
  }

  /**
   * Close and remove all connections accepted by this server.
   */
  @Override
  public synchronized void removeAllConnections() {
    if (!isStopped()) return;
    final List<AsyncClientClassContext> list = providerConnections.allValues();
    providerConnections.clear();
    super.removeAllConnections();
    if (list != null) {
      for (final AsyncClientClassContext channel: list) {
        try {
          closeConnection(channel);
        } catch (final Exception e) {
          log.error("error closing channel {} : {}", channel, ExceptionUtils.getStackTrace(e));
        }
      }
    }
  }

  /**
   * @return the message handler for this server.
   */
  public AsyncClientClassMessageHandler getMessageHandler() {
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
  public JPPFDriver getDriver() {
    return driver;
  }

  /**
   * @return the cache of class definitions.
   */
  public ClassCache getClassCache() {
    return classCache;
  }
}
