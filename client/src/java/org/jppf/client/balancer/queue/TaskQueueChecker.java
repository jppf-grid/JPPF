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

package org.jppf.client.balancer.queue;

import org.jppf.client.JPPFClientConnectionStatus;
import org.jppf.client.JPPFContextClient;
import org.jppf.client.JPPFJob;
import org.jppf.client.balancer.ChannelWrapper;
import org.jppf.client.balancer.ClientJob;
import org.jppf.client.balancer.ClientTaskBundle;
import org.jppf.client.balancer.job.ChannelJobPair;
import org.jppf.client.balancer.job.JPPFJobManager;
import org.jppf.client.balancer.stats.JPPFDriverStatsManager;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.JPPFContext;
import org.jppf.server.scheduler.bundle.JobAwareness;
import org.jppf.server.scheduler.bundle.fixedsize.FixedSizeBundler;
import org.jppf.server.scheduler.bundle.fixedsize.FixedSizeProfile;
import org.jppf.utils.StringUtils;
import org.jppf.utils.ThreadSynchronization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
public class TaskQueueChecker extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Random number generator used to randomize the choice of idle channel.
   */
  private final Random random = new Random(System.currentTimeMillis());
  /**
   * Reference to the job queue.
   */
  private final AbstractJPPFQueue queue;
  /**
   * Reference to the statistics manager.
   */
  private final JPPFDriverStatsManager statsManager;
  /**
   * Reference to the job manager.
   */
  private final JPPFJobManager jobManager;
  /**
   * Lock on the job queue.
   */
  private final Lock queueLock;
  /**
   * The list of idle node channels.
   */
  private final List<ChannelWrapper<?>> idleChannels = new ArrayList<ChannelWrapper<?>>();
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private Bundler bundler;
  /**
   * Holds information about the execution context.
   */
  private final JPPFContext jppfContext;

  /**
   * Initialize this task queue checker with the specified node server.
   * @param queue        the reference queue to use.
   * @param statsManager the reference to statistics manager.
   * @param jobManager   the job manager that submits the events.
   */
  public TaskQueueChecker(final AbstractJPPFQueue queue, final JPPFDriverStatsManager statsManager, final JPPFJobManager jobManager)
  {
    this.queue = queue;
    this.jppfContext = new JPPFContextClient(queue);
    this.statsManager = statsManager;
    this.jobManager = jobManager;
    this.queueLock = queue.getLock();
    this.bundler = createDefault();
  }

  /**
   * Create new instance of default bundler.
   * @return a new {@link Bundler} instance.
   */
  protected Bundler createDefault()
  {
    FixedSizeProfile profile = new FixedSizeProfile();
    profile.setSize(1);
    return new FixedSizeBundler(profile);
  }

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link Bundler} instance.
   */
  public Bundler getBundler()
  {
    return bundler;
  }

  /**
   * Set the bundler used to schedule tasks for the corresponding node.
   * @param bundler a {@link Bundler} instance.
   */
  public void setBundler(final Bundler bundler)
  {
    if (bundler == null)
    {
      this.bundler = createDefault();
    }
    else
    {
      this.bundler = bundler;
    }
  }

  /**
   * Add a channel to the list of idle channels.
   * @param channel the channel to add to the list.
   */
  public void addIdleChannel(final ChannelWrapper<?> channel)
  {
//    System.out.println("TaskQueueChecker.addIdleChannel: " + channel + "\t queue: " + queue.getMaxBundleSize());
    if (debugEnabled) log.trace("Adding idle channel " + channel);
    int count;
    synchronized (idleChannels)
    {
      idleChannels.add(channel);
      count = idleChannels.size();
    }
    wakeUp();
    statsManager.idleNodes(count);
  }

  /**
   * Get the list of idle channels.
   * @return a new copy of the underlying list of idle channels.
   */
  public List<ChannelWrapper<?>> getIdleChannels()
  {
    synchronized (idleChannels)
    {
      return new ArrayList<ChannelWrapper<?>>(idleChannels);
    }
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  public ChannelWrapper<?> removeIdleChannel(final ChannelWrapper<?> channel)
  {
//    System.out.println("TaskQueueChecker.removeIdleChannel: " + channel + "\t queue: " + queue.getMaxBundleSize());
    if (debugEnabled) log.trace("Removing idle channel " + channel);
    int count;
    synchronized (idleChannels)
    {
      idleChannels.remove(channel);
      count = idleChannels.size();
    }
    statsManager.idleNodes(count);
    return channel;
  }

  /**
   * Return whether any idle channel is available.
   * @return true when there are no idle channels.
   */
  public boolean hasIdleChannel()
  {
    synchronized (idleChannels)
    {
      return !idleChannels.isEmpty();
    }
  }

  /**
   * Perform the assignment of tasks.
   * @see Runnable#run()
   */
  @Override
  public void run()
  {
    while (!isStopped())
    {
      if (!dispatch()) goToSleep(1000L, 10000);
    }
  }

  /**
   * Perform the assignment of tasks.
   * @return true if a job was dispatched, false otherwise.
   * @see Runnable#run()
   */
  public boolean dispatch()
  {
    boolean dispatched = false;
    try
    {
      synchronized (idleChannels)
      {
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) log.debug(Integer.toString(idleChannels.size()) + " channels idle");
        ChannelWrapper<?> channel = null;
        ClientJob selectedBundle = null;
        queueLock.lock();
        try
        {
          Iterator<ClientJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty())
          {
            ClientJob bundleWrapper = it.next();
            channel = retrieveChannel(bundleWrapper);
            if (channel != null) selectedBundle = bundleWrapper;
          }
          if (debugEnabled)
          {
            log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
          }
          if (channel != null)
          {
            dispatchJobToChannel(channel, selectedBundle);
            dispatched = true;
          }
        }
        catch (Exception ex)
        {
          log.error("An error occurred while attempting to dispatch task bundles. This is most likely due to an error in the load balancer implementation.", ex);
        }
        finally
        {
          queueLock.unlock();
        }
      }
    }
    catch (Exception ex)
    {
      log.error("An error occurred while preparing for bundle creation and dispatching.", ex);
    }
    return dispatched;
  }

  /**
   * Retrieve a suitable channel for the specified job.
   * @param bundleWrapper the job to execute.
   * @return a channel for a node on which to execute the job.
   * @throws Exception if any error occurs.
   */
  private ChannelWrapper<?> retrieveChannel(final ClientJob bundleWrapper) throws Exception
  {
    if (checkJobState(bundleWrapper))
    {
      return findIdleChannelIndex(bundleWrapper);
    }
    return null;
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel        the node channel to dispatch the job to.
   * @param selectedBundle the job to dispatch.
   */
  private void dispatchJobToChannel(final ChannelWrapper<?> channel, final ClientJob selectedBundle)
  {
    if (debugEnabled)
    {
      log.debug("dispatching jobUuid=" + selectedBundle.getJob().getUuid() + " to node " + channel +
              ", nodeUuid=" + channel.getConnectionUuid());
    }
    synchronized (channel)
    {
      int size = 1;
      try
      {
        updateBundler(getBundler(), selectedBundle.getJob(), channel);
        size = channel.getBundler().getBundleSize();
      }
      catch (Exception e)
      {
        log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
        FixedSizeProfile profile = new FixedSizeProfile();
        profile.setSize(1);
        setBundler(new FixedSizeBundler(profile));
      }
      ClientTaskBundle bundleWrapper = queue.nextBundle(selectedBundle, size);
//      bundleWrapper.jobDispatched(channel);
      Future<?> future = channel.submit(bundleWrapper);
      selectedBundle.jobDispatched(bundleWrapper, channel, future);
      jobManager.jobDispatched(bundleWrapper, channel);
    }
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param bundle the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private ChannelWrapper<?> findIdleChannelIndex(final ClientJob bundle)
  {
    int idleChannelsSize = idleChannels.size();
    ExecutionPolicy policy = bundle.getJob().getSLA().getExecutionPolicy();
    if (debugEnabled && (policy != null)) log.debug("Bundle " + bundle + " has an execution policy:\n" + policy);
    List<ChannelWrapper<?>> acceptableChannels = new ArrayList<ChannelWrapper<?>>(idleChannelsSize);
    Iterator<ChannelWrapper<?>> iterator = idleChannels.iterator();
    while (iterator.hasNext())
    {
      ChannelWrapper<?> ch = iterator.next();
      if (ch.getStatus() != JPPFClientConnectionStatus.ACTIVE)
      {
        if (debugEnabled) log.debug("channel is not opened: " + ch);
        iterator.remove();
        continue;
      }
      if (policy != null)
      {
        JPPFManagementInfo mgtInfo = ch.getManagementInfo();
        JPPFSystemInformation info = ch.getSystemInfo();
        boolean b = false;
        try
        {
          b = policy.accepts(info);
        }
        catch (Exception ex)
        {
          log.error("An error occurred while running the execution policy to determine node participation.", ex);
        }
        if (debugEnabled)
        {
          log.debug("rule execution is *" + b + "* for jobUuid=" + bundle.getUuid() + ", node=" + ch + ", nodeUuid=" + mgtInfo.getId());
        }
        if (!b) continue;
      }
      acceptableChannels.add(ch);
    }
    int size = acceptableChannels.size();
    if (debugEnabled) log.debug("found " + size + " acceptable channels");
    if (size > 0)
    {
      return acceptableChannels.get(size > 1 ? random.nextInt(size) : 0);
    }
    return null;
  }

  /**
   * Check if the job state allows it to be dispatched on another node.
   * There are two cases when this method will return false: when the job is suspended and
   * when the job is already executing on its maximum allowed number of nodes.
   * @param bundle the bundle from which to get the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private boolean checkJobState(final ClientJob bundle)
  {
    JobSLA sla = bundle.getJob().getSLA();
    if (debugEnabled)
    {
      String s = StringUtils.buildString("job '", bundle.getName(), "' : ",
              "suspended=", sla.isSuspended(), ", pending=", bundle.isPending(),
              ", expired=", bundle.isJobExpired());
      log.debug(s);
    }
    if (sla.isSuspended()) return false;
    boolean b = bundle.isPending();
    if (b) return false;
    b = bundle.isJobExpired();
    if (b) return false;
    int maxNodes = sla.getMaxNodes();
    List<ChannelJobPair> list = jobManager.getNodesForJob(bundle.getUuid());
    int n = list.size();
    if (debugEnabled) log.debug("current nodes = " + n + ", maxNodes = " + maxNodes);
    return n < maxNodes;
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param bundler    the bundler to check and update.
   * @param taskBundle the job.
   * @param context    the current node context.
   */
  private void updateBundler(final Bundler bundler, final JPPFJob taskBundle, final ChannelWrapper<?> context)
  {
    context.checkBundler(bundler, jppfContext);
    if (context.getBundler() instanceof JobAwareness)
    {
      JobMetadata metadata = taskBundle.getMetadata();
      ((JobAwareness) context.getBundler()).setJobMetadata(metadata);
    }
  }
}
