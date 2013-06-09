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

package org.jppf.client.balancer;

import java.util.*;
import java.util.concurrent.atomic.*;

import org.jppf.client.*;
import org.jppf.client.balancer.queue.*;
import org.jppf.client.balancer.stats.JPPFClientStatsManager;
import org.jppf.client.event.*;
import org.jppf.client.submission.SubmissionManager;
import org.jppf.management.*;
import org.jppf.queue.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.*;

/**
 * This task provides asynchronous management of tasks submitted through the resource adapter.
 * It relies on a queue where job are first added, then submitted when a connection becomes available.
 * It also provides methods to check the status of a submission and retrieve the results.
 * @author Laurent Cohen
 * @author Martin JANDA
 */
public class SubmissionManagerClient extends ThreadSynchronization implements SubmissionManager
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SubmissionManagerClient.class);
  /**
   * A reference to the tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * The statistics manager.
   */
  private final JPPFClientStatsManager statsManager = new JPPFClientStatsManager();
  /**
   * The bundler factory.
   */
  private final JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory(JPPFBundlerFactory.Defaults.CLIENT);
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final TaskQueueChecker<ChannelWrapper<?>> taskQueueChecker;
  /**
   * Mapping client connections to channel wrapper.
   */
  private final Map<AbstractJPPFClientConnection, ChannelWrapper<?>> wrapperMap = new HashMap<AbstractJPPFClientConnection, ChannelWrapper<?>>();
  /**
   * A list of all the connections.
   */
  private final List<ChannelWrapper<?>> allConnections = new ArrayList<ChannelWrapper<?>>();
  /**
   * Listener used for monitoring state changes.
   */
  private final ClientConnectionStatusListener statusListener = new ClientConnectionStatusListener()
  {
    @Override
    public void statusChanged(final ClientConnectionStatusEvent event)
    {
      if (event.getSource() instanceof JPPFClientConnection)
      {
        updateConnectionStatus(((JPPFClientConnection) event.getSource()), event.getOldStatus());
      }
      else if (event.getSource() instanceof ChannelWrapper)
      {
        updateConnectionStatus((ChannelWrapper<?>) event.getSource(), event.getOldStatus());
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
   * Counts the current number of connections with ACTIVE or EXECUTING status. 
   */
  private final AtomicInteger nbWorkingConnections = new AtomicInteger(0);
  /**
   * Determines whether this submission manager has been closed. 
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Instantiates client submission manager.
   * @param client JPPF client that manages connections to the JPPF drivers.
   * @throws Exception if any error occurs.
   */
  public SubmissionManagerClient(final AbstractGenericClient client) throws Exception
  {
    if (client == null) throw new IllegalArgumentException("client is null");

    this.localEnabled = client.getConfig().getBoolean("jppf.local.execution.enabled", false);
    Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();
    this.queue = new JPPFPriorityQueue(this);

    taskQueueChecker = new TaskQueueChecker<ChannelWrapper<?>>(queue, statsManager);
    taskQueueChecker.setBundler(bundler);

    this.queue.addQueueListener(new QueueListener<ClientJob, ClientJob, ClientTaskBundle>()
    {
      @Override
      public void newBundle(final QueueEvent<ClientJob, ClientJob, ClientTaskBundle> event)
      {
        taskQueueChecker.wakeUp();
      }
    });
    new Thread(taskQueueChecker, "TaskQueueChecker").start();

    client.addClientListener(new ClientListener()
    {
      @Override
      public void newConnection(final ClientEvent event)
      {
        addConnection(event.getConnection());
      }

      @Override
      public void connectionFailed(final ClientEvent event)
      {
        removeConnection(event.getConnection());
      }
    });

    updateLocalExecution(this.localEnabled);

    List<JPPFClientConnection> connections = client.getAllConnections();
    for (JPPFClientConnection connection : connections)
    {
      addConnection(connection);
    }
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param wrapper the connection wrapper to add.
   */
  protected synchronized void addConnection(final ChannelWrapper<?> wrapper)
  {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");
    if (closed.get()) throw new IllegalStateException("this submission manager was closed");

    allConnections.add(wrapper);
    updateConnectionStatus(wrapper, JPPFClientConnectionStatus.NEW, wrapper.getStatus());
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param wrapper the connection wrapper to remove.
   */
  protected synchronized void removeConnection(final ChannelWrapper<?> wrapper)
  {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");

    try
    {
      updateConnectionStatus(wrapper, wrapper.getStatus(), JPPFClientConnectionStatus.DISCONNECTED);
    }
    finally
    {
      allConnections.remove(wrapper);
    }
  }

  /**
   * Add the specified client connection to the list of connections handled by this manager.
   * @param cnn the client connection to add.
   * @return wrapper for the added client connection.
   */
  protected synchronized ChannelWrapper<?> addConnection(final JPPFClientConnection cnn)
  {
    if (log.isDebugEnabled()) log.debug("adding connection " + cnn);
    if (closed.get()) throw new IllegalStateException("this submission manager was closed");
    AbstractJPPFClientConnection connection = (AbstractJPPFClientConnection) cnn;

    ChannelWrapper wrapper = wrapperMap.get(connection);
    if (wrapper == null)
    {
      try
      {
        wrapper = new ChannelWrapperRemote(connection);
        JMXDriverConnectionWrapper jmxConnection = connection.getJmxConnection();
        JPPFSystemInformation systemInfo = connection.getSystemInfo();
        if (systemInfo != null) wrapper.setSystemInformation(systemInfo);
        JPPFManagementInfo info = new JPPFManagementInfo(jmxConnection.getHost(), jmxConnection.getPort(),
            ((AbstractJPPFClientConnection) cnn).getDriverUuid(), JPPFManagementInfo.DRIVER, cnn.isSSL());
        if (systemInfo != null) info.setSystemInfo(systemInfo);
        wrapper.setManagementInfo(info);
      }
      catch (Throwable e)
      {
        log.error("Error while adding connection " + cnn, e);
      }
      finally
      {
        wrapperMap.put(connection, wrapper);
        addConnection(wrapper);
      }
    }
    if (log.isDebugEnabled()) log.debug("end of adding connection " + cnn);
    return wrapper;
  }

  /**
   * Remove the specified client connection from the list of connections handled by this manager.
   * @param cnn the client connection to remove.
   * @return wrapper for the removed client connection or null.
   */
  protected synchronized ChannelWrapper removeConnection(final JPPFClientConnection cnn)
  {
    AbstractJPPFClientConnection connection = (AbstractJPPFClientConnection) cnn;

    ChannelWrapper<?> wrapper = wrapperMap.remove(connection);
    if (wrapper != null)
    {
      removeConnection(wrapper);
    }
    return wrapper;
  }

  /**
   * Get all the client connections handled by this manager.
   * @return a list of <code>ChannelWrapper</code> instances.
   */
  public synchronized List<ChannelWrapper> getAllConnections()
  {
    return new ArrayList<ChannelWrapper>(allConnections);
  }

  /**
   * Dtermine whether there is at east one connection, idle or not.
   * @return <code>true</code> if there is at least one connection, <code>false</code> otherwise.
   */
  public synchronized boolean hasWorkingConnection()
  {
    return nbWorkingConnections.get() > 0;
  }

  /**
   * @param cnn       the client connection.
   * @param oldStatus the connection status before the change.
   */
  private void updateConnectionStatus(final JPPFClientConnection cnn, final JPPFClientConnectionStatus oldStatus)
  {
    AbstractJPPFClientConnection connection = (AbstractJPPFClientConnection) cnn;
    ChannelWrapper<?> wrapper = wrapperMap.get(connection);
    if (wrapper != null)
    {
      if (oldStatus == JPPFClientConnectionStatus.CONNECTING && wrapper.getStatus() == JPPFClientConnectionStatus.ACTIVE)
      {
        JPPFSystemInformation systemInfo = connection.getSystemInfo();
        JMXDriverConnectionWrapper jmxConnection = connection.getJmxConnection();

        wrapper.setSystemInformation(systemInfo);
        JPPFManagementInfo info = new JPPFManagementInfo(jmxConnection.getHost(), jmxConnection.getPort(), jmxConnection.getId(), JPPFManagementInfo.DRIVER, cnn.isSSL());
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
  private void updateConnectionStatus(final ChannelWrapper<?> wrapper, final JPPFClientConnectionStatus oldStatus)
  {
    if (wrapper == null) return;
    updateConnectionStatus(wrapper, oldStatus, wrapper.getStatus());
  }

  /**
   * @param wrapper   the connection wrapper.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  private void updateConnectionStatus(final ChannelWrapper<?> wrapper, final JPPFClientConnectionStatus oldStatus, final JPPFClientConnectionStatus newStatus)
  {
    if (oldStatus == null) throw new IllegalArgumentException("oldStatus is null");
    if (newStatus == null) throw new IllegalArgumentException("newStatus is null");
    if (wrapper == null || oldStatus == newStatus) return;

    if (newStatus == JPPFClientConnectionStatus.ACTIVE)
      taskQueueChecker.addIdleChannel(wrapper);
    else
    {
      taskQueueChecker.removeIdleChannel(wrapper);
      if(newStatus == JPPFClientConnectionStatus.FAILED || newStatus == JPPFClientConnectionStatus.DISCONNECTED) queue.cancelBroadcastJobs(wrapper.getUuid());
    }
    boolean bNew = (newStatus == JPPFClientConnectionStatus.ACTIVE) || (newStatus == JPPFClientConnectionStatus.EXECUTING);
    boolean bOld = (oldStatus == JPPFClientConnectionStatus.ACTIVE) || (oldStatus == JPPFClientConnectionStatus.EXECUTING);
    if (bNew && !bOld) nbWorkingConnections.incrementAndGet();
    else if (!bNew && bOld) nbWorkingConnections.decrementAndGet();
  }

  @Override
  public String submitJob(final JPPFJob job)
  {
    return submitJob(job, null);
  }

  @Override
  public String submitJob(final JPPFJob job, final SubmissionStatusListener listener)
  {
    if (closed.get()) throw new IllegalStateException("this submission manager was closed");
    List<JPPFTask> pendingTasks = new ArrayList<JPPFTask>();
    if ((listener != null) && (job.getResultListener() instanceof JPPFResultCollector))
    {
      ((JPPFResultCollector) job.getResultListener()).addSubmissionStatusListener(listener);
    }
    List<JPPFTask> tasks = job.getTasks();
    for (JPPFTask task: tasks) if (!job.getResults().hasResult(task.getPosition())) pendingTasks.add(task);
    queue.addBundle(new ClientJob(job, pendingTasks));
    return job.getUuid();
  }

  @Override
  public String resubmitJob(final JPPFJob job)
  {
    return submitJob(job);
  }

  /**
   * Cancel the job with the specified id.
   * @param jobId the id of the job to cancel.
   * @throws Exception if any error occurs.
   * @see org.jppf.server.job.management.DriverJobManagementMBean#cancelJob(java.lang.String)
   * @return a <code>true</code> when cancel was successful <code>false</code> otherwise.
   */
  public boolean cancelJob(final String jobId) throws Exception
  {
    queue.cancelJob(jobId);
    return true;
  }

  @Override
  public synchronized boolean hasAvailableConnection()
  {
    return taskQueueChecker.hasIdleChannel() || wrapperLocal != null && wrapperLocal.getStatus() == JPPFClientConnectionStatus.ACTIVE;
  }

  @Override
  public synchronized boolean isLocalExecutionEnabled()
  {
    return localEnabled;
  }

  @Override
  public synchronized void setLocalExecutionEnabled(final boolean localExecutionEnabled)
  {
    if (this.localEnabled == localExecutionEnabled) return;
    this.localEnabled = localExecutionEnabled;
    updateLocalExecution(this.localEnabled);
  }

  /**
   * Starts or stops local execution node according to specified parameter.
   * @param localExecutionEnabled <code>true</code> to enable local execution, <code>false</code> otherwise
   */
  protected synchronized void updateLocalExecution(final boolean localExecutionEnabled)
  {
    if (closed.get()) throw new IllegalStateException("this submission manager was closed");
    if (localExecutionEnabled)
    {
      wrapperLocal = new ChannelWrapperLocal();
      wrapperLocal.addClientConnectionStatusListener(statusListener);
      addConnection(wrapperLocal);
    }
    else
    {
      if (wrapperLocal != null)
      {
        try
        {
          wrapperLocal.close();
        }
        finally
        {
          removeConnection(wrapperLocal);
          wrapperLocal = null;
        }
      }
    }
  }

  @Override
  public Vector<JPPFClientConnection> getAvailableConnections()
  {
    List<ChannelWrapper<?>> idleChannels = taskQueueChecker.getIdleChannels();
    Vector<JPPFClientConnection> availableConnections = new Vector<JPPFClientConnection>(idleChannels.size());
    for (ChannelWrapper<?> idleChannel : idleChannels)
    {
      if (idleChannel instanceof ChannelWrapperRemote)
      {
        ChannelWrapperRemote wrapperRemote = (ChannelWrapperRemote) idleChannel;
        availableConnections.add(wrapperRemote.getChannel());
      }
    }
    return availableConnections;
  }

  @Override
  public ClientConnectionStatusListener getClientConnectionStatusListener()
  {
    return this.statusListener;
  }

  @Override
  public void close()
  {
    closed.set(true);
    setStopped(true);
    wakeUp();
    if (taskQueueChecker != null)
    {
      taskQueueChecker.setStopped(true);
      taskQueueChecker.wakeUp();
    }
    queue.close();
    synchronized(this)
    {
      for (ChannelWrapper channel: allConnections) channel.close();
      allConnections.clear();
    }
  }
}
