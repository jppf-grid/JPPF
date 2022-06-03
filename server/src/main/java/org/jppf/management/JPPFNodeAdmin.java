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

package org.jppf.management;

import java.util.Map;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.classloader.*;
import org.jppf.execute.ExecutionInfo;
import org.jppf.node.connection.ConnectionReason;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadUtils;
import org.jppf.utils.configuration.ConfigurationOverridesHandler;
import org.slf4j.*;

/**
 * Management bean for a JPPF node.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeAdmin implements JPPFNodeAdminMBean {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFNodeAdmin.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The latest event that occurred within a task.
   */
  private final JPPFNodeState nodeState = new JPPFNodeState();
  /**
   * The node whose state is monitored.
   */
  transient JPPFNode node;

  /**
   * Initialize this node management bean with the specified node.
   * @param node the node whose state is monitored.
   */
  public JPPFNodeAdmin(final JPPFNode node) {
    if (debugEnabled) log.debug("instantiating JPPFNodeAdmin");
    this.node = node;
    node.setNodeAdmin(this);
    nodeState.setThreadPriority(node.getExecutionManager().getThreadsPriority());
    nodeState.setThreadPoolSize(node.getExecutionManager().getThreadPoolSize());
    node.getLifeCycleEventHandler().addProvider(new NodeStatusNotifier(this));
  }

  /**
   * Get the latest state information from the node.
   * @return a <code>JPPFNodeState</code> information.
   * @throws Exception if any error occurs.
   */
  @Override
  public JPPFNodeState state() throws Exception {
    final JPPFNodeState ns = nodeState.copy();
    if (log.isTraceEnabled()) log.trace("nn threads = " + ns.getThreadPoolSize());
    return ns;
  }

  /**
   * Set the size of the node's thread pool.
   * @param size the size as an int.
   * @throws Exception if any error occurs.
   */
  @Override
  public void updateThreadPoolSize(final Integer size) throws Exception {
    if (debugEnabled) log.debug("node request to change thread pool size to " + size);
    node.getExecutionManager().setThreadPoolSize(size);
    nodeState.setThreadPoolSize(size);
  }

  /**
   * Get detailed information about the node's JVM properties, environment variables
   * and runtime information such as memory usage and available processors.
   * @return a <code>JPPFSystemInformation</code> instance.
   * @throws Exception if any error occurs.
   */
  @Override
  public JPPFSystemInformation systemInformation() throws Exception {
    final JPPFSystemInformation info = node.getSystemInformation();
    final ExecutionInfo nei = node.getExecutionManager().getThreadManager().computeExecutionInfo();
    info.getRuntime().setProperty("cpuTime", nei == null ? "-1" : Long.toString(nei.cpuTime / 1000000L));
    return info;
  }

  /**
   * Restart the node.
   * @throws Exception if any error occurs.
   */
  @Override
  public void restart() throws Exception {
    restart(true);
  }

  /**
   * {@inheritDoc}
   * @since 5.0
   */
  @Override
  public void restart(final Boolean interruptIfRunning) throws Exception {
    if (debugEnabled) log.debug("node restart requested with interruptIfRunning={}", interruptIfRunning);
    final boolean interrupt = (interruptIfRunning == null) ? true : interruptIfRunning;
    shutdownOrRestart(interrupt, true);
  }

  /**
   * Shutdown the node.
   * @throws Exception if any error occurs.
   */
  @Override
  public void shutdown() throws Exception {
    shutdown(true);
  }

  /**
   * {@inheritDoc}
   * @since 5.0
   */
  @Override
  public void shutdown(final Boolean interruptIfRunning) throws Exception {
    if (debugEnabled) log.debug("node shutdown requested with interruptIfRunning={}", interruptIfRunning);
    final boolean interrupt = (interruptIfRunning == null) ? true : interruptIfRunning;
    shutdownOrRestart(interrupt, false);
  }

  /**
   * Perform or request a shtudown or restart.
   * @param interrupt whether the operation should be performed immediately, or the node
   * should wait until it is not executing any task.
   * @param restart {@code true} to perform/request a restart, {@code false} for a shutdown.
   * @throws Exception if any error occurs.
   * @since 5.0
   */
  private void shutdownOrRestart(final boolean interrupt, final boolean restart) throws Exception {
    //if (node.isLocal()) return;
    final String s = restart ? "restart" : "shutdown";
    final String msg = String.format("%s node %s requested", (interrupt ? "immediate" : "deferred"), s);
    System.out.println(msg);
    log.info(msg);
    if (interrupt || !node.isExecuting()) {
      if (node.getShuttingDown().compareAndSet(false, true)) {
        if (debugEnabled) log.debug("scheduling immediate {}", s);
        final Runnable r = () -> {
          try {
            node.shutdown(restart);
          } catch (final Exception|Error e) {
            log.error("error trying to {} the node: ", s, e);
            if (e instanceof Error) throw (Error) e;
            if (e instanceof RuntimeException) throw (RuntimeException) e;
          }
        };
        ThreadUtils.startThread(r, "Node " + s);
      }
    } else {
      final NodePendingAction action = restart ? NodePendingAction.RESTART : NodePendingAction.SHUTDOWN;
      node.setPendingAction(action);
      nodeState.setPendingAction(action);
      if (debugEnabled) log.debug("pending action after {} request: {}", s, action);
    }
  }

  @Override
  public void reconnect(final Boolean interrupt) throws Exception {
    log.info("{} reconnection requested", interrupt ? "immediate" : "deferred");
    if (interrupt || !node.isExecuting()) {
      node.setReconnectionNotification(new JPPFNodeReconnectionNotification("request to reconnect the node", null, ConnectionReason.MANAGEMENT_REQUEST));
      node.closeDataChannel();
    } else {
      final NodePendingAction action = NodePendingAction.RECONNECT;
      node.setPendingAction(action);
      nodeState.setPendingAction(action);
      if (debugEnabled) log.debug("pending action after reconnection request: {}", action);
    }
  }

  /**
   * Reset the node's executed tasks counter to zero.
   * @throws Exception if any error occurs.
   */
  @Override
  public void resetTaskCounter() throws Exception {
    if (debugEnabled) log.debug("node task counter reset requested");
    setTaskCounter(0);
  }

  /**
   * Set the node's executed tasks counter to the specified value.
   * @param n the new value of the task counter.
   * @throws Exception if any error occurs.
   */
  @Override
  public synchronized void setTaskCounter(final Integer n) throws Exception {
    if (debugEnabled) log.debug("node tasks counter reset to " + n + " requested");
    node.setExecutedTaskCount(n);
    nodeState.setNbTasksExecuted(n);
  }

  /**
   * Update the priority of all execution threads.
   * @param newPriority the new priority to set.
   * @throws Exception if any error occurs.
   */
  @Override
  public void updateThreadsPriority(final Integer newPriority) throws Exception {
    if (debugEnabled) log.debug("node threads priority reset to " + newPriority + " requested");
    node.getExecutionManager().updateThreadsPriority(newPriority);
    nodeState.setThreadPriority(newPriority);
  }

  /**
   * Update the configuration properties of the node.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should reconnect ot the driver after updating the properties.
   * @param interruptIfRunning when {@code true}, then restart the node even if it is executing tasks,
   * when {@code false}, then only shutdown the node when it is no longer executing.
   * This parameter only applies when the {@code restart} parameter is {@code true}.
   * @throws Exception if any error occurs.
   * @since 5.2
   */
  @Override
  public void updateConfiguration(final Map<Object, Object> configOverrides, final Boolean restart, final Boolean interruptIfRunning) throws Exception {
    if (configOverrides == null) return;
    if (debugEnabled) log.debug("node request to change configuration, restart={}, interruptIfRunning={}, configOverrides={}", restart, interruptIfRunning, configOverrides);
    // we don't allow the node uuid to be overriden
    if (configOverrides.containsKey("jppf.node.uuid")) configOverrides.remove("jppf.node.uuid");
    if (!configOverrides.isEmpty()) {
      final TypedProperties overrides = new TypedProperties(configOverrides);
      new ConfigurationOverridesHandler(node.getConfiguration()).save(overrides);
      node.getConfiguration().putAll(overrides);
      node.triggerConfigChanged();
    }
    //if (restart) triggerReconnect();
    if (restart) shutdownOrRestart(interruptIfRunning, true);
  }

  /**
   * Update the configuration properties of the node. This method is equivalent to calling {@link #updateConfiguration(Map, Boolean, Boolean) updateConfiguration(configOverrides, restart, true)}.
   * @param configOverrides the set of properties to update.
   * @param restart specifies whether the node should be restarted after updating the properties.
   * This parameter only applies when the {@code restart} parameter is {@code true}.
   * @throws Exception if any error occurs.
   */
  @Override
  public void updateConfiguration(final Map<Object, Object> configOverrides, final Boolean restart) throws Exception {
    updateConfiguration(configOverrides, restart, true);
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @throws Exception if any error occurs.
   */
  @Override
  public void cancelJob(final String jobId, final Boolean requeue) throws Exception {
    try {
      if (debugEnabled) log.debug("Request to cancel jobuUid = '{}', requeue = {}", jobId, requeue);
      if (jobId == null) return;
      node.getExecutionManager().cancelJob(jobId, true, requeue);
    } catch (final RuntimeException e) {
      log.debug("error cancelling job with uuid={}:", jobId, e);
      throw e;
    }
  }

  @Override
  public DelegationModel getDelegationModel() throws Exception {
    return AbstractJPPFClassLoader.getDelegationModel();
  }

  @Override
  public void setDelegationModel(final DelegationModel model) throws Exception {
    if (model != null) AbstractJPPFClassLoader.setDelegationModel(model);
  }

  /**
   * Get the current state of the node
   * @return a {@link JPPFNodeState} instance.
   */
  @Override
  public NodePendingAction pendingAction() {
    if (!node.isShutdownRequested()) return NodePendingAction.NONE;
    return node.isRestart() ? NodePendingAction.RESTART : NodePendingAction.SHUTDOWN;
  }

  @Override
  public boolean cancelPendingAction() {
    log.info("cancelPendingAction() requested");
    final boolean b = node.cancelPendingAction();
    if (b) nodeState.setPendingAction(NodePendingAction.NONE);
    return b;
  }

  /**
   * Get the current state of the node
   * @return a {@link JPPFNodeState} instance.
   */
  synchronized JPPFNodeState getNodeState() {
    return nodeState;
  }
}
