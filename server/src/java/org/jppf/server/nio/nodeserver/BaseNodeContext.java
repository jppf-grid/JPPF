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
import org.jppf.nio.StatelessNioContext;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.nodeserver.async.AsyncNodeNioServer;
import org.jppf.server.protocol.ServerTaskBundleNode;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * @author Laurent Cohen
 */
public abstract class BaseNodeContext extends StatelessNioContext implements  ExecutorChannel<ServerTaskBundleNode>  {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(BaseNodeContext.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private Bundler<?> bundler;
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
  protected final JPPFDriver driver;
  /**
   * The server that handles this context.
   */
  protected final AsyncNodeNioServer server;
  /**
   * Determines whether the node is idle or not.
   */
  private final AtomicBoolean idle = new AtomicBoolean(false);

  /**
   *
   * @param server .
   */
  public BaseNodeContext(final AsyncNodeNioServer server) {
    this.server = server;
    this.driver = server.getDriver();
    this.bundlerHandler = server.getBundlerHandler();
    this.listener = server;
  }

  @Override
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
  @Override
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

  @Override
  public JPPFSystemInformation getSystemInformation() {
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
    systemInfo.getJppf().setProperty("jppf.channel.local", String.valueOf(isLocal()));
    if (managementInfo != null) managementInfo.setSystemInfo(systemInfo);
  }

  @Override
  public JPPFManagementInfo getManagementInfo() {
    return managementInfo;
  }

  /**
   * Set the management information for the node.
   * @param managementInfo a {@link JPPFManagementInfo} instance.
   */
  public void setManagementInfo(final JPPFManagementInfo managementInfo) {
    if (debugEnabled) log.debug("context " + this + " setting management info [" + managementInfo + "]");
    this.managementInfo = managementInfo;
    if ((managementInfo.getIpAddress() != null) && (managementInfo.getPort() >= 0)) initializeJmxConnection();
  }

  /**
   * @return the {@code Runnable} called when node context is closed.
   */
  public Runnable getOnClose() {
    return onClose;
  }

  /**
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
   * Set the connection to the node's JMX server.
   * @param jmxConnection a {@link JMXNodeConnectionWrapper} instance.
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
   * Set the connection to the peer driver's JMX server.
   * @param peerJmxConnection a {@link JMXDriverConnectionWrapper} instance.
   */
  public void setPeerJmxConnection(final JMXDriverConnectionWrapper peerJmxConnection) {
    this.peerJmxConnection = peerJmxConnection;
  }

  /**
   * Initialize the jmx connection using the specified jmx id.
   */
  public void initializeJmxConnection() {
    if (!isClosed()) {
      JMXConnectionWrapper jmx = null;
      if (isLocal()) {
        jmxConnection = new JMXNodeConnectionWrapper();
        jmx = jmxConnection;
      } else {
        final JPPFManagementInfo info = getManagementInfo();
        if (debugEnabled) log.debug("establishing JMX connection for {}", info);
        if (!isPeer()) {
          jmxConnection = new JMXNodeConnectionWrapper(info.getIpAddress(), info.getPort(), info.isSecure());
          jmx = jmxConnection;
        } else {
          peerJmxConnection = new JMXDriverConnectionWrapper(info.getIpAddress(), info.getPort(), info.isSecure());
          jmx = peerJmxConnection;
        }
      }
      jmx.addJMXWrapperListener(new NodeJMXWrapperListener(this, listener));
      jmx.connect();
    } else {
      log.warn("node closed: {}", this);
    }
  }

  @Override
  public ExecutorStatus getExecutionStatus() {
    return executionStatus;
  }

  /**
   * Set the execution status for the node.
   * @param newStatus the execution status to set.
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
  @Override
  public boolean isActive() {
    return active.get();
  }

  /**
   * @param active whether the node is active.
   */
  public void setActive(final boolean active) {
    this.active.set(active);
    if (managementInfo != null) managementInfo.setIsActive(active);
  }

  @Override
  public void addExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    executorChannelListeners.add(listener);
  }

  @Override
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
    if (debugEnabled) log.debug("changing execution status from {} to {} on {}", oldValue, newValue, this);
    final ExecutorChannelStatusEvent event = new ExecutorChannelStatusEvent(this, oldValue, newValue);
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
   * @return whether the node is idle or not.
   */
  public AtomicBoolean getIdle() {
    return idle;
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @return a {@code true} when cancel was successful {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  public boolean cancelJob(final String jobId, final boolean requeue) throws Exception {
    if (debugEnabled) log.debug("cancelling job uuid={} from {}, jmxConnection={}, peerJmxConnection={}", jobId, this, getJmxConnection(), getPeerJmxConnection());
    if (isOffline()) return false;
    JPPFVoidCallable cancelCallback = null;
    if (!isPeer() && (getJmxConnection() != null) && getJmxConnection().isConnected()) cancelCallback = () -> getJmxConnection().cancelJob(jobId, requeue);
    else if (isPeer() && (getPeerJmxConnection() != null) && getPeerJmxConnection().isConnected()) cancelCallback = () -> getPeerJmxConnection().cancelJob(jobId);
    if (cancelCallback != null) {
      try {
        cancelCallback.call();
      } catch (final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
        throw e;
      }
      return true;
    }
    return false;
  }
}
