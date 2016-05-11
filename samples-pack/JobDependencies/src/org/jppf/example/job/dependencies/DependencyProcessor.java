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

package org.jppf.example.job.dependencies;

import java.util.List;

import javax.management.*;

import org.jppf.job.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.server.JPPFDriver;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.startup.JPPFDriverStartupSPI;
import org.slf4j.*;

/**
 * This job dependency processor is deployed as a <a href="http://www.jppf.org/doc/5.2/index.php?title=JPPF_startup_classes#Server_startup_classes">driver startup class</a>.
 * <p>At startup time, it registers itself as a listener to the server's job management MBean notifications, to receive jobs lifecycle events.
 * Based on these events it will:
 * <ul>
 * <li>incrementally build / update the jobs dependency graph</li>
 * <li>resume jobs when they have no more pending dependencies</li>
 * <li>detect cycles in the dependency graph and react accordingly</li>
 * </ul>
 * @author Laurent Cohen
 */
public class DependencyProcessor implements NotificationListener, JPPFDriverStartupSPI {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DependencyProcessor.class);
  /**
   * Proxy to the job management MBean, used to suspend/resume jobs and register for job notifications.
   */
  DriverJobManagementMBean jobManager;
  /**
   * Indicator that lazy initialization occurred once.
   */
  private boolean initialized = false;

  /**
   * Default contructor.
   */
  public DependencyProcessor() {
    Utils.print("processor: In %s()", getClass().getSimpleName());
  }

  // Implementation of the NotificationListener interface

  @Override
  public synchronized void handleNotification(final Notification notification, final Object handback) {
    try {
      DependencyGraph graph = DependencyGraph.getInstance();
      JobNotification jobNotif = (JobNotification) notification;
      JobInformation info = jobNotif.getJobInformation();
      String jobUuid = info.getJobUuid();
      String jobName = info.getJobName();

      switch(jobNotif.getEventType()) {
        // job received from a client
        case JOB_QUEUED:
          Utils.print("processor: '%s' was queued", jobName);
          JPPFDistributedJob job = getJob(jobUuid);
          DependencySpec spec = job.getMetadata().getParameter(DependencySpec.DEPENDENCIES_METADATA_KEY, null);
          // incrementally update the dependency graph
          DependencyNode node = graph.addNode(spec, jobUuid);
          if (node.hasPendingDependency()) {
            Utils.print("processor: suspending '%s'", spec.getId());
            jobManager.suspendJob(jobUuid, true); // suspend the job
          } else {
            Utils.print("processor: resuming '%s'", spec.getId());
            jobManager.resumeJob(jobUuid); // resume the job
          }
          break;

        // job has completed
        case JOB_ENDED:
          Utils.print("processor: '%s' has ended", jobName);
          // Retrieve the jobs whose only remaining dependency is the current job and resume them
          List<DependencyNode> toResume = graph.jobEnded(jobUuid);
          for (DependencyNode jobNode: toResume) {
            Utils.print("processor: resuming '%s'", jobNode.getId());
            jobManager.resumeJob(jobNode.getJobUuid());
          }
          node = graph.getNodeByJobUuid(jobUuid);
          if ((node != null) && node.isRemoveUponCompletion()) graph.removeNode(node);
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get a job from the server given its uuid.
   * @param jobUuid the uuid of the job to lookup.
   * @return a {@link JPPFDistributedJob} object, or {@code null} if there is no job with this uuid.
   */
  private JPPFDistributedJob getJob(final String jobUuid) {
    return JPPFDriver.getInstance().getJob(jobUuid);
  }

  // Implementation of the JPPFDriverStartupSPI interface

  /**
   * Perform the initialization of the job manager.
   */
  @Override
  public final void run() {
    Utils.print("processor: Initializing %s", getClass().getSimpleName());
    initialized = true;
    try {
      // create a connection to the local (same JVM as the server) JMX server
      JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper();
      jmx.connect();
      jobManager = jmx.getJobManager();
      // register for notifications of job events
      jobManager.addNotificationListener(this, null, null);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
