/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

import org.jppf.client.*;
import org.jppf.client.balancer.queue.*;
import org.jppf.client.event.*;
import org.jppf.load.balancer.Bundler;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.queue.*;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * This task provides asynchronous management of tasks submitted through the resource adapter.
 * It relies on a queue where job are first added, then submitted when a connection becomes available.
 * It also provides methods to check the status of a job and retrieve the results.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @exclude
 */
public class JobManagerClient extends ThreadSynchronization implements JobManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JobManagerClient.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * A reference to the tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * The bundler factory.
   */
  private final JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory(JPPFBundlerFactory.Defaults.CLIENT);
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final TaskQueueChecker taskQueueChecker;
  /**
   * Mapping client connections to channel wrapper.
   */
  private final Map<JPPFClientConnection, ChannelWrapper> wrapperMap = new HashMap<>();
  /**
   * A list of all the connections.
   */
  private final List<ChannelWrapper> allConnections = new ArrayList<>();
  /**
   * Listener used for monitoring state changes.
   */
  private final ClientConnectionStatusListener statusListener = new ClientConnectionStatusListener() {
    @Override
    public void statusChanged(final ClientConnectionStatusEvent event) {
      if (event.getSource() instanceof JPPFClientConnection) {
        updateConnectionStatus(((JPPFClientConnection) event.getSource()), event.getOldStatus());
      } else if (event.getSource() instanceof ChannelWrapper) {
        updateConnectionStatus((ChannelWrapper) event.getSource(), event.getOldStatus());
      }
    }
  };
  /**
   * Determines whether local execution is enabled on this client.
   */
  private boolean localEnabled;
  /**
   * Wrapper for local execution node.
   */
  private ChannelWrapperLocal wrapperLocal = null;
  /**
   * Holds the current connections with ACTIVE or EXECUTING status.
   */
  private final ConcurrentHashMap<String, ChannelWrapper> workingConnections = new ConcurrentHashMap<>();
  /**
   * Determines whether this job manager has been closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Instantiates client job manager.
   * @param client JPPF client that manages connections to the JPPF drivers.
   * @throws Exception if any error occurs.
   */
  public JobManagerClient(final AbstractGenericClient client) throws Exception {
    if (client == null) throw new IllegalArgumentException("client is null");
    this.localEnabled = client.getConfig().getBoolean("jppf.local.execution.enabled", false);
    if (debugEnabled) log.debug("local execution enabled = " + this.localEnabled);
    Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
    this.queue = new JPPFPriorityQueue(this);
    taskQueueChecker = new TaskQueueChecker(queue);
    taskQueueChecker.setBundler(bundler);
    this.queue.addQueueListener(new QueueListenerAdapter<ClientJob, ClientJob, ClientTaskBundle>() {
      @Override
      public void bundleAdded(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> event) {
        taskQueueChecker.wakeUp();
      }
    });
    new Thread(taskQueueChecker, "TaskQueueChecker").start();
    this.queue.addQueueListener(client);
    client.addClientListener(new ClientListener() {
      @Override
      public void newConnection(final ClientEvent event) {
        addConnection(event.getConnection());
      }

      @Override
      public void connectionFailed(final ClientEvent event) {
        removeConnection(event.getConnection());
      }
    });
    updateLocalExecution(this.localEnabled);
    List<JPPFClientConnection> connections = client.getAllConnections();
    for (JPPFClientConnection connection : connections) addConnection(connection);
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param wrapper the connection wrapper to add.
   */
  protected synchronized void addConnection(final ChannelWrapper wrapper) {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");
    if (closed.get()) throw new IllegalStateException("this job manager was closed");
    allConnections.add(wrapper);
    updateConnectionStatus(wrapper, JPPFClientConnectionStatus.NEW, wrapper.getStatus());
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param wrapper the connection wrapper to remove.
   */
  protected synchronized void removeConnection(final ChannelWrapper wrapper) {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");
    try {
      updateConnectionStatus(wrapper, wrapper.getStatus(), JPPFClientConnectionStatus.DISCONNECTED);
    } finally {
      allConnections.remove(wrapper);
    }
  }

  /**
   * Add the specified client connection to the list of connections handled by this manager.
   * @param cnn the client connection to add.
   * @return wrapper for the added client connection.
   */
  protected synchronized ChannelWrapper addConnection(final JPPFClientConnection cnn) {
    if (log.isDebugEnabled()) log.debug("adding connection " + cnn);
    if (closed.get()) throw new IllegalStateException("this job manager was closed");
    ChannelWrapper wrapper = wrapperMap.get(cnn);
    if (wrapper == null) {
      try
      {
        wrapper = new ChannelWrapperRemote(cnn);
        JMXDriverConnectionWrapper jmx = cnn.getConnectionPool().getJmxConnection();
        JPPFSystemInformation systemInfo = cnn.getSystemInfo();
        if (systemInfo != null) wrapper.setSystemInformation(systemInfo);
        JPPFManagementInfo info = new JPPFManagementInfo(cnn.getHost(), jmx == null ? -1 : jmx.getPort(), cnn.getDriverUuid(), JPPFManagementInfo.DRIVER, cnn.isSSLEnabled());
        if (systemInfo != null) info.setSystemInfo(systemInfo);
        wrapper.setManagementInfo(info);
      } catch (Throwable e) {
        log.error("Error while adding connection " + cnn, e);
      } finally {
        wrapperMap.put(cnn, wrapper);
        addConnection(wrapper);
      }
    }
    if (log.isDebugEnabled()) log.debug("end of adding connection " + cnn);
    return wrapper;
  }

  /**
   * Remove the specified client connection from the list of connections handled by this manager.
   * @param connection the client connection to remove.
   * @return wrapper for the removed client connection or null.
   */
  protected synchronized ChannelWrapper removeConnection(final JPPFClientConnection connection) {
    ChannelWrapper wrapper = wrapperMap.remove(connection);
    if (wrapper != null) removeConnection(wrapper);
    return wrapper;
  }

  /**
   * Get all the client connections handled by this manager.
   * @return a list of <code>ChannelWrapper</code> instances.
   */
  public synchronized List<ChannelWrapper> getAllConnections() {
    return new ArrayList<>(allConnections);
  }

  /**
   * Get all the client connections with a working status.
   * @return a list of {@link ChannelWrapper} instances.
   */
  public List<ChannelWrapper> getWorkingConnections() {
    return new ArrayList<>(workingConnections.values());
  }

  /**
   * Get all the client connections with a working status, excluding the local executor if it is enabled.
   * @return a list of {@link ChannelWrapper} instances.
   */
  public List<ChannelWrapper> getWorkingRemoteConnections() {
    List<ChannelWrapper> result = getWorkingConnections();
    if (isLocalExecutionEnabled()) {
      Iterator<ChannelWrapper> it = result.iterator();
      while (it.hasNext()) {
        ChannelWrapper channel = it.next();
        if (channel.isLocal()) {
          it.remove();
          break;
        }
      }
    }
    return result;
  }

  /**
   * Determine whether there is at least one working connection, idle or not.
   * @return {@code true} if there is at least one connection, {@code false} otherwise.
   */
  public boolean hasWorkingConnection() {
    return !workingConnections.isEmpty();
  }

  /**
   * @param connection the client connection.
   * @param oldStatus the connection status before the change.
   */
  private void updateConnectionStatus(final JPPFClientConnection connection, final JPPFClientConnectionStatus oldStatus) {
    ChannelWrapper wrapper = wrapperMap.get(connection);
    if (wrapper != null) {
      if (oldStatus == JPPFClientConnectionStatus.CONNECTING && wrapper.getStatus() == JPPFClientConnectionStatus.ACTIVE) {
        JPPFSystemInformation systemInfo = connection.getSystemInfo();
        JMXDriverConnectionWrapper jmx = connection.getConnectionPool().getJmxConnection();
        wrapper.setSystemInformation(systemInfo);
        JPPFManagementInfo info = new JPPFManagementInfo(connection.getHost(), jmx != null ? jmx.getPort() : -1,
          jmx != null ? jmx.getId() : (connection.getDriverUuid() != null ? connection.getDriverUuid() : "?"),
          JPPFManagementInfo.DRIVER, connection.isSSLEnabled());
        info.setSystemInfo(systemInfo);
        wrapper.setManagementInfo(info);
      }
      updateConnectionStatus(wrapper, oldStatus);
    }
  }

  /**
   * @param wrapper   the connection wrapper.
   * @param oldStatus the connection status before the change.
   */
  private void updateConnectionStatus(final ChannelWrapper wrapper, final JPPFClientConnectionStatus oldStatus) {
    if (wrapper == null) return;
    updateConnectionStatus(wrapper, oldStatus, wrapper.getStatus());
  }

  /**
   * @param wrapper   the connection wrapper.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  private void updateConnectionStatus(final ChannelWrapper wrapper, final JPPFClientConnectionStatus oldStatus, final JPPFClientConnectionStatus newStatus) {
    if (oldStatus == null) throw new IllegalArgumentException("oldStatus is null");
    if (newStatus == null) throw new IllegalArgumentException("newStatus is null");
    if (wrapper == null || oldStatus == newStatus) return;
    if (newStatus == JPPFClientConnectionStatus.ACTIVE) taskQueueChecker.addIdleChannel(wrapper);
    else {
      taskQueueChecker.removeIdleChannel(wrapper);
      if(newStatus.isTerminatedStatus() || newStatus == JPPFClientConnectionStatus.DISCONNECTED) queue.cancelBroadcastJobs(wrapper.getUuid());
    }
    boolean bNew = newStatus.isWorkingStatus();
    boolean bOld = oldStatus.isWorkingStatus();
    if (bNew && !bOld) workingConnections.put(wrapper.getConnectionUuid(), wrapper);
    else if (!bNew && bOld) workingConnections.remove(wrapper.getConnectionUuid());
  }

  @Override
  public String submitJob(final JPPFJob job) {
    return submitJob(job, null);
  }

  @Override
  @SuppressWarnings("deprecation")
  public String submitJob(final JPPFJob job, final JobStatusListener listener) {
    if (closed.get()) throw new IllegalStateException("this jobmanager was closed");
    List<Task<?>> pendingTasks = new ArrayList<>();
    if (listener != null) job.getResultCollector().addJobStatusListener(listener);
    List<Task<?>> tasks = job.getJobTasks();
    for (Task<?> task: tasks) if (!job.getResults().hasResult(task.getPosition())) pendingTasks.add(task);
    queue.addBundle(new ClientJob(job, pendingTasks));
    return job.getUuid();
  }

  @Override
  public String resubmitJob(final JPPFJob job) {
    return submitJob(job);
  }

  @Override
  public boolean cancelJob(final String jobId) throws Exception {
    if (debugEnabled) log.debug("requesting cancel of jobId=" + jobId);
    queue.cancelJob(jobId);
    return true;
  }

  @Override
  public synchronized boolean hasAvailableConnection() {
    return taskQueueChecker.hasIdleChannel() || ((wrapperLocal != null) && (wrapperLocal.getStatus() == JPPFClientConnectionStatus.ACTIVE));
  }

  @Override
  public synchronized boolean isLocalExecutionEnabled() {
    return localEnabled;
  }

  @Override
  public synchronized void setLocalExecutionEnabled(final boolean localExecutionEnabled) {
    if (this.localEnabled == localExecutionEnabled) return;
    this.localEnabled = localExecutionEnabled;
    updateLocalExecution(this.localEnabled);
  }

  /**
   * Starts or stops local execution node according to specified parameter.
   * @param localExecutionEnabled {@code true} to enable local execution, {@code false} otherwise
   */
  private synchronized void updateLocalExecution(final boolean localExecutionEnabled) {
    if (closed.get()) throw new IllegalStateException("this job manager was closed");
    if (localExecutionEnabled) {
      if (debugEnabled) log.debug("enabling local execution");
      wrapperLocal = new ChannelWrapperLocal();
      wrapperLocal.addClientConnectionStatusListener(statusListener);
      addConnection(wrapperLocal);
    } else { 
      if (debugEnabled) log.debug("disabling local execution");
      if (wrapperLocal != null) {
        try {
          wrapperLocal.close();
        } finally {
          removeConnection(wrapperLocal);
          wrapperLocal = null;
        }
      }
    }
  }

  @Override
  public Vector<JPPFClientConnection> getAvailableConnections() {
    List<ChannelWrapper> idleChannels = taskQueueChecker.getIdleChannels();
    Vector<JPPFClientConnection> availableConnections = new Vector<>(idleChannels.size());
    for (ChannelWrapper idleChannel : idleChannels) {
      if (idleChannel instanceof ChannelWrapperRemote) {
        ChannelWrapperRemote wrapperRemote = (ChannelWrapperRemote) idleChannel;
        availableConnections.add(wrapperRemote.getChannel());
      }
    }
    return availableConnections;
  }

  @Override
  public ClientConnectionStatusListener getClientConnectionStatusListener() {
    return this.statusListener;
  }

  @Override
  public void reset() {
    synchronized(this) {
      for (ChannelWrapper channel: allConnections) channel.close();
      allConnections.clear();
      if (taskQueueChecker != null) taskQueueChecker.clearChannels();
    }
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}", this);
    closed.set(true);
    setStopped(true);
    wakeUp();
    if (taskQueueChecker != null) {
      taskQueueChecker.setStopped(true);
      taskQueueChecker.wakeUp();
    }
    queue.close();
    synchronized(this) {
      for (ChannelWrapper channel: allConnections) channel.close();
      allConnections.clear();
    }
  }
}
