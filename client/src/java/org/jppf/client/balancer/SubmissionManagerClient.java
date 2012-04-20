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

import org.jppf.client.*;
import org.jppf.client.balancer.job.JPPFJobManager;
import org.jppf.client.balancer.queue.JPPFPriorityQueue;
import org.jppf.client.balancer.queue.QueueEvent;
import org.jppf.client.balancer.queue.QueueListener;
import org.jppf.client.balancer.queue.TaskQueueChecker;
import org.jppf.client.balancer.stats.JPPFDriverStatsManager;
import org.jppf.client.event.*;
import org.jppf.client.submission.SubmissionManager;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.ThreadSynchronization;

import java.util.*;

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
   * The job manager.
   */
  private final JPPFJobManager jobManager;
  /**
   * A reference to the tasks queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * The statistics manager.
   */
  private JPPFDriverStatsManager statsManager = new JPPFDriverStatsManager();
  /**
   * The bundler factory.
   */
  private JPPFBundlerFactory bundlerFactory = new JPPFBundlerFactory();
  /**
   * Task that dispatches queued jobs to available nodes.
   */
  private final TaskQueueChecker taskQueueChecker;
  /**
   *
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
  private boolean localEnabled = JPPFConfiguration.getProperties().getBoolean("jppf.local.execution.enabled", false);
  /**
   * Wrapper for local execution node.
   */
  private ChannelWrapperLocal wrapperLocal = null;

  /**
   * Instantiates client submission manager.
   * @param client JPPF client that manages connections to the JPPF drivers.
   * @throws Exception if any error occurs.
   */
  public SubmissionManagerClient(final AbstractGenericClient client) throws Exception
  {
    if (client == null) throw new IllegalArgumentException("client is null");

    Bundler bundler = bundlerFactory.createBundlerFromJPPFConfiguration();

    this.jobManager = new JPPFJobManager();
    this.queue = new JPPFPriorityQueue(jobManager, this);
    this.jobManager.setQueue(this.queue);

    taskQueueChecker = new TaskQueueChecker(queue, statsManager, jobManager);
    taskQueueChecker.setBundler(bundler);

    this.queue.addQueueListener(new QueueListener()
    {
      @Override
      public void newBundle(final QueueEvent event)
      {
//        selector.wakeup();
        taskQueueChecker.wakeUp();
      }
    });
    new Thread(taskQueueChecker, "TaskQueueChecker").start();

    List<JPPFClientConnection> connections = client.getAllConnections();
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

    for (JPPFClientConnection connection : connections)
    {
      addConnection(connection);
    }
  }

  /**
   * Create and register new local connection.
   * @return connection wrapper for new local connection.
   */
  protected synchronized ChannelWrapper<?> addConnectionLocal()
  {
    ChannelWrapper wrapper = new ChannelWrapperLocal();
//    try
//    {
//      JMXDriverConnectionWrapper jmxConnection = i.getJmxConnection();
//      JPPFSystemInformation systemInfo = jmxConnection.systemInformation();
//      wrapper.setNodeInfo(systemInfo);
//      JPPFManagementInfo info = new JPPFManagementInfo(jmxConnection.getHost(), jmxConnection.getPort(), jmxConnection.getId(), JPPFManagementInfo.DRIVER);
//      info.setSystemInfo(systemInfo);
//      wrapper.setManagementInfo(info);
//    }
//    catch (Exception e)
//    {
//      e.printStackTrace();
//    } finally {
    addConnection(wrapper);
//    }

    return wrapper;
  }

  /**
   * Add the specified connection wrapper to the list of connections handled by this manager.
   * @param wrapper the connection wrapper to add.
   */
  protected synchronized void addConnection(final ChannelWrapper<?> wrapper)
  {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");

    System.out.println("addConnection: " + wrapper);
    allConnections.add(wrapper);
    wrapper.addClientConnectionStatusListener(statusListener);
    updateConnectionStatus(wrapper, JPPFClientConnectionStatus.NEW, wrapper.getStatus());
  }

  /**
   * Remove the specified connection wrapper from the list of connections handled by this manager.
   * @param wrapper the connection wrapper to remove.
   */
  protected synchronized void removeConnection(final ChannelWrapper<?> wrapper)
  {
    if (wrapper == null) throw new IllegalArgumentException("wrapper is null");

    System.out.println("removeConnection: " + wrapper);
    try
    {
      wrapper.removeClientConnectionStatusListener(statusListener);
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
    AbstractJPPFClientConnection connection = (AbstractJPPFClientConnection) cnn;

    ChannelWrapper wrapper = wrapperMap.get(connection);
    if (wrapper == null)
    {
      try
      {
        wrapper = new ChannelWrapperRemote(connection);
        JPPFClientConnectionImpl i = (JPPFClientConnectionImpl) connection;
        JMXDriverConnectionWrapper jmxConnection = i.getJmxConnection();

        JPPFSystemInformation systemInfo = i.getSystemInfo();
        wrapper.setSystemInfo(systemInfo);
        JPPFManagementInfo info = new JPPFManagementInfo(jmxConnection.getHost(), jmxConnection.getPort(), jmxConnection.getId(), JPPFManagementInfo.DRIVER);
        info.setSystemInfo(systemInfo);
        wrapper.setManagementInfo(info);
      }
      catch (Throwable e)
      {
        e.printStackTrace();
      }
      finally
      {
        wrapperMap.put(connection, wrapper);
        addConnection(wrapper);
      }
    }

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
        JMXDriverConnectionWrapper jmxConnection = ((JPPFClientConnectionImpl) connection).getJmxConnection();

        wrapper.setSystemInfo(systemInfo);
        JPPFManagementInfo info = new JPPFManagementInfo(jmxConnection.getHost(), jmxConnection.getPort(), jmxConnection.getId(), JPPFManagementInfo.DRIVER);
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

    boolean oldAvailable = oldStatus == JPPFClientConnectionStatus.ACTIVE;
    boolean newAvailable = newStatus == JPPFClientConnectionStatus.ACTIVE;

    if (newAvailable && !oldAvailable) taskQueueChecker.addIdleChannel(wrapper);
    if (oldAvailable && !newAvailable) taskQueueChecker.removeIdleChannel(wrapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String submitJob(final JPPFJob job)
  {
    return submitJob(job, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String submitJob(final JPPFJob job, final SubmissionStatusListener listener)
  {
//    ClientTaskBundle bundle = createBundle(job);

    queue.addBundle(new ClientJob(job, job.getTasks()));
    return job.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String resubmitJob(final JPPFJob job)
  {
    return submitJob(job);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized boolean hasAvailableConnection()
  {
    return taskQueueChecker.hasIdleChannel() || wrapperLocal != null && wrapperLocal.getStatus() == JPPFClientConnectionStatus.ACTIVE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized boolean isLocalExecutionEnabled()
  {
    return localEnabled;
  }

  /**
   * {@inheritDoc}
   */
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
    if (localExecutionEnabled)
    {
      wrapperLocal = new ChannelWrapperLocal();
      addConnection(wrapperLocal);
    }
    else
    {
      if (wrapperLocal != null)
      {
        try
        {
          wrapperLocal.stopNode();
        }
        finally
        {
          removeConnection(wrapperLocal);
          wrapperLocal = null;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
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
}
