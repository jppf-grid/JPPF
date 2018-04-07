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

package org.jppf.nio;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

import javax.net.ssl.*;

import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.SynchronizedBoolean;
import org.slf4j.*;


/**
 * Generic server for non-blocking asynchronous socket channel based communications.<br>
 * Instances of this class rely on a number of possible states for each socket channel,
 * along with the possible transitions between thoses states.<br>
 * The design of this class enforces the use of typesafe enumerations for the states
 * and transitions, so the developers must think ahead of how to implement their server
 * as a state machine.
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @author Laurent Cohen
 * @author Lane Schwartz (dynamically allocated server port)
 */
public abstract class NioServer<S extends Enum<S>, T extends Enum<T>> extends Thread {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * the selector of all socket channels open with providers or nodes.
   */
  protected final Selector selector;
  /**
   * Flag indicating that this socket server is closed.
   */
  protected final AtomicBoolean stopped = new AtomicBoolean(false);
  /**
   * The ports this server is listening to.
   */
  protected int[] ports;
  /**
   * The SSL ports this server is listening to.
   */
  protected int[] sslPorts;
  /**
   * Timeout for the select() operations. A value of 0 means no timeout, i.e.
   * the <code>Selector.select()</code> will be invoked without parameters.
   */
  protected long selectTimeout;
  /**
   * The factory for this server.
   */
  protected NioServerFactory<S, T> factory;
  /**
   * Lock used to synchronize selector operations.
   */
  protected final Lock lock = new ReentrantLock();
  /**
   * Performs all operations that relate to channel states.
   */
  protected final StateTransitionManager<S, T> transitionManager;
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
   * List of opened server socket channels.
   */
  protected final Map<Integer, ServerSocketChannel> servers = new HashMap<>();
  /**
   * Whether the selector is currently selecting.
   */
  protected final SynchronizedBoolean selecting = new SynchronizedBoolean(false);
  /**
   *
   */
  protected final Runnable wakeUpAction = new Runnable() {
    @Override
    public void run() {
      getSelector().wakeup();
    }
  };
  /**
   * Used to synchronize on th selector for blocking operations.
   */
  protected final SelectorSynchronizer sync;

  /**
   * Initialize this server with a specified port number and name.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected NioServer(final int identifier, final boolean useSSL) throws Exception {
    this(JPPFIdentifiers.serverName(identifier), identifier, useSSL);
  }

  /**
   * Initialize this server with a specified port number and name.
   * @param name the name of this thread.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected NioServer(final String name, final int identifier, final boolean useSSL) throws Exception {
    super(name);
    setDaemon(true);
    if (debugEnabled) log.debug(String.format("starting %s with identifier=%s and useSSL=%b", getClass().getSimpleName(), JPPFIdentifiers.serverName(identifier), useSSL));
    log.info("initializing {}({})", getClass().getSimpleName(), getName());
    this.identifier = identifier;
    selector = Selector.open();
    sync = new SelectorSynchronizerLock(selector);
    transitionManager = createStateTransitionManager();
    factory = createFactory();
    transitionManager.setFactory(factory);
    if (useSSL) createSSLContext();
  }

  /**
   * Initialize this server with a specified list of port numbers and name.
   * @param ports the list of ports this server accepts connections from.
   * @param sslPorts the list of SSL ports this server accepts connections from.
   * @param identifier the channel identifier for channels handled by this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NioServer(final int[] ports, final int[] sslPorts, final int identifier) throws Exception {
    this(identifier, false);
    if (debugEnabled) log.debug(String.format("starting %s with ports=%s and sslPorts=%s", getClass().getSimpleName(), Arrays.toString(ports), Arrays.toString(sslPorts)));
    this.ports = ports;
    this.sslPorts = sslPorts;
    init();
  }

  /**
   * @return a {@link StateTransitionManager} instance.
   */
  protected StateTransitionManager<S, T> createStateTransitionManager() {
    return new StateTransitionManager<>(this);
  }

  /**
   * Create the factory holding all the states and transition mappings.
   * @return an <code>NioServerFactory</code> instance.
   */
  protected abstract NioServerFactory<S, T> createFactory();

  /**
   * Initialize the underlying server sockets.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  protected final void init() throws Exception {
    if ((ports != null) && (ports.length != 0)) init(ports, false);
    if ((sslPorts != null) && (sslPorts.length != 0)) init(sslPorts, true);
  }

  /**
   * Initialize the underlying server sockets for the specified array of ports.
   * @param portsToInit the array of ports to initiialize.
   * @param ssl <code>true</code> if the server sockets should be initialized with SSL enabled, <code>false</code> otherwise.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  private void init(final int[] portsToInit, final boolean ssl) throws Exception {
    for (int i=0; i<portsToInit.length; i++) addServer(portsToInit[i], ssl, null, true);
  }

  /**
   * Initialize the underlying server sockets for the spcified array of ports.
   * @param portToInit the array of ports to initiialize.
   * @param ssl <code>true</code> if the server sockets should be initialized with SSL enabled, <code>false</code> otherwise.
   * @param env optional map of parameters to associate with the server socket channel.
   * @param retryOnException whether to retry if an exception occurs when binding the server.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  public void addServer(final int portToInit, final boolean ssl, final Map<String, ?> env, final boolean retryOnException) throws Exception {
  }

  /**
   * Remove the server identified by the local port it is listneing to.
   * @param port the port the sever is listening to.
   * @throws IOException if any error occurs closing the specified server socket channel.
   */
  public void removeServer(final int port) throws IOException {
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

  @Override
  public void run() {
    try {
      final boolean hasTimeout = selectTimeout > 0L;
      int n = 0;
      while (!isStopped() && !externalStopCondition()) {
        try {
          lock.lock();
        } finally {
          lock.unlock();
        }
        selecting.set(true);
        try {
          n = hasTimeout ? selector.select(selectTimeout) : selector.select();
        } finally {
          selecting.set(false);
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
   * Process the keys selected by the selector for IO operations.
   * @param selectedKeys the set of keys that were selected by the latest <code>select()</code> invocation.
   * @throws Exception if an error is raised while processing the keys.
   */
  protected void go(final Set<SelectionKey> selectedKeys) throws Exception {
    final Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      final SelectionKey key = it.next();
      it.remove();
      NioContext<?> context = null;
      try {
        if (!key.isValid()) continue;
        if (key.isAcceptable()) doAccept(key);
        else {
          context = (NioContext<?>) key.attachment();
          transitionManager.submitTransition(context.getChannel());
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
    transitionManager.execute(task);
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
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl,
    final boolean peer, final Object...params) throws Exception {
    if (debugEnabled) log.debug("{} performing accept() of channel {}, ssl={}", new Object[] {this, channel, ssl});
    final NioContext<?> context = createNioContext(params);
    context.setPeer(peer);
    SelectionKey selKey = null;
    if (sslHandler != null) context.setSSLHandler(sslHandler);
    lock.lock();
    try {
      wakeUpSelectorIfNeeded();
      if (channel.isBlocking()) channel.configureBlocking(false);
      selKey = channel.register(selector, 0, context);
    } finally {
      lock.unlock();
    }
    final SelectionKeyWrapper wrapper = new SelectionKeyWrapper(selKey);
    context.setChannel(wrapper);
    context.setSsl(ssl);
    if (ssl && (sslHandler == null) && (sslContext != null)) {
      if (debugEnabled) log.debug("creating SSLEngine for  {}", wrapper);
      final SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
      configureSSLEngine(engine);
      context.setSSLHandler(new SSLHandlerImpl(wrapper, engine));
    }
    postAccept(wrapper);
    return wrapper;
  }

  /**
   * Process a channel that was accepted by the server socket channel.
   * @param key the selection key for the socket channel to process.
   */
  public void postAccept(final ChannelWrapper<?> key) {
  }

  /**
   * Define a context for a newly created channel.
   * @param params optional parameters.
   * @return an <code>NioContext</code> instance.
   */
  public abstract NioContext<S> createNioContext(final Object...params);

  /**
   * Close the underlying server socket and stop this socket server.
   */
  public void end() {
    if (stopped.compareAndSet(false, true)) {
      if (debugEnabled) log.debug("closing server {}", this);
      wakeUpSelectorIfNeeded();
      removeAllConnections();
    }
  }

  /**
   * Close and remove all connections accepted by this server.
   */
  public void removeAllConnections() {
    if (!isStopped()) return;
    lock.lock();
    try {
      wakeUpSelectorIfNeeded();
      selector.close();
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
    synchronized(servers) {
      for (Map.Entry<Integer, ServerSocketChannel> entry: servers.entrySet()) {
        try {
          entry.getValue().close();
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
      servers.clear();
    }
  }

  /**
   * Get all connections accepted by this server.
   * @return a list of {@link ChannelWrapper} instances.
   */
  public List<ChannelWrapper<?>> getAllConnections() {
    final List<ChannelWrapper<?>> channels = new ArrayList<>();
    lock.lock();
    try {
      wakeUpSelectorIfNeeded();
      final Set<SelectionKey> keySet = selector.keys();
      for (SelectionKey key: keySet) {
        final NioContext<?> ctx = (NioContext<?>) key.attachment();
        channels.add(ctx.getChannel());
      }
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
    return channels;
  }

  /**
   * Get the selector for this server.
   * @return a Selector instance.
   */
  public Selector getSelector() {
    return selector;
  }

  /**
   * Get the factory for this server.
   * @return an <code>NioServerFactory</code> instance.
   */
  public synchronized NioServerFactory<S, T> getFactory() {
    if (factory == null) factory = createFactory();
    return factory;
  }

  /**
   * Get the lock used to synchronize selector operations.
   * @return a <code>ReentrantLock</code> instance.
   */
  public Lock getLock() {
    return lock;
  }

  /**
   * Get the stopped state of this server.
   * @return  true if this server is stopped, false otherwise.
   */
  protected boolean isStopped() {
    return stopped.get();
  }

  /**
   * Get the manager that performs all operations that relate to channel states.
   * @return a <code>StateTransitionManager</code> instance.
   */
  public StateTransitionManager<S, T> getTransitionManager() {
    return transitionManager;
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
   * Determines whether the specified channel is in an idle state.
   * @param channel the channel to check.
   * @return {@code true} if the channel is idle, {@code false} otherwise.
   */
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return false;
  }

  /**
   * Configure the SSL options for the specified channel.
   * @param channel the channel for which to configure SSL.
   * @throws Exception if any error occurs.
   */
  public void configurePeerSSL(final ChannelWrapper<?> channel) throws Exception {
    final SocketChannel socketChannel = (SocketChannel) ((SelectionKey) channel.getChannel()).channel();
    final NioContext<?> context = channel.getContext();
    final Socket socket = socketChannel.socket();
    final SSLEngine engine = sslContext.createSSLEngine(socket.getInetAddress().getHostAddress(), socket.getPort());
    final SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(true);
    engine.setSSLParameters(params);
    final SSLHandler sslHandler = new SSLHandlerImpl(channel, engine);
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
}
