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

package org.jppf.server.node;

import static org.jppf.utils.configuration.JPPFProperties.MANAGEMENT_PORT_NODE;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.management.MBeanServer;

import org.jppf.JPPFReconnectionNotification;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.execute.ThreadManager;
import org.jppf.execute.async.AsyncExecutionManager;
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.nio.*;
import org.jppf.node.*;
import org.jppf.node.protocol.*;
import org.jppf.startup.JPPFNodeStartupSPI;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.*;
import org.jppf.utils.hooks.*;
import org.slf4j.*;

/**
 * This class is used as a container for common methods that cannot be implemented in {@link AbstractNode}.
 * @author Laurent Cohen
 */
public abstract class AbstractCommonNode extends AbstractNode {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractCommonNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Manages the class loaders and how they are used.
   * @exclude
   */
  protected AbstractClassLoaderManager<?> classLoaderManager;
  /**
   * Flag which determines whether a reset of the resource caches
   * should be performed at the next opportunity.
   */
  AtomicBoolean cacheResetFlag = new AtomicBoolean(false);
  /**
   * The current pending action, if any.
   */
  final AtomicReference<NodePendingAction> pendingAction = new AtomicReference<>(NodePendingAction.NONE);
  /**
   * Determines whetehr the node is currently processing tasks.
   */
  boolean executing;
  /**
   * Flag indicating whether the node is suspended, i.e. it is still alive but has stopped taking on new jobs.
   */
  final AtomicBoolean suspended = new AtomicBoolean(false);
  /**
   * Lock for synchronization on the suspended state.
   */
  final ThreadSynchronization suspendedLock = new ThreadSynchronization();
  /**
   * Flag indicating whether the node is suspended, i.e. it is still alive but has stopped taking on new jobs.
   */
  final AtomicBoolean reading = new AtomicBoolean(false);
  /**
   * @exclude
   */
  protected JPPFReconnectionNotification reconnectionNotification;
  /**
   * The task execution manager for this node.
   */
  AsyncExecutionManager executionManager;
  /**
   * The executor for serialization and deserialization of the tasks.
   */
  final ThreadPoolExecutor serializationExecutor;
  /**
   * Whether this node was sdtarted from {@code NodeRunner.main()} (standalone) or not (embedded).
   */
  boolean startedFromMain;
  /**
   * The default node's management MBean.
   */
  private JPPFNodeAdminMBean nodeAdmin;
  /**
   * Manager for the MBean defined through the service provider interface.
   */
  NodeMBeanProviderManager providerManager;

  /**
   * Initialize this node.
   * @param uuid this node's uuid.
   * @param configuration the configuration of this node.
   * @param hookFactory used to create and invoke hook instances.
   */
  public AbstractCommonNode(final String uuid, final TypedProperties configuration, final HookFactory hookFactory) {
    super(uuid, configuration, hookFactory);
    final int poolSize = ThreadManager.computePoolSize(configuration, JPPFProperties.PROCESSING_THREADS);
    final long ttl = ThreadManager.retrieveTTL(configuration, JPPFProperties.PROCESSING_THREADS_TTL);
    serializationExecutor = new ThreadPoolExecutor(poolSize, poolSize, ttl, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new JPPFThreadFactory("NodeSerializer"));
    serializationExecutor.allowCoreThreadTimeOut(true);
  }

  /**
   * Add management parameters to the specified bundle, before sending it back to a server.
   * @param bundle the bundle to add parameters to.
   * @exclude
   */
  protected void setupBundleParameters(final TaskBundle bundle) {
    try {
      final JMXServer jmxServer = getJmxServer();
      bundle.setParameter(BundleParameter.NODE_MANAGEMENT_PORT_PARAM, jmxServer.getManagementPort());
      bundle.setParameter(BundleParameter.NODE_PROVISIONING_MASTER, isMasterNode());
      bundle.setParameter(BundleParameter.NODE_PROVISIONING_SLAVE, isSlaveNode());
      if (isSlaveNode()) bundle.setParameter(BundleParameter.NODE_PROVISIONING_MASTER_UUID, getMasterNodeUuid());
    } catch(final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
  }

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @return a <code>ClassLoader</code> used for loading the classes of the framework.
   * @exclude
   */
  public AbstractJPPFClassLoader getClassLoader() {
    return classLoaderManager.getClassLoader();
  }

  @Override
  public boolean isOffline() {
    return getClassLoader().isOffline();
  }

  /**
   * Set the main classloader for the node.
   * @param cl the class loader to set.
   * @exclude
   */
  public void setClassLoader(final AbstractJPPFClassLoader cl) {
    classLoaderManager.setClassLoader(cl);
  }

  /**
   * Get a reference to the JPPF container associated with an application uuid.
   * @param uuidPath the uuid path containing the key to the container.
   * @return a <code>JPPFContainer</code> instance.
   * @throws Exception if an error occurs while getting the container.
   * @exclude
   */
  public JPPFContainer getContainer(final List<String> uuidPath) throws Exception {
    return classLoaderManager.getContainer(uuidPath);
  }

  /**
   * Clear the resource caches of all class loaders managed by this object.
   */
  void clearResourceCachesIfRequested() {
    if (cacheResetFlag.get()) {
      try {
        classLoaderManager.clearResourceCaches();
      } finally {
        cacheResetFlag.set(false);
      }
    }
  }

  /**
   * Request a reset of the class loaders resource caches.
   * This method merely sets a floag, the actual reset will
   * be performed at the next opportunity, when it is safe to do so.
   * @exclude
   */
  public void requestResourceCacheReset() {
    cacheResetFlag.compareAndSet(false, true);
  }

  /**
   * Determine whether a node shurdown or restart was requested..
   * @return {@code true} if a shudown or restart was requested, {@code false} otherwise.
   * @exclude
   */
  public boolean isShutdownRequested() {
    final NodePendingAction action = pendingAction.get();
    return (action == NodePendingAction.RESTART) || (action == NodePendingAction.SHUTDOWN);
  }

  /**
   * Determine whether a restart or shutdown was requested.
   * @return {@code true} if a restart was requested, false if a {@code shutdown} was requested.
   * @exclude
   */
  public boolean isRestart() {
    return pendingAction.get() == NodePendingAction.RESTART;
  }

  /**
   * Determine whether the node is currently processing tasks.
   * @return {@code true} if the node is processing tasks, {@code false} otherwise.
   * @exclude
   */
  public boolean isExecuting() {
    return executing;
  }

  /**
   * Specifiy whether the node is currently processing tasks.
   * @param executing {@code true} to specify that the node is processing tasks, {@code false} otherwise.
   * @exclude
   */
  public void setExecuting(final boolean executing) {
    this.executing = executing;
  }

  /**
   * Determine whether the node is suspended, i.e. it is still alive but has stopped taking on new jobs.
   * @return {@code true} if the node is suspended, {@code false} otherwise.
   * @exclude
   */
  public boolean isSuspended() {
    return suspended.get();
  }

  /**
   * Set the node's suspended state, i.e. whether it should sto taking on new jobs.
   * @param suspended {@code true} to suspend the node, {@code false} otherwise.
   * @exclude
   */
  public void setSuspended(final boolean suspended) {
    this.suspended.set(suspended);
    if (!suspended) suspendedLock.wakeUp();
  }

  /**
   * Get the service that manages the class loaders and how they are used.
   * @return an {@link AbstractClassLoaderManager} instance.
   * @exclude
   */
  @Override
  public AbstractClassLoaderManager<?> getClassLoaderManager() {
    return classLoaderManager;
  }

  /**
   * @exclude
   */
  @Override
  public AsyncExecutionManager getExecutionManager() {
    return executionManager;
  }

  @Override
  public AbstractJPPFClassLoader resetTaskClassLoader(final Object...params) {
    if (debugEnabled) log.debug("using params = {}", Arrays.toString(params));
    if ((params == null) || (params.length <= 0)) return null;
    if (!(params[0] instanceof JPPFDistributedJob)) return null;
    final TaskBundle bundle = (TaskBundle) params[0];
    try {
      TaskThreadLocals.setRequestUuid(bundle.getUuid());
      final List<String> uuidPath = bundle.getUuidPath().getList();
      final boolean remoteClassLoadingDisabled = classLoaderManager.getContainer(uuidPath, params).getClassLoader().isRemoteClassLoadingDisabled();
      final AbstractJPPFClassLoader newCL = classLoaderManager.resetClassLoader(uuidPath, params);
      newCL.setRemoteClassLoadingDisabled(remoteClassLoadingDisabled);
      return newCL;
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return null;
  }

  /**
   * 
   */
  void initStartups() {
    final Hook<JPPFNodeStartupSPI> hook = hookFactory.registerSPIMultipleHook(JPPFNodeStartupSPI.class, null, null);
    for (final HookInstance<JPPFNodeStartupSPI> hookInstance: hook.getInstances()) {
      final JPPFNodeStartupSPI instance = hookInstance.getInstance();
      final Method m = ReflectionUtils.getSetter(instance.getClass(), "setNode");
      if ((m != null) && (Node.class.isAssignableFrom(m.getParameterTypes()[0]))) {
        try {
          m.invoke(instance, this);
        } catch (final IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          log.error("error setting Node on startup of type {}", instance.getClass().getName(), e);
        }
      }
      hookInstance.invoke("run");
    }
  }

  /**
   * @return whether this node was sdtarted from {@code NodeRunner.main()} (standalone) or not (embedded).
   * @exclude
   */
  public boolean isStartedFromMain() {
    return startedFromMain;
  }

  /**
   * @param startedFromMain whether this node was sdtarted from {@code NodeRunner.main()} (standalone) or not (embedded).
   * @exclude
   */
  public void setStartedFromMain(final boolean startedFromMain) {
    this.startedFromMain = startedFromMain;
  }

  /**
   * @exclude
   */
  @Override
  public JMXServer getJmxServer() throws Exception {
    return getJmxServer(true);
  }

  /**
   * 
   * @param register whether to register the MBeans.
   * @return the JMX server.
   * @throws Exception if any error occurs.
   */
  JMXServer getJmxServer(final boolean register) throws Exception {
    synchronized(this) {
      if ((jmxServer == null) || jmxServer.isStopped()) {
        log.info("creating the JMX server for " + getUuid());
        if (debugEnabled) log.debug("starting JMX server");
        final boolean ssl = configuration.get(JPPFProperties.SSL_ENABLED);
        JPPFProperty<Integer> jmxProp = null;
        jmxProp = MANAGEMENT_PORT_NODE;
        final MBeanServer mbeanServer = JPPFMBeanServerFactory.getMBeanServer();
        if (register) registerMBeans(mbeanServer);
        jmxServer = JMXServerFactory.createServer(configuration, uuid, ssl, jmxProp, mbeanServer);
        jmxServer.start(getClass().getClassLoader());
        System.out.println("JPPF Node management initialized on port " + jmxServer.getManagementPort());
      }
    }
    return jmxServer;
  }

  /**
   * Register the MBeans for this node.
   * @param mbeanServer the MBean server to use.
   */
  private void registerMBeans(final MBeanServer mbeanServer) {
    try {
      if (ManagementUtils.isManagementAvailable() && !ManagementUtils.isMBeanRegistered(JPPFNodeAdminMBean.MBEAN_NAME, mbeanServer)) {
        final ClassLoader cl = getClass().getClassLoader();
        if (providerManager == null) providerManager = new NodeMBeanProviderManager(JPPFNodeMBeanProvider.class, cl, mbeanServer, this);
      }
    } catch (final Exception e) {
      log.error("Error registering the MBeans for node " + getUuid(), e);
    }
  }

  /**
   * Stop the jmx server.
   * @throws Exception if any error occurs.
   * @exclude
   */
  public void stopJmxServer() throws Exception {
    if (jmxServer != null) jmxServer.stop();
  }

  /**
   * @exclude
   */
  @Override
  public synchronized void stopNode() {
    if (debugEnabled) log.debug("stopping node");
    setStopped(true);
    executionManager.shutdown();
    serializationExecutor.shutdownNow();
    reset(true);
  }

  /**
   * Shutdown and eventually restart the node.
   * @param restart determines whether this node should be restarted by the node launcher.
   * @exclude
   */
  public void shutdown(final boolean restart) {
    if (!isLocal()) {
      setStopped(true);
      lifeCycleEventHandler.fireNodeEnding();
      new ShutdownOrRestart(restart, startedFromMain, this).run();
    } else {
      if (debugEnabled) log.debug("shutting down local node");
      stopNode();
    }
  }

  /**
   * Reset this node for shutdown/restart/reconnection.
   * @param stopJmx <code>true</code> if the JMX server is to be stopped, <code>false</code> otherwise.
   */
  void reset(final boolean stopJmx) {
    if (debugEnabled) log.debug("resetting with stopJmx=" + stopJmx);
    if (lifeCycleEventHandler != null) {
      lifeCycleEventHandler.fireNodeEnding();
      lifeCycleEventHandler.removeAllProviders();
    }
    setNodeAdmin(null);
    if (stopJmx) {
      try {
        if (providerManager != null) providerManager.unregisterProviderMBeans();
        if (jmxServer != null) {
          jmxServer.stop();
          JPPFMBeanServerFactory.releaseMBeanServer(jmxServer.getMBeanServer());
        }
        final NioServer acceptor = NioHelper.getAcceptorServer(false);
        if (acceptor != null) {
          if (jmxServer != null) acceptor.removeServer(jmxServer.getManagementPort());
        }
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    classLoaderManager.closeClassLoader();
    try {
      synchronized(this) {
        closeDataChannel();
      }
      classLoaderManager.clearContainers();
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the administration and monitoring MBean for this node.
   * @return a {@link JPPFNodeAdminMBean} instance.
   * @exclude
   */
  public synchronized JPPFNodeAdminMBean getNodeAdmin() {
    return nodeAdmin;
  }

  /**
   * Set the administration and monitoring MBean for this node.
   * @param nodeAdmin a <code>JPPFNodeAdminMBean</code>m instance.
   * @exclude
   */
  public synchronized void setNodeAdmin(final JPPFNodeAdminMBean nodeAdmin) {
    this.nodeAdmin = nodeAdmin;
  }

  /**
   * @return the executor for serialization and deserialization of the tasks.
   * @exclude
   */
  public ExecutorService getSerializationExecutor() {
    return serializationExecutor;
  }

  /**
   * @return the reconnection notification.
   * @exclude
   */
  public JPPFReconnectionNotification getReconnectionNotification() {
    return reconnectionNotification;
  }

  /**
   * @param reconnectionNotification the reconnection notification.
   * @exclude
   */
  public void setReconnectionNotification(final JPPFReconnectionNotification reconnectionNotification) {
    this.reconnectionNotification = reconnectionNotification;
  }

  /**
   * Get the current pending action, if any. 
   * @return the current pending action, or {@code null} if there isn't one.
   * @exclude
   */
  public NodePendingAction getPendingAction() {
    return pendingAction.get();
  }

  /**
   * Set the current pending action.
   * @param action the pending aciton to set.
   * @return {@code true} if the new pending action was set successfully, {@code false} otherwise (if a pending action was already set).
   * @exclude
   */
  public boolean setPendingAction(final NodePendingAction action) {
    return pendingAction.compareAndSet(NodePendingAction.NONE, (action == null) ? NodePendingAction.NONE : action);
  }

  /**
   * Cancel the current pending action, if anty.
   * @return {@code true} if the pending action was successfully cncelled, {@code false} otherwise.
   * @exclude
   */
  public boolean cancelPendingAction() {
    final boolean b = pendingAction.get() != NodePendingAction.NONE;
    if (b) pendingAction.set(NodePendingAction.NONE);
    return b;
  }

  /**
   * @return whether a deferred action was requested.
   * @exclude
   */
  public boolean hasPendingAction() {
    final NodePendingAction action = pendingAction.get();
    return (action != null) && (action != NodePendingAction.NONE);
  }
}
