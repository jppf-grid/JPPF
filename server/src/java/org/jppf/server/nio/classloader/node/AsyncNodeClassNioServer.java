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

package org.jppf.server.nio.classloader.node;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.*;

import org.jppf.classloader.*;
import org.jppf.nio.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.client.AsyncClientContext;
import org.jppf.server.nio.nodeserver.BaseNodeContext;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * The NIO server that handles asynchronous client connections, which can handle multiple jobs concurrently.
 * @author Laurent Cohen
 */
public final class AsyncNodeClassNioServer extends StatelessNioServer<AsyncNodeClassContext> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeClassNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The message handler for this server.
   */
  private final AsyncNodeClassMessageHandler messageHandler;
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;
  /**
   * Mapping of channels to their uuid.
   */
  protected final Map<String, AsyncNodeClassContext> nodeConnections = new ConcurrentHashMap<>();
  /**
   * Reads resource files from the classpath.
   */
  private final ResourceProvider resourceProvider = new ResourceProviderImpl();

  /**
   * @param driver reference to the driver.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  public AsyncNodeClassNioServer(final JPPFDriver driver, final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL, driver.getConfiguration());
    this.driver = driver;
    selectTimeout = 1000L;
    messageHandler = new AsyncNodeClassMessageHandler(driver);
  }

  @Override
  protected void initReaderAndWriter() {
    messageReader = new AsyncNodeClassMessageReader(this);
    messageWriter = new AsyncNodeClassMessageWriter(this);
  }

  @Override
  protected void handleSelectionException(final SelectionKey key, final Exception e) {
    final AsyncNodeClassContext context = (AsyncNodeClassContext) key.attachment();
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
      final AsyncNodeClassContext context = createContext(channel, ssl);
      context.setPeer(peer);
      registerChannel(context, channel);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Create a new channel context.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @return a new {@link AsyncClientContext} instance.
   * @throws Exception if any error occurs.
   */
  private AsyncNodeClassContext createContext(final SocketChannel channel, final boolean ssl)
    throws Exception {
    final AsyncNodeClassContext context = createNioContext(channel);
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
  private static void configureSSL(final AsyncNodeClassContext context) throws Exception {
    if (debugEnabled) log.debug("configuring SSL for {}", context);
    final SocketChannel channel = context.getSocketChannel();
    final SSLContext sslContext = SSLHelper.getSSLContext(JPPFIdentifiers.NODE_CLASSLOADER_CHANNEL);
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
  public AsyncNodeClassContext createNioContext(final Object...params) {
    return new AsyncNodeClassContext(this, (SocketChannel) params[0]);
  }

  /**
   * Close the specified channel.
   * @param context the channel to close.
   */
  public void closeConnection(final AsyncNodeClassContext context) {
    if (debugEnabled) log.debug("closing {}", context);
    try {
      final SelectionKey key = context.getSelectionKey();
      if (key != null) {
        key.cancel();
        key.channel().close();
      }
      final String uuid = context.getUuid();
      if (uuid != null) removeNodeConnection(uuid);
      try {
        context.close();
      } catch(final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(e.getMessage());
      }
      if (context.isPeer()) {
        try {
          final BaseNodeContext ctx = getDriver().getAsyncNodeNioServer().getConnection(uuid);
          if (ctx != null) ctx.handleException(null);
        } catch(final Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(e.getMessage());
        }
      }
    } catch (final Exception e) {
      log.error("error closing channel {}: {}", context, ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * Get a channel from its uuid.
   * @param uuid the uuid key to look up in the the map.
   * @return channel the corresponding channel.
   */
  public AsyncNodeClassContext getNodeConnection(final String uuid) {
    return nodeConnections.get(uuid);
  }

  /**
   * Put the specified uuid / channel pair into the uuid map.
   * @param uuid the uuid key to add to the map.
   * @param channel the corresponding channel.
   */
  public void addNodeConnection(final String uuid, final AsyncNodeClassContext channel) {
    if (debugEnabled) log.debug("adding node connection: uuid=" + uuid + ", channel=" + channel);
    nodeConnections.put(uuid, channel);
  }

  /**
   * Remove the specified uuid entry from the uuid map.
   * @param uuid the uuid key to remove from the map.
   * @return channel the corresponding channel.
   */
  public AsyncLocalNodeClassloaderContext removeNodeConnection(final String uuid) {
    if (debugEnabled) log.debug("removing node connection: uuid=" + uuid);
    return nodeConnections.remove(uuid);
  }

  /**
   * Called when the node failed to respond to a heartbeat message.
   * @param channel the channel to close.
   */
  public void connectionFailed(final AsyncNodeClassContext channel) {
    if (channel != null) {
      if (debugEnabled) log.debug("about to close channel = {} with uuid = {}", channel, channel.getUuid());
      closeConnection(channel);
    }
  }

  /**
   * Close and remove all connections accepted by this server.
   * @see org.jppf.nio.NioServer#removeAllConnections()
   */
  @Override
  public synchronized void removeAllConnections() {
    if (!isStopped()) return;
    final List<AsyncNodeClassContext> list  = new ArrayList<>(nodeConnections.values());
    nodeConnections.clear();
    super.removeAllConnections();
    for (AsyncNodeClassContext channel: list) {
      try {
        closeConnection(channel);
      } catch (final Exception e) {
        log.error("error closing channel {} : {}", channel, ExceptionUtils.getStackTrace(e));
      }
    }
  }

  /**
   * @return the list of all current connections.
   */
  public List<AsyncNodeClassContext> getAllNodeConnections() {
    final List<AsyncNodeClassContext> list = new ArrayList<>(nodeConnections.values());
    //if (localChannel != null) list.add(localChannel);
    return list;
  }

  /**
   * Get the resource provider for this server.
   * @return a ResourceProvider instance.
   */
  public ResourceProvider getResourceProvider() {
    return resourceProvider;
  }
  /**
   * @return the message handler for this server.
   */
  public AsyncNodeClassMessageHandler getMessageHandler() {
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
