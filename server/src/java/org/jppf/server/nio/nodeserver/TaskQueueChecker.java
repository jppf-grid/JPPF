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

import org.jppf.execute.*;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.*;
import org.jppf.server.*;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.protocol.*;
import org.jppf.server.queue.JPPFPriorityQueue;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.fixedsize.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class ensures that idle nodes get assigned pending tasks in the queue.
 * @param <T> type of the <code>ExecutorChannel</code>.
 */
public class TaskQueueChecker<T extends ExecutorChannel> extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(TaskQueueChecker.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * Random number generator used to randomize the choice of idle channel.
   */
  private final Random random = new Random(System.nanoTime());
  /**
   * Reference to the job queue.
   */
  private final JPPFPriorityQueue queue;
  /**
   * Reference to the statistics manager.
   */
  private final JPPFDriverStatsManager statsManager;
  /**
   * Lock on the job queue.
   */
  private final Lock queueLock;
  /**
   * The list of idle node channels.
   */
  private final Set<T> idleChannels = new LinkedHashSet<T>();
  /**
   * Bundler used to schedule tasks for the corresponding node.
   */
  private Bundler bundler;
  /**
   * Holds information about the execution context.
   */
  private final JPPFContext jppfContext;
  /**
   * The jpob manager.
   */
  private final JPPFJobManager jobManager;

  /**
   * Initialize this task queue checker with the specified node server.
   * @param queue        the reference queue to use.
   * @param statsManager the reference to statistics manager.
   * @param jobManager   the job manager which sends job notifications.
   */
  public TaskQueueChecker(final JPPFPriorityQueue queue, final JPPFDriverStatsManager statsManager, final JPPFJobManager jobManager)
  {
    this.queue = queue;
    this.jobManager = jobManager;
    this.jppfContext = new JPPFContextDriver(queue);
    this.statsManager = statsManager;
    this.queueLock = queue.getLock();
    this.bundler = createDefault();
  }

  /**
   * Get the corresponding node's context information.
   * @return a {@link JPPFContext} instance.
   */
  public JPPFContext getJPPFContext() {
    return jppfContext;
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
   * Get the number of idle channels.
   * @return the size of the underlying list of idle channels.
   */
  public int getNbIdleChannels()
  {
    synchronized (idleChannels)
    {
      return idleChannels.size();
    }
  }

  /**
   * Add a channel to the list of idle channels.
   * @param channel the channel to add to the list.
   */
  public void addIdleChannel(final T channel)
  {
    if (channel == null) throw new IllegalArgumentException("channel is null");
    if (channel.getExecutionStatus() != ExecutorStatus.ACTIVE) throw new IllegalStateException("channel is not active: " + channel);

    if (traceEnabled) log.trace("Adding idle channel " + channel);
    int count;
    synchronized(idleChannels)
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
  public List<T> getIdleChannels()
  {
    synchronized (idleChannels)
    {
      return new ArrayList<T>(idleChannels);
    }
  }

  /**
   * Remove a channel from the list of idle channels.
   * @param channel the channel to remove from the list.
   * @return a reference to the removed channel.
   */
  public T removeIdleChannel(final T channel)
  {
    if (traceEnabled) log.trace("Removing idle channel " + channel);
    int count;
    synchronized(idleChannels)
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
      if (!dispatch()) goToSleep(10L, 10000);
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
      queue.processPendingBroadcasts();
      T channel = null;
      ServerTaskBundle taskBundle = null;
      synchronized(idleChannels)
      {
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) log.debug(Integer.toString(idleChannels.size()) + " channels idle");
        queueLock.lock();
        try
        {
          Iterator<ServerJob> it = queue.iterator();
          while ((channel == null) && it.hasNext() && !idleChannels.isEmpty())
          {
            ServerJob bundleWrapper = it.next();
            channel = retrieveChannel(bundleWrapper);
            if (channel != null) {
              taskBundle = prepareJobDispatch(channel, bundleWrapper);
              removeIdleChannel(channel);
            }
          }
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
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
      if (channel != null && taskBundle != null)
      {
        dispatchJobToChannel(channel, taskBundle);
        dispatched = true;
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
  private T retrieveChannel(final ServerJob bundleWrapper) throws Exception
  {
    if (checkJobState(bundleWrapper))
    {
      return findIdleChannelIndex(bundleWrapper);
    }
    return null;
  }

  /**
   * Prepare the specified job for the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to prepare dispatch the job to.
   * @param selectedBundle the job to dispatch.
   */
  private ServerTaskBundle prepareJobDispatch(final T channel, final ServerJob selectedBundle)
  {
    if (debugEnabled)
    {
      log.debug("dispatching jobUuid=" + selectedBundle.getJob().getUuid() + " to node " + channel +
              ", nodeUuid=" + channel.getConnectionUuid());
    }
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
    return queue.nextBundle(selectedBundle, size);
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel the node channel to dispatch the job to.
   * @param bundleWrapper the job to dispatch.
   */
  @SuppressWarnings("unchecked")
  private void dispatchJobToChannel(final T channel, final ServerTaskBundle bundleWrapper) {
    synchronized(channel.getMonitor())
    {
      JPPFFuture<?> future = channel.submit(bundleWrapper);
      bundleWrapper.jobDispatched(channel, future);
      jobManager.jobDispatched(bundleWrapper.getClientJob(), ((AbstractNodeContext) channel).getChannel());
    }
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param bundle the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private T findIdleChannelIndex(final ServerJob bundle)
  {
    int idleChannelsSize = idleChannels.size();
    ExecutionPolicy policy = bundle.getJob().getSLA().getExecutionPolicy();
    if (debugEnabled && (policy != null)) log.debug("Bundle " + bundle + " has an execution policy:\n" + policy);
    List<T> acceptableChannels = new ArrayList<T>(idleChannelsSize);
    List<String> uuidPath = bundle.getJob().getUuidPath().getList();
    Iterator<T> iterator = idleChannels.iterator();
    while (iterator.hasNext())
    {
      T ch = iterator.next();
      if (ch.getExecutionStatus() != ExecutorStatus.ACTIVE)
      {
        if (debugEnabled) log.debug("channel is not opened: " + ch);
        iterator.remove();
        continue;
      }
      if (debugEnabled) log.debug("uuid path=" + uuidPath + ", node uuid=" + ch.getUuid());
      if (uuidPath.contains(ch.getUuid()))
      {
        if (debugEnabled) log.debug("bundle uuid path already contains node " + ch + " : uuidPath=" + uuidPath + ", nodeUuid=" + ch.getUuid());
        continue;
      }
      if(bundle.getBroadcastUUID() != null && !bundle.getBroadcastUUID().equals(ch.getUuid())) continue;
      if (policy != null)
      {
        JPPFSystemInformation info = ch.getSystemInfo();
        boolean b = false;
        try
        {
          b = policy.accepts(info);
        }
        catch(Exception ex)
        {
          log.error("An error occurred while running the execution policy to determine node participation.", ex);
        }
        if (debugEnabled)
        {
          log.debug("rule execution is *" + b + "* for jobUuid=" + bundle.getUuid() + " on local channel=" + ch);
        }
        if (!b) continue;
      }
      // add a bias toward local node
      if (ch.isLocal())
      {
        return ch;
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
  private static boolean checkJobState(final ServerJob bundle)
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
    int n = bundle.getNbChannels();
    if (debugEnabled) log.debug("current nodes = " + n + ", maxNodes = " + maxNodes);
    return n < maxNodes;
  }

  /**
   * Perform the checks on the bundler before submitting a job.
   * @param bundler the bundler to check and update.
   * @param taskBundle the job.
   * @param context the current node context.
   */
  private void updateBundler(final Bundler bundler, final JPPFTaskBundle taskBundle, final T context)
  {
    context.checkBundler(bundler, jppfContext);
    if (context.getBundler() instanceof JobAwareness)
    {
      JobMetadata metadata = taskBundle.getMetadata();
      ((JobAwareness) context.getBundler()).setJobMetadata(metadata);
    }
  }
}
