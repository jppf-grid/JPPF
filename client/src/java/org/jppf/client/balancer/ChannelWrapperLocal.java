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

import java.util.*;
import java.util.concurrent.Executors;

import org.jppf.JPPFException;
import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.*;
import org.jppf.comm.socket.SocketWrapper;
import org.jppf.execute.ExecutorStatus;
import org.jppf.execute.JPPFFuture;
import org.jppf.execute.JPPFFutureTask;
import org.jppf.management.*;
import org.jppf.node.Node;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.server.node.NodeExecutionManagerImpl;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context associated with a local channel serving state and tasks submission.
 * @author Martin JANDA
 */
public class ChannelWrapperLocal extends ChannelWrapper<ClientTaskBundle> implements ClientConnectionStatusHandler, Node
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ChannelWrapperLocal.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The task execution manager for this wrapper.
   */
  private final NodeExecutionManagerImpl executionManager;
  /**
   * Status of the connection.
   */
  private JPPFClientConnectionStatus status = JPPFClientConnectionStatus.ACTIVE;
  /**
   * Unique ID for the connection.
   */
  private final String connectionUuid = UUID.randomUUID().toString();
  /**
   * List of status listeners for this connection.
   */
  private final List<ClientConnectionStatusListener> listeners = new ArrayList<ClientConnectionStatusListener>();
  /**
   * The jmx server that handles administration and monitoring functions for this node.
   */
  private static JMXServer jmxServer = null;
  /**
   * Handles the firing of node life cycle events and the listeners that subscribe to these events.
   */
  protected final LifeCycleEventHandler lifeCycleEventHandler;

  /**
   * Default initializer for local channel wrapper.
   */
  public ChannelWrapperLocal()
  {
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("LocalChannelWrapper-"));
    executionManager = new NodeExecutionManagerImpl(this, "jppf.local.execution.threads");
    lifeCycleEventHandler = new LifeCycleEventHandler(this);

    JPPFSystemInformation info = new JPPFSystemInformation(getConnectionUuid());
    info.populate(false);

    JPPFManagementInfo managementInfo = new JPPFManagementInfo("local", -1, getConnectionUuid(), JPPFManagementInfo.NODE);
    managementInfo.setSystemInfo(info);
    setSystemInfo(info);
    setManagementInfo(managementInfo);
  }

  @Override
  public String getUuid()
  {
    return connectionUuid;
  }

  @Override
  public String getConnectionUuid()
  {
    return connectionUuid;
  }

  @Override
  public JPPFClientConnectionStatus getStatus()
  {
    return status;
  }

  @Override
  public void setStatus(final JPPFClientConnectionStatus status)
  {
    ExecutorStatus oldExecutionStatus = getExecutionStatus();
    JPPFClientConnectionStatus oldValue = this.status;
    this.status = status;
    fireStatusChanged(oldValue, this.status);
    ExecutorStatus newExecutionStatus = getExecutionStatus();
    fireExecutionStatusChanged(oldExecutionStatus, newExecutionStatus);
  }

  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    synchronized (listeners)
    {
      listeners.add(listener);
    }
  }

  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    synchronized (listeners)
    {
      listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  protected void fireStatusChanged(final JPPFClientConnectionStatus oldStatus, final JPPFClientConnectionStatus newStatus)
  {
    if (oldStatus == newStatus) return;
    ClientConnectionStatusListener[] temp;
    synchronized (listeners)
    {
      temp = listeners.toArray(new ClientConnectionStatusListener[listeners.size()]);
    }
    ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (ClientConnectionStatusListener listener : temp)
    {
      listener.statusChanged(event);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public JPPFFuture<?> submit(final ClientTaskBundle bundle)
  {
    setStatus(JPPFClientConnectionStatus.EXECUTING);
    JPPFFutureTask<?> task = new JPPFFutureTask(new LocalRunnable(getBundler(), bundle), null) {
      @Override
      public boolean cancel(final boolean mayInterruptIfRunning)
      {
        if (super.cancel(mayInterruptIfRunning))
        {
          try
          {
            executionManager.cancelAllTasks(true, false);
          }
          catch (Exception e)
          {
            log.error(e.getMessage(), e);
          }
          return true;
        }
        return false;
      }
    };
    executor.execute(task);
    return task;
  }

  @Override
  public boolean isLocal()
  {
    return true;
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("ChannelWrapperLocal");
    sb.append("{status=").append(status);
    sb.append(", connectionUuid='").append(connectionUuid).append('\'');
    sb.append('}');
    return sb.toString();
  }

  /**
   *
   */
  private class LocalRunnable implements Runnable
  {
    /**
     * The task bundle to execute.
     */
    private final ClientTaskBundle bundle;
    /**
     * Bundler used to schedule tasks for the corresponding node.
     */
    private final Bundler bundler;

    /**
     * Initialize this runnable for local execution.
     * @param bundler    the bundler to send the resulting statistics to.
     * @param bundle the execution to perform.
     */
    public LocalRunnable(final Bundler bundler, final ClientTaskBundle bundle)
    {
      this.bundler = bundler;
      this.bundle = bundle;
    }

    @Override
    public void run()
    {
      Exception exception = null;
      List<JPPFTask> tasks = this.bundle.getTasksL();
      try
      {
        long start = System.nanoTime();
        DataProvider dataProvider = bundle.getJob().getDataProvider();
        for (JPPFTask task : tasks)
        {
          task.setDataProvider(dataProvider);
        }
        executionManager.execute(bundle, tasks);
        bundle.resultsReceived(tasks);

        double elapsed = System.nanoTime() - start;
        bundler.feedback(tasks.size(), elapsed);
      }
      catch (Throwable t)
      {
        log.error(t.getMessage(), t);
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        bundle.resultsReceived(t);
      }
      finally
      {
        bundle.taskCompleted(exception);
        setStatus(JPPFClientConnectionStatus.ACTIVE);
        bundle.getClientJob().removeChannel(ChannelWrapperLocal.this);
      }
    }
  }

  @Override
  public SocketWrapper getSocketWrapper()
  {
    return null;
  }

  @Override
  public void setSocketWrapper(final SocketWrapper socketWrapper)
  {
  }

  @Override
  public void stopNode()
  {
    setStatus(JPPFClientConnectionStatus.DISCONNECTED);
    executionManager.shutdown();
  }

  /**
   * Get the jmx server that handles administration and monitoring functions for this node.
   * @return a <code>JMXServerImpl</code> instance.
   * @throws Exception if any error occurs.
   */
  @Override
  public JMXServer getJmxServer() throws Exception
  {
    synchronized (this)
    {
      if ((jmxServer == null) || jmxServer.isStopped())
      {
        boolean ssl = JPPFConfiguration.getProperties().getBoolean("jppf.ssl.enabled", false);
        jmxServer = JMXServerFactory.createServer(getUuid(), ssl);
        jmxServer.start(getClass().getClassLoader());
        System.out.println("JPPF Node management initialized");
      }
    }
    return jmxServer;
  }

  /**
   * Get the object that handles the firing of node life cycle events and the listeners that subscribe to these events.
   * @return an instance of <code>LifeCycleEventHandler</code>.
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler()
  {
    return lifeCycleEventHandler;
  }

  @Override
  public void run()
  {
  }

  @Override
  public void close()
  {
    super.close();
    if (executionManager != null) executionManager.shutdown();
  }
}
