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

package org.jppf.client.balancer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.*;
import org.jppf.client.balancer.queue.*;
import org.jppf.client.event.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.load.balancer.spi.JPPFBundlerFactory;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.queue.*;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
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
  private final JPPFBundlerFactory bundlerFactory;
  /**
   * The latest load-balancing information.
   */
  private LoadBalancingInformation currentLoadBalancingInformation;
  /**
   * Synchronization lock.
   */
  private final Object loadBalancingInformationLock = new Object();
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final JobScheduler jobScheduler;
  /**
   * Mapping client connections to channel wrapper.
   */
  private final Map<JPPFClientConnection, ChannelWrapper> wrapperMap = new HashMap<>();
  /**
   * A list of all the connections.
   */
  private final CollectionSortedMap<Integer, ChannelWrapper> allConnections = new LinkedListSortedMap<>(new DescendingIntegerComparator());
  /**
   * Listener used for monitoring state changes.
   */
  private final ClientConnectionStatusListener statusListener = event -> {
    if (event.getSource() instanceof ChannelWrapperLocal) updateConnectionStatus((ChannelWrapper) event.getSource(), event.getOldStatus());
    else if (event.getSource() instanceof JPPFClientConnection) updateConnectionStatus(((JPPFClientConnection) event.getSource()), event.getOldStatus());
  };
  /**
   * Determines whether local execution is enabled on this client.
   */
  private boolean localEnabled;
  /**
   * Wrapper for local execution node.
   */
  private ChannelWrapperLocal wrapperLocal;
  /**
   * Holds the current connections with ACTIVE or EXECUTING status.
   */
  private final CollectionSortedMap<Integer, ChannelWrapper> workingConnections = new LinkedListSortedMap<>(new DescendingIntegerComparator());
  /**
   * Determines whether this job manager has been closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);
  /**
   * The JPPF client.
   */
  private final JPPFClient client;

  /**
   * Instantiates client job manager.
   * @param client JPPF client that manages connections to the JPPF drivers.
   * @param bundlerFactory the factory that creates load-balancer instances.
   * @throws Exception if any error occurs.
   */
  public JobManagerClient(final JPPFClient client, final JPPFBundlerFactory bundlerFactory) throws Exception {
    if (client == null) throw new IllegalArgumentException("client is null");
    this.client = client;
    this.localEnabled = client.getConfig().get(JPPFProperties.LOCAL_EXECUTION_ENABLED);
    this.queue = new JPPFPriorityQueue(this);
    this.bundlerFactory = bundlerFactory;
    currentLoadBalancingInformation = bundlerFactory.getCurrentInfo();
    jobScheduler = new JobScheduler(queue, bundlerFactory);
    this.queue.addQueueListener(new QueueListenerAdapter<ClientJob, ClientJob, ClientTaskBundle>() {
      @Override
      public void bundleAdded(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> event) {
        jobScheduler.wakeUp();
      }
    });
    ThreadUtils.startDaemonThread(jobScheduler, "JobScheduler");
    this.queue.addQueueListener(client);
    client.addConnectionPoolListener(new ConnectionPoolListenerAdapter() {
      @Override
      public void connectionRemoved(final ConnectionPoolEvent event) {
        removeConnection(event.getConnection());
      }
    });
    updateLocalExecution(this.localEnabled);
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param wrapper the connection wrapper to add.
   */
  protected void addConnection(final ChannelWrapper wrapper) {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");
    if (closed.get()) throw new IllegalStateException("this job manager was closed");
    if (log.isDebugEnabled()) log.debug("adding connection " + wrapper);
    synchronized(allConnections) {
      allConnections.putValue(wrapper.getPriority(), wrapper);
    }
    updateConnectionStatus(wrapper, JPPFClientConnectionStatus.NEW, wrapper.getStatus());
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param wrapper the connection wrapper to remove.
   */
  protected void removeConnection(final ChannelWrapper wrapper) {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");
    try {
      final JPPFClientConnectionStatus status = wrapper.getStatus();
      if (!status.isTerminatedStatus()) updateConnectionStatus(wrapper, wrapper.getStatus(), JPPFClientConnectionStatus.DISCONNECTED);
      else updateConnectionStatus(wrapper, wrapper.getOldStatus(), wrapper.getStatus());
    } finally {
      synchronized(allConnections) {
        allConnections.removeValue(wrapper.getPriority(), wrapper);
      }
    }
  }

  /**
   * Add the specified client connection to the list of connections handled by this manager.
   * @param cnn the client connection to add.
   * @return wrapper for the added client connection.
   */
  public ChannelWrapper addConnection(final JPPFClientConnection cnn) {
    if (debugEnabled) log.debug("adding connection " + cnn);
    if (closed.get()) throw new IllegalStateException("this job manager was closed");
    ChannelWrapper wrapper = null;
    synchronized(wrapperMap) {
      wrapper = wrapperMap.get(cnn);
    }
    if (wrapper == null) {
      try {
        wrapper = new ChannelWrapperRemoteAsync(cnn);
        final JPPFSystemInformation systemInfo = cnn.getSystemInfo();
        if (systemInfo != null) wrapper.setSystemInformation(systemInfo);
        final JPPFConnectionPool pool = cnn.getConnectionPool();
        final JPPFManagementInfo info = new JPPFManagementInfo(pool.getDriverHost(), pool.getDriverIPAddress(), pool.getJmxPort(), pool.getDriverUuid(), JPPFManagementInfo.DRIVER, pool.isSslEnabled());
        if (systemInfo != null) info.setSystemInfo(systemInfo);
        wrapper.setManagementInfo(info);
      } catch (final Throwable e) {
        log.error("Error while adding connection " + cnn, e);
      } finally {
        synchronized(wrapperMap) {
          wrapperMap.put(cnn, wrapper);
        }
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
  protected ChannelWrapper removeConnection(final JPPFClientConnection connection) {
    ChannelWrapper wrapper = null;
    synchronized(wrapperMap) {
      wrapper = wrapperMap.remove(connection);
    }
    if (wrapper != null) {
      if (debugEnabled) log.debug("removing connection {}", connection);
      removeConnection(wrapper);
    }
    return wrapper;
  }

  /**
   * Get all the client connections handled by this manager.
   * @return a list of <code>ChannelWrapper</code> instances.
   */
  public List<ChannelWrapper> getAllConnections() {
    synchronized(allConnections) {
      return new ArrayList<>(allConnections.allValues());
    }
  }

  /**
   * Get all the client connections with a working status.
   * @return a list of {@link ChannelWrapper} instances.
   */
  public List<ChannelWrapper> getWorkingConnections() {
    synchronized(workingConnections) {
      return new ArrayList<>(workingConnections.allValues());
    }
  }

  /**
   * Get all the client connections with a working status, excluding the local executor if it is enabled.
   * @return a list of {@link ChannelWrapper} instances.
   */
  public List<ChannelWrapper> getWorkingRemoteConnections() {
    final List<ChannelWrapper> result = getWorkingConnections();
    if (isLocalExecutionEnabled()) {
      final Iterator<ChannelWrapper> it = result.iterator();
      while (it.hasNext()) {
        final ChannelWrapper channel = it.next();
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
    synchronized(workingConnections) {
      return !workingConnections.isEmpty();
    }
  }

  /**
   * @param connection the client connection.
   * @param oldStatus the connection status before the change.
   */
  private void updateConnectionStatus(final JPPFClientConnection connection, final JPPFClientConnectionStatus oldStatus) {
    ChannelWrapper wrapper = null;
    synchronized(wrapperMap) {
      wrapper = wrapperMap.get(connection);
    }
    if (wrapper != null) {
      if ((oldStatus == JPPFClientConnectionStatus.CONNECTING) && (wrapper.getStatus() == JPPFClientConnectionStatus.ACTIVE)) {
        final JPPFSystemInformation systemInfo = connection.getSystemInfo();
        final JMXDriverConnectionWrapper jmx = connection.getConnectionPool().getJmxConnection();
        wrapper.setSystemInformation(systemInfo);
        if (!wrapper.isLocal()) {
          final String driverUuid = connection.getDriverUuid();
          JPPFManagementInfo info = null;
          final JPPFConnectionPool pool = connection.getConnectionPool();
          if (jmx != null) info = new JPPFManagementInfo(pool.getDriverHost(), pool.getDriverIPAddress(), jmx.getPort(), jmx.getId(), JPPFManagementInfo.DRIVER, connection.isSSLEnabled());
          else info = new JPPFManagementInfo(pool.getDriverHost(), pool.getDriverIPAddress(), -1, driverUuid != null ? driverUuid : "?", JPPFManagementInfo.DRIVER, connection.isSSLEnabled());
          info.setSystemInfo(systemInfo);
          wrapper.setManagementInfo(info);
        }
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
    if (closed.get()) return;
    if (oldStatus == null) throw new IllegalArgumentException("oldStatus is null");
    if (newStatus == null) throw new IllegalArgumentException("newStatus is null");
    if (debugEnabled) log.debug("updating status from {} to {} for {}", oldStatus, newStatus, wrapper);
    if ((wrapper == null) || (oldStatus == newStatus)) return;
    final boolean bNew = newStatus.isWorkingStatus();
    final boolean bOld = oldStatus.isWorkingStatus();
    //final int priority = wrapper.getPriority();
    if (bNew && !bOld) {
      synchronized(workingConnections) {
        workingConnections.putValue(wrapper.getPriority(), wrapper);
        jobScheduler.setHighestPriority(workingConnections.firstKey());
      }
    } else if (!bNew && bOld) {
      synchronized(workingConnections) {
        workingConnections.removeValue(wrapper.getPriority(), wrapper);
        if (!workingConnections.isEmpty()) jobScheduler.setHighestPriority(workingConnections.firstKey());
      }
    }
    if (newStatus == JPPFClientConnectionStatus.ACTIVE) {
      if (debugEnabled) log.debug("processing active status for {}", wrapper);
      wrapper.initChannelID();
      if (debugEnabled) log.debug("about to add idle channel {}", wrapper);
      jobScheduler.addIdleChannel(wrapper);
    } else {
      jobScheduler.removeIdleChannel(wrapper);
      if (newStatus.isTerminatedStatus() || newStatus == JPPFClientConnectionStatus.DISCONNECTED) queue.cancelBroadcastJobs(wrapper.getUuid());
    }
    jobScheduler.wakeUp();
  }

  @Override
  public String submitJob(final JPPFJob job) {
    return submitJob(job, null);
  }

  @Override
  public String submitJob(final JPPFJob job, final JobStatusListener listener) {
    if (closed.get()) throw new IllegalStateException("this jobmanager was closed");
    if (debugEnabled) log.debug("submitting job {}", job);
    if (listener != null) job.addJobStatusListener(listener);
    final List<Task<?>> tasks = job.getJobTasks();
    final List<Task<?>> pendingTasks = new ArrayList<>(tasks.size());
    for (final Task<?> task: tasks) {
      if (!job.getResults().hasResult(task.getPosition())) pendingTasks.add(task);
    }
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
  public boolean hasAvailableConnection() {
    final boolean localConnectionavailable;
    synchronized(this) {
      localConnectionavailable = (wrapperLocal != null) && (wrapperLocal.getStatus() == JPPFClientConnectionStatus.ACTIVE);
    }
    return jobScheduler.hasIdleChannel() || localConnectionavailable;
  }

  @Override
  public synchronized boolean isLocalExecutionEnabled() {
    return localEnabled;
  }

  @Override
  public synchronized void setLocalExecutionEnabled(final boolean localExecutionEnabled) {
    if (debugEnabled) log.debug("setting localExecutionEnabled = {}", localExecutionEnabled);
    if (this.localEnabled == localExecutionEnabled) return;
    this.localEnabled = localExecutionEnabled;
    updateLocalExecution(this.localEnabled);
  }

  /**
   * Starts or stops local execution node according to specified parameter.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  private void updateLocalExecution(final boolean localExecutionEnabled) {
    if (closed.get()) throw new IllegalStateException("this job manager was closed");
    if (localExecutionEnabled) {
      wrapperLocal = new ChannelWrapperLocal(client);
      wrapperLocal.addClientConnectionStatusListener(statusListener);
      addConnection((ChannelWrapper) wrapperLocal);
    } else if (wrapperLocal != null) {
      try {
        wrapperLocal.close();
      } finally {
        removeConnection((ChannelWrapper) wrapperLocal);
        wrapperLocal = null;
      }
    }
  }

  /**
   * Get the number of connections available for job scheduling.
   * @return the number of available connections.
   */
  public int nbAvailableConnections() {
    return jobScheduler.getNbIdleChannels();
  }

  @Override
  public Vector<JPPFClientConnection> getAvailableConnections() {
    final List<ChannelWrapper> idleChannels = jobScheduler.getIdleChannels();
    final Vector<JPPFClientConnection> availableConnections = new Vector<>(idleChannels.size());
    for (final ChannelWrapper idleChannel : idleChannels) {
      if (!idleChannel.isLocal()) {
        final AbstractChannelWrapperRemote wrapperRemote = (AbstractChannelWrapperRemote) idleChannel;
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
    synchronized(allConnections) {
      for (ChannelWrapper channel: allConnections) {
        channel.setResetting(true);
        channel.close();
      }
      allConnections.clear();
      if (jobScheduler != null) jobScheduler.clearChannels();
    }
  }

  @Override
  public void close() {
    if (debugEnabled) log.debug("closing {}", this);
    closed.set(true);
    setStopped(true);
    wakeUp();
    if (jobScheduler != null) {
      jobScheduler.setStopped(true);
      jobScheduler.wakeUp();
    }
    queue.close();
    synchronized(allConnections) {
      for (ChannelWrapper channel: allConnections) channel.close();
      allConnections.clear();
    }
  }

  @Override
  public LoadBalancingInformation getLoadBalancerSettings() {
    synchronized(loadBalancingInformationLock) {
      if (currentLoadBalancingInformation == null) {
        final LoadBalancingInformation info = bundlerFactory.getCurrentInfo();
        currentLoadBalancingInformation = new LoadBalancingInformation(info.getAlgorithm(), info.getParameters(), bundlerFactory.getBundlerProviderNames());
      }
      return currentLoadBalancingInformation;
    }
  }

  @Override
  public void setLoadBalancerSettings(final String algorithm, final Properties parameters) throws Exception {
    if (algorithm == null) throw new IllegalArgumentException("Error: no algorithm specified (null value)");
    if (!bundlerFactory.getBundlerProviderNames().contains(algorithm)) throw new IllegalArgumentException("Error: unknown algorithm '" + algorithm + '\'');
    final TypedProperties props = (parameters == null) ? new TypedProperties() : new TypedProperties(parameters);
    synchronized(loadBalancingInformationLock) {
      final LoadBalancingInformation lbi = new LoadBalancingInformation(algorithm, props, currentLoadBalancingInformation.getAlgorithmNames());
      currentLoadBalancingInformation = bundlerFactory.setAndGetCurrentInfo(lbi);
    }
  }

  /**
   * @return the job scheuler.
   */
  public JobScheduler getJobScheduler() {
    return jobScheduler;
  }

  /**
   * @return the job queue.
   */
  public JPPFPriorityQueue getQueue() {
    return queue;
  }
}
