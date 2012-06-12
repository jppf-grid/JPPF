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

import java.io.NotSerializableException;
import java.util.List;
import java.util.concurrent.*;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.balancer.utils.*;
import org.jppf.client.event.*;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.management.*;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * Context associated with a remote channel serving state and tasks submission.
 * @author Martin JANDA
 */
public class ChannelWrapperRemote extends ChannelWrapper implements ClientConnectionStatusHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ChannelWrapperRemote.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The channel to the driver to use.
   */
  private final AbstractJPPFClientConnection channel;
  /**
   * Unique identifier of the client.
   */
  protected String uuid = null;

  /**
   * Default initializer for remote channel wrapper.
   * @param channel to the driver to use.
   */
  public ChannelWrapperRemote(final AbstractJPPFClientConnection channel)
  {
    if (channel == null) throw new IllegalArgumentException("channel is null");

    this.channel = channel;
    this.uuid = channel.getUuid();

    JPPFSystemInformation info = new JPPFSystemInformation(this.uuid);

    JPPFManagementInfo managementInfo = new JPPFManagementInfo("remote", -1, getConnectionUuid(), JPPFManagementInfo.DRIVER);
    managementInfo.setSystemInfo(info);
    super.setSystemInfo(info);
    setManagementInfo(managementInfo);
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("channel-" + channel.getName() + "-"));
  }

  @Override
  public void setSystemInfo(final JPPFSystemInformation systemInfo)
  {
    if (systemInfo != null && uuid == null)
    {
      uuid = systemInfo.getUuid().getProperty("jppf.uuid");
      if (uuid != null && uuid.isEmpty()) uuid = null;
    }
    super.setSystemInfo(systemInfo);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUuid()
  {
    return uuid;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getConnectionUuid()
  {
    return channel.getConnectionUuid();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFClientConnectionStatus getStatus()
  {
    return channel.getStatus();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setStatus(final JPPFClientConnectionStatus status)
  {
    channel.getTaskServerConnection().setStatus(status);
  }

  /**
   * Get the wrapped channel.
   * @return a <code>AbstractJPPFClientConnection</code> instance.
   */
  public AbstractJPPFClientConnection getChannel()
  {
    return channel;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    channel.addClientConnectionStatusListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    channel.removeClientConnectionStatusListener(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public JPPFFuture<?> submit(final ClientTaskBundle bundle)
  {
    setStatus(JPPFClientConnectionStatus.EXECUTING);
    JPPFFutureTask<?> task = new JPPFFutureTask(new RemoteRunnable(getBundler(), bundle, channel), null)
    {
      @Override
      public boolean cancel(final boolean mayInterruptIfRunning)
      {
        if (super.cancel(false))
        {
          try
          {
            channel.cancelJob(bundle.getJob().getUuid());
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isLocal()
  {
    return false;
  }

  /**
   * Called when reconnection of this channel is required.
   */
  public void reconnect()
  {
    setStatus(JPPFClientConnectionStatus.DISCONNECTED);
    try
    {
      channel.getTaskServerConnection().init();
    }
    catch (Exception e2)
    {
      log.error(e2.getMessage(), e2);
    }
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append("ChannelWrapperRemote");
    sb.append("{channel=").append(channel);
    sb.append('}');
    return sb.toString();
  }

  /**
   *
   */
  private class RemoteRunnable implements Runnable
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
     * The connection to the driver to use.
     */
    private final AbstractJPPFClientConnection connection;

    /**
     * Initialize this runnable for remote execution.
     * @param bundler    the bundler to send the resulting statistics to.
     * @param bundle     the execution to perform.
     * @param connection the connection to the driver to use.
     */
    public RemoteRunnable(final Bundler bundler, final ClientTaskBundle bundle, final AbstractJPPFClientConnection connection)
    {
      this.bundler = bundler;
      this.bundle = bundle;
      this.connection = connection;
    }

    @Override
    public void run()
    {
      Exception exception = null;
      List<JPPFTask> tasks = this.bundle.getTasksL();
      AbstractGenericClient client = connection.getClient();
      AbstractGenericClient.RegisteredClassLoader registeredClassLoader = null;
      try
      {
        long start = System.nanoTime();
        int count = 0;
        boolean completed = false;
        JPPFJob newJob = createNewJob(bundle.getJob(), tasks);
        ClassLoader classLoader = getClassLoader(newJob);
        registeredClassLoader = client.registerClassLoader(classLoader, newJob.getUuid());
        while (!completed)
        {
          JPPFTaskBundle bundle = createBundle(newJob);
          bundle.setRequestUuid(registeredClassLoader.getUuid());
          connection.sendTasks(classLoader, bundle, newJob);
          while (count < tasks.size())
          {
            List<JPPFTask> results = connection.receiveResults(classLoader);
            int n = results.size();
            count += n;
            if (debugEnabled) log.debug("received " + n + " tasks from server" + (n > 0 ? ", first position=" + results.get(0).getPosition() : ""));
            this.bundle.resultsReceived(results);
//            else log.warn("result listener is null for job " + newJob);
          }
          completed = true;
        }

        double elapsed = System.nanoTime() - start;
        bundler.feedback(tasks.size(), elapsed);
      }
      catch (Throwable t)
      {
        log.error(t.getMessage(), t);
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        bundle.resultsReceived(t);

        if ((t instanceof NotSerializableException) || (t instanceof InterruptedException)) return;

        bundle.resubmit();
        reconnect();
      }
      finally
      {
        if (registeredClassLoader != null) registeredClassLoader.dispose();
        bundle.taskCompleted(exception);
        if(exception == null) setStatus(JPPFClientConnectionStatus.ACTIVE);
      }
    }

    /**
     * Create a new job based on the initial one.
     * @param job   initial job.
     * @param tasks the tasks to execute.
     * @return a new {@link JPPFJob} with the same characteristics as the initial one, except for the tasks.
     * @throws Exception if any error occurs.
     */
    private JPPFJob createNewJob(final JPPFJob job, final List<JPPFTask> tasks) throws Exception
    {
      JPPFJob newJob = new JPPFJob(job.getUuid());
      newJob.setDataProvider(job.getDataProvider());
      newJob.setSLA(job.getSLA());
      newJob.setMetadata(job.getMetadata());
      newJob.setBlocking(job.isBlocking());
//      newJob.setResultListener(job.getResultListener());
      newJob.setName(job.getName());
      for (JPPFTask task : tasks)
      {
        // needed as JPPFJob.addTask() resets the position
        int pos = task.getPosition();
        newJob.addTask(task);
        task.setPosition(pos);
      }
      return newJob;
    }

    /**
     * Create a task bundle for the specified job.
     * @param job the job to use as a base.
     * @return a JPPFTaskBundle instance.
     */
    private JPPFTaskBundle createBundle(final JPPFJob job)
    {
      String requestUuid = job.getUuid();
      JPPFTaskBundle bundle = new JPPFTaskBundle();
      bundle.setRequestUuid(requestUuid);
      return bundle;
    }

    /**
     * Return class loader for the specified job.
     * @param job the job used to determine class loader.
     * @return a <code>ClassLoader</code> instance or <code>null</code> when job has no tasks.
     */
    private ClassLoader getClassLoader(final JPPFJob job)
    {
      if (job == null) throw new IllegalArgumentException("job is null");
      if (job.getTasks().isEmpty())
      {
        return null;
      }
      else
      {
        Object task = job.getTasks().get(0);
        if (task instanceof JPPFAnnotatedTask) task = ((JPPFAnnotatedTask) task).getTaskObject();
        return task.getClass().getClassLoader();
      }
    }
  }
}
