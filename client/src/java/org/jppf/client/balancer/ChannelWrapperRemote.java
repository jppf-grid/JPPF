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

import java.io.NotSerializableException;
import java.util.List;
import java.util.concurrent.Executors;

import org.jppf.JPPFException;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.execute.*;
import org.jppf.management.*;
import org.jppf.node.protocol.Task;
import org.jppf.server.protocol.*;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.utils.*;
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
  public ChannelWrapperRemote(final JPPFClientConnection channel)
  {
    if (channel == null) throw new IllegalArgumentException("channel is null");

    this.channel = (AbstractJPPFClientConnection) channel;
    this.uuid = channel.getDriverUuid();
    priority = channel.getPriority();
    systemInfo = new JPPFSystemInformation(this.uuid, false, true);
    managementInfo = new JPPFManagementInfo("remote", -1, getConnectionUuid(), JPPFManagementInfo.DRIVER, channel.isSSLEnabled());
    managementInfo.setSystemInfo(systemInfo);
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("RemoteChannelWrapper" + channel.getName() + '-'));
  }

  @Override
  public void setSystemInformation(final JPPFSystemInformation systemInfo)
  {
    if (systemInfo != null && uuid == null)
    {
      uuid = systemInfo.getUuid().getProperty("jppf.uuid");
      if (uuid != null && uuid.isEmpty()) uuid = null;
    }
    super.setSystemInformation(systemInfo);
  }

  @Override
  public String getUuid()
  {
    return uuid;
  }

  @Override
  public String getConnectionUuid()
  {
    return channel.getConnectionUuid();
  }

  @Override
  public JPPFClientConnectionStatus getStatus()
  {
    return channel.getStatus();
  }

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

  @Override
  public void addClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    channel.addClientConnectionStatusListener(listener);
  }

  @Override
  public void removeClientConnectionStatusListener(final ClientConnectionStatusListener listener)
  {
    channel.removeClientConnectionStatusListener(listener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public JPPFFuture<?> submit(final ClientTaskBundle bundle)
  {
    setStatus(JPPFClientConnectionStatus.EXECUTING);
    JPPFFutureTask<?> task = new JPPFFutureTask(new RemoteRunnable(getBundler(), bundle, channel), null) {
      @Override
      public boolean cancel(final boolean mayInterruptIfRunning)
      {
        if (isDone()) return false;
        String uuid = bundle.getClientJob().getUuid();
        if (debugEnabled) log.debug("JPPFFutureTask.cancel() : requesting cancel of jobId=" + uuid);
        if (isCancelled()) return true;
        bundle.cancel();
        try {
          channel.cancelJob(uuid);
        } catch (Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
        } finally {
          return super.cancel(false);
        }
      }
    };
    bundle.jobDispatched(this, task);
    executor.execute(task);
    return task;
  }

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
    Runnable r = new Runnable() {
      public void run() {
        setStatus(JPPFClientConnectionStatus.DISCONNECTED);
        try {
          channel.getTaskServerConnection().init();
        } catch (Exception e2) {
          log.error(e2.getMessage(), e2);
        }
      }
    };
    new Thread(r, "connecting " + channel).start();
  }

  @Override
  public String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append("[channel=").append(channel);
    sb.append(']');
    return sb.toString();
  }

  /**
   *
   */
  private class RemoteRunnable implements Runnable {
    /**
     * The task bundle to execute.
     */
    private final ClientTaskBundle clientBundle;
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
     * @param clientBundle     the execution to perform.
     * @param connection the connection to the driver to use.
     */
    public RemoteRunnable(final Bundler bundler, final ClientTaskBundle clientBundle, final AbstractJPPFClientConnection connection) {
      this.bundler = bundler;
      this.clientBundle = clientBundle;
      this.connection = connection;
    }

    @Override
    public void run() {
      Exception exception = null;
      List<Task<?>> tasks = this.clientBundle.getTasksL();
      AbstractGenericClient client = connection.getClient();
      AbstractGenericClient.RegisteredClassLoader registeredClassLoader = null;
      try {
        long start = System.nanoTime();
        int count = 0;
        boolean completed = false;
        JPPFJob newJob = createNewJob(clientBundle, tasks);
        ClassLoader classLoader = getClassLoader(newJob);
        registeredClassLoader = client.registerClassLoader(classLoader, newJob.getUuid());
        while (!completed) {
          JPPFTaskBundle bundle = createBundle(newJob);
          bundle.setUuid(registeredClassLoader.getUuid());
          bundle.setInitialTaskCount(clientBundle.getClientJob().initialTaskCount);
          connection.sendTasks(classLoader, bundle, newJob);
          while (count < tasks.size()) {
            List<Task<?>> results = connection.receiveResults(classLoader);
            int n = results.size();
            count += n;
            if (debugEnabled) log.debug("received " + n + " tasks from server" + (n > 0 ? ", first position=" + results.get(0).getPosition() : ""));
            this.clientBundle.resultsReceived(results);
          }
          completed = true;
        }

        double elapsed = System.nanoTime() - start;
        bundler.feedback(tasks.size(), elapsed);
      } catch (Throwable t) {
        if (debugEnabled) log.debug(t.getMessage(), t);
        else log.warn(ExceptionUtils.getMessage(t));
        exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        if ((t instanceof NotSerializableException) || (t instanceof InterruptedException)) {
          clientBundle.resultsReceived(t);
          return;
        }
        clientBundle.resubmit();
        reconnect();
      } finally {
        if (registeredClassLoader != null) registeredClassLoader.dispose();
        clientBundle.taskCompleted(exception);
        clientBundle.getClientJob().removeChannel(ChannelWrapperRemote.this);
        if (getStatus() == JPPFClientConnectionStatus.EXECUTING) setStatus(JPPFClientConnectionStatus.ACTIVE);
      }
    }

    /**
     * Create a new job based on the initial one.
     * @param job   initial job.
     * @param tasks the tasks to execute.
     * @return a new {@link JPPFJob} with the same characteristics as the initial one, except for the tasks.
     * @throws Exception if any error occurs.
     */
    private JPPFJob createNewJob(final ClientTaskBundle job, final List<Task<?>> tasks) throws Exception
    {
      JPPFJob newJob = new JPPFJob(job.getClientJob().getUuid());
      newJob.setDataProvider(job.getJob().getDataProvider());
      newJob.setSLA(job.getSLA());
      newJob.setClientSLA(job.getJob().getClientSLA());
      newJob.setMetadata(job.getMetadata());
      newJob.setBlocking(job.getJob().isBlocking());
//      newJob.setResultListener(job.getResultListener());
      newJob.setName(job.getName());
      for (Task<?> task : tasks)
      {
        // needed as JPPFJob.addTask() resets the position
        int pos = task.getPosition();
        newJob.add(task);
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
      JPPFTaskBundle bundle = new JPPFTaskBundle();
      bundle.setUuid(job.getUuid());
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
      if (job.getJobTasks().isEmpty()) return null;
      else
      {
        Object task = job.getJobTasks().get(0);
        if (task instanceof JPPFAnnotatedTask) task = ((JPPFAnnotatedTask) task).getTaskObject();
        return task.getClass().getClassLoader();
      }
    }
  }
}
