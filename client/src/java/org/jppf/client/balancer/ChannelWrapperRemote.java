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

import org.jppf.JPPFException;
import org.jppf.client.AbstractGenericClient;
import org.jppf.client.AbstractJPPFClientConnection;
import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.JPPFJob;
import org.jppf.client.event.ClientConnectionStatusHandler;
import org.jppf.client.event.ClientConnectionStatusListener;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.client.event.TaskResultListener;
import org.jppf.client.taskwrapper.JPPFAnnotatedTask;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.server.scheduler.bundle.Bundler;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Context associated with a remote channel serving state and tasks submission.
 * @author Martin JANDA
 */
public class ChannelWrapperRemote extends ChannelWrapper implements ClientConnectionStatusHandler
{
  /**
   * The channel to the driver to use.
   */
  private final AbstractJPPFClientConnection channel;
  /**
   * Executor for submitting bundles for processing.
   */
  private final Executor executor = Executors.newSingleThreadExecutor();

  /**
   * Default initializer for remote channel wrapper.
   * @param channel to the driver to use.
   */
  public ChannelWrapperRemote(final AbstractJPPFClientConnection channel)
  {
    if (channel == null) throw new IllegalArgumentException("channel is null");

    this.channel = channel;

    JPPFSystemInformation info = new JPPFSystemInformation(getConnectionUuid());

    JPPFManagementInfo managementInfo = new JPPFManagementInfo("remote", -1, getConnectionUuid(), JPPFManagementInfo.DRIVER);
    managementInfo.setSystemInfo(info);
    setSystemInfo(info);
    setManagementInfo(managementInfo);
  }

  @Override
  public void setSystemInfo(final JPPFSystemInformation systemInfo)
  {
    super.setSystemInfo(systemInfo);    //To change body of overridden methods use File | Settings | File Templates.
  }

  @Override
  public void setManagementInfo(final JPPFManagementInfo managementInfo)
  {
    super.setManagementInfo(managementInfo);    //To change body of overridden methods use File | Settings | File Templates.
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getConnectionUuid()
  {
    return channel.getUuid();
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
  public void submit(final ClientJob bundle)
  {
    setStatus(JPPFClientConnectionStatus.EXECUTING);
    executor.execute(new RemoteRunnable(getBundler(), bundle, channel));
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
     * The connection to the driver to use.
     */
    private final AbstractJPPFClientConnection connection;
    /**
     * The task bundle to send or receive.
     */
    private final ClientJob bundle;
    /**
     * Bundler used to schedule tasks for the corresponding node.
     */
    private final Bundler bundler;

    /**
     * Initialize this runnable for remote execution.
     * @param bundler    the bundler to send the resulting statistics to.
     * @param bundle     the execution to perform.
     * @param connection the connection to the driver to use.
     */
    private RemoteRunnable(final Bundler bundler, final ClientJob bundle, final AbstractJPPFClientConnection connection)
    {
      this.connection = connection;
      this.bundler = bundler;
      this.bundle = bundle;
    }

    @Override
    public void run()
    {
      AbstractGenericClient client = connection.getClient();
      List<JPPFTask> tasks = this.bundle.getTasks();
      String requestUuid = null;
      try
      {
        long start = System.nanoTime();
        int count = 0;
        boolean completed = false;
        JPPFJob newJob = createNewJob(bundle.getJob().getJob(), bundle.getTasks());
        while (!completed)
        {
          requestUuid = newJob.getUuid();
          JPPFTaskBundle bundle = createBundle(client, newJob);
          System.out.println("RemoteRunnable.sendTasks");
          connection.sendTasks(bundle, newJob);
          while (count < tasks.size())
          {
            List<JPPFTask> results = connection.receiveResults(client.getRequestClassLoader(bundle.getRequestUuid()));
            System.out.println("RemoteRunnable.received: " + results.size());
            int n = results.size();
            count += n;
//            if (debugEnabled) log.debug("received " + n + " tasks from server" + (n > 0 ? ", first position=" + results.get(0).getPosition() : ""));
            TaskResultListener listener = newJob.getResultListener();
            if (listener != null)
            {
              synchronized (listener)
              {
                listener.resultsReceived(new TaskResultEvent(results));
              }
            }
//            else log.warn("result listener is null for job " + newJob);
          }
          completed = true;
        }

        double elapsed = System.nanoTime() - start;
        bundler.feedback(tasks.size(), elapsed);
//        connection.getTaskServerConnection().setStatus(JPPFClientConnectionStatus.ACTIVE);
      }
      catch (Throwable t)
      {
//        log.error(t.getMessage(), t);
        Exception exception = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        t.printStackTrace();
      }
      finally
      {
        bundle.fireTaskCompleted();
        client.removeRequestClassLoader(requestUuid);
        setStatus(JPPFClientConnectionStatus.ACTIVE);
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
      newJob.setResultListener(job.getResultListener());
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
     * @param client necessary for registering class loader.
     * @param job    the job to use as a base.
     * @return a JPPFTaskBundle instance.
     */
    private JPPFTaskBundle createBundle(final AbstractGenericClient client, final JPPFJob job)
    {
      String requestUuid = job.getUuid();
      JPPFTaskBundle bundle = new JPPFTaskBundle();
      bundle.setRequestUuid(requestUuid);
      ClassLoader cl = null;
      if (!job.getTasks().isEmpty())
      {
        Object task = job.getTasks().get(0);
        if (task instanceof JPPFAnnotatedTask) task = ((JPPFAnnotatedTask) task).getTaskObject();
        cl = task.getClass().getClassLoader();
        client.addRequestClassLoader(requestUuid, cl);
//        if (log.isDebugEnabled()) log.debug("adding request class loader=" + cl + " for uuid=" + requestUuid + ", from class " + task.getClass());
      }
      return bundle;
    }
  }
}
