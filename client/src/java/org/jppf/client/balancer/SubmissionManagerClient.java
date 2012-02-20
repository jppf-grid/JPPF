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
import org.jppf.server.protocol.JPPFJobSLA;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.spi.JPPFBundlerFactory;
import org.jppf.utils.ThreadSynchronization;
import org.jppf.utils.TraversalList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   * The JPPF client that manages connections to the JPPF drivers.
   */
  private final AbstractGenericClient client;
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
   * Instantiates client submission manager.
   * @param client JPPF client that manages connections to the JPPF drivers.
   * @throws Exception if any error occurs.
   */
  public SubmissionManagerClient(final AbstractGenericClient client) throws Exception
  {
    if (client == null) throw new IllegalArgumentException("client is null");

    this.client = client;

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

//    addConnectionLocal();
//    addConnectionLocal();
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
      wrapper = new ChannelWrapperRemote(connection);
      JPPFClientConnectionImpl i = (JPPFClientConnectionImpl) connection;
      try
      {
        JMXDriverConnectionWrapper jmxConnection = i.getJmxConnection();
        long timeout = 5000L;
        long start = System.currentTimeMillis();
        long elapsed;
        while (!jmxConnection.isConnected() && ((elapsed = System.currentTimeMillis() - start) < timeout))
          jmxConnection.goToSleep(timeout - elapsed);
        System.out.println("Connection .connected: " + jmxConnection.isConnected() + "\t after: " + (System.currentTimeMillis() - start));
        if (jmxConnection.isConnected())
        {
          JPPFSystemInformation systemInfo = jmxConnection.systemInformation();
          wrapper.setSystemInfo(systemInfo);
          JPPFManagementInfo info = new JPPFManagementInfo(jmxConnection.getHost(), jmxConnection.getPort(), jmxConnection.getId(), JPPFManagementInfo.DRIVER);
          info.setSystemInfo(systemInfo);
          wrapper.setManagementInfo(info);
        }
      }
      catch (Exception e)
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
    updateConnectionStatus(wrapperMap.get(connection), oldStatus);
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
    System.out.printf("SubmissionManager.updateConnectionStatus  %s: %s -> %s%n", wrapper, oldStatus, newStatus);
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
    System.out.println("SubmissionManager.submitJob: " + job);
    ClientTaskBundle bundle = createBundle(job);

    queue.addBundle(new ClientJob(bundle, job.getTasks()));
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
  public AbstractGenericClient getClient()
  {
    return client;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
  }

  /**
   * Create a task bundle for the specified job.
   * @param job the job to use as a base.
   * @return a JPPFTaskBundle instance.
   */
  private static ClientTaskBundle createBundle(final JPPFJob job)
  {
    String requestUuid = job.getUuid();
    ClientTaskBundle bundle = new ClientTaskBundle(job);
    bundle.setRequestUuid(requestUuid);

    int count = job.getTasks().size() - job.getResults().size();
    TraversalList<String> uuidPath = new TraversalList<String>();
//    uuidPath.add(client.getUuid());
    bundle.setUuidPath(uuidPath);
//    if (debugEnabled) log.debug("[client: " + name + "] sending job '" + job.getName() + "' with " + count + " tasks, uuidPath=" + uuidPath.getList());
    bundle.setTaskCount(count);
    bundle.setRequestUuid(job.getUuid());
    bundle.setName(job.getName());
    bundle.setUuid(job.getUuid());
    if (job.getSLA() instanceof JPPFJobSLA)
    {
      bundle.setSLA(((JPPFJobSLA) job.getSLA()).copy());
    }
    else
    {
      bundle.setSLA(job.getSLA());
    }
    bundle.setMetadata(job.getMetadata());

//    ClassLoader cl = null;
//    if (!job.getTasks().isEmpty())
//    {
//      Object task = job.getTasks().get(0);
//      if (task instanceof JPPFAnnotatedTask) task = ((JPPFAnnotatedTask) task).getTaskObject();
//      cl = task.getClass().getClassLoader();
//      connection.getClient().addRequestClassLoader(requestUuid, cl);
//      if (log.isDebugEnabled()) log.debug("adding request class loader=" + cl + " for uuid=" + requestUuid + ", from class " + task.getClass());
//    }
    return bundle;
  }
}
