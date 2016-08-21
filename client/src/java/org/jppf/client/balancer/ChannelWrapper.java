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

package org.jppf.client.balancer;

import java.util.List;
import java.util.concurrent.*;

import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.execute.*;
import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.slf4j.*;

/**
 * Context associated with a channel serving state and tasks submission.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class ChannelWrapper implements ExecutorChannel<ClientTaskBundle> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ChannelWrapper.class);
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  Bundler<?> bundler = null;
  /**
   * Represents the system information.
   */
  JPPFSystemInformation systemInfo = null;
  /**
   * Represents the management information.
   */
  JPPFManagementInfo managementInfo = null;
  /**
   * Executor for submitting bundles for processing.
   */
  ExecutorService executor;
  /**
   * List of execution status listeners for this channel.
   */
  private final List<ExecutorChannelStatusListener> listenerList = new CopyOnWriteArrayList<>();
  /**
   * The priority assigned to this channel.
   */
  int priority = 0;
  /**
   * Whether the client is resetting.
   */
  boolean resetting = false;

  /**
   * Default constructor.
   */
  protected ChannelWrapper() {
  }

  /**
   * Get the status of this connection.
   * @return a <code>JPPFClientConnectionStatus</code> enumerated value.
   */
  public abstract JPPFClientConnectionStatus getStatus();

  @Override
  public ExecutorStatus getExecutionStatus() {
    switch (getStatus()) {
      case ACTIVE:
        return ExecutorStatus.ACTIVE;
      case CLOSED:
      case FAILED:
        return ExecutorStatus.FAILED;
      case EXECUTING:
        return ExecutorStatus.EXECUTING;
      default:
        return ExecutorStatus.DISABLED;
    }
  }

  /**
   * Add a connection status listener to this connection's list of listeners.
   * @param listener the listener to add to the list.
   */
  public abstract void addClientConnectionStatusListener(final ClientConnectionStatusListener listener);

  /**
   * Remove a connection status listener from this connection's list of listeners.
   * @param listener the listener to remove from the list.
   */
  public abstract void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener);

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
  @Override
  public Bundler<?> getBundler() {
    return bundler;
  }

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param factory the load balancer factory.
   * @param jppfContext execution context.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  @SuppressWarnings("deprecation")
  @Override
  public boolean checkBundler(final JPPFBundlerFactory factory, final JPPFContext jppfContext) {
    if (factory == null) throw new IllegalArgumentException("Bundler factory is null");
    if (this.bundler == null || this.bundler.getTimestamp() < factory.getLastUpdateTime()) {
      if (this.bundler != null) {
        this.bundler.dispose();
        if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(null);
      }
      this.bundler = factory.newBundler();
      if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(jppfContext);
      this.bundler.setup();
      if (this.bundler instanceof ChannelAwareness) ((ChannelAwareness) this.bundler).setChannelConfiguration(systemInfo);
      return true;
    }
    return false;
  }

  @Override
  public JPPFSystemInformation getSystemInformation() {
    if (traceEnabled) log.trace("getting system info for " + this + ", jppf.channel.local=" + systemInfo.getJppf().getProperty("jppf.channel.local") + ", isLocal()="+isLocal());
    return systemInfo;
  }

  /**
   * Set the system information.
   * @param systemInfo a {@link JPPFSystemInformation} instance.
   */
  public void setSystemInformation(final JPPFSystemInformation systemInfo) {
    if (systemInfo != null) {
      systemInfo.getJppf().setBoolean("jppf.channel.local", isLocal());
      this.systemInfo = systemInfo;
      if (traceEnabled) log.trace("setting system info for " + this + ", jppf.channel.local=" + this.systemInfo.getJppf().getProperty("jppf.channel.local") + ", isLocal()="+isLocal());
    } else if (traceEnabled) {
      Exception e = new Exception("call stack for setSystemInfo(null)");
      log.trace(e.getMessage(), e);
    }
  }

  @Override
  public JPPFManagementInfo getManagementInfo() {
    return managementInfo;
  }

  /**
   * Set the management information.
   * @param managementInfo a {@link JPPFManagementInfo} instance.
   */
  public void setManagementInfo(final JPPFManagementInfo managementInfo) {
    if (managementInfo != null) this.managementInfo = managementInfo;
  }

  @Override
  public void close() {
    if (executor != null) executor.shutdownNow();
    if (bundler != null) bundler.dispose();
  }

  @Override
  public Object getMonitor() {
    return this;
  }

  @Override
  public void addExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    listenerList.add(listener);
  }

  @Override
  public void removeExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");
    listenerList.remove(listener);
  }

  /**
   * Notify all listeners that the execution status of this channel has changed.
   * @param oldValue the channel execution status before the change.
   * @param newValue the channel execution status after the change.
   */
  protected void fireExecutionStatusChanged(final ExecutorStatus oldValue, final ExecutorStatus newValue) {
    if (oldValue == newValue) return;
    ExecutorChannelStatusEvent event = new ExecutorChannelStatusEvent(this, oldValue, newValue);
    for (ExecutorChannelStatusListener listener : listenerList) listener.executionStatusChanged(event);
  }

  @Override
  public boolean isActive() {
    return true;
  }

  /**
   * The priority assigned to this channel.
   * @return the priority as an int value.
   */
  public int getPriority() {
    return priority;
  }

  /**
   * Cancel the currently executing job, if any.
   * @param bundle the bundle to cancel.
   * @return {@code true} if the job is effectively cancelled, {@code false} otherwise.
   */
  public abstract boolean cancel(ClientTaskBundle bundle);

  /**
   * Determine whether the client is resetting.
   * @return {@code true} if the client is resetting, {@code false} otherwise.
   */
  public boolean isResetting() {
    return resetting;
  }

  /**
   * Specify whether the client is resetting.
   * @param resetting {@code true} if the client is resetting, {@code false} otherwise.
   */
  public void setResetting(final boolean resetting) {
    this.resetting = resetting;
  }
}
