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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.execute.*;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.nio.NioContext;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.ServerTaskBundleNode;
import org.jppf.utils.Pair;

/**
 * @param <S> the type of state.
 * @author Laurent Cohen
 */
public interface AbstractBaseNodeContext<S extends Enum<S>> extends NioContext<S>,  ExecutorChannel<ServerTaskBundleNode>  {
  /**
   * @return this node context's attributes.
   */
  NodeContextAttributes getAttributes();

  @Override
  default void addExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    getAttributes().addExecutionStatusListener(listener);
  }

  @Override
  default void removeExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    getAttributes().removeExecutionStatusListener(listener);
  }

  /**
   * Notify all listeners that the execution status of this channel has changed.
   * @param oldValue the channel execution status before the change.
   * @param newValue the channel execution status after the change.
   */
  default void fireExecutionStatusChanged(final ExecutorStatus oldValue, final ExecutorStatus newValue) {
    getAttributes().fireExecutionStatusChanged(oldValue, newValue);
  }

  @Override
  default ExecutorStatus getExecutionStatus() {
    return getAttributes().getExecutionStatus();
  }

  /**
   *
   * @param executionStatus the execution status for the node.
   */
  default void setExecutionStatus(final ExecutorStatus executionStatus) {
    getAttributes().setExecutionStatus(executionStatus);
  }

  @Override
  default boolean isActive() {
    return getAttributes().istActive();
  }

  /**
   * Activate or deactivate the node.
   * @param active {@code true} to activate the node, {@code false} to deactivate it.
   */
  default void setActive(final boolean active) {
    getAttributes().setActive(active);
  }

  @Override
  default Bundler<?> getBundler() {
    return getAttributes().getBundler();
  }

  /**
   * Set the bundler used to schedule tasks for the corresponding node.
   * @param bundler a {@link Bundler} instance.
   */
  default void setBundler(final Bundler<?> bundler) {
    getAttributes().setBundler(bundler);
  }

  @Override
  default Bundler<?> checkBundler(final JPPFBundlerFactory factory, final JPPFContext jppfContext) {
    return getAttributes().checkBundler(factory, jppfContext);
  }

  @Override
  default JPPFSystemInformation getSystemInformation() {
    return getAttributes().getSystemInfo();
  }

  /**
   * Set the management information.
   * @param managementInfo a {@link JPPFManagementInfo} instance.
   */
  default void setManagementInfo(final JPPFManagementInfo managementInfo) {
    getAttributes().setManagementInfo(managementInfo);
  }

  /**
   * Set the node system information.
   * @param nodeInfo a {@link JPPFSystemInformation} instance.
   * @param update a flag indicates whether update system information in management information.
   */
  default void setNodeInfo(final JPPFSystemInformation nodeInfo, final boolean update) {
    getAttributes().setNodeInfo(nodeInfo, update);
  }

  @Override
  default JPPFManagementInfo getManagementInfo() {
    return getAttributes().getManagementInfo();
  }

  /**
   * Get the object that provides access to the management functions of the node.
   * @return a {@code JMXConnectionWrapper} instance.
   */
  default JMXNodeConnectionWrapper getJmxConnection() {
    return getAttributes().getJmxConnection();
  }

  /**
  *
  * @param jmxConnection the connection to the node's JMX server.
  */
 default void setJmxConnection(final JMXNodeConnectionWrapper jmxConnection) {
   getAttributes().setJmxConnection(jmxConnection);
 }

 /**
   * Get the object that provides access to the management functions of the driver.
   * @return a {@code JMXConnectionWrapper} instance.
   */
  default JMXDriverConnectionWrapper getPeerJmxConnection() {
    return getAttributes().getPeerJmxConnection();
  }

  /**
   *
   * @param peerJmxConnection the connection to the peer driver's JMX server.
   */
  default void setPeerJmxConnection(final JMXDriverConnectionWrapper peerJmxConnection) {
    getAttributes().setPeerJmxConnection(peerJmxConnection);
  }

 /**
   * Set the {@code Runnable} that will be called when node context is closed.
   * @return a {@code Runnable} called when node context is closed or {@code null}.
   */
  default Runnable getOnClose() {
    return getAttributes().getOnClose();
  }

  /**
   * Set the {@code Runnable} that will be called when node context is closed.
   * @param onClose a {@code Runnable} called when node context is closed or {@code null}.
   */
  default void setOnClose(final Runnable onClose) {
    getAttributes().setOnClose(onClose);
  }

  /**
   * @return whether to remove any job reservation for this node.
   */
  default NodeReservationHandler.Transition getReservationTansition() {
    return getAttributes().getReservationTansition();
  }

  /**
   *
   * @param reservationTansition whether to remove any job reservation for this node.
   */
  default void setReservationTansition(final NodeReservationHandler.Transition reservationTansition) {
    getAttributes().setReservationTansition(reservationTansition);
  }

  /**
   * @return the latest computed score for a given desired configuration.
   */
  default int getReservationScore() {
    return getAttributes().getReservationScore();
  }

  /**
   *
   * @param reservationScore the latest computed score for a given desired configuration.
   */
  default void setReservationScore(final int reservationScore) {
    getAttributes().setReservationScore(reservationScore);
  }

  /**
   * @return the unique node identfier reusable over node restarts.
   */
  default Pair<String, String> getNodeIdentifier() {
    return getAttributes().getNodeIdentifier();
  }

  /**
   *
   * @param nodeIdentifier the unique node identfier reusable over node restarts.
   */
  default void setNodeIdentifier(final Pair<String, String> nodeIdentifier) {
    getAttributes().setNodeIdentifier(nodeIdentifier);
  }

  /**
   * @return the algorithm name for the bundler.
   */
  default String getBundlerAlgorithm() {
    return getAttributes().getBundlerAlgorithm();
  }

  /**
   *
   * @param bundlerAlgorithm the algorithm name for the bundler.
   */
  default void setBundlerAlgorithm(final String bundlerAlgorithm) {
    getAttributes().setBundlerAlgorithm(bundlerAlgorithm);
  }

  /**
   * @return whether the node works in offline mode.
   */
  default boolean isOffline() {
    return getAttributes().isOffline();
  }

  /**
   * Specify whether the node works in offline mode.
   * @param offline <code>true</code> if the node is in offline mode, <code>false</code> otherwise.
   */
  default void setOffline(final boolean offline) {
    getAttributes().setOffline(offline);
  }

  /**
   * @return a reference to the JPPF driver.
   */
  default JPPFDriver getDriver() {
    return getAttributes().getDriver();
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @param requeue true if the job should be requeued on the server side, false otherwise.
   * @return a {@code true} when cancel was successful {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  boolean cancelJob(final String jobId, final boolean requeue) throws Exception;

  /**
   * @return whether the node is idle or not.
   */
  default AtomicBoolean getIdle() {
    return getAttributes().getIdle();
  }
}
