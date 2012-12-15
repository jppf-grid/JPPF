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
package org.jppf.server.nio.nodeserver;

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.jppf.management.*;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.server.*;
import org.jppf.server.job.ChannelJobPair;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.AbstractJPPFQueue;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.fixedsize.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 */
class TaskQueueChecker extends ThreadSynchronization implements Runnable
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
   * The owner of this queue checker.
   */
  private final NodeNioServer server;
  /**
   * Reference to the job queue.
   */
  private final AbstractJPPFQueue queue;
  /**
   * Lock on the job queue.
   */
  private final Lock queueLock;
  /**
   * The list of idle node channels.
   */
  private final List<ChannelWrapper<?>> idleChannels = new ArrayList<ChannelWrapper<?>>();
  /**
   * Reference to the driver.
   */
  private final JPPFDriver driver = JPPFDriver.getInstance();
  /**
   * Default value to return if there is an execution policy but the system information for the node is unavailable
   */
  private static final boolean DEFAULT_POLICY_VALUE = JPPFConfiguration.getProperties().getBoolean("jppf.default.policy.outcome", true);

  /**
   * Initialize this task queue checker with the specified node server.
   * @param server the owner of this queue checker.
   */
  public TaskQueueChecker(final NodeNioServer server)
  {
    this.server = server;
    this.queue = (AbstractJPPFQueue) server.getQueue();
    this.queueLock = queue.getLock();
  }

  /**
   * Add a channel to the list of idle channels.
   * @param channel the channel to add to the list.
   */
  public void addIdleChannel(final ChannelWrapper<?> channel)
  {
    if (debugEnabled) log.trace("Adding idle channel " + channel);
    synchronized(idleChannels)
    {
      idleChannels.add(channel);
    }
    wakeUp();
    driver.getStatsManager().idleNodes(idleChannels.size());
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
    if (debugEnabled) log.trace("Removing idle channel " + channel);
    synchronized(idleChannels)
    {
      idleChannels.remove(channel);
    }
    driver.getStatsManager().idleNodes(idleChannels.size());
    return channel;
  }

  /**
   * Remove the channel at the specified index from the list of idle channels.
   * @param index the index of the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  public ChannelWrapper<?> removeIdleChannel(final int index)
  {
    ChannelWrapper<?> channel = null;
    synchronized(idleChannels)
    {
      try
      {
        channel = idleChannels.remove(index);
        if (debugEnabled) log.trace("Removed idle chanel " + channel + " at index " + index);
      }
      catch(Exception e)
      {
        String s = "error removing channel at index " + index + ExceptionUtils.getMessage(e);
        if (debugEnabled) log.debug(s, e);
        else log.warn(s);
      }
    }
    driver.getStatsManager().idleNodes(idleChannels.size());
    return channel;
  }

  /**
   * Perform the assignment of tasks.
   * @see java.lang.Runnable#run()
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
   * @see java.lang.Runnable#run()
   */
  public boolean dispatch()
  {
    boolean dispatched = false;
    try
    {
      synchronized(idleChannels)
      {
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) log.debug(Integer.toString(idleChannels.size()) + " channels idle");
        ChannelWrapper<?> channel = null;
        ServerJob selectedBundle = null;
        queueLock.lock();
        try
        {
          Iterator<ServerJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty())
          {
            ServerJob bundleWrapper = it.next();
            channel = retrieveChannel(bundleWrapper);
            if (channel != null) selectedBundle = bundleWrapper;
          }
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
          if (channel != null)
          {
            dispatchJobToChannel(channel, selectedBundle);
            dispatched = true;
          }
        }
        catch(Exception ex)
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
  private ChannelWrapper<?> retrieveChannel(final ServerJob bundleWrapper) throws Exception
  {
    ChannelWrapper<?> channel = null;
    JPPFTaskBundle bundle = (JPPFTaskBundle) bundleWrapper.getJob();
    if (checkJobState(bundle))
    {
      int n = findIdleChannelIndex(bundle);
      if (n >= 0) channel = removeIdleChannel(n);
    }
    return channel;
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to dispatch the job to.
   * @param selectedBundle the job to dispatch.
   */
  private void dispatchJobToChannel(final ChannelWrapper<?> channel, final ServerJob selectedBundle)
  {
    if (debugEnabled)
    {
      log.debug("dispatching jobUuid=" + selectedBundle.getJob().getUuid() + " to node " + channel +
          ", nodeUuid=" + ((AbstractNodeContext) channel.getContext()).getUuid());
    }
    synchronized(channel)
    {
      AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
      int size = 1;
      try
      {
        updateBundler(server.getBundler(), (JPPFTaskBundle) selectedBundle.getJob(), context);
        size = context.getBundler().getBundleSize();
      }
      catch (Exception e)
      {
        log.error("Error in load balancer implementation, switching to 'manual' with a bundle size of 1", e);
        FixedSizeProfile profile = new FixedSizeProfile();
        profile.setSize(1);
        server.setBundler(new FixedSizeBundler(profile));
      }
      ServerJob bundleWrapper = queue.nextBundle(selectedBundle, size);
      context.setBundle(bundleWrapper);
      server.getTransitionManager().transitionChannel(channel, NodeTransition.TO_SENDING);
      driver.getJobManager().jobDispatched(context.getBundle(), channel);
    }
    if (channel.getSelector() != null) channel.getSelector().wakeUp();
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param bundle the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private int findIdleChannelIndex(final JPPFTaskBundle bundle)
  {
    int n = -1;
    int idleChannelsSize = idleChannels.size();
    ExecutionPolicy policy = bundle.getSLA().getExecutionPolicy();
    if (debugEnabled && (policy != null)) log.debug("Bundle " + bundle + " has an execution policy:\n" + policy);
    List<Integer> acceptableChannels = new ArrayList<Integer>(idleChannelsSize);
    List<Integer> channelsToRemove =  new ArrayList<Integer>(idleChannelsSize);
    List<String> uuidPath = bundle.getUuidPath().getList();
    for (int i=0; i<idleChannelsSize; i++)
    {
      ChannelWrapper<?> ch = idleChannels.get(i);
      if (!ch.isOpen())
      {
        channelsToRemove.add(i);
        if (debugEnabled) log.debug("channel is not opened: " + ch);
        continue;
      }
      AbstractNodeContext context = (AbstractNodeContext) ch.getContext();
      if (debugEnabled) log.debug("uuid path=" + uuidPath + ", node uuid=" + context.getUuid());
      if (uuidPath.contains(context.getUuid()))
      {
        if (debugEnabled) log.debug("bundle uuid path already contains node " + ch + " : uuidPath=" + uuidPath + ", nodeUuid=" + context.getUuid());
        continue;
      }
      if (policy != null)
      {
        boolean b = evaluateExecutionPolicy(policy, ch);
        if (debugEnabled) log.debug("rule execution is *" + b + "* for jobUuid=" + bundle.getUuid() + ", node=" + ch + ", nodeUuid=" + ch.getContext().getUuid());
        if (!b) continue;
      }
      // add a bias toward local node
      if (ch instanceof LocalNodeChannel)
      {
        n = i;
        break;
      }
      acceptableChannels.add(i);
    }
    if (n < 0)
    {
      int size = acceptableChannels.size();
      if (debugEnabled) log.debug("found " + size + " acceptable channels");
      if (size > 0) n = acceptableChannels.remove(size > 1 ? random.nextInt(size) : 0);
      for (Integer i: channelsToRemove) removeIdleChannel(i);
    }
    return n;
  }

  /**
   * Evaluate the specified execution policy against the specified node channel.
   * @param policy the execution policy to evaluate.
   * @param channel the node to match against the execution policy.
   * @return <code>true</code> if the node matches the execution policy, <code>false</code> otherwise.
   */
  private boolean evaluateExecutionPolicy(final ExecutionPolicy policy, final ChannelWrapper<?> channel)
  {
    JPPFManagementInfo mgtInfo = driver.getNodeHandler().getNodeInformation(channel);
    JPPFSystemInformation info = (mgtInfo == null) ? null : mgtInfo.getSystemInfo();
    if (info == null)
    {
      log.warn("system information is not available for node " + channel + ", returning default evaluation outcome '" + DEFAULT_POLICY_VALUE + '\'');
      return DEFAULT_POLICY_VALUE;
    }
    boolean b = false;
    try
    {
      b = policy.accepts(info);
    }
    catch(Exception ex)
    {
      log.error("An error occurred while running the execution policy to determine participation of node " + channel +
        ", returning default evaluation outcome '" + DEFAULT_POLICY_VALUE + '\'', ex);
      return DEFAULT_POLICY_VALUE;
    }
    return b;
  }

  /**
   * Check if the job state allows it to be dispatched on another node.
   * There are two cases when this method will return false: when the job is suspended and
   * when the job is already executing on its maximum allowed number of nodes.
   * @param bundle the bundle from which to get the job information.
   * @return true if the job can be dispatched to at least one more node, false otherwise.
   */
  private boolean checkJobState(final JPPFTaskBundle bundle)
  {
    JobSLA sla = bundle.getSLA();
    if (debugEnabled)
    {
      String s = StringUtils.buildString("job '", bundle.getName(), "' : ",
          "suspended=", sla.isSuspended(), ", pending=", bundle.getParameter(BundleParameter.JOB_PENDING, Boolean.FALSE),
          ", expired=", bundle.getParameter(BundleParameter.JOB_EXPIRED, Boolean.FALSE));
      log.debug(s);
    }
    if (sla.isSuspended()) return false;
    boolean b = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING, Boolean.FALSE);
    if (b) return false;
    b = (Boolean) bundle.getParameter(BundleParameter.JOB_EXPIRED, Boolean.FALSE);
    if (b) return false;
    int maxNodes = sla.getMaxNodes();
    List<ChannelJobPair> list = server.getJobManager().getNodesForJob(bundle.getUuid());
    int n = list.size();
    if (debugEnabled) log.debug("current nodes = " + n + ", maxNodes = " + maxNodes);
    return n < maxNodes;
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param bundler the bundler to check and update.
   * @param taskBundle the job.
   * @param context the current node context.
   */
  private void updateBundler(final Bundler bundler, final JPPFTaskBundle taskBundle, final AbstractNodeContext context)
  {
    context.checkBundler(server.getBundler(), JPPFContextDriver.getInstance());
    if (context.getBundler() instanceof JobAwareness)
    {
      JobMetadata metadata = taskBundle.getMetadata();
      ((JobAwareness) context.getBundler()).setJobMetadata(metadata);
    }
  }
}
