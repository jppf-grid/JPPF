/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.nodeserver.async;

import java.io.EOFException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.*;

import org.jppf.execute.*;
import org.jppf.io.MultipleBuffersLocation;
import org.jppf.load.balancer.JPPFContext;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManager;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.nio.*;
import org.jppf.node.protocol.*;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.queue.*;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.serialization.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.event.NodeConnectionEventHandler;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * The NIO server that handles asynchronous client connections, which can handle multiple jobs concurrently.
 * @author Laurent Cohen
 */
public final class AsyncNodeNioServer extends StatelessNioServer<AsyncNodeContext> implements NodeConnectionCompletionListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeNioServer.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The message handler for this server.
   */
  private final AsyncNodeMessageHandler messageHandler;
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;
  /**
   * The the task bundle sent to a newly connected node.
   */
  private final ServerJob initialServerJob;
  /**
   * A reference to the driver's tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Used to create bundler instances.
   */
  private final JPPFBundlerFactory bundlerFactory;
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final AsyncJobScheduler jobScheduler;
  /**
   * A list of all the connections.
   */
  private final Map<String, AsyncNodeContext> allConnections = new ConcurrentHashMap<>();
  /**
   * Handles listeners to node connection events.
   */
  final NodeConnectionEventHandler nodeConnectionHandler;
  /**
   * Listener used for monitoring state changes.
   */
  private final ExecutorChannelStatusListener statusListener = event -> {
    if (event.getSource() instanceof AsyncNodeContext) updateConnectionStatus((AsyncNodeContext) event.getSource(), event.getOldValue(), event.getNewValue());
  };
  /**
   * The object that holds the node bundles waiting for a node to reconnect and send the rsults.
   */
  private final OfflineNodeHandler offlineNodeHandler = new OfflineNodeHandler();
  /**
   * Handles expiration of dispatched bundles.
   */
  private final JPPFScheduleHandler dispatchExpirationHandler = new JPPFScheduleHandler("DispatchExpiration");
  /**
   * The peer handler.
   */
  private final PeerAttributesHandler peerHandler;
  /**
   * Handles reservation of nodes to jobs.
   */
  private final NodeReservationHandler nodeReservationHandler;
  /**
   * Handler for th epersistence fo the state of the load-balancers.
   */
  private final LoadBalancerPersistenceManager bundlerHandler;

  /**
   * @param driver reference to the driver.
   * @param identifier the channel identifier for channels handled by this server.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if any error occurs.
   */
  @SuppressWarnings("unchecked")
  public AsyncNodeNioServer(final JPPFDriver driver, final int identifier, final boolean useSSL) throws Exception {
    super(identifier, useSSL);
    this.driver = driver;
    selectTimeout = 1000L;
    messageHandler = new AsyncNodeMessageHandler(driver);
    this.queue = driver.getQueue();
    final Callable<List<BaseNodeContext>> callable = () -> getAllChannels();
    this.queue.setCallableAllConnections(callable);
    this.peerHandler = new PeerAttributesHandler(driver, Math.max(1, driver.getConfiguration().getInt("jppf.peer.handler.threads", 1)));
    nodeConnectionHandler = driver.getInitializer().getNodeConnectionEventHandler();
    bundlerFactory = new JPPFBundlerFactory(driver.getConfiguration());
    bundlerHandler = new LoadBalancerPersistenceManager(bundlerFactory);
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;
    jobScheduler = new AsyncJobScheduler(this, queue, driver.getStatistics(), bundlerFactory);
    this.queue.addQueueListener(new QueueListenerAdapter<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode>() {
      @Override
      public void bundleAdded(final QueueEvent<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> event) {
        if (debugEnabled) log.debug("received queue event {}", event);
        jobScheduler.wakeUp();
      }
    });
    initialServerJob = createInitialServerJob(driver);
    nodeReservationHandler = new NodeReservationHandler(driver);
    ThreadUtils.startDaemonThread(jobScheduler, "JobScheduler");
  }

  @Override
  protected void initReaderAndWriter() {
    messageReader = new AsyncNodeMessageReader(this);
    messageWriter = new AsyncNodeMessageWriter(this);
  }

  @Override
  protected void handleSelectionException(final SelectionKey key, final Exception e) {
    final AsyncNodeContext context = (AsyncNodeContext) key.attachment();
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
  public ChannelWrapper<?> accept(final ServerSocketChannel serverSocketChannel, final SocketChannel channel, final SSLHandler sslHandler, final boolean ssl, final boolean peer, final Object... params) {
    try {
      if (debugEnabled) log.debug("accepting socketChannel = {}", channel);
      final AsyncNodeContext context = createContext(channel, ssl);
      registerChannel(context, channel);
      messageHandler.sendHandshakeBundle(context, getHandshakeBundle());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    driver.getStatistics().addValue(JPPFStatisticsHelper.NODES, 1);
    return null;
  }

  /**
   * Create a new channel context.
   * @param channel the associated socket channel.
   * @param ssl whether the connection is secure.
   * @return a new {@link AsyncNodeContext} instance.
   * @throws Exception if any error occurs.
   */
  private AsyncNodeContext createContext(final SocketChannel channel, final boolean ssl)
    throws Exception {
    final AsyncNodeContext context = createNioContext(channel);
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
  private static void configureSSL(final AsyncNodeContext context) throws Exception {
    if (debugEnabled) log.debug("configuring SSL for {}", context);
    final SocketChannel channel = context.getSocketChannel();
    final SSLContext sslContext = SSLHelper.getSSLContext(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL);
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
  public AsyncNodeContext createNioContext(final Object...params) {
    return new AsyncNodeContext(this, (SocketChannel) params[0], false);
  }

  /**
   * Close the specified channel.
   * @param context the channel to close.
   */
  @SuppressWarnings("static-method")
  public void closeConnection(final AsyncNodeContext context) {
    if (debugEnabled) log.debug("closing {}", context);
    try {
      context.close();
      final SelectionKey key = context.getSelectionKey();
      if (key != null) {
        key.cancel();
        key.channel().close();
      }
      peerHandler.onCloseNode(context);
      JPPFManagementInfo info = context.getManagementInfo();
      if (info == null) info = new JPPFManagementInfo("unknown host", "unknown host", -1, context.getUuid(), context.isPeer() ? JPPFManagementInfo.PEER : JPPFManagementInfo.NODE, context.isSecure());
      if (debugEnabled) log.debug("firing nodeDisconnected() for {}", info);
      nodeConnectionHandler.fireNodeDisconnected(info);
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
   * @return the message handler for this server.
   */
  public AsyncNodeMessageHandler getMessageHandler() {
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
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return jobScheduler.getJPPFContext();
  }

  /**
   * Get the object that holds the node bundles waiting for a node to reconnect and send the rsults.
   * @return a {@link OfflineNodeHandler} instance.
   */
  public OfflineNodeHandler getOfflineNodeHandler() {
    return offlineNodeHandler;
  }

  /**
   * Get the handler for the expiration of dispatched bundles.
   * @return a {@link JPPFScheduleHandler} instance.
   */
  public JPPFScheduleHandler getDispatchExpirationHandler() {
    return dispatchExpirationHandler;
  }

  /**
   * Get the peer handler.
   * @return a {@link AsyncPeerAttributesHandler} instance.
   */
  public PeerAttributesHandler getPeerHandler() {
    return peerHandler;
  }

  /**
   * Get the object that handles reservation of nodes to jobs.
   * @return a {@link AsyncNodeReservationHandler} instance.
   */
  public NodeReservationHandler getNodeReservationHandler() {
    return nodeReservationHandler;
  }

  /**
   * Get the task that dispatches queued jobs to available nodes.
   * @return a {@link AsyncJobScheduler} object.
   * @exclude
   */
  public AsyncJobScheduler getJobScheduler() {
    return jobScheduler;
  }

  /**
   * @return the handler for the persistence fo the state of the load-balancers.
   * @exclude
   */
  public LoadBalancerPersistenceManager getBundlerHandler() {
    return bundlerHandler;
  }

  /**
   * Get the factory object used to create bundler instances.
   * @return an instance of {@code JPPFBundlerFactory}.
   */
  public JPPFBundlerFactory getBundlerFactory() {
    return bundlerFactory;
  }

  /**
   * Get all the node connections handled by this server.
   * @return a list of {@link BaseNodeContext} instances.
   */
  public List<BaseNodeContext> getAllChannels() {
    return new ArrayList<>(allConnections.values());
  }

  /**
   * @return a set of {@link BaseNodeContext} instances.
   */
  public Set<BaseNodeContext> getAllChannelsAsSet() {
    return new HashSet<>(allConnections.values());
  }

  /**
   * Called when the node failed to respond to a heartbeat message.
   * @param context the channel to close.
   */
  public void connectionFailed(final BaseNodeContext context) {
    if (context != null) {
      if (debugEnabled) log.debug("about to close channel = {} with uuid = {}", context, context.getUuid());
      removeConnection(context.getUuid());
      context.handleException(null);
    }
  }

  /**
   * Add the specified connection to the list of connections handled by this server.
   * @param nodeContext the connection to add.
   */
  void putConnection(final AsyncNodeContext nodeContext) {
    if (debugEnabled) log.debug("putting connection {}", nodeContext);
    allConnections.put(nodeContext.getUuid(), nodeContext);
  }

  /**
   * Get the connection wrapper for the specified uuid.
   * @param uuid the id of the connection to get.
   * @return the context of the connect that was found, or {@code null} if the channel was not found.
   */
  public AsyncNodeContext getConnection(final String uuid) {
    return uuid == null ? null : allConnections.get(uuid);
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param nodeContext the connection wrapper to add.
   */
  private void addConnection(final AsyncNodeContext nodeContext) {
    try {
      if (nodeContext == null) throw new IllegalArgumentException("nodeContext is null");
      if (debugEnabled) log.debug("adding connection {}", nodeContext);
      if (!nodeContext.isClosed()) {
        nodeContext.addExecutionStatusListener(statusListener);
        if (!nodeContext.isClosed()) nodeContext.setExecutionStatus(ExecutorStatus.ACTIVE);
      }
      if (nodeContext.isClosed()) nodeContext.handleException(null);
    } catch(final Exception e) {
      if (debugEnabled) log.debug("error adding connection {} : {}", nodeContext, e);
    }
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param uuid the id of the connection to remove.
   * @return the context of th channel that was removed, or {@code null} if the channel was not found.
   */
  private AsyncNodeContext removeConnection(final String uuid) {
    if (uuid == null) return null;
    final AsyncNodeContext nodeContext = getConnection(uuid);
    if (nodeContext != null) removeConnection(nodeContext);
    return nodeContext;
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param nodeContext the connection wrapper to remove.
   */
  private void removeConnection(final AsyncNodeContext nodeContext) {
    if (nodeContext == null) throw new IllegalArgumentException("wrapper is null");
    if (debugEnabled) log.debug("removing connection {}", nodeContext);
    try {
      jobScheduler.removeIdleChannelAsync(nodeContext);
      updateConnectionStatus(nodeContext, nodeContext.getExecutionStatus(), ExecutorStatus.DISABLED);
    } catch(final Exception e) {
      if (debugEnabled) log.debug("error removing connection {} : {}", nodeContext, e);
    } finally {
      try {
        final String uuid = nodeContext.getUuid();
        if (uuid != null) allConnections.remove(uuid);
        nodeContext.removeExecutionStatusListener(statusListener);
      } catch (final Throwable e) {
        if (debugEnabled) log.debug("error removing connection {} : {}", nodeContext, e);
      }
    }
  }

  /**
   * @param nodeContext   the connection wrapper.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  private void updateConnectionStatus(final AsyncNodeContext nodeContext, final ExecutorStatus oldStatus, final ExecutorStatus newStatus) {
    if (oldStatus == null) throw new IllegalArgumentException("oldStatus is null");
    if (newStatus == null) throw new IllegalArgumentException("newStatus is null");
    if (nodeContext == null || oldStatus == newStatus) return;
    if (debugEnabled) log.debug("updating channel status from {} to {}: {}", oldStatus, newStatus, nodeContext);
    if (newStatus == ExecutorStatus.ACTIVE) jobScheduler.addIdleChannel(nodeContext);
    else {
      jobScheduler.removeIdleChannelAsync(nodeContext);
      if (newStatus == ExecutorStatus.FAILED || newStatus == ExecutorStatus.DISABLED) transitionManager.execute(() -> queue.getBroadcastManager().cancelBroadcastJobs(nodeContext.getUuid()));
    }
    queue.updateWorkingConnections(oldStatus, newStatus);
  }

  @Override
  public void nodeConnected(final BaseNodeContext context) {
    if (debugEnabled) log.debug("node connected: {}", context);
    final JPPFManagementInfo info = context.getManagementInfo();
    if (!context.isClosed()) {
      peerHandler.onNodeConnected(context);
      addConnection((AsyncNodeContext) context);
      if (!context.isClosed() && (info != null)) nodeConnectionHandler.fireNodeConnected(info);
    }
    if (context.isClosed()) context.handleException(null);
  }

  /**
   * Get the task bundle sent to a newly connected node,
   * so that it can check whether it is up to date, without having
   * to wait for an actual request to be sent.
   * @return a {@link ServerJob} instance, with no task in it.
   */
  public ServerTaskBundleNode getHandshakeBundle() {
    return initialServerJob.copy(0);
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param uuid the id of the node to activate or deactivate.
   * @param activate {@code true} to activate the node, {@code false} to deactivate it.
   * @return the context of th channel that was removed, or {@code null} if the channel was not found.
   */
  public AsyncNodeContext activateNode(final String uuid, final boolean activate) {
    final AsyncNodeContext nodeContext = getConnection(uuid);
    if (nodeContext == null) return null;
    if (activate != nodeContext.isActive()) nodeContext.setActive(activate);
    return nodeContext;
  }

  /**
   * Create the base server job used to generate the initial bundle sent to each node.
   * @param driver the JPPF driver.
   * @return a {@link ServerJob} instance, with no task in it.
   */
  private static ServerJob createInitialServerJob(final JPPFDriver driver) {
    try {
      final SerializationHelper helper = new SerializationHelperImpl();
      // serializing a null data provider.
      final JPPFBuffer buf = helper.getSerializer().serialize(null);
      final byte[] lengthBytes = SerializationUtils.writeInt(buf.getLength());
      final TaskBundle bundle = new JPPFTaskBundle();
      bundle.setName("server handshake");
      bundle.setUuid(driver.getUuid());
      bundle.getUuidPath().add(driver.getUuid());
      bundle.setTaskCount(0);
      bundle.setHandshake(true);
      final JPPFDatasourceFactory factory = JPPFDatasourceFactory.getInstance();
      final TypedProperties config = driver.getConfiguration();
      final Map<String, TypedProperties> defMap = new HashMap<>();
      defMap.putAll(factory.extractDefinitions(config, JPPFDatasourceFactory.Scope.REMOTE));
      bundle.setParameter(BundleParameter.DATASOURCE_DEFINITIONS, defMap);
      return new ServerJob(new ReentrantLock(), null, bundle, new MultipleBuffersLocation(new JPPFBuffer(lengthBytes), buf));
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }
}
