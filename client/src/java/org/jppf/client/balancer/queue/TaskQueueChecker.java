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

import java.util.*;
import java.util.concurrent.locks.Lock;

import org.jppf.client.*;
import org.jppf.client.balancer.*;
import org.jppf.client.balancer.stats.JPPFClientStatsManager;
import org.jppf.execute.*;
import org.jppf.node.protocol.JobMetadata;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.server.scheduler.bundle.fixedsize.*;
import org.jppf.utils.ThreadSynchronization;
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
  private final JPPFClientStatsManager statsManager;
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
   * Initialize this task queue checker with the specified node server.
   * @param queue        the reference queue to use.
   * @param statsManager the reference to statistics manager.
   */
  public TaskQueueChecker(final JPPFPriorityQueue queue, final JPPFClientStatsManager statsManager)
  {
    this.queue = queue;
    this.jppfContext = new JPPFContextClient(queue);
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
      synchronized (idleChannels)
      {
        if (idleChannels.isEmpty() || queue.isEmpty()) return false;
        if (debugEnabled) log.debug(Integer.toString(idleChannels.size()) + " channels idle");
        T channel = null;
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
          if (debugEnabled) log.debug((channel == null) ? "no channel found for bundle" : "channel found for bundle: " + channel);
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
  private T retrieveChannel(final ClientJob bundleWrapper) throws Exception
  {
    return findIdleChannelIndex(bundleWrapper);
  }

  /**
   * Dispatch the specified job to the selected channel, after applying the load balancer to the job.
   * @param channel        the node channel to dispatch the job to.
   * @param selectedBundle the job to dispatch.
   */
  @SuppressWarnings("unchecked")
  private void dispatchJobToChannel(final T channel, final ClientJob selectedBundle)
  {
    if (debugEnabled)
    {
      log.debug("dispatching jobUuid=" + selectedBundle.getJob().getUuid() + " to channel " + channel +
              ", connectionUuid=" + channel.getConnectionUuid());
    }
    synchronized (channel.getMonitor())
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
      selectedBundle.addChannel(channel);
      channel.submit(bundleWrapper);
    }
  }

  /**
   * Find a channel that can send the specified task bundle for execution.
   * @param bundle the bundle to execute.
   * @return the index of an available and acceptable channel, or -1 if no channel could be found.
   */
  private T findIdleChannelIndex(final ClientJob bundle)
  {
    int idleChannelsSize = idleChannels.size();
    List<T> acceptableChannels = new ArrayList<T>(idleChannelsSize);
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
      if (!bundle.acceptsChannel(ch)) continue;
      if(bundle.getBroadcastUUID() != null && !bundle.getBroadcastUUID().equals(ch.getUuid())) continue;
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
   * Perform the checks on the bundler before submitting a job.
   * @param bundler    the bundler to check and update.
   * @param taskBundle the job.
   * @param context    the current node context.
   */
  private void updateBundler(final Bundler bundler, final JPPFJob taskBundle, final T context)
  {
    context.checkBundler(bundler, jppfContext);
    if (context.getBundler() instanceof JobAwareness)
    {
      JobMetadata metadata = taskBundle.getMetadata();
      ((JobAwareness) context.getBundler()).setJobMetadata(metadata);
    }
  }
}
