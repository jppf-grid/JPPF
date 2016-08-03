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
package org.jppf.server.job;

import org.jppf.execute.ExecutorChannel;
import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.*;
import org.jppf.server.protocol.ServerJob;

/**
 * Instances of this class are submitted into an event queue and generate actual
 * job manager events that are then dispatched to registered listeners.
 */
public class JobEventTask implements Runnable {
  /**
   * The job manager that submits the events.
   */
  private final JobNotificationEmitter jobManager;
  /**
   * The type of event to generate.
   */
  private final JobEventType eventType;
  /**
   * Creation timestamp for this task.
   */
  private final long timestamp = System.currentTimeMillis();
  /**
   * Inofrmation on the job.
   */
  private final JobInformation jobInfo;
  /**
   * Information on the node.
   */
  private final JPPFManagementInfo nodeInfo;

  /**
   * Initialize this job manager event task with the specified parameters.
   * @param jobManager the job manager that submits the events.
   * @param eventType the type of event to generate.
   * @param bundle the task bundle data.
   * @param job the job data.
   * @param channel the id of the job source of the event.
   */
  public JobEventTask(final JobNotificationEmitter jobManager, final JobEventType eventType, final TaskBundle bundle, final ServerJob job, final ExecutorChannel<?> channel) {
    this.jobManager = jobManager;
    this.eventType = eventType;
    JobSLA sla = null;
    if (job != null) {
      sla = job.getSLA();
      jobInfo = new JobInformation(job.getUuid(), job.getName(), job.getTaskCount(), job.getInitialTaskCount(), sla.getPriority(), job.isSuspended(), job.isPending());
    } else {
      sla = bundle.getSLA();
      jobInfo = new JobInformation(bundle.getUuid(), bundle.getName(), bundle.getCurrentTaskCount(), bundle.getInitialTaskCount(), sla.getPriority(), sla.isSuspended(),
          bundle.getParameter(BundleParameter.JOB_PENDING, false));
    }
    jobInfo.setMaxNodes(sla.getMaxNodes());
    nodeInfo = (channel == null) ? null : channel.getManagementInfo();
  }

  /**
   * Execute this task.
   */
  @Override
  public void run() {
    JobNotification event = new JobNotification(jobManager.getEmitterUuid(), eventType, jobInfo, nodeInfo, timestamp);
    jobManager.fireJobEvent(event);
  }
}
