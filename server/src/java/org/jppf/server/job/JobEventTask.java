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

import org.jppf.job.*;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.protocol.JobSLA;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.jppf.server.protocol.*;

/**
 * Instances of this class are submitted into an event queue and generate actual
 * job manager events that are then dispatched to registered listeners.
 */
public class JobEventTask implements Runnable
{
  /**
   * The job manager that submits the events.
   */
  private final JPPFJobManager jobManager;
  /**
   * The type of event to generate.
   */
  private final JobEventType eventType;
  /**
   * The node, if any, for which the event happened.
   */
  private final ChannelWrapper channel;
  /**
   * The job data.
   */
  private final JPPFTaskBundle bundle;
  /**
   * Creation timestamp for this task.
   */
  private final long timestamp = System.currentTimeMillis();

  /**
   * Initialize this job manager event task with the specified parameters.
   * @param jobManager - the job manager that submits the events.
   * @param eventType - the type of event to generate.
   * @param bundle - the job data.
   * @param channel - the id of the job source of the event.
   */
  public JobEventTask(final JPPFJobManager jobManager, final JobEventType eventType, final JPPFTaskBundle bundle, final ChannelWrapper channel)
  {
    this.jobManager = jobManager;
    this.eventType = eventType;
    this.channel = channel;
    this.bundle = bundle;
  }

  /**
   * Execute this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    JobSLA sla = bundle.getSLA();
    Boolean pending = (Boolean) bundle.getParameter(BundleParameter.JOB_PENDING);
    JobInformation jobInfo = new JobInformation(bundle.getUuid(), bundle.getName(), bundle.getTaskCount(),
        bundle.getInitialTaskCount(), sla.getPriority(), sla.isSuspended(), (pending != null) && pending);
    jobInfo.setMaxNodes(sla.getMaxNodes());
    JPPFManagementInfo nodeInfo = (channel == null) ? null : ((AbstractNodeContext) channel.getContext()).getManagementInfo();
    JobNotification event = new JobNotification(eventType, jobInfo, nodeInfo, timestamp);
    if(eventType == JobEventType.JOB_UPDATED)
    {
      Integer n = (Integer) bundle.getParameter(BundleParameter.REAL_TASK_COUNT);
      if (n != null) jobInfo.setTaskCount(n);
    }

    jobManager.fireJobEvent(event);
  }
}
