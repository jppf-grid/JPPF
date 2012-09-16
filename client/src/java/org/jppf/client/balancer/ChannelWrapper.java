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

package org.jppf.client.balancer;

import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.execute.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.ContextAwareness;
import org.jppf.server.scheduler.bundle.JPPFContext;
import org.jppf.server.scheduler.bundle.NodeAwareness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Context associated with a channel serving state and tasks submission.
 * @param <T> type of task bundle.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public abstract class ChannelWrapper<T> implements ExecutorChannel<ClientTaskBundle>
{
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private Bundler bundler = null;
  /**
   * Represents the system information.
   */
  private JPPFSystemInformation systemInfo = null;
  /**
   * Represents the management information.
   */
  private JPPFManagementInfo managementInfo = null;
  /**
   * Executor for submitting bundles for processing.
   */
  protected ExecutorService executor;
  /**
   * List of execution status listeners for this channel.
   */
  private final List<ExecutorChannelStatusListener> listenerList = new ArrayList<ExecutorChannelStatusListener>();

  /**
   * Default constructor.
   */
  protected ChannelWrapper()
  {
  }

  /**
   * Get the unique identifier of the client.
   * @return the uuid as a string.
   */
  @Override
  public abstract String getUuid();

  /**
   * Get the unique ID for the connection.
   * @return the connection id.
   */
  @Override
  public abstract String getConnectionUuid();

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
  public Bundler getBundler()
  {
    return bundler;
  }

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param serverBundler the bundler to compare with.
   * @param jppfContext execution context.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  @Override
  public boolean checkBundler(final Bundler serverBundler, final JPPFContext jppfContext)
  {
    if (serverBundler == null) throw new IllegalArgumentException("serverBundler is null");

    if (this.bundler == null || this.bundler.getTimestamp() < serverBundler.getTimestamp())
    {
      if (this.bundler != null)
      {
        this.bundler.dispose();
        if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(null);
      }
      this.bundler = serverBundler.copy();
      if (this.bundler instanceof ContextAwareness) ((ContextAwareness)this.bundler).setJPPFContext(jppfContext);
      this.bundler.setup();
      if (this.bundler instanceof NodeAwareness) ((NodeAwareness) this.bundler).setNodeConfiguration(systemInfo);
      return true;
    }
    return false;
  }

  /**
   * Get the system information.
   * @return a {@link JPPFSystemInformation} instance.
   */
  @Override
  public JPPFSystemInformation getSystemInfo()
  {
    return systemInfo;
  }

  /**
   * Set the system information.
   * @param systemInfo a {@link JPPFSystemInformation} instance.
   */
  public void setSystemInfo(final JPPFSystemInformation systemInfo)
  {
    this.systemInfo = systemInfo;
  }

  /**
   * Get the management information.
   * @return a {@link JPPFManagementInfo} instance.
   */
  @Override
  public JPPFManagementInfo getManagementInfo()
  {
    return managementInfo;
  }

  /**
   * Set the management information.
   * @param managementInfo a {@link JPPFManagementInfo} instance.
   */
  public void setManagementInfo(final JPPFManagementInfo managementInfo)
  {
    this.managementInfo = managementInfo;
  }

  /**
   * Submit bundle for execution on corresponding node.
   * @param bundle a {@link ClientTaskBundle} instance.
   * @return a {@link JPPFFuture}.
   */
  @Override
  public abstract JPPFFuture<?> submit(final ClientTaskBundle bundle);

  /**
   * Determine whether this channel is local (for an in-JVM node).
   * @return <code>false</code> if the channel is local, <code>false</code> otherwise.
   */
  @Override
  public abstract boolean isLocal();

  /**
   * Close this channel and release the resources it uses.
   */
  @Override
  public void close()
  {
    if (executor != null) executor.shutdownNow();
  }

  @Override
  public Object getMonitor() {
    return this;
  }

  @Override
  public void addExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList)
    {
      listenerList.add(listener);
    }
  }

  @Override
  public void removeExecutionStatusListener(final ExecutorChannelStatusListener listener) {
    if (listener == null) throw new IllegalArgumentException("listener is null");

    synchronized (listenerList)
    {
      listenerList.remove(listener);
    }
  }

  /**
   * Notify all listeners that the execution status of this channel has changed.
   * @param oldValue the channel execution status before the change.
   * @param newValue the channel execution status after the change.
   */
  protected void fireExecutionStatusChanged(final ExecutorStatus oldValue, final ExecutorStatus newValue)
  {
    if (oldValue == newValue) return;
    ExecutorChannelStatusListener[] listeners;
    synchronized (listenerList)
    {
      listeners = listenerList.toArray(new ExecutorChannelStatusListener[listenerList.size()]);
    }
    ExecutorChannelStatusEvent event = new ExecutorChannelStatusEvent(this, oldValue, newValue);
    for (ExecutorChannelStatusListener listener : listeners) {
      listener.executionStatusChanged(event);
    }
  }
}
