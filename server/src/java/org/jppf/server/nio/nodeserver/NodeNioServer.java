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

package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import org.jppf.comm.recovery.*;
import org.jppf.execute.*;
import org.jppf.io.MultipleBuffersLocation;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.nio.*;
import org.jppf.queue.*;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.serialization.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.event.NodeConnectionEventHandler;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatisticsHelper;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class NodeNioServer extends NioServer<NodeState, NodeTransition> implements ReaperListener {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeNioServer.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The uuid for the task bundle sent to a newly connected node.
   */
  private final String INITIAL_BUNDLE_UUID;
  /**
   * The the task bundle sent to a newly connected node.
   */
  //private ServerTaskBundleNode initialNodeBundle = null;
  private final ServerJob initialServerJob;
  /**
   * A reference to the driver's tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Used to create bundler instances.
   */
  private final JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory();
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final TaskQueueChecker<AbstractNodeContext> taskQueueChecker;
  /**
   * A list of all the connections.
   */
  private final Map<String, AbstractNodeContext> allConnections = new ConcurrentHashMap<>();
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver;
  /**
   * The thread polling the local channel.
   */
  private ChannelSelectorThread selectorThread = null;
  /**
   * The local channel, if any.
   */
  private ChannelWrapper localChannel = null;
  /**
   * Handles listeners to node connection events.
   */
  private final NodeConnectionEventHandler nodeConnectionHandler;
  /**
   * Listener used for monitoring state changes.
   */
  private final ExecutorChannelStatusListener statusListener = new ExecutorChannelStatusListener() {
    @Override
    public void executionStatusChanged(final ExecutorChannelStatusEvent event) {
      if (event.getSource() instanceof AbstractNodeContext) {
        updateConnectionStatus((AbstractNodeContext) event.getSource(), event.getOldValue(), event.getNewValue());
      }
    }
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
   * Initialize this node server.
   * @param driver reference to the driver.
   * @param queue the reference queue to use.
   * @param useSSL determines whether an SSLContext should be created for this server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NodeNioServer(final JPPFDriver driver, final JPPFPriorityQueue queue, final boolean useSSL) throws Exception {
    super(JPPFIdentifiers.NODE_JOB_DATA_CHANNEL, useSSL);
    if (driver == null) throw new IllegalArgumentException("driver is null");
    if (queue == null) throw new IllegalArgumentException("queue is null");

    this.queue = queue;
    this.queue.setCallableAllConnections(new Callable<List<AbstractNodeContext>>() {
      @Override
      public List<AbstractNodeContext> call() throws Exception {
        return getAllChannels();
      }
    });
    nodeConnectionHandler = driver.getInitializer().getNodeConnectionEventHandler();
    INITIAL_BUNDLE_UUID = driver.getUuid();
    this.driver = driver;
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;

    Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
    taskQueueChecker = new TaskQueueChecker<>(queue, driver.getStatistics());
    taskQueueChecker.setBundler(bundler);
    this.queue.addQueueListener(new QueueListener<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode>() {
      @Override
      public void newBundle(final QueueEvent<ServerJob, ServerTaskBundleClient, ServerTaskBundleNode> event) {
        selector.wakeup();
        taskQueueChecker.wakeUp();
      }
    });
    initialServerJob = createInitialServerJob();
    new Thread(taskQueueChecker, "TaskQueueChecker").start();
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param nodeContext the connection wrapper to add.
   */
  public void addConnection(final AbstractNodeContext nodeContext) {
    try {
      if (nodeContext == null) throw new IllegalArgumentException("nodeContext is null");
      if (nodeContext.getChannel() == null) throw new IllegalArgumentException("channel is null");
      if (debugEnabled) log.debug("adding connection {}", nodeContext.getChannel());
      allConnections.put(nodeContext.getUuid(), nodeContext);
      nodeContext.addExecutionStatusListener(statusListener);
      updateConnectionStatus(nodeContext, ExecutorStatus.DISABLED, nodeContext.getExecutionStatus());
    } catch(Exception e) {
      if (debugEnabled) log.debug("error adding connection {} : {}", nodeContext, e);
    }
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param nodeContext the connection wrapper to remove.
   */
  public void removeConnection(final AbstractNodeContext nodeContext) {
    if (nodeContext == null) throw new IllegalArgumentException("wrapper is null");
    if (debugEnabled) log.debug("removing connection {}", nodeContext.getChannel());
    try {
      taskQueueChecker.removeIdleChannel(nodeContext);
      updateConnectionStatus(nodeContext, nodeContext.getExecutionStatus(), ExecutorStatus.DISABLED);
    } catch(Exception e) {
      if (debugEnabled) log.debug("error removing connection {} : {}", nodeContext, e);
    } finally {
      try {
        String uuid = nodeContext.getUuid();
        if (uuid != null) allConnections.remove(uuid);
        nodeContext.removeExecutionStatusListener(statusListener);
      } catch (Throwable e) {
        if (debugEnabled) log.debug("error removing connection {} : {}", nodeContext, e);
      }
    }
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param uuid the id of the connection to remove.
   * @return the context of th channel that was removed, or <code>null</code> if the channel was not found.
   */
  public AbstractNodeContext removeConnection(final String uuid) {
    final AbstractNodeContext nodeContext = getConnection(uuid);
    if (nodeContext == null) return null;
    removeConnection(nodeContext);
    return nodeContext;
  }

  /**
   * Get the connection wrapper for the specified uuid.
   * @param uuid the id of the connection to get.
   * @return the context of the cpnnect that was found, or <code>null</code> if the channel was not found.
   */
  public AbstractNodeContext getConnection(final String uuid) {
    return uuid == null ? null : allConnections.get(uuid);
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param uuid the id of the node to activate or deactivate.
   * @param activate <code>true</code> to activate the node, <code>false</code> to deactivate it.
   * @return the context of th channel that was removed, or <code>null</code> if the channel was not found.
   */
  public AbstractNodeContext activateNode(final String uuid, final boolean activate) {
    AbstractNodeContext nodeContext = getConnection(uuid);
    if (nodeContext == null) return null;
    if (activate != nodeContext.isActive()) nodeContext.setActive(activate);
    return nodeContext;
  }

  /**
   * Initialize the local channel connection.
   * @param localChannel the local channel to use.
   */
  public void initLocalChannel(final ChannelWrapper<?> localChannel) {
    this.localChannel = localChannel;
    ChannelSelector channelSelector = new LocalChannelSelector(localChannel);
    localChannel.setSelector(channelSelector);
    selectorThread = new ChannelSelectorThread(channelSelector, this, 1L);
    localChannel.setInterestOps(0);
    new Thread(selectorThread, "NodeChannelSelector").start();
    postAccept(localChannel);
  }

  /**
   * @param nodeContext   the connection wrapper.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  private void updateConnectionStatus(final AbstractNodeContext nodeContext, final ExecutorStatus oldStatus, final ExecutorStatus newStatus) {
    if (oldStatus == null) throw new IllegalArgumentException("oldStatus is null");
    if (newStatus == null) throw new IllegalArgumentException("newStatus is null");
    if (nodeContext == null || oldStatus == newStatus) return;

    if (newStatus == ExecutorStatus.ACTIVE) taskQueueChecker.addIdleChannel(nodeContext);
    else {
      taskQueueChecker.removeIdleChannel(nodeContext);
      if (newStatus == ExecutorStatus.FAILED || newStatus == ExecutorStatus.DISABLED) queue.cancelBroadcastJobs(nodeContext.getUuid());
    }
    queue.updateWorkingConnections(oldStatus, newStatus);
  }

  @Override
  protected NioServerFactory<NodeState, NodeTransition> createFactory() {
    return new NodeServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper channel) {
    //statsManager.newNodeConnection();
    driver.getStatistics().addValue(JPPFStatisticsHelper.NODES, 1);
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    try {
      context.setBundle(getInitialBundle());
      transitionManager.transitionChannel(channel, NodeTransition.TO_SEND_INITIAL);
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeNode(context);
    }
  }

  @Override
  public AbstractNodeContext createNioContext() {
    final RemoteNodeContext context = new RemoteNodeContext(getTransitionManager());
    context.setOnClose(new Runnable() {
      @Override
      public void run() {
        if (debugEnabled) log.debug("runninng onClose() for {}", context);
        closeNode(context);
      }
    });
    return context;
  }

  /**
   * Get the task bundle sent to a newly connected node,
   * so that it can check whether it is up to date, without having
   * to wait for an actual request to be sent.
   * @return a <code>ServerJob</code> instance, with no task in it.
   */
  ServerTaskBundleNode getInitialBundle() {
    return initialServerJob.copy(0);
  }

  /**
   * Create the base server job used to generate the initial bundle sent to each node.
   * @return a <code>ServerJob</code> instance, with no task in it.
   */
  private ServerJob createInitialServerJob() {
    try {
      SerializationHelper helper = new SerializationHelperImpl();
      // serializing a null data provider.
      JPPFBuffer buf = helper.getSerializer().serialize(null);
      int len = buf.getLength();
      byte[] bytes = new byte[4 + len];
      SerializationUtils.writeInt(len, bytes, 0);
      System.arraycopy(buf.getBuffer(), 0, bytes, 4, len);
      JPPFTaskBundle bundle = new JPPFTaskBundle();
      bundle.setName("server handshake");
      bundle.setUuid(INITIAL_BUNDLE_UUID);
      bundle.getUuidPath().add(driver.getUuid());
      bundle.setTaskCount(0);
      bundle.setHandshake(true);
      return new ServerJob(new ReentrantLock(), null, bundle, new MultipleBuffersLocation(new JPPFBuffer(bytes)));
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  /**
   * Close a connection to a node.
   * @param context a <code>SocketChannel</code> that encapsulates the connection.
   */
  public void closeNode(final AbstractNodeContext context) {
    lock.lock();
    try {
      selector.wakeup();
      if (context != null) context.close();
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    } finally {
      lock.unlock();
    }
    try {
      JPPFManagementInfo info = context.getManagementInfo();
      if (info == null) info = new JPPFManagementInfo("unknown host", -1, context.getUuid(), context.isPeer() ? JPPFManagementInfo.PEER : JPPFManagementInfo.NODE, context.isSecure());
      nodeConnectionHandler.fireNodeDisconnected(info);
      //driver.getStatsManager().nodeConnectionClosed();
      driver.getStatistics().addValue(JPPFStatisticsHelper.NODES, -1);
      removeConnection(context);
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Get the algorithm that dynamically computes the task bundle size.
   * @return a <code>Bundler</code> instance.
   */
  public Bundler getBundler() {
    return taskQueueChecker.getBundler();
  }

  /**
   * Set the algorithm that dynamically computes the task bundle size.
   * @param bundler a <code>Bundler</code> instance.
   */
  public void setBundler(final Bundler bundler) {
    taskQueueChecker.setBundler(bundler);
  }

  /**
   * Get a reference to the driver's tasks queue.
   * @return a <code>JPPFQueue</code> instance.
   */
  public JPPFQueue getQueue() {
    return queue;
  }

  /**
   * Get the factory object used to create bundler instances.
   * @return an instance of <code>JPPFBundlerFactory</code>.
   */
  public JPPFBundlerFactory getBundlerFactory() {
    return bundlerFactory;
  }

  /**
   * Get number of nodes attached to the server.
   * @return the number of nodes as an <code>int</code> value.
   */
  public int getNbNodes() {
    return allConnections.size();
  }

  /**
   * Get all the node connections handled by this server.
   * @return a list of <code>ChannelWrapper</code> instances.
   */
  public List<AbstractNodeContext> getAllChannels() {
    return new ArrayList<>(allConnections.values());
  }

  /**
   * Get all the node connections handled by this server.
   * @return a set of <code>ChannelWrapper</code> instances.
   */
  public Set<AbstractNodeContext> getAllChannelsAsSet() {
    return new HashSet<>(allConnections.values());
  }

  /**
   * Get the number of idle channels.
   * @return the size of the underlying list of idle channels.
   */
  public int getNbIdleChannels() {
    return taskQueueChecker.getNbIdleChannels();
  }

  /**
   * Get the list of currently idle channels.
   * @return a list of <code>AbstractNodeContext</code> instances.
   */
  public List<AbstractNodeContext> getIdleChannels() {
    return taskQueueChecker.getIdleChannels();
  }

  @Override
  public void connectionFailed(final ReaperEvent event) {
    ServerConnection c = event.getConnection();
    AbstractNodeContext context = null;
    if (!c.isOk()) {
      String uuid = c.getUuid();
      if (uuid != null) context = removeConnection(uuid);
      if (context != null) {
        if (debugEnabled) log.debug("about to close channel={} with uuid={}", (context.getChannel().isOpen() ? context : context.getClass().getSimpleName()), uuid);
        context.handleException(context.getChannel(), null);
      } else {
        log.warn("found null context - a job may be stuck!");
        //closeNode(context);
      }
    }
  }

  @Override
  public boolean isIdle(final ChannelWrapper<?> channel) {
    return NodeState.IDLE == channel.getContext().getState();
  }

  @Override
  public void removeAllConnections() {
    lock.lock();
    try {
      selector.wakeup();
      if (taskQueueChecker != null) {
        taskQueueChecker.setStopped(true);
        taskQueueChecker.wakeUp();
        taskQueueChecker.clearIdleChannels();
      }
      queue.close();
      for (AbstractNodeContext channel: allConnections.values()) {
        try {
          channel.close();
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
      allConnections.clear();
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      lock.unlock();
    }
    super.removeAllConnections();
  }

  /**
   * Called when a channel is connected.
   * @param channel the connected channel.
   */
  public void nodeConnected(final AbstractNodeContext channel) {
    JPPFManagementInfo info = channel.getManagementInfo();
    if (info != null) nodeConnectionHandler.fireNodeConnected(info);
    addConnection(channel);
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return taskQueueChecker.getJPPFContext();
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
}
