/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.*;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.execute.*;
import org.jppf.load.balancer.BundlerHelper;
import org.jppf.load.balancer.persistence.*;
import org.jppf.management.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Context associated with a local channel serving state and tasks submission.
 * @author Martin JANDA
 */
public class ChannelWrapperLocal extends ChannelWrapper implements ClientConnectionStatusHandler {
  /**
   * Logger for this class.
   */
  private final static Logger log = LoggerFactory.getLogger(ChannelWrapperLocal.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private final static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The task execution manager for this wrapper.
   */
  private final ExecutionManager executionManager;
  /**
   * Status of the connection.
   */
  private JPPFClientConnectionStatus status = JPPFClientConnectionStatus.ACTIVE;
  /**
   * Unique ID for the connection.
   */
  private final String connectionUuid = JPPFUuid.normalUUID();
  /**
   * List of status listeners for this connection.
   */
  private final List<ClientConnectionStatusListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * Reference to the client 's executor.
   */
  private final JPPFClient client;
  /**
   * 
   */
  private boolean closed;

  /**
   * Default initializer for local channel wrapper.
   * @param client reference to the JPPF client.
   */
  public ChannelWrapperLocal(final JPPFClient client) {
    this.client = client;
    executionManager = new ClientExecutionManager(JPPFProperties.LOCAL_EXECUTION_THREADS);
    priority = client.getConfig().get(JPPFProperties.LOCAL_EXECUTION_PRIORITY);
    systemInfo = new JPPFSystemInformation(getConnectionUuid(), true, false);
    managementInfo = new JPPFManagementInfo("local", "local", -1, getConnectionUuid(), JPPFManagementInfo.NODE | JPPFManagementInfo.LOCAL, false);
    managementInfo.setSystemInfo(systemInfo);
    final String s = "client-local-executor";
    channelID = new Pair<>(s, CryptoUtils.computeHash(s, client.getBundlerFactory().getHashAlgorithm()));
  }

  @Override
  public String getUuid() {
    return connectionUuid;
  }

  @Override
  public String getConnectionUuid() {
    return connectionUuid;
  }

  @Override
  public JPPFClientConnectionStatus getStatus() {
    return status;
  }

  @Override
  public void setStatus(final JPPFClientConnectionStatus status) {
    synchronized(getMonitor()) {
      if (closed) return;
      final ExecutorStatus oldExecutionStatus = getExecutionStatus();
      final JPPFClientConnectionStatus oldValue = this.status;
      if (debugEnabled) log.debug(String.format("status changing from %s to %s for %s", oldValue, status, this));
      this.status = status;
      if (oldValue.isTerminatedStatus()) return;
      fireStatusChanged(oldValue, this.status);
      final ExecutorStatus newExecutionStatus = getExecutionStatus();
      fireExecutionStatusChanged(oldExecutionStatus, newExecutionStatus);
    }
  }

  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener) {
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  protected void fireStatusChanged(final JPPFClientConnectionStatus oldStatus, final JPPFClientConnectionStatus newStatus) {
    if (isClosed() || (oldStatus == newStatus)) return;
    final ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (final ClientConnectionStatusListener listener : listeners) listener.statusChanged(event);
  }

  @Override
  public Future<?> submit(final ClientTaskBundle bundle) {
    if (!isClosed()) {
      if (debugEnabled) log.debug("locally submitting {}", bundle);
      setStatus(JPPFClientConnectionStatus.EXECUTING);
      final Runnable task = new LocalRunnable(bundle);
      bundle.jobDispatched(this);
      client.getExecutor().execute(task);
      if (debugEnabled) log.debug("end locally submitting {}", bundle);
    }
    return null;
  }

  @Override
  public boolean isLocal() {
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
    .append("status=").append(status)
    .append(", connectionUuid=").append(connectionUuid)
    .append(']').toString();
  }

  /**
   *
   */
  private class LocalRunnable implements Runnable {
    /**
     * The task bundle to execute.
     */
    private final ClientTaskBundle bundle;

    /**
     * Initialize this runnable for local execution.
     * @param bundle the execution to perform.
     */
    public LocalRunnable(final ClientTaskBundle bundle) {
      this.bundle = bundle;
    }

    @Override
    public void run() {
      Exception exception = null;
      final List<Task<?>> tasks = this.bundle.getTasksL();
      try {
        final long start = System.nanoTime();
        final DataProvider dataProvider = bundle.getJob().getDataProvider();
        for (final Task<?> task : tasks) task.setDataProvider(dataProvider);
        executionManager.execute(bundle, tasks);
        bundle.resultsReceived(tasks);
        final double elapsed = System.nanoTime() - start;
        BundlerHelper.updateBundler(bundler, tasks.size(), elapsed);
        getLoadBalancerPersistenceManager().storeBundler(channelID, bundler, bundlerAlgorithm);
      } catch (final Throwable t) {
        log.error(t.getMessage(), t);
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        bundle.resultsReceived(t);
      } finally {
        bundle.taskCompleted(exception);
        bundle.getClientJob().removeChannel(ChannelWrapperLocal.this);
        setStatus(JPPFClientConnectionStatus.ACTIVE);
      }
    }
  }

  @Override
  public void close() {
    synchronized(getMonitor()) {
      if (closed) return;
      closed = true;
      if (debugEnabled) log.debug("closing {}", this);
      super.close();
      try {
        if (!status.isTerminatedStatus()) setStatus(JPPFClientConnectionStatus.CLOSED);
        executionManager.shutdown();
      } finally {
        listenerList.clear();
        listeners.clear();
      }
    }
  }

  @Override
  public boolean cancel(final ClientTaskBundle bundle) {
    if (bundle.isCancelled()) return false;
    if (debugEnabled) log.debug("requesting cancel of jobId={}", bundle.getUuid());
    bundle.cancel();
    try {
      executionManager.cancelAllTasks(true, false);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return true;
  }

  @Override
  LoadBalancerPersistenceManager getLoadBalancerPersistenceManager() {
    return (LoadBalancerPersistenceManager) client.getLoadBalancerPersistenceManagement();
  }

  /**
   * @return whether this channel is closed.
   */
  public boolean isClosed() {
    synchronized(getMonitor()) {
      return closed;
    }
  }
}
