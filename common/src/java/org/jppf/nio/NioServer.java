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

package org.jppf.nio;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.*;

import org.jppf.JPPFUnsupportedOperationException;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Generic server for non-blocking asynchronous socket channel based communications.<br>
 * Instances of this class rely on a number of possible states for each socket channel,
 * along with the possible transitions between thoses states.<br>
 * The design of this class enforces the use of typesafe enumerations for the states
 * and transitions, so the developers must think ahead of how to implement their server
 * as a state machine.
 * @author Laurent Cohen
 * @author Lane Schwartz (dynamically allocated server port)
 */
public abstract class NioServer extends Thread {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * the selector of all socket channels open with providers or nodes.
   */
  protected final Selector selector;
  /**
   * Flag indicating that this socket server is closed.
   */
  protected final AtomicBoolean stopped = new AtomicBoolean(false);
  /**
   * Timeout for the select() operations. A value of 0 means no timeout, i.e. the {@code Selector.select()} method will be invoked without parameters.
   */
  protected long selectTimeout;
  /**
   * Shutdown requested for this server
   */
  protected final AtomicBoolean requestShutdown = new AtomicBoolean(false);
  /**
   * The SSL context associated with this server.
   */
  protected SSLContext sslContext;
  /**
   * The channel identifier for channels handled by this server.
   */
  protected final int identifier;
  /**
   * Whether the selector is currently selecting.
   */
  protected final SynchronizedBoolean selecting = new SynchronizedBoolean(false);
  /**
   *
   */
  protected final Runnable wakeUpAction = () -> getSelector().wakeup();
  /**
   * Used to synchronize on th selector for blocking operations.
   */
  protected final SelectorSynchronizer sync;
  /**
   * An arbitrary object attached to this server.
   */
  protected final Object attachment;
  /**
   * The configuration to use.
   */
  protected TypedProperties configuration;

  /**
   * Initialize this server with a specified identifier and name.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @param attachment an arbitrary object attached to this server.
   * @param configuration the JPPF configuration to use.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected NioServer(final int identifier, final boolean useSSL, final Object attachment, final TypedProperties configuration) throws Exception {
    this(JPPFIdentifiers.serverName(identifier), identifier, useSSL, attachment, configuration);
  }

  /**
   * Initialize this server with a specified port number and name.
   * @param name the name of this thread.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @param attachment an arbitrary object attached to this server.
   * @param configuration the JPPF configuration to use.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected NioServer(final String name, final int identifier, final boolean useSSL, final Object attachment, final TypedProperties configuration) throws Exception {
    super(name);
    setDaemon(true);
    if (debugEnabled) log.debug("starting {} with identifier={} and useSSL={}, name={}", getClass().getSimpleName(), JPPFIdentifiers.serverName(identifier), useSSL, getName());
    this.identifier = identifier;
    this.attachment = attachment;
    selector = Selector.open();
    sync = new SelectorSynchronizerLock(selector);
    this.configuration = configuration;
    if (useSSL && (identifier != JPPFIdentifiers.ACCEPTOR_CHANNEL)) createSSLContext();
    init();
  }

  /**
   * Initialize the underlying server sockets.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  protected abstract void init() throws Exception;

  /**
   * Initialize the underlying server sockets for the spcified array of ports.
   * This implementation throws a {@link JPPFUnsupportedOperationException}, it should be overriden by subclasses for a different outcome.
   * @param portToInit the array of ports to initiialize.
   * @param ssl {@code true} if the server sockets should be initialized with SSL enabled, {@code false} otherwise.
   * @param env optional map of parameters to associate with the server socket channel.
   * @param retryOnException whether to retry if an exception occurs when binding the server.
   * @return {@code true} ifd a new server was created, {@code false} otherwxise.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  public boolean addServer(final int portToInit, final boolean ssl, final Map<String, ?> env, final boolean retryOnException) throws Exception {
    throw new JPPFUnsupportedOperationException("cannot initialize a server on port " + portToInit + " (ssl = " + ssl + ") here");
  }

  /**
   * Remove the server identified by the local port it is listneing to.
   * This implementation throws a {@link JPPFUnsupportedOperationException}, it should be overriden by subclasses for a different outcome.
   * @param port the port the sever is listening to.
   * @throws Exception if any error occurs closing the specified server socket channel.
   */
  public void removeServer(final int port) throws Exception {
    throw new JPPFUnsupportedOperationException("cannot a server on port " + port + " here");
  }

  /**
   * Configure all SSL settings. This method is for interested subclasses classes to override.
   * @throws Exception if any error occurs during the SSL configuration.
   */
  protected void createSSLContext() throws Exception {
    sslContext = SSLHelper.getSSLContext(identifier);
  }

  /**
   * Configure all SSL settings for the specified SSL engine.
   * This method is for interested subclasses classes to override.
   * @param engine the SSL engine to configure.
   * @throws Exception if any error occurs during the SSL configuration.
   */
  protected void configureSSLEngine(final SSLEngine engine) throws Exception {
    final SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
  }

  /**
   * Determine whether a stop condition external to this server has been reached.
   * The default implementation always returns whether shutdown was requested.<br>
   * Subclasses may override this behavior.
   * @return true if this server should be stopped, false otherwise.
   */
  protected boolean externalStopCondition() {
    return requestShutdown.get();
  }

  /**
   * Initiates shutdown of this server.
   */
  public void shutdown() {
    if (requestShutdown.compareAndSet(false, true)) end();
  }

  /**
   * accept the incoming connection.
   * It accept and put it in a state to define what type of peer is.
   * @param key the selection key that represents the channel's registration with the selector.
   */
  protected void doAccept(final SelectionKey key) {
    final ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    @SuppressWarnings("unchecked")
    final Map<String, ?> map = (Map<String, ?>) key.attachment();
    final boolean ssl = (Boolean) map.get("jppf.ssl");
    final SocketChannel channel;
    try {
      channel = serverSocketChannel.accept();
    } catch (final IOException e) {
      log.error(e.getMessage(), e);
      return;
    }
    if (channel == null) return;
    final Runnable task = new AcceptChannelTask(this, serverSocketChannel, channel, ssl);
    GlobalExecutor.getGlobalexecutor().execute(task);
  }

  /**
   * Register an incoming connection with this server's selector.
   * The channel is registered with an empty set of initial interest operations,
   * which means a call to the corresponding {@link SelectionKey}'s {@code interestOps()} method will return 0.
   * @param serverSocketChannel the server socket channel accepting the connection.
   * @param channel the socket channel representing the connection.
   * @param sslHandler an sslEngine eventually passed on from a different server.
   * @param ssl specifies whether an {@code SSLHandler} should be initialized for the channel.
   * @param peer specifiies whether the channel is for a peer driver.
   * @param params optional parameters.
   * @throws Exception if any error occurs.
   */
  public abstract void accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object...params) throws Exception;

  /**
   * Define a context for a newly created channel.
   * @param params optional parameters.
   * @return an {@code NioContext} instance.
   */
  public abstract NioContext createNioContext(final Object...params);

  /**
   * Close the underlying server socket and stop this socket server.
   */
  public void end() {
    if (stopped.compareAndSet(false, true)) {
      if (debugEnabled) log.debug("ending server {}", this);
      wakeUpSelectorIfNeeded();
      removeAllConnections();
    }
  }

  /**
   * Close and remove all connections accepted by this server.
   */
  public void removeAllConnections() {
    if (debugEnabled) log.debug("removing all connections of {}", this);
    if (!isStopped()) return;
    sync.wakeUpAndSetOrIncrement();
    try {
      selector.close();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      sync.decrement();
    }
  }

  /**
   * Get the selector for this server.
   * @return a Selector instance.
   */
  public Selector getSelector() {
    return selector;
  }

  /**
   * Get the stopped state of this server.
   * @return  true if this server is stopped, false otherwise.
   */
  protected boolean isStopped() {
    return stopped.get();
  }

  /**
   * Configure the SSL options for the specified channel.
   * @param context the channel context for which to configure SSL.
   * @throws Exception if any error occurs.
   */
  public void configurePeerSSL(final NioContext context) throws Exception {
    final SocketChannel socketChannel = context.getSocketChannel();
    final Socket socket = socketChannel.socket();
    final SSLEngine engine = sslContext.createSSLEngine(socket.getInetAddress().getHostAddress(), socket.getPort());
    final SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(true);
    engine.setSSLParameters(params);
    final SSLHandler sslHandler = new SSLHandlerImpl(socketChannel, engine);
    context.setSSLHandler(sslHandler);
  }

  /**
   * Get the channel identifier for channels handled by this server.
   * @return an int whose value is one of the constants defined in {@link org.jppf.utils.JPPFIdentifiers}.
   */
  public int getIdentifier() {
    return identifier;
  }

  /**
   * @return whether the selector had to be awaken.
   */
  public boolean wakeUpSelectorIfNeeded() {
    return selecting.compareAndRun(true, wakeUpAction);
  }

  /**
   * @return the configuration to use.
   */
  public TypedProperties getConfiguration() {
    return configuration;
  }

  /**
   * Set the configuration to use.
   * @param configuration the configuration to use.
   */
  public void setConfiguration(final TypedProperties configuration) {
    this.configuration = configuration;
  }
}
