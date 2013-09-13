/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import org.jppf.management.*;
import org.jppf.management.spi.*;
import org.jppf.node.NodeRunner;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.*;
import org.jppf.startup.JPPFNodeStartupSPI;
import org.jppf.utils.*;
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
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The task execution manager for this node.
   */
  protected NodeExecutionManagerImpl executionManager = null;
  /**
   * The object responsible for this node's I/O.
   */
  protected NodeIO nodeIO = null;
  /**
   * Determines whether JMX management and monitoring is enabled for this node.
   */
  private boolean jmxEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.management.enabled", true);
  /**
   * Action executed when the node exits the main loop, in its {@link #run() run()} method.
   */
  private Runnable exitAction = null;
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
  private JPPFMBeanProviderManager providerManager = null;
  /**
   * Handles the firing of node life cycle events and the listeners that subscribe to these events.
   */
  protected LifeCycleEventHandler lifeCycleEventHandler = null;
  /**
   * The connection checker for this node.
   */
  protected NodeConnectionChecker connectionChecker = null;
  /**
   * Determines whether the node connection checker should be used.
   */
  protected final boolean checkConnection = JPPFConfiguration.getProperties().getBoolean("jppf.node.check.connection", false);
  /**
   * 
   */
  protected Pair<JPPFTaskBundle, List<Task>> currentBundle  = null;

  /**
   * Default constructor.
   */
  public JPPFNode() {
    uuid = NodeRunner.getUuid();
    executionManager = new NodeExecutionManagerImpl(this);
    lifeCycleEventHandler = new LifeCycleEventHandler(this);
    updateSystemInformation();
  }

  /**
   * Main processing loop of this node.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    setStopped(false);
    boolean initialized = false;
    if (debugEnabled) log.debug("Start of node main loop, nodeUuid=" + uuid);
    while (!isStopped()) {
      try {
        if (NodeRunner.isShuttingDown()) break;
        init();
        if (!initialized) {
          System.out.println("Node successfully initialized");
          initialized = true;
        }
        perform();
      } catch(SecurityException e) {
        if (checkConnection) connectionChecker.stop();
        throw new JPPFError(e);
      } catch(IOException e) {
        log.error(e.getMessage(), e);
        if (checkConnection) connectionChecker.stop();
        reset(true);
        throw new JPPFNodeReconnectionNotification(e);
      } catch(Exception e) {
        log.error(e.getMessage(), e);
        if (checkConnection) connectionChecker.stop();
        reset(true);
      }
    }
    if (debugEnabled) log.debug("End of node main loop");
    if (exitAction != null) {
      Runnable r = exitAction;
      setExitAction(null);
      r.run();
    }
  }

  /**
   * Perform the main execution loop for this node. At each iteration, this method listens for a task to execute,
   * receives it, executes it and sends the results back.
   * @throws Exception if an error was raised from the underlying socket connection or the class loader.
   */
  public void perform() throws Exception {
    if (debugEnabled) log.debug("Start of node secondary loop");
    while (!isStopped()) {
      clearResourceCachesIfRequested();
      processNextJob();
    }
    if (debugEnabled) log.debug("End of node secondary loop");
  }

  /**
   * Read a job to execute or a hanshake job.
   * @throws Exception if any error occurs.
   */
  private void processNextJob() throws Exception {
    Pair<JPPFTaskBundle, List<Task>> pair = nodeIO.readTask();
    JPPFTaskBundle bundle = pair.first();
    //if (bundle.isHandshake()) checkInitialBundle(bundle);
    List<Task> taskList = pair.second();
    //boolean notEmpty = (taskList != null) && (!taskList.isEmpty());
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
    }
  }

  /**
   * Checks whether the received bundle is the initial one sent by the driver,
   * and prepare a specific response if it is.
   * @param bundle the bundle to check.
   * @throws Exception if any error occurs.
   */
  private void checkInitialBundle(final JPPFTaskBundle bundle) throws Exception {
    if (debugEnabled) log.debug("setting initial bundle, offline=" + isOffline() + (currentBundle == null ? ", bundle=" + bundle : ", currentBundle=" + currentBundle.first()));
    bundle.setParameter(BundleParameter.NODE_UUID_PARAM, uuid);
    if (isOffline()) {
      bundle.setParameter(BundleParameter.NODE_OFFLINE, true);
      if (currentBundle != null) {
        bundle.setParameter(BundleParameter.NODE_OFFLINE_OPEN_REQUEST, true);
        bundle.setParameter(BundleParameter.NODE_BUNDLE_ID, currentBundle.first().getParameter(BundleParameter.NODE_BUNDLE_ID));
        bundle.setParameter(BundleParameter.JOB_UUID, currentBundle.first().getUuid());
      }
    }
    if (isJmxEnabled()) setupManagementParameters(bundle);
  }

  /**
   * Send the results back to the server and perform final checks for the current execution.
   * @param bundle the bundle that contains the tasks and header information.
   * @param taskList the tasks results.
   * @throws Exception if any error occurs.
   */
  private void processResults(final JPPFTaskBundle bundle, final List<Task> taskList) throws Exception {
    currentBundle = null;
    if (debugEnabled) log.debug("processing " + (taskList == null ? 0 : taskList.size()) + " task results for job '" + bundle.getName() + '\'');
    //if (executionManager.checkConfigChanged() || (bundle.isHandshake() && (currentBundle == null))) {
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
  }

  /**
   * Initialize this node's resources.
   * @throws Exception if an error is raised during initialization.
   */
  private synchronized void init() throws Exception {
    if (debugEnabled) log.debug("start node initialization");
    initHelper();
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      if (!server.isRegistered(new ObjectName(JPPFNodeAdminMBean.MBEAN_NAME))) registerProviderMBeans();
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
   */
  public void initHelper() throws Exception {
    if (debugEnabled) log.debug("Initializing serializer");
    Class<?> c = getClassLoader().loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
    if (debugEnabled) log.debug("Loaded serializer class " + c);
    Object o = c.newInstance();
    serializer = (ObjectSerializer) o;
    c = getClassLoader().loadJPPFClass("org.jppf.utils.SerializationHelperImpl");
    if (debugEnabled) log.debug("Loaded helper class " + c);
    o = c.newInstance();
    helper = (SerializationHelper) o;
    if (debugEnabled) log.debug("Serializer initialized");
  }

  /**
   * Get the administration and monitoring MBean for this node.
   * @return a <code>JPPFNodeAdminMBean</code> instance.
   */
  public synchronized JPPFNodeAdminMBean getNodeAdmin() {
    return nodeAdmin;
  }

  /**
   * Set the administration and monitoring MBean for this node.
   * @param nodeAdmin a <code>JPPFNodeAdminMBean</code>m instance.
   */
  public synchronized void setNodeAdmin(final JPPFNodeAdminMBean nodeAdmin) {
    this.nodeAdmin = nodeAdmin;
  }

  /**
   * Get the task execution manager for this node.
   * @return a <code>NodeExecutionManager</code> instance.
   */
  public NodeExecutionManagerImpl getExecutionManager() {
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
   */
  @Override
  public synchronized void stopNode() {
    if (debugEnabled) log.debug("stopping node");
    setStopped(true);
    executionManager.shutdown();
    try {
      this.closeDataChannel();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    reset(true);
  }

  /**
   * Shutdown and eventually restart the node.
   * @param restart determines whether this node should be restarted by the node launcher.
   */
  public void shutdown(final boolean restart) {
    NodeRunner.setShuttingDown(true);
    lifeCycleEventHandler.fireNodeEnding();
    NodeRunner.shutdown(this, restart);
  }

  /**
   * Reset this node for shutdown/restart/reconnection.
   * @param stopJmx <code>true</code> if the JMX server is to be stopped, <code>false</code> otherwise.
   */
  private void reset(final boolean stopJmx) {
    if (debugEnabled) log.debug("resetting with stopJmx=" + stopJmx);
    lifeCycleEventHandler.fireNodeEnding();
    lifeCycleEventHandler.removeAllListeners();
    setNodeAdmin(null);
    classLoaderManager.closeClassLoader();
    try {
      synchronized(this) {
        closeDataChannel();
      }
      classLoaderManager.clearContainers();
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    if (stopJmx) {
      try {
        if (providerManager != null) providerManager.unregisterProviderMBeans();
        if (jmxServer != null) jmxServer.stop();
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Set the action executed when the node exits the main loop.
   * @param exitAction the action to execute.
   */
  public synchronized void setExitAction(final Runnable exitAction) {
    this.exitAction = exitAction;
  }

  /**
   * Register all MBeans defined through the service provider interface.
   * @throws Exception if the registration failed.
   */
  @SuppressWarnings("unchecked")
  private void registerProviderMBeans() throws Exception {
    ClassLoader cl = getClass().getClassLoader();
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    if (providerManager == null) providerManager = new JPPFMBeanProviderManager<>(JPPFNodeMBeanProvider.class, cl, server, this);
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
        boolean ssl = JPPFConfiguration.getProperties().getBoolean("jppf.ssl.enabled", false);
        jmxServer = JMXServerFactory.createServer(NodeRunner.getUuid(), ssl);
        jmxServer.start(getClass().getClassLoader());
        System.out.println("JPPF Node management initialized");
      }
    }
    return jmxServer;
  }

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
   */
  public void triggerConfigChanged() {
    updateSystemInformation();
    executionManager.triggerConfigChanged();
  }

  @Override
  public AbstractJPPFClassLoader resetTaskClassLoader() {
    JPPFTaskBundle bundle = executionManager.getBundle();
    if (bundle == null) return null;
    try {
      return classLoaderManager.resetClassLoader(bundle.getUuidPath().getList());
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
    return getClassLoader().isOffline();
  }
}
