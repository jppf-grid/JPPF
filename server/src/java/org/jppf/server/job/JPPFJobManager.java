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

package org.jppf.server.job;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.execute.ExecutorChannel;
import org.jppf.job.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.protocol.*;
import org.jppf.server.submission.SubmissionStatus;
import org.jppf.utils.JPPFThreadFactory;
import org.slf4j.*;

/**
 * Instances of this class manage and monitor the jobs throughout their processing within the JPPF driver.
 * @author Laurent Cohen
 */
public class JPPFJobManager implements ServerJobChangeListener, JobNotificationEmitter
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFJobManager.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of jobs to the nodes they are executing on.
   */
  private final Map<String, List<ChannelJobPair>> jobMap = new HashMap<String, List<ChannelJobPair>>();
  /**
   * Mapping of job ids to the corresponding <code>JPPFTaskBundle</code>.
   */
  private final Map<String, ServerJob> bundleMap = new HashMap<String, ServerJob>();
  /**
   * Processes the event queue asynchronously.
   */
  private final ExecutorService executor;
  /**
   * The list of registered listeners.
   */
  private final List<JobListener> eventListeners = new ArrayList<JobListener>();

  /**
   * Default constructor.
   */
  public JPPFJobManager()
  {
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobManager"));
  }

  /**
   * Get all the nodes to which a all or part of a job is dispatched.
   * @param jobUuid the id of the job.
   * @return a list of <code>SelectableChannel</code> instances.
   */
  public synchronized List<ChannelJobPair> getNodesForJob(final String jobUuid)
  {
    if (jobUuid == null) return Collections.emptyList();
    List<ChannelJobPair> list = jobMap.get(jobUuid);
    return list == null ? Collections.<ChannelJobPair>emptyList() : Collections.unmodifiableList(list);
  }

  /**
   * Get the set of ids for all the jobs currently queued or executing.
   * @return an array of ids as strings.
   */
  public synchronized String[] getAllJobIds()
  {
    return jobMap.keySet().toArray(new String[jobMap.size()]);
  }

  /**
   * Get the queued bundle wrapper for the specified job.
   * @param jobUuid the id of the job to look for.
   * @return a <code>ServerJob</code> instance, or null if the job is not queued anymore.
   */
  public synchronized ServerJob getBundleForJob(final String jobUuid)
  {
    return bundleMap.get(jobUuid);
  }

  @Override
  public synchronized void jobDispatched(final ServerJob bundleWrapper, final ExecutorChannel channel, final ServerTaskBundleNode bundleNode)
  {
    JPPFTaskBundle bundle = bundleWrapper.getJob();
    String jobUuid = bundle.getUuid();
    List<ChannelJobPair> list = jobMap.get(jobUuid);
    if (list == null)
    {
      list = new ArrayList<ChannelJobPair>();
      jobMap.put(jobUuid, list);
    }
    list.add(new ChannelJobPair(channel, bundleWrapper));
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : added node " + channel);
    submitEvent(JobEventType.JOB_DISPATCHED, bundle, channel);
  }

  @Override
  public synchronized void jobReturned(final ServerJob bundleWrapper, final ExecutorChannel channel, final ServerTaskBundleNode bundleNode)
  {
    JPPFTaskBundle bundle = bundleWrapper.getJob();
    String jobUuid = bundle.getUuid();
    List<ChannelJobPair> list = jobMap.get(jobUuid);
    if (list == null)
    {
      log.info("attempt to remove node " + channel + " but JobManager shows no node for jobId = " + bundle.getName());
      return;
    }
    list.remove(new ChannelJobPair(channel, bundleWrapper));
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' : removed node " + channel);
    submitEvent(JobEventType.JOB_RETURNED, bundle, channel);
  }

  /**
   * Called when a job is added to the server queue.
   * @param bundleWrapper the queued job.
   */
  public synchronized void jobQueued(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = bundleWrapper.getJob();
    String jobUuid = bundle.getUuid();
    bundleMap.put(jobUuid, bundleWrapper);
    jobMap.put(jobUuid, new ArrayList<ChannelJobPair>());
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' queued");
    submitEvent(JobEventType.JOB_QUEUED, bundle, null);
    JPPFDriver.getInstance().getStatsUpdater().jobQueued(bundle.getTaskCount());
  }

  /**
   * Called when a job is complete and returned to the client.
   * @param bundleWrapper the completed job.
   */
  public synchronized void jobEnded(final ServerJob bundleWrapper)
  {
    if (bundleWrapper == null) throw new IllegalArgumentException("bundleWrapper is null");
    if (bundleWrapper.getJob().getState() == JPPFTaskBundle.State.INITIAL_BUNDLE) return; // skip notifications for initial bundles

    JPPFTaskBundle bundle = bundleWrapper.getJob();
    //long time = System.currentTimeMillis() - (Long) bundle.getParameter(BundleParameter.JOB_RECEIVED_TIME);
    long time = System.currentTimeMillis() - bundleWrapper.getJobReceivedTime();
    String jobUuid = bundle.getUuid();
    jobMap.remove(jobUuid);
    bundleMap.remove(jobUuid);
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' ended");
    submitEvent(JobEventType.JOB_ENDED, bundle, null);
    JPPFDriver.getInstance().getStatsUpdater().jobEnded(time);
  }

  @Override
  public synchronized void jobUpdated(final ServerJob bundleWrapper)
  {
    JPPFTaskBundle bundle = bundleWrapper.getJob();
    if (debugEnabled) log.debug("jobId '" + bundle.getName() + "' updated");
    submitEvent(JobEventType.JOB_UPDATED, bundle, null);
  }

  @Override
  public void jobStatusChanged(final ServerJob source, final SubmissionStatus oldValue, final SubmissionStatus newValue)
  {
    jobUpdated(source);
  }

  /**
   * Submit an event to the event queue.
   * @param eventType the type of event to generate.
   * @param bundle the job data.
   * @param channel the id of the job source of the event.
   */
  private void submitEvent(final JobEventType eventType, final JPPFTaskBundle bundle, final ExecutorChannel channel)
  {
    executor.submit(new JobEventTask(this, eventType, bundle, channel));
  }

  /**
   * Close this job manager and release its resources.
   */
  public synchronized void close()
  {
    executor.shutdownNow();
    jobMap.clear();
    bundleMap.clear();
  }

  /**
   * Add a listener to the list of listeners.
   * @param listener the listener to add to the list.
   */
  public void addJobListener(final JobListener listener)
  {
    synchronized(eventListeners)
    {
      eventListeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of listeners.
   * @param listener the listener to remove from the list.
   */
  public void removeJobListener(final JobListener listener)
  {
    synchronized(eventListeners)
    {
      eventListeners.remove(listener);
    }
  }

  /**
   * Fire job listener event.
   * @param event the event to be fired.
   */
  @Override
  public void fireJobEvent(final JobNotification event)
  {
    if(event == null) throw new IllegalArgumentException("event is null");

    synchronized(eventListeners)
    {
      switch (event.getEventType())
      {
        case JOB_QUEUED:
          for (JobListener listener: eventListeners) listener.jobQueued(event);
          break;

        case JOB_ENDED:
          for (JobListener listener: eventListeners) listener.jobEnded(event);
          break;

        case JOB_UPDATED:
          for (JobListener listener: eventListeners) listener.jobUpdated(event);
          break;

        case JOB_DISPATCHED:
          for (JobListener listener: eventListeners) listener.jobDispatched(event);
          break;

        case JOB_RETURNED:
          for (JobListener listener: eventListeners) listener.jobReturned(event);
          break;

        default:
          throw new IllegalStateException("Unsupported event type: " + event.getEventType());
      }
    }
  }
}
