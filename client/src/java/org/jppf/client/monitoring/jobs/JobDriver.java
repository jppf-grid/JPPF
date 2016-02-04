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

import java.util.*;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.server.job.management.DriverJobManagementMBean;

/**
 * AN instance of this class represents a JPPF driver in the jobs hierarchy.
 * It is essentially a container for a {@link TopologyDriver} with the additional ability to navigate the jobs hierarchy, which is orthogonal to the topology hierarchy.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobDriver extends AbstractJobComponent {
  /**
   * The associated driver from the topology monitor.
   */
  private final TopologyDriver driver;

  /**
   * Initialize this job driver with the specified topology driver.
   * @param driver the object that represent a JPPF driver.
   */
  JobDriver(final TopologyDriver driver) {
    super(driver.getUuid());
    this.driver = driver;
  }

  /**
   * Get the proxy to the driver MBean that manages and monitors jobs.
   * @return an instance of {@link DriverJobManagementMBean}.
   */
  public DriverJobManagementMBean getJobManager() {
    return driver.getJobManager();
  }

  /**
   * Get the associated driver from the topology monitor.
   * @return an instance of {@link TopologyDriver}.
   */
  public TopologyDriver getTopologyDriver() {
    return driver;
  }

  /**
   * Get a job handled by this driver from its uuid.
   * @param jobUuid the uuid of the job to retrieve.
   * @return a {@link Job} instance, or {@code null} if the driver has no such job.
   */
  public Job getJob(final String jobUuid) {
    return (Job) getChild(jobUuid);
  }

  /**
   * Get the list of jobs handled by this driver.
   * @return a list of {@link Job} instances, possibly empty.
   */
  public List<Job> getJobs() {
    List<Job> list = new ArrayList<>(getChildCount());
    for (AbstractJobComponent comp: getChildren()) list.add((Job) comp);
    return list;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", driver=").append(driver);
    sb.append(']');
    return sb.toString();
  }

  @Override
  public String getDisplayName() {
    return driver != null ? driver.getDisplayName() : "" + uuid;
  }
}
