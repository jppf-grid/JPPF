/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;

import javax.management.*;

import org.jppf.*;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.execute.ExecutionManager;
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.node.NodeRunner;
import org.jppf.node.connection.ConnectionReason;
import org.jppf.node.debug.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.*;
import org.jppf.node.provisioning.SlaveNodeManager;
import org.jppf.serialization.*;
import org.jppf.ssl.SSLConfigurationException;
import org.jppf.startup.JPPFNodeStartupSPI;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * Instances of this class encapsulate execution nodes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public abstract class JPPFNode extends AbstractCommonNode implements ClassLoaderProvider {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFNode.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The task execution manager for this node.
   * @exclude
   */
  protected ExecutionManager executionManager = null;
  /**
   * The object responsible for this node's I/O.
   * @exclude
   */
  protected NodeIO nodeIO = null;
  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   */
  private boolean jmxEnabled = JPPFConfiguration.get(JPPFProperties.MANAGEMENT_ENABLED);
  /**
   * Determines whether this node can execute .Net tasks.
   */
  private final boolean dotnetCapable = JPPFConfiguration.get(JPPFProperties.DOTNET_BRIDGE_INITIALIZED);
  /**
   * The default node's management MBean.
   */
  private JPPFNodeAdminMBean nodeAdmin = null;
  /**
   * The jmx server that handles administration and monitoring functions for this node.
   */
  private static JMXServer jmxServer = null;
  /**
   * Manager for the MBean defined through the service provider interface.
   */
  private JPPFMBeanProviderManager<?> providerManager = null;
  /**
   * Handles the firing of node life cycle events and the listeners that subscribe to these events.
   * @exclude
   */
  protected LifeCycleEventHandler lifeCycleEventHandler = null;
  /**
   * The connection checker for this node.
   * @exclude
   */
  protected NodeConnectionChecker connectionChecker = null;
  /**
   * Determines whether the node connection checker should be used.
   * @exclude
   */
  protected final boolean checkConnection = JPPFConfiguration.get(JPPFProperties.NODE_CHECK_CONNECTION);
  /**
   * The bundle currently processed in offline mode.
   * @exclude
   */
  protected Pair<TaskBundle, List<Task<?>>> currentBundle  = null;

  /**
   * Default constructor.
   */
  public JPPFNode() {
    uuid = NodeRunner.getUuid();
    executionManager = new NodeExecutionManager(this);
    lifeCycleEventHandler = new LifeCycleEventHandler(this);
    updateSystemInformation();
  }

  /**
   * Main processing loop of this node.
   * @exclude
   */
  @Override
  public void run() {
    setStopped(false);
    boolean initialized = false;
    if (debugEnabled) log.debug("Start of node main loop, nodeUuid=" + uuid);
    while (!isStopped()) {
      try {
        if (!isLocal() && NodeRunner.getShuttingDown().get()) break;
        init();
        if (!initialized) {
          System.out.println("Node successfully initialized");
          initialized = true;
        }
        perform();
      } catch(SecurityException|SSLConfigurationException e) {
        if (checkConnection) connectionChecker.stop();
        if (!isStopped()) reset(true);
        throw new JPPFError(e);
      } catch(IOException e) {
        log.error(e.getMessage(), e);
        if (checkConnection) connectionChecker.stop();
        if (!isStopped()) {
          reset(true);
          throw new JPPFNodeReconnectionNotification("I/O exception occurred during node processing", e, ConnectionReason.JOB_CHANNEL_PROCESSING_ERROR);
        }
      } catch(Exception e) {
        log.error(e.getMessage(), e);
        if (checkConnection) connectionChecker.stop();
        if (!isStopped()) reset(true);
      }
    }
    if (debugEnabled) log.debug("End of node main loop");
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   * @exclude
   */
  public void perform() throws Exception {
    if (debugEnabled) log.debug("Start of node secondary loop");
    boolean shouldInitDataChannel = false;
    while (!checkStopped()) {
      clearResourceCachesIfRequested();
      if (isShutdownRequested()) shutdown(isRestart());
      else {
        try {
          while (isSuspended()) suspendedLock.goToSleep(1000L);
          if (shouldInitDataChannel) {
            shouldInitDataChannel = false;
            initDataChannel();
          }
          processNextJob();
        } catch (IOException|JPPFSuspendedNodeException e) {
          if (!isSuspended()) throw e;
          shouldInitDataChannel = true;
        } finally {
          setExecuting(false);
        }
      }
    }
    if (debugEnabled) log.debug("End of node secondary loop");
  }

  /**
   * Read a job to execute or a handshake job.
   * @throws Exception if any error occurs.
   */
  private void processNextJob() throws Exception {
    Pair<TaskBundle, List<Task<?>>> pair = nodeIO.readTask();
    TaskBundle bundle = pair.first();
    List<Task<?>> taskList = pair.second();
    if (debugEnabled) log.debug(!bundle.isHandshake() ? "received a bundle with " + taskList.size()  + " tasks" : "received a handshake bundle");
    if (!bundle.isHandshake()) {
      try {
        if (checkConnection) connectionChecker.resume();
        executionManager.execute(bundle, taskList);
      } finally {
        if (checkConnection) {
          connectionChecker.suspend();
          if (connectionChecker.getException() != null) throw connectionChecker.getException();
        }
      }
      if (isOffline()) {
        currentBundle = pair;
        initDataChannel();
        processNextJob(); // new handshake
      } else processResults(bundle, taskList);
    } else {
      if (currentBundle != null) {
        bundle = currentBundle.first();
        taskList = currentBundle.second();
      }
      checkInitialBundle(bundle);
      currentBundle = null;
      processResults(bundle, taskList);
      if (isMasterNode()) SlaveNodeManager.handleStartup();
    }
  }

  /**
   * Checks whether the received bundle is the initial one sent by the driver,
   * and prepare a specific response if it is.
   * @param bundle the bundle to check.
   * @throws Exception if any error occurs.
   */
  private void checkInitialBundle(final TaskBundle bundle) throws Exception {
    checkStopped();
    if (debugEnabled) log.debug("setting initial bundle, offline=" + isOffline() + (currentBundle == null ? ", bundle=" + bundle : ", currentBundle=" + currentBundle.first()));
    bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
    if (isOffline()) {
      bundle.setParameter(BundleParameter.NODE_OFFLINE, true);
      if (isSuspended()) bundle.setParameter(BundleParameter.CLOSE_COMMAND, true);
      if (currentBundle != null) {
        bundle.setParameter(BundleParameter.NODE_OFFLINE_OPEN_REQUEST, true);
        bundle.setParameter(BundleParameter.NODE_BUNDLE_ID, currentBundle.first().getParameter(BundleParameter.NODE_BUNDLE_ID));
        bundle.setParameter(BundleParameter.JOB_UUID, currentBundle.first().getUuid());
      }
    }
    if (isJmxEnabled()) setupBundleParameters(bundle);
  }

  /**
   * Send the results back to the server and perform final checks for the current execution.
   * @param bundle the bundle that contains the tasks and header information.
   * @param taskList the tasks results.
   * @throws Exception if any error occurs.
   */
  private void processResults(final TaskBundle bundle, final List<Task<?>> taskList) throws Exception {
    checkStopped();
    currentBundle = null;
    if (debugEnabled) log.debug("processing " + (taskList == null ? 0 : taskList.size()) + " task results for job '" + bundle.getName() + '\'');
    if (executionManager.checkConfigChanged() || bundle.isHandshake() || isOffline()) {
      if (debugEnabled) log.debug("detected configuration change or initial bundle request, sending new system information to the server");
      TypedProperties jppf = systemInformation.getJppf();
      jppf.clear();
      jppf.putAll(JPPFConfiguration.getProperties());
      bundle.setParameter(BundleParameter.SYSTEM_INFO_PARAM, systemInformation);
    }
    nodeIO.writeResults(bundle, taskList);
    if ((taskList != null) && (!taskList.isEmpty())) {
      if (!isJmxEnabled()) setTaskCount(getTaskCount() + taskList.size());
    }
    if (!bundle.isHandshake()) lifeCycleEventHandler.fireBeforeNextJob();
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  private synchronized void init() throws Exception {
    checkStopped();
    if (debugEnabled) log.debug("start node initialization");
    initHelper();
    try {
      if (ManagementUtils.isManagementAvailable() && !ManagementUtils.isMBeanRegistered(JPPFNodeAdminMBean.MBEAN_NAME)) {
        ClassLoader cl = getClass().getClassLoader();
        if (providerManager == null) providerManager = new JPPFMBeanProviderManager<>(JPPFNodeMBeanProvider.class, cl, ManagementUtils.getPlatformServer(), this);
        if (JPPFConfiguration.get(JPPFProperties.DEBUG_ENABLED)) {
          try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            NodeDebug nodeDebug = new NodeDebug();
            StandardMBean mbean = new StandardMBean(nodeDebug, NodeDebugMBean.class);
            server.registerMBean(mbean, new ObjectName(NodeDebugMBean.MBEAN_NAME));
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    } catch (Exception e) {
      log.error("Error registering the MBeans", e);
    }
    if (isJmxEnabled()) {
      JMXServer jmxServer = null;
      try {
        jmxServer = getJmxServer();
      } catch(Exception e) {
        jmxEnabled = false;
        System.out.println("JMX initialization failure - management is disabled for this node");
        System.out.println("see the log file for details");
        try {
          if (jmxServer != null) jmxServer.stop();
        } catch(Exception e2) {
          log.error("Error stopping the JMX server", e2);
        }
        jmxServer = null;
        log.error("Error creating the JMX server", e);
      }
    }
    HookFactory.registerSPIMultipleHook(JPPFNodeStartupSPI.class, null, null).invoke("run");
    initDataChannel();
    if (checkConnection) {
      connectionChecker = createConnectionChecker();
      connectionChecker.start();
    }
    lifeCycleEventHandler.loadListeners();
    lifeCycleEventHandler.fireNodeStarting();
    if (debugEnabled) log.debug("end node initialization");
  }

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   * @exclude
   */
  public abstract void initDataChannel() throws Exception;

  /**
   * Initialize this node's data channel.
   * @throws Exception if an error is raised during initialization.
   * @exclude
   */
  public abstract void closeDataChannel() throws Exception;

  /**
   * Get the main classloader for the node. This method performs a lazy initialization of the classloader.
   * @throws Exception if an error occurs while instantiating the class loader.
   * @exclude
   */
  public void initHelper() throws Exception {
    if (debugEnabled) log.debug("Initializing serializer");
    Class<?> c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
    if (debugEnabled) log.debug("Loaded serializer class " + c);
    serializer = (ObjectSerializer) c.newInstance();
    c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
    if (debugEnabled) log.debug("Loaded helper class " + c);
    helper = (SerializationHelper) c.newInstance();
    if (debugEnabled) log.debug("Serializer initialized");
  }

  /**
   * Get the administration and monitoring MBean for this node.
   * @return a <code>JPPFNodeAdminMBean</code> instance.
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

  @Override
  public ExecutionManager getExecutionManager() {
    return executionManager;
  }

  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   * @return true if JMX is enabled, false otherwise.
   */
  boolean isJmxEnabled() {
    return jmxEnabled && !isOffline();
  }

  /**
   * Stop this node and release the resources it is using.
   * @exclude
   */
  @Override
  public synchronized void stopNode() {
    if (debugEnabled) log.debug("stopping node");
    setStopped(true);
    executionManager.shutdown();
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
      NodeRunner.shutdown(this, restart);
    }
  }

  /**
   * Reset this node for shutdown/restart/reconnection.
   * @param stopJmx <code>true</code> if the JMX server is to be stopped, <code>false</code> otherwise.
   */
  private void reset(final boolean stopJmx) {
    if (debugEnabled) log.debug("resetting with stopJmx=" + stopJmx);
    System.out.println("resetting with stopJmx=" + stopJmx);
    lifeCycleEventHandler.fireNodeEnding();
    lifeCycleEventHandler.removeAllListeners();
    setNodeAdmin(null);
    if (stopJmx) {
      try {
        if (providerManager != null) providerManager.unregisterProviderMBeans();
        if (jmxServer != null) jmxServer.stop();
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    classLoaderManager.closeClassLoader();
    try {
      synchronized(this) {
        closeDataChannel();
      }
      classLoaderManager.clearContainers();
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Get the jmx server that handles administration and monitoring functions for this node.
   * @return a <code>JMXServerImpl</code> instance.
   * @throws Exception if any error occurs.
   */
  @Override
  public JMXServer getJmxServer() throws Exception {
    synchronized(this) {
      if ((jmxServer == null) || jmxServer.isStopped()) {
        boolean ssl = JPPFConfiguration.getProperties().get(JPPFProperties.SSL_ENABLED);
        jmxServer = JMXServerFactory.createServer(NodeRunner.getUuid(), ssl, ssl ? JPPFProperties.MANAGEMENT_SSL_PORT_NODE : JPPFProperties.MANAGEMENT_PORT_NODE);
        jmxServer.start(getClass().getClassLoader());
        System.out.println("JPPF Node management initialized on port " + jmxServer.getManagementPort());
      }
    }
    return jmxServer;
  }

  /**
   * Stop the jmx server.
   * @throws Exception if any error occurs.
   * @since 5.0
   * @exclude
   */
  public void stopJmxServer() throws Exception {
    if (jmxServer != null) jmxServer.stop();
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler() {
    return lifeCycleEventHandler;
  }

  /**
   * Create the connection checker for this node.
   * @return an implementation of {@link NodeConnectionChecker}.
   * @exclude
   */
  protected abstract NodeConnectionChecker createConnectionChecker();

  /**
   * Trigger the configuration changed flag.
   * @exclude
   */
  public void triggerConfigChanged() {
    updateSystemInformation();
    executionManager.triggerConfigChanged();
  }

  @Override
  public AbstractJPPFClassLoader resetTaskClassLoader(final Object...params) {
    TaskBundle bundle = executionManager.getBundle();
    if (bundle == null) return null;
    try {
      List<String> uuidPath = bundle.getUuidPath().getList();
      boolean remoteClassLoadingDisabled = classLoaderManager.getContainer(uuidPath, params).getClassLoader().isRemoteClassLoadingDisabled();
      AbstractJPPFClassLoader newCL = classLoaderManager.resetClassLoader(uuidPath, params);
      newCL.setRemoteClassLoadingDisabled(remoteClassLoadingDisabled);
      return newCL;
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return null;
  }

  @Override
  public ClassLoader getClassLoader(final List<String> uuidPath) throws Exception {
    return getContainer(uuidPath).getClassLoader();
  }

  @Override
  public boolean isOffline() {
    return isAndroid() || getClassLoader().isOffline();
  }

  @Override
  public boolean isMasterNode() {
    return !isOffline() && (systemInformation != null) && systemInformation.getJppf().get(JPPFProperties.PROVISIONING_MASTER);
  }

  @Override
  public boolean isSlaveNode() {
    return (systemInformation != null) && systemInformation.getJppf().get(JPPFProperties.PROVISIONING_SLAVE);
  }

  /**
   * Check whether this node is stopped or shutting down.
   * If not, an unchecked {@code IllegalStateException} is thrown.
   * @return {@code true} if the node is stopped or shutting down.
   */
  private boolean checkStopped() {
    if (isStopped()) throw new IllegalStateException("this node is shutting down");
    return false;
  }

  @Override
  public boolean isDotnetCapable() {
    return dotnetCapable;
  }
}
