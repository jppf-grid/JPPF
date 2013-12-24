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
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.event.*;
import org.jppf.execute.*;
import org.jppf.management.*;
import org.jppf.node.*;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.Task;
import org.jppf.server.node.NodeExecutionManagerImpl;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Context associated with a local channel serving state and tasks submission.
 * @author Martin JANDA
 */
public class ChannelWrapperLocal extends ChannelWrapper implements ClientConnectionStatusHandler, NodeInternal
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
  private final NodeExecutionManager executionManager;
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
  private final List<ClientConnectionStatusListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Default initializer for local channel wrapper.
   */
  public ChannelWrapperLocal()
  {
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("LocalChannelWrapper"));
    executionManager = new NodeExecutionManagerImpl(this, "jppf.local.execution.threads", "jppf.local.execution.threads");
    priority = JPPFConfiguration.getProperties().getInt("jppf.local.execution.priority", 0);
    systemInfo = new JPPFSystemInformation(getConnectionUuid(), true, false);
    managementInfo = new JPPFManagementInfo("local", -1, getConnectionUuid(), JPPFManagementInfo.NODE | JPPFManagementInfo.LOCAL, false);
    managementInfo.setSystemInfo(systemInfo);
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
    listeners.add(listener);
  }

  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Notify all listeners that the status of this connection has changed.
   * @param oldStatus the connection status before the change.
   * @param newStatus the connection status after the change.
   */
  protected void fireStatusChanged(final JPPFClientConnectionStatus oldStatus, final JPPFClientConnectionStatus newStatus)
  {
    if (oldStatus == newStatus) return;
    ClientConnectionStatusEvent event = new ClientConnectionStatusEvent(this, oldStatus);
    for (ClientConnectionStatusListener listener : listeners) listener.statusChanged(event);
  }

  @Override
  @SuppressWarnings("unchecked")
  public JPPFFuture<?> submit(final ClientTaskBundle bundle) {
    if (debugEnabled) log.debug("locally submitting {}", bundle);
    setStatus(JPPFClientConnectionStatus.EXECUTING);
    JPPFFutureTask<?> task = new JPPFFutureTask(new LocalRunnable(getBundler(), bundle), null) {
      @Override
      public boolean cancel(final boolean mayInterruptIfRunning) {
        if (debugEnabled) log.debug("requesting cancel of jobId=" + bundle.getUuid());
        if (super.cancel(mayInterruptIfRunning)) {
          try {
            executionManager.cancelAllTasks(true, false);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
          return true;
        }
        return false;
      }
    };
    bundle.jobDispatched(this, task);
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
    sb.append(getClass().getSimpleName());
    sb.append("[status=").append(status);
    sb.append(", connectionUuid='").append(connectionUuid).append('\'');
    sb.append(']');
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
      List<Task<?>> tasks = this.bundle.getTasksL();
      try
      {
        long start = System.nanoTime();
        DataProvider dataProvider = bundle.getJob().getDataProvider();
        for (Task<?> task : tasks)
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
  public NodeConnection getNodeConnection()
  {
    return null;
  }

  @Override
  public void stopNode()
  {
    setStatus(JPPFClientConnectionStatus.DISCONNECTED);
    executionManager.shutdown();
  }

  /**
   * This method returns null.
   * @return <code>null</code>.
   * @throws Exception if any error occurs.
   */
  @Override
  public JMXServer getJmxServer() throws Exception
  {
    return null;
  }

  /**
   * This method returns null..
   * @return <code>null</code>.
   */
  @Override
  public LifeCycleEventHandler getLifeCycleEventHandler()
  {
    return null;
  }

  @Override
  public void run()
  {
  }

  @Override
  public void close()
  {
    if (debugEnabled) log.debug("closing " + this);
    super.close();
    try
    {
      stopNode();
    }
    finally
    {
      listeners.clear();
    }
  }

  /**
   * This implementation does nothing.
   * @return <code>null</code>.
   */
  @Override
  public AbstractJPPFClassLoader resetTaskClassLoader()
  {
    return null;
  }

  @Override
  public boolean isOffline()
  {
    return false;
  }

  @Override
  public NodeExecutionManager getExecutionManager()
  {
    return executionManager;
  }
}
