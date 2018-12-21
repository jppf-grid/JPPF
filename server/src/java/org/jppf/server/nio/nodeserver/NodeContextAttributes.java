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

package org.jppf.server.nio.nodeserver;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.execute.*;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.persistence.LoadBalancerPersistenceManager;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.utils.Pair;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeContextAttributes {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeContextAttributes.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private  Bundler<?> bundler;
  /**
   * Represents the node system information.
   */
  private JPPFSystemInformation systemInfo;
  /**
   * Represents the management information.
   */
  private JPPFManagementInfo managementInfo;
  /**
   * List of execution status listeners for this channel.
   */
  private final List<ExecutorChannelStatusListener> executorChannelListeners = new CopyOnWriteArrayList<>();
  /**
   * The {@code Runnable} called when node context is closed.
   */
  private Runnable onClose;
  /**
   * Determines whether the node is active or inactive.
   */
  private final AtomicBoolean active = new AtomicBoolean(true);
  /**
   * Provides access to the management functions of the node.
   */
  private JMXNodeConnectionWrapper jmxConnection;
  /**
   * Provides access to the management functions of the peer driver.
   */
  private JMXDriverConnectionWrapper peerJmxConnection;
  /**
   * Execution status for the node.
   */
  private ExecutorStatus executionStatus = ExecutorStatus.DISABLED;
  /**
   * Whether to remove any job reservation for this node.
   */
  private NodeReservationHandler.Transition reservationTansition = NodeReservationHandler.Transition.REMOVE;
  /**
   * The latest computed score for a given desired configuration.
   */
  private int reservationScore;
  /**
   * Unique node identfier reusable over node restarts.
   */
  private Pair<String, String> nodeIdentifier;
  /**
   * The algorithm name for the bundler.
   */
  private String bundlerAlgorithm;
  /**
   * 
   */
  private final AbstractBaseNodeContext<?> context;
  /**
   * The load-balancer persistence manager.
   */
  private final LoadBalancerPersistenceManager bundlerHandler;
  /**
   * 
   */
  private final NodeConnectionCompletionListener listener;
  /**
   * Determines whether the node works in offline mode.
   */
  private boolean offline = false;
  /**
   * Reference to the JPPF driver.
   */
  private JPPFDriver driver;
  /**
   * Determines whether the node is idle or not.
   */
  private final AtomicBoolean idle = new AtomicBoolean(false);

  /**
   * 
   * @param context the context.
   * @param bundlerHandler the load-balancer persistence manager.
   * @param listener the listener to conneciton completion event.
   */
  public NodeContextAttributes(final AbstractBaseNodeContext<?> context, final LoadBalancerPersistenceManager bundlerHandler, final NodeConnectionCompletionListener listener) {
    this.context = context;
    this.bundlerHandler = bundlerHandler;
    this.listener = listener;
  }

  /**
   * @return the bundler used to schedule tasks for the corresponding node.
   */
  public Bundler<?> getBundler() {
    return bundler;
  }

  /**
   *
   * @param bundler the bundler used to schedule tasks for the corresponding node.
   */
  public void setBundler(final Bundler<?> bundler) {
    this.bundler = bundler;
  }

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param factory the load-balancer factory.
   * @param jppfContext execution context.
   * @return the (possibly new) bundle for this executor channel.
   */
  public Bundler<?> checkBundler(final JPPFBundlerFactory factory, final JPPFContext jppfContext) {
    if (factory == null) throw new IllegalArgumentException("Bundler factory is null");
    Bundler<?> bundler = getBundler();
    if (bundler == null || bundler.getTimestamp() < factory.getLastUpdateTime()) {
      if (bundler != null) {
        bundler.dispose();
        if (bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(null);
      }
      final Pair<String, Bundler<?>> pair = bundlerHandler.loadBundler(nodeIdentifier);
      setBundler(pair.second());
      bundlerAlgorithm = pair.first();
      bundler = pair.second();
      if (bundler instanceof ContextAwareness) ((ContextAwareness) bundler).setJPPFContext(jppfContext);
      bundler.setup();
      if (bundler instanceof ChannelAwareness) ((ChannelAwareness) bundler).setChannelConfiguration(systemInfo);
    }
    return bundler;
  }

  /**
   * @return the node system information.
   */
  public JPPFSystemInformation getSystemInfo() {
    return systemInfo;
  }

  /**
   * Set the node system information.
   * @param systemInfo a {@link JPPFSystemInformation} instance.
   * @param update a flag indicates whether update system information in management information.
   */
  public void setNodeInfo(final JPPFSystemInformation systemInfo, final boolean update) {
    if (update && debugEnabled) log.debug("updating node information for {}", systemInfo);
    this.systemInfo = systemInfo;
    systemInfo.getJppf().setProperty("jppf.channel.local", String.valueOf(context.isLocal()));
    if (update && managementInfo != null) managementInfo.setSystemInfo(systemInfo);
  }

  /**
   * @return the management information.
   */
  public JPPFManagementInfo getManagementInfo() {
    return managementInfo;
  }

  /**
   *
   * @param managementInfo the management information.
   */
  public void setManagementInfo(final JPPFManagementInfo managementInfo) {
    if (debugEnabled) log.debug("context " + this + " setting management info [" + managementInfo + "]");
    this.managementInfo = managementInfo;
    if ((managementInfo.getIpAddress() != null) && (managementInfo.getPort() >= 0)) initializeJmxConnection();
    this.managementInfo = managementInfo;
  }

  /**
   * @return the {@code Runnable} called when node context is closed.
   */
  public Runnable getOnClose() {
    return onClose;
  }

  /**
   *
   * @param onClose the {@code Runnable} called when node context is closed.
   */
  public void setOnClose(final Runnable onClose) {
    this.onClose = onClose;
  }

  /**
   * @return the connection to the node's JMX server.
   */
  public JMXNodeConnectionWrapper getJmxConnection() {
    return jmxConnection;
  }

  /**
   * 
   * @param jmxConnection the connection to the node's JMX server.
   */
  public void setJmxConnection(final JMXNodeConnectionWrapper jmxConnection) {
    this.jmxConnection = jmxConnection;
  }

  /**
   * @return the connection to the perr driver's JMX server.
   */
  public JMXDriverConnectionWrapper getPeerJmxConnection() {
    return peerJmxConnection;
  }

  /**
   *
   * @param peerJmxConnection the connection to the peer driver's JMX server.
   */
  public void setPeerJmxConnection(final JMXDriverConnectionWrapper peerJmxConnection) {
    this.peerJmxConnection = peerJmxConnection;
  }

  /**
   * Initialize the jmx connection using the specified jmx id.
   */
  public void initializeJmxConnection() {
    if (!context.isClosed()) {
      JMXConnectionWrapper jmx = null;
      if (context.isLocal()) jmx = jmxConnection = new JMXNodeConnectionWrapper();
      else {
        final JPPFManagementInfo info = getManagementInfo();
        if (debugEnabled) log.debug("establishing JMX connection for {}", info);
        if (!context.isPeer()) jmx = jmxConnection = new JMXNodeConnectionWrapper(info.getIpAddress(), info.getPort(), info.isSecure());
        else jmx = peerJmxConnection = new JMXDriverConnectionWrapper(info.getIpAddress(), info.getPort(), info.isSecure());
      }
      jmx.addJMXWrapperListener(new NodeJMXWrapperListener(context, listener));
      jmx.connect();
    }
  }

  /**
   * @return the execution status for the node.
   */
  public ExecutorStatus getExecutionStatus() {
    return executionStatus;
  }

  /**
   *
   * @param newStatus the execution status for the node.
   */
  public void setExecutionStatus(final ExecutorStatus newStatus) {
    final ExecutorStatus oldStatus = this.executionStatus;
    this.executionStatus = newStatus;
    fireExecutionStatusChanged(oldStatus, newStatus);
  }

  /**
   * @return whether to remove any job reservation for this node.
   */
  public NodeReservationHandler.Transition getReservationTansition() {
    return reservationTansition;
  }

  /**
   *
   * @param reservationTansition whether to remove any job reservation for this node.
   */
  public void setReservationTansition(final NodeReservationHandler.Transition reservationTansition) {
    this.reservationTansition = reservationTansition;
  }

  /**
   * @return the latest computed score for a given desired configuration.
   */
  public int getReservationScore() {
    return reservationScore;
  }

  /**
   *
   * @param reservationScore the latest computed score for a given desired configuration.
   */
  public void setReservationScore(final int reservationScore) {
    this.reservationScore = reservationScore;
  }

  /**
   * @return the unique node identfier reusable over node restarts.
   */
  public Pair<String, String> getNodeIdentifier() {
    return nodeIdentifier;
  }

  /**
   *
   * @param nodeIdentifier the unique node identfier reusable over node restarts.
   */
  public void setNodeIdentifier(final Pair<String, String> nodeIdentifier) {
    this.nodeIdentifier = nodeIdentifier;
  }

  /**
   * @return the algorithm name for the bundler.
   */
  public String getBundlerAlgorithm() {
    return bundlerAlgorithm;
  }

  /**
   *
   * @param bundlerAlgorithm the algorithm name for the bundler.
   */
  public void setBundlerAlgorithm(final String bundlerAlgorithm) {
    this.bundlerAlgorithm = bundlerAlgorithm;
  }

  /**
   * @return whether the node is active.
   */
  public boolean istActive() {
    return active.get();
  }

  /**
   * @param active whether the node is active.
   */
  public void setActive(final boolean active) {
    this.active.set(active);
    if (managementInfo != null) managementInfo.setIsActive(active);
  }

  /**
   * Add a execution status listener to this channel's list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    executorChannelListeners.add(listener);
  }

  /**
   * Remove a execution status listener from this channel's list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    executorChannelListeners.remove(listener);
  }

  /**
   * Notify all listeners that the execution status of this channel has changed.
   * @param oldValue the channel execution status before the change.
   * @param newValue the channel execution status after the change.
   */
  public void fireExecutionStatusChanged(final ExecutorStatus oldValue, final ExecutorStatus newValue) {
    if (oldValue == newValue) return;
    final ExecutorChannelStatusEvent event = new ExecutorChannelStatusEvent(context, oldValue, newValue);
    for (final ExecutorChannelStatusListener listener : executorChannelListeners) listener.executionStatusChanged(event);
  }

  /**
   * @return whether the node works in offline mode.
   */
  public boolean isOffline() {
    return offline;
  }

  /**
   * Specify whether the node works in offline mode.
   * @param offline <code>true</code> if the node is in offline mode, <code>false</code> otherwise.
   */
  public void setOffline(final boolean offline) {
    this.offline = offline;
  }

  /**
   * @return a reference to the JPPF driver.
   */
  public JPPFDriver getDriver() {
    return driver;
  }

  /**
   * 
   * @param driver a reference to the JPPF driver.
   */
  public void setDriver(final JPPFDriver driver) {
    this.driver = driver;
  }

  /**
   * @return whether the node is idle or not.
   */
  public AtomicBoolean getIdle() {
    return idle;
  }
}
