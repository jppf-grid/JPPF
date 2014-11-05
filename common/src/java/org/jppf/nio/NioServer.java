/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.*;

import org.jppf.io.IO;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.JPPFIdentifiers;
import org.jppf.utils.streams.StreamUtils;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * the selector of all socket channels open with providers or nodes.
   */
  protected Selector selector;
  /**
   * Flag indicating that this socket server is closed.
   */
  private AtomicBoolean stopped = new AtomicBoolean(false);
  /**
   * The ports this server is listening to.
   */
  protected int[] ports = null;
  /**
   * The SSL ports this server is listening to.
   */
  protected int[] sslPorts = null;
  /**
   * Timeout for the select() operations. A value of 0 means no timeout, i.e.
   * the <code>Selector.select()</code> will be invoked without parameters.
   */
  protected long selectTimeout = 0L;
  /**
   * The factory for this server.
   */
  protected NioServerFactory<S, T> factory = null;
  /**
   * Lock used to synchronize selector operations.
   */
  protected ReentrantLock lock = new ReentrantLock();
  /**
   * Performs all operations that relate to channel states.
   */
  protected StateTransitionManager<S, T> transitionManager = null;
  /**
   * Shutdown requested for this server
   */
  protected final AtomicBoolean requestShutdown = new AtomicBoolean(false);
  /**
   * The SSL context associated with this server.
   */
  protected SSLContext sslContext = null;
  /**
   * The channel identifier for channels handled by this server.
   */
  protected final int identifier;
  /**
   * List of opened server socket channels.
   */
  private List<ServerSocketChannel> servers = new Vector<>();

  /**
   * Initialize this server with a specified port number and name.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  protected NioServer(final int identifier, final boolean useSSL) throws Exception {
    super(JPPFIdentifiers.serverName(identifier));
    this.identifier = identifier;
    selector = Selector.open();
    factory = createFactory();
    transitionManager = new StateTransitionManager<>(this);
    if (useSSL) createSSLContext();
  }

  /**
   * Initialize this server with a specified list of port numbers and name.
   * @param ports the list of ports this server accepts connections from.
   * @param sslPorts the list of SSL ports this server accepts connections from.
   * @param identifier the channel identifier for channels handled by this server.
   * performed sequentially or through the executor thread pool.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NioServer(final int[] ports, final int[] sslPorts, final int identifier) throws Exception {
    this(identifier, false);
    this.ports = ports;
    this.sslPorts = sslPorts;
    init();
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
   * Initialize the underlying server sockets for the spcified array of ports.
   * @param portsToInit the array of ports to initiialize.
   * @param ssl <code>true</code> if the server sockets should be initialized with SSL enabled, <code>false</code> otherwise.
   * @throws Exception if any error occurs while initializing the server sockets.
   */
  private void init(final int[] portsToInit, final Boolean ssl) throws Exception {
    for (int i=0; i<portsToInit.length; i++) {
      if (portsToInit[i] < 0) continue;
      ServerSocketChannel server = ServerSocketChannel.open();
      server.socket().setReceiveBufferSize(IO.SOCKET_BUFFER_SIZE);
      InetSocketAddress addr = new InetSocketAddress(portsToInit[i]);
      //server.socket().bind(addr, 100);
      server.socket().bind(addr);
      // If the user specified port zero, the operating system should dynamically allocate a port number.
      // we store the actual assigned port number so that it can be broadcast.
      if (portsToInit[i] == 0) portsToInit[i] = server.socket().getLocalPort();
      server.configureBlocking(false);
      server.register(selector, SelectionKey.OP_ACCEPT, ssl);
      servers.add(server);
    }
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
    SSLParameters params = SSLHelper.getSSLParameters();
    engine.setUseClientMode(false);
    engine.setSSLParameters(params);
  }

  /**
   * Start the underlying server socket by making it accept incoming connections.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    try {
      boolean hasTimeout = selectTimeout > 0L;
      while (!isStopped() && !externalStopCondition()) {
        try {
          lock.lock();
        } finally {
          lock.unlock();
        }
        int n = hasTimeout ? selector.select(selectTimeout) : selector.select();
        //if (!isStopped() && (n > 0) && !externalStopCondition()) go(selector.selectedKeys());
        if (n > 0) go(selector.selectedKeys());
      }
    } catch (Throwable t) {
      log.error("error in selector loop for {} : {}", getClass().getSimpleName(), t);
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
    requestShutdown.set(true);
    try {
      lock.lock();
      selector.wakeup();
    } finally {
      lock.unlock();
    }
  }

  /**
   * Process the keys selected by the selector for IO operations.
   * @param selectedKeys the set of keys that were selected by the latest <code>select()</code> invocation.
   * @throws Exception if an error is raised while processing the keys.
   */
  public void go(final Set<SelectionKey> selectedKeys) throws Exception {
    Iterator<SelectionKey> it = selectedKeys.iterator();
    while (it.hasNext()) {
      SelectionKey key = it.next();
      it.remove();
      NioContext context = null;
      try {
        if (!key.isValid()) continue;
        if (key.isAcceptable()) doAccept(key);
        else {
          context = (NioContext) key.attachment();
          transitionManager.submitTransition(context.getChannel());
        }
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        if (context != null) context.handleException(context.getChannel(), e);
        if (!(key.channel() instanceof ServerSocketChannel)) {
          try {
            key.channel().close();
          } catch (Exception e2) {
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
  @SuppressWarnings("unchecked")
  private void doAccept(final SelectionKey key) {
    ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
    boolean ssl = (Boolean) key.attachment();
    SocketChannel channel;
    try {
      channel = serverSocketChannel.accept();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      return;
    }
    if (channel == null) return;
    Runnable task = new AcceptChannelTask(channel, ssl);
    transitionManager.submit(task);
    //task.run();
  }

  /**
   * Register an incoming connection with this server's selector.
   * The channel is registered with an empty set of initial interest operations,
   * which means a call to the corresponding {@link SelectionKey}'s <code>interestOps()</code> method will return 0.
   * @param channel the socket channel representing the connection.
   * @param sslHandler an sslEngine eventually passed on from a different server.
   * @param ssl specifies whether an <code>SSLHandler</code> should be initialized for the channel.
   * @return a wrapper for the newly registered channel.
   */
  @SuppressWarnings("unchecked")
  public ChannelWrapper<?> accept(final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl) {
    if (debugEnabled) log.debug("{} performing accept() of channel {}, ssl={}", new Object[] {this, channel, ssl});
    NioContext context = createNioContext();
    SelectionKeyWrapper wrapper = null;
    lock.lock();
    try {
      if (sslHandler != null) context.setSSLHandler(sslHandler);
      SelectionKey selKey = channel.register(selector.wakeup(),	0, context);
      wrapper = new SelectionKeyWrapper(selKey);
      context.setChannel(wrapper);
      context.setSsl(ssl);
      if (ssl && (sslHandler == null) && (sslContext != null)) {
        if (debugEnabled) log.debug("creating SSLEngine for  {}", wrapper);
        SSLEngine engine = sslContext.createSSLEngine(channel.socket().getInetAddress().getHostAddress(), channel.socket().getPort());
        configureSSLEngine(engine);
        context.setSSLHandler(new SSLHandler(wrapper, engine));
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
   * Process a channel that was accepted by the server socket channel.
   * @param key the selection key for the socket channel to process.
   */
  public abstract void postAccept(ChannelWrapper<?> key);

  /**
   * Define a context for a newly created channel.
   * @return an <code>NioContext</code> instance.
   */
  public abstract NioContext<?> createNioContext();

  /**
   * Close the underlying server socket and stop this socket server.
   */
  public void end() {
    if (!isStopped()) {
      if (debugEnabled) log.debug("closing server {}", this);
      setStopped(true);
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
      try {
        selector.wakeup();
        selector.close();
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      for (ServerSocketChannel server: servers) {
        try {
          server.close();
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Get all connections accepted by this server.
   * @return a list of {@link ChannelWrapper} instances.
   */
  public List<ChannelWrapper<?>> getAllConnections() {
    List<ChannelWrapper<?>> channels = new ArrayList<>();
    lock.lock();
    try {
      selector.wakeup();
      Set<SelectionKey> keySet = selector.keys();
      for (SelectionKey key: keySet) {
        NioContext ctx = (NioContext) key.attachment();
        channels.add(ctx.getChannel());
      }
    } catch (Exception e) {
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
  public ReentrantLock getLock() {
    return lock;
  }

  /**
   * Set this server in the specified stopped state.
   * @param stopped true if this server is stopped, false otherwise.
   */
  protected void setStopped(final boolean stopped) {
    this.stopped.set(stopped);
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
   * Get the SSL context associated with this server.
   * @return a {@link SSLContext} instance.
   */
  public SSLContext getSSLContext() {
    return sslContext;
  }

  /**
   * Determines whether the specified channel is in an idle state.
   * @param channel the channel to check.
   * @return <code>true</code> if the channel is idle, <code>false</code> otherwise.
   */
  public abstract boolean isIdle(ChannelWrapper<?> channel);

  /**
   * This task performs the processing of a newly accepted channel.
   */
  private class AcceptChannelTask implements Runnable {
    /**
     * The newly accepted socket channel.
     */
    private final SocketChannel channel;
    /**
     * Determines whether ssl is enabled for the channel
     */
    private final boolean ssl;

    /**
     * Initialize this task with the specified selection key.
     * @param channel the newly accepted socket channel.
     * @param ssl determines whether ssl is enabled for the channel.
     */
    public AcceptChannelTask(final SocketChannel channel, final boolean ssl) {
      this.channel = channel;
      this.ssl = ssl;
    }

    @Override
    public void run() {
      try {
        if (debugEnabled) log.debug("accepting channel {}, ssl={}", channel, ssl);
        channel.socket().setSendBufferSize(IO.SOCKET_BUFFER_SIZE);
        channel.socket().setReceiveBufferSize(IO.SOCKET_BUFFER_SIZE);
        channel.socket().setTcpNoDelay(IO.SOCKET_TCP_NODELAY);
        channel.socket().setKeepAlive(IO.SOCKET_KEEPALIVE);
        if (channel.isBlocking()) channel.configureBlocking(false);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        StreamUtils.close(channel, log);
        return;
      }
      accept(channel, null, ssl);
    }
  }

  /**
   * Get the channel identifier for channels handled by this server.
   * @return an int whose value is one of the constants defined in {@link org.jppf.utils.JPPFIdentifiers}.
   */
  public int getIdentifier() {
    return identifier;
  }
}
