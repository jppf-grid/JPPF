/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.util.*;

import org.jppf.client.monitoring.AbstractRefreshHandler;
import org.jppf.client.monitoring.topology.TopologyNode;
import org.jppf.job.*;
import org.jppf.server.job.management.*;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Performs periodic refreshes of the jobs and job dispatches in one or more drivers and emits event for the associated {@link JobMonitor}.
 * Each refresh involves querying the drivers via JMX for jobs and dispatches information.
 * @author Laurent Cohen
 * @since 5.1
 */
class JobPollingHandler extends AbstractRefreshHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JobPollingHandler.class);
  /**
   * The job monitor.
   */
  private final JobMonitor monitor;

  /**
   * Intiialize this job refresh handler.
   * @param jobMonitor the job monitor.
   * @param refreshInterval interval in milliseconds between refreshes.
   * @param name the name given to this refresher and its timer thread.
   */
  JobPollingHandler(final JobMonitor jobMonitor, final String name, final long refreshInterval) {
    super(name, refreshInterval);
    this.monitor = jobMonitor;
    startRefreshTimer();
  }

  @Override
  protected void performRefresh() {
    try {
      for (JobDriver jobDriver: monitor.getJobDrivers()) {
        DriverJobManagementMBean proxy = jobDriver.getJobManager();
        if (proxy == null) continue;
        try {
          JobInformation[] jobInfos = proxy.getJobInformation(JobSelector.ALL_JOBS);
          Map<String, NodeJobInformation[]> nodeJobInfos = proxy.getNodeInformation(JobSelector.ALL_JOBS);
          refreshJobs(jobDriver, jobInfos);
          for (Map.Entry<String, NodeJobInformation[]> entry: nodeJobInfos.entrySet()) {
            Job job = jobDriver.getJob(entry.getKey());
            refreshDispatches(jobDriver, job, entry.getValue());
          }
        } catch(Exception e) {
          log.error("error getting jobs information for driver {} : {}", jobDriver, ExceptionUtils.getStackTrace(e));
        }
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Refresh the jobs handled by the job monitor.
   * @param jobDriver the driver to which the jobs were submitted.
   * @param jobInfos the information on the jobs currently held in the driver.
   * @throws Exception if any error occurs.
   */
  private void refreshJobs(final JobDriver jobDriver, final JobInformation[] jobInfos) throws Exception {
    List<Job> monitorJobs = jobDriver.getJobs();
    Map<String, JobInformation> jobInfoMap = new HashMap<>();
    for (JobInformation info: jobInfos) jobInfoMap.put(info.getJobUuid(), info);
    Set<String> driverUuids = new HashSet<>(jobInfoMap.keySet());
    List<String> monitorUuids = new ArrayList<>(monitorJobs.size());
    for (Job job: monitorJobs) monitorUuids.add(job.getUuid());
    // handle the jobs to remove
    List<String> toHandle = new ArrayList<>(monitorUuids);
    toHandle.removeAll(driverUuids);
    for (String uuid: toHandle) monitor.jobRemoved(jobDriver, jobDriver.getJob(uuid));
    // handle the jobs to add
    toHandle = new ArrayList<>(driverUuids);
    toHandle.removeAll(monitorUuids);
    for (String uuid: toHandle) monitor.jobAdded(jobDriver, new Job(jobInfoMap.get(uuid)));
    // handle the jobs to update
    toHandle = new ArrayList<>(monitorUuids);
    toHandle.retainAll(driverUuids);
    for (String uuid: toHandle) {
      JobInformation oldInfo = jobDriver.getJob(uuid).getJobInformation();
      JobInformation newInfo = jobInfoMap.get(uuid);
      if (monitor.isJobUpdated(oldInfo, newInfo)) {
        Job job = jobDriver.getJob(uuid);
        job.setJobInformation(newInfo);
        monitor.jobUpdated(jobDriver, job);
      }
    }
  }

  /**
   * Refresh the dispatches for the specified job.
   * @param jobDriver the driver to which the job was submitted.
   * @param job the job to refresh.
   * @param nodeInfos the information on the current job dispatches for the job.
   * @throws Exception if any error occurs.
   */
  private void refreshDispatches(final JobDriver jobDriver, final Job job, final NodeJobInformation[] nodeInfos) throws Exception {
    List<JobDispatch> monitorDispatches = job.getJobDispatches();
    Map<String, NodeJobInformation> nodeJobInfoMap = new HashMap<>();
    for (NodeJobInformation nji: nodeInfos) nodeJobInfoMap.put(nji.getNodeInfo().getUuid(), nji);
    Set<String> driverUuids = new HashSet<>(nodeJobInfoMap.keySet());
    List<String> monitorUuids = new ArrayList<>(monitorDispatches.size());
    for (JobDispatch dispatch: monitorDispatches) monitorUuids.add(dispatch.getUuid());
    // handle the dispatches to remove
    List<String> toHandle = new ArrayList<>(monitorUuids);
    toHandle.removeAll(driverUuids);
    for (String uuid: toHandle) monitor.dispatchRemoved(jobDriver, job, job.getJobDispatch(uuid));
    // handle the dispatches to add
    toHandle = new ArrayList<>(driverUuids);
    toHandle.removeAll(monitorUuids);
    for (String uuid: toHandle) {
      NodeJobInformation nji = nodeJobInfoMap.get(uuid);
      TopologyNode node = monitor.getTopologyManager().getNode(uuid);
      if (node != null) monitor.dispatchAdded(jobDriver, job, new JobDispatch(nji.getJobInformation(), node));
    }
  }
}
