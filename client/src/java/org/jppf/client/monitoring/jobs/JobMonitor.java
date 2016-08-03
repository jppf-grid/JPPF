/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.client.monitoring.jobs;

import static org.jppf.client.monitoring.jobs.JobMonitoringEvent.Type.*;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jppf.client.monitoring.topology.*;
import org.jppf.job.JobInformation;
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * This class provides a representation of the jobs and corresponding node dispatches executing in a JPPF grid.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobMonitor extends TopologyListenerAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobMonitor.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The topology manager.
   */
  private final TopologyManager topologyManager;
  /**
   * Mapping of job drivers to their uuid.
   */
  private final Map<String, JobDriver> driverMap = new HashMap<>();
  /**
   * Mapping of each job to all the drivers it was submitted to.
   */
  private final CollectionMap<String, JobDriver> jobDriverMap = new SetHashMap<>();
  /**
   * For synchronized access and modifications.
   */
  private final Object lock = new Object();
  /**
   * The list of {@link JobMonitoringListener}s.
   */
  private final List<JobMonitoringListener> listeners = new CopyOnWriteArrayList<>();
  /**
   * The object that receives information on the jobs and publishes it a job monitoring events.
   */
  final Object refreshHandler;
  /**
   * Initialize this job manager with the specified topology manager in {@link JobMonitorUpdateMode#IMMEDIATE_NOTIFICATIONS IMMEDIATE_NOTFICATIONS} mode.
   * @param topologyManager the topology manager to use.
   * @param listeners optional listeners that can be registered immediately with this job monitor.
   */
  public JobMonitor(final TopologyManager topologyManager, final JobMonitoringListener...listeners) {
    this(JobMonitorUpdateMode.IMMEDIATE_NOTIFICATIONS, 0L, topologyManager, listeners);
  }

  /**
   * Initialize this job manager with the specified topology manager and event mode.
   * @param updateMode the update mode which determines how job monitoring events are generated.
   * @param period the interval between publications of updates. This is not used in {@link JobMonitorUpdateMode#IMMEDIATE_NOTIFICATIONS IMMEDIATE_NOTFICATIONS} mode.
   * @param topologyManager the topology manager to use.
   * @param listeners optional listeners that can be registered immediately with this job monitor.
   */
  public JobMonitor(final JobMonitorUpdateMode updateMode, final long period, final TopologyManager topologyManager, final JobMonitoringListener...listeners) {
    if (debugEnabled) log.debug(String.format("initializing job monitor in %s mode with period = %d", updateMode, period));
    this.topologyManager = topologyManager;
    if (listeners != null) {
      for (JobMonitoringListener listener: listeners) addJobMonitoringListener(listener);
    }
    for (TopologyDriver driver: topologyManager.getDrivers()) driverAdded(new JobDriver(driver));
    topologyManager.addTopologyListener(this);
    switch(updateMode) {
      case POLLING:
        refreshHandler = new JobPollingHandler(this, "JobRefreshHandler", period);
        break;
      case DEFERRED_NOTIFICATIONS:
        refreshHandler = new DeferredJobNotificationsHandler(this, "JobRefreshHandler", period);
        break;
      case IMMEDIATE_NOTIFICATIONS:
      default:
        refreshHandler = new ImmediateJobNotificationsHandler(this);
        break;
    }
  }

  /**
   * Get the topology manager associated with this job monitor.
   * @return a {@link TopologyManager} object.
   */
  public TopologyManager getTopologyManager() {
    return topologyManager;
  }

  /**
   * Get the driver with the specified uuid.
   * @param driverUuid the uuid of the driver to retrieve.
   * @return a {@link JobDriver} object, or {@code null} if there si no such driver.
   */
  public JobDriver getJobDriver(final String driverUuid) {
    synchronized(driverMap) {
      return driverMap.get(driverUuid);
    }
  }

  /**
   * Get the drivers monitored by this job monitor.
   * @return a list of {@link JobDriver} objects.
   */
  public List<JobDriver> getJobDrivers() {
    synchronized(driverMap) {
      return new ArrayList<>(driverMap.values());
    }
  }

  /**
   * Get the drivers to which a job was submitted, either in parallel from the same client, or from one driver to another in a multi-driver topology.
   * @param jobUuid the uuid of the job for which to find the drivers.
   * @return a list of {@link JobDriver} instances, possibly empty.
   */
  public List<JobDriver> getDriversForJob(final String jobUuid) {
    List<JobDriver> result = new ArrayList<>();
    synchronized(lock) {
      Collection<JobDriver> drivers = jobDriverMap.getValues(jobUuid);
      if (drivers != null) result.addAll(drivers);
    }
    return result;
  }

  /**
   * Get the dispatches of the specified job accrosss the entire topology. This will return a list of dispatches where the nodes may be attached to more than one server.
   * @param jobUuid the uuid of the job for which to find the dispatches.
   * @return a list of {@link JobDispatch} instances, possibly empty.
   */
  public List<JobDispatch> getAllJobDispatches(final String jobUuid) {
    List<JobDispatch> result = new ArrayList<>();
    synchronized(lock) {
      Collection<JobDriver> drivers = jobDriverMap.getValues(jobUuid);
      if (drivers != null) {
        for (JobDriver driver: drivers) {
          Job job = driver.getJob(jobUuid);
          if (job != null) result.addAll(job.getJobDispatches());
        }
      }
    }
    return result;
  }

  /**
   * Add a listener to the events emitted by this job monitor.
   * @param listener the listener to add.
   */
  public void addJobMonitoringListener(final JobMonitoringListener listener) {
    if (listener != null) listeners.add(listener);
  }

  /**
   * Remove a listener to the events emitted by this job monitor.
   * @param listener the listener to remove.
   */
  public void removeJobMonitoringListener(final JobMonitoringListener listener) {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void driverAdded(final TopologyEvent event) {
    driverAdded(new JobDriver(event.getDriver()));
  }

  /**
   * Called when a driver is added.
   * @param driver the driver to add.
   */
  void driverAdded(final JobDriver driver) {
    if (debugEnabled) log.debug("driver {} added", driver.getDisplayName());
    synchronized(lock) {
      if (driverMap.get(driver.getUuid()) != null) return;
      driverMap.put(driver.getUuid(), driver);
    }
    dispatchEvent(DRIVER_ADDED, new JobMonitoringEvent(this, driver, null, null));
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  public void driverRemoved(final TopologyEvent event) {
    String uuid = event.getDriver().getUuid();
    JobDriver driver = null;
    synchronized(lock) {
      driver = driverMap.get(uuid);
    }
    if (driver != null) driverRemoved(driver);
  }

  /**
   * Called when a driver is removed.
   * @param driver the driver to remove.
   */
  void driverRemoved(final JobDriver driver) {
    if (debugEnabled) log.debug("driver {} removed", driver.getDisplayName());
    synchronized(lock) {
      driverMap.remove(driver);
    }
    dispatchEvent(DRIVER_REMOVED, new JobMonitoringEvent(this, driver, null, null));
  }

  /**
   * Called when a job is added to the specified driver.
   * @param driver the driver where the job was received.
   * @param job the job to add.
   */
  void jobAdded(final JobDriver driver, final Job job) {
    if (debugEnabled) log.debug("job '{}' added to driver {}", job.getDisplayName(), driver.getDisplayName());
    driver.add(job);
    synchronized(lock) {
      jobDriverMap.putValue(job.getUuid(), driver);
    }
    dispatchEvent(JOB_ADDED, new JobMonitoringEvent(this, driver, job, null));
  }

  /**
   * Called when a job is removed from the specified driver.
   * @param driver the driver where the job was received.
   * @param job the job to remove.
   */
  void jobRemoved(final JobDriver driver, final Job job) {
    if (debugEnabled) log.debug("job '{}' removed from driver {}", job.getDisplayName(), driver.getDisplayName());
    driver.remove(job);
    synchronized(lock) {
      jobDriverMap.removeValue(job.getUuid(), driver);
    }
    dispatchEvent(JOB_REMOVED, new JobMonitoringEvent(this, driver, job, null));
  }

  /**
   * Called when a job in the specified driver is updated.
   * @param driver the driver where the job was received.
   * @param job the job to update.
   */
  void jobUpdated(final JobDriver driver, final Job job) {
    if (debugEnabled) log.debug("job '{}' updated in driver {}", job.getDisplayName(), driver.getDisplayName());
    dispatchEvent(JOB_UPDATED, new JobMonitoringEvent(this, driver, job, null));
  }

  /**
   * Called when a job in the specified driver is dispatched to a node.
   * @param driver the driver where the job was received.
   * @param job the job to update.
   * @param dispatch the job dispatch to add.
   */
  void dispatchAdded(final JobDriver driver, final Job job, final JobDispatch dispatch) {
    if (debugEnabled) log.debug("adding dispatch {} to job '{}'", dispatch.getDisplayName(), job.getDisplayName());
    job.add(dispatch);
    dispatchEvent(DISPATCH_ADDED, new JobMonitoringEvent(this, driver, job, dispatch));
  }

  /**
   * Called when a job in the specified driver returns from a node.
   * @param driver the driver where the job was received.
   * @param job the job to update.
   * @param dispatch the job dispatch to remove.
   */
  void dispatchRemoved(final JobDriver driver, final Job job, final JobDispatch dispatch) {
    if (debugEnabled) log.debug("removing dispatch {} from job '{}'", (dispatch == null ? "null" : dispatch.getDisplayName()), job.getDisplayName());
    if (dispatch != null) {
      job.remove(dispatch);
      dispatchEvent(DISPATCH_REMOVED, new JobMonitoringEvent(this, driver, job, dispatch));
    }
  }

  /**
   * Dispatch the specified event to all registered listeners.
   * @param type the type of event to dispatch.
   * @param event the event to dispatch.
   */
  void dispatchEvent(final JobMonitoringEvent.Type type, final JobMonitoringEvent event) {
    try {
      switch(type) {
        case DRIVER_ADDED:
          for (JobMonitoringListener listener: listeners) listener.driverAdded(event);
          break;
  
        case DRIVER_REMOVED:
          for (JobMonitoringListener listener: listeners) listener.driverRemoved(event);
          break;
  
        case JOB_ADDED:
          for (JobMonitoringListener listener: listeners) listener.jobAdded(event);
          break;
  
        case JOB_REMOVED:
          for (JobMonitoringListener listener: listeners) listener.jobRemoved(event);
          break;
  
        case JOB_UPDATED:
          for (JobMonitoringListener listener: listeners) listener.jobUpdated(event);
          break;
  
        case DISPATCH_ADDED:
          for (JobMonitoringListener listener: listeners) listener.jobDispatchAdded(event);
          break;
  
        case DISPATCH_REMOVED:
          for (JobMonitoringListener listener: listeners) listener.jobDispatchRemoved(event);
          break;
  
        default:
          break;
      }
    } catch(Exception e) {
      log.error(String.format("error dispatching event of type %s, event=%, exception: %s", type, event, ExceptionUtils.getStackTrace(e)));
    }
  }

  /**
   * Detemrine whether a job was updated by comapring its current and latest informtion.
   * @param oldJob the current job information.
   * @param newJob the latest job information received from the driver.
   * @return {@code true} if any of the job information changed, {@code false} otherwise.
   */
  boolean isJobUpdated(final JobInformation oldJob, final JobInformation newJob) {
    return (oldJob.getTaskCount() != newJob.getTaskCount()) || (oldJob.getMaxNodes() != newJob.getMaxNodes()) ||
        (oldJob.getPriority() != newJob.getPriority()) || (oldJob.isSuspended() ^ newJob.isSuspended()) || (oldJob.isPending() ^ newJob.isPending());
  }
}
