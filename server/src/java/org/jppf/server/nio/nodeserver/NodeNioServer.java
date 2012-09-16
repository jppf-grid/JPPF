/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.comm.recovery.*;
import org.jppf.io.MultipleBuffersLocation;
import org.jppf.job.JobListener;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.JPPFDriver;
import org.jppf.server.JPPFDriverStatsManager;
import org.jppf.server.event.NodeConnectionEventHandler;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.job.JobManager;
import org.jppf.server.nio.*;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.JPPFContext;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class serve task execution requests to the JPPF nodes.
 * @author Laurent Cohen
 */
public class NodeNioServer extends NioServer<NodeState, NodeTransition> implements ReaperListener, JobManager
{
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
  private ServerTaskBundle initialBundle = null;
  /**
   * A reference to the driver's tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * The statistics manager.
   */
  private final JPPFDriverStatsManager statsManager;
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
  private final Map<String, AbstractNodeContext> allConnections = new LinkedHashMap<String, AbstractNodeContext>();
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
   * Counts the current number of connections with ACTIVE or EXECUTING status.
   */
  private final AtomicInteger nbWorkingConnections = new AtomicInteger(0);

  private final NodeConnectionEventHandler nodeConnectionHandler;

  private final List<JobListener> jobListeners = new ArrayList<JobListener>();

  /**
   * Initialize this node server.
   * @throws Exception if the underlying server socket can't be opened.
   */
  public NodeNioServer(final JPPFDriver driver) throws Exception
  {
    super(NioConstants.NODE_SERVER);
    if (driver == null) throw new IllegalArgumentException("driver is null");

    nodeConnectionHandler = driver.getInitializer().getNodeConnectionEventHandler();
    INITIAL_BUNDLE_UUID = driver.getUuid();
    this.statsManager = driver.getStatsManager();
    this.driver = driver;
    this.selectTimeout = NioConstants.DEFAULT_SELECT_TIMEOUT;

    Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
    this.queue = new JPPFPriorityQueue(this, statsManager);

    taskQueueChecker = new TaskQueueChecker<AbstractNodeContext>(queue, statsManager);
    taskQueueChecker.setBundler(bundler);

    this.queue.addQueueListener(new QueueListener() {
      @Override
      public void newBundle(final QueueEvent event) {
        selector.wakeup();
        taskQueueChecker.wakeUp();
      }
    });
    new Thread(taskQueueChecker, "TaskQueueChecker").start();
  }

  public JPPFDriverStatsManager getStatsManager()
  {
    return statsManager;
  }

  public void addIdleChannel(final AbstractNodeContext channel) {
    taskQueueChecker.addIdleChannel(channel);
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param wrapper the connection wrapper to add.
   */
  public synchronized void addConnection(final AbstractNodeContext wrapper)
  {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");
    if (wrapper.getChannel() == null) throw new IllegalArgumentException("wrapper.getChannel() is null");

    allConnections.put(wrapper.getUuid(), wrapper);
    taskQueueChecker.addIdleChannel(wrapper);
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param wrapper the connection wrapper to remove.
   */
  public synchronized void removeConnection(final AbstractNodeContext wrapper)
  {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");

    try
    {
      taskQueueChecker.removeIdleChannel(wrapper);
    }
    finally
    {
      allConnections.remove(wrapper.getUuid());
    }
  }

  /**
   * Initialize the local channel connection.
   * @param localChannel the local channel to use.
   */
  public void initLocalChannel(final ChannelWrapper<?> localChannel)
  {
    this.localChannel = localChannel;
    ChannelSelector channelSelector = new LocalChannelSelector(localChannel);
    localChannel.setSelector(channelSelector);
    selectorThread = new ChannelSelectorThread(channelSelector, this, 1L);
    localChannel.setKeyOps(0);
    new Thread(selectorThread, "NodeChannelSelector").start();
    postAccept(localChannel);
  }

  /**
   * Dtermine whether there is at east one connection, idle or not.
   * @return <code>true</code> if there is at least one connection, <code>false</code> otherwise.
   */
  public synchronized boolean hasWorkingConnection()
  {
    return nbWorkingConnections.get() > 0;
  }

  @Override
  protected NioServerFactory<NodeState, NodeTransition> createFactory()
  {
    return new NodeServerFactory(this);
  }

  @Override
  public void postAccept(final ChannelWrapper channel)
  {
    if (JPPFDriver.JPPF_DEBUG) driver.getInitializer().getServerDebug().addChannel(channel, getName());
    statsManager.newNodeConnection();
    AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
    try
    {
      context.setBundle(getInitialBundle());
      transitionManager.transitionChannel(channel, NodeTransition.TO_SEND_INITIAL);
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      closeNode(context);
    }
  }

  @Override
  public AbstractNodeContext createNioContext()
  {
    final RemoteNodeContext context = new RemoteNodeContext(getTransitionManager());
    context.setOnClose(new Runnable() {
      @Override
      public void run() {
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
  ServerTaskBundle getInitialBundle()
  {
    if (initialBundle == null)
    {
      try
      {
        SerializationHelper helper = new SerializationHelperImpl();
        // serializing a null data provider.
        JPPFBuffer buf = helper.getSerializer().serialize(null);
        byte[] dataProviderBytes = new byte[4 + buf.getLength()];
        ByteBuffer bb = ByteBuffer.wrap(dataProviderBytes);
        bb.putInt(buf.getLength());
        bb.put(buf.getBuffer());
        JPPFTaskBundle bundle = new JPPFTaskBundle();
        bundle.setName("server handshake");
        bundle.setUuid(INITIAL_BUNDLE_UUID);
        bundle.setRequestUuid("0");
        bundle.getUuidPath().add(driver.getUuid());
        bundle.setTaskCount(0);
        bundle.setState(JPPFTaskBundle.State.INITIAL_BUNDLE);
        ServerJob serverJob = new ServerJob(getJobManager(), bundle, new MultipleBuffersLocation(new JPPFBuffer(dataProviderBytes, dataProviderBytes.length)));
        initialBundle = serverJob.copy(serverJob.getTaskCount());
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
    return initialBundle;
  }

  /**
   * Close a connection to a node.
   * @param context a <code>SocketChannel</code> that encapsulates the connection.
   *
   */
  public void closeNode(final AbstractNodeContext context)
  {
    if (JPPFDriver.JPPF_DEBUG && (context != null)) driver.getInitializer().getServerDebug().removeChannel(context.getChannel(), NioConstants.NODE_SERVER);
    try
    {
      if(context != null) context.close();
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
    try
    {
      JPPFManagementInfo info = context.getManagementInfo();
      if (info == null) info = new JPPFManagementInfo("unknown host", -1, "unknown id");
      nodeConnectionHandler.fireNodeDisconnected(info);
      driver.getStatsManager().nodeConnectionClosed();
      removeConnection(context);
      taskQueueChecker.removeIdleChannel(context);
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the algorithm that dynamically computes the task bundle size.
   * @return a <code>Bundler</code> instance.
   */
  public Bundler getBundler()
  {
    return taskQueueChecker.getBundler();
  }

  /**
   * Set the algorithm that dynamically computes the task bundle size.
   * @param bundler a <code>Bundler</code> instance.
   */
  public void setBundler(final Bundler bundler)
  {
    taskQueueChecker.setBundler(bundler);
  }

  /**
   * Get a reference to the driver's tasks queue.
   * @return a <code>JPPFQueue</code> instance.
   */
  public JPPFQueue getQueue()
  {
    return queue;
  }

  /**
   * Get a reference to the driver's job manager.
   * @return a <code>JPPFQueue</code> instance.
   */
  protected JPPFJobManager getJobManager()
  {
    return driver.getJobManager();
  }

  /**
   * Get the factory object used to create bundler instances.
   * @return an instance of <code>JPPFBundlerFactory</code>.
   */
  public JPPFBundlerFactory getBundlerFactory()
  {
    return bundlerFactory;
  }

  /**
   * Get number of nodes attached to the server.
   * @return the number of nodes as an <code>int</code> value.
   */
  public synchronized int getNbNodes() {
    return allConnections.size();
  }

  /**
   * Get all the client connections handled by this manager.
   * @return a list of <code>ChannelWrapper</code> instances.
   */
  public synchronized List<AbstractNodeContext> getAllChannels()
  {
    return new ArrayList<AbstractNodeContext>(allConnections.values());
  }

  /**
   * Get the number of idle channels.
   * @return the size of the underlying list of idle channels.
   */
  public int getNbIdleChannels()
  {
    return taskQueueChecker.getNbIdleChannels();
  }

  /**
   * Get the list of currently idle channels.
   * @return a list of <code>SelectableChannel</code> instances.
   */
  public List<AbstractNodeContext> getIdleChannels()
  {
    return taskQueueChecker.getIdleChannels();
  }

  @Override
  public void connectionFailed(final ReaperEvent event)
  {
    ServerConnection c = event.getConnection();
    if (!c.isOk())
    {
      String uuid = c.getUuid();
      AbstractNodeContext channel =  /*(uuid != null) ? removeUuid(uuid) :*/ null; // todo implement
      if (channel != null)
      {
        if (debugEnabled) log.debug("about to close channel = " + (channel.getChannel().isOpen() ? channel : channel.getClass().getSimpleName()) + " with uuid = " + uuid);
        if (channel != null) channel.handleException(channel.getChannel(), null);
        else
        {
          log.warn("found null context - a job may be stuck!");
          closeNode(channel);
        }
      }
    }
  }


  @Override
  public boolean isIdle(final ChannelWrapper<?> channel)
  {
    return NodeState.IDLE == channel.getContext().getState();
  }

  /**
   * {@inheritDoc}
   */
  public void close()
  {
    setStopped(true);
//    wakeUp();
    if (taskQueueChecker != null)
    {
      taskQueueChecker.setStopped(true);
      taskQueueChecker.wakeUp();
    }
    queue.close();
    synchronized(this)
    {
      for (AbstractNodeContext channel: allConnections.values()) {
        try {
          channel.close();
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }

  public void nodeConnected(final AbstractNodeContext channel) {
    JPPFManagementInfo info = channel.getManagementInfo();
    if(info != null) nodeConnectionHandler.fireNodeConnected(info);
    addConnection(channel);
  }

  @Override
  public boolean cancelJob(final String jobId) throws Exception {
    return queue.cancelJob(jobId);
  }

  @Override
  public void updatePriority(final String jobUuid, final int newPriority) {
    queue.updatePriority(jobUuid, newPriority);
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return a set of ids as strings.
   */
  @Override
  public Set<String> getAllJobIds() {
    return queue.getAllJobIds();
  }

  @Override
  public void addJobListener(final JobListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (jobListeners) {
      jobListeners.add(listener);
    }
  }

  @Override
  public void removeJobListener(final JobListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (jobListeners) {
      jobListeners.remove(listener);
    }
  }

  @Override
  public ServerJob getBundleForJob(final String jobUuid) {
    return queue.getJob(jobUuid);
  }

  public JPPFContext getJPPFContext() {
    return taskQueueChecker.getJPPFContext();
  }
}
