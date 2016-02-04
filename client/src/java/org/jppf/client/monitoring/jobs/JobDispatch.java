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

import org.jppf.client.monitoring.topology.TopologyNode;
import org.jppf.job.JobInformation;

/**
 * This class represents the dispatch of a job to a node.
 * @author Laurent Cohen
 * @since 5.1
 */
public class JobDispatch extends AbstractJobComponent {
  /**
   * Information on the node.
   */
  private final TopologyNode node;
  /**
   * Information on the job.
   */
  private final JobInformation jobInfo;

  /**
   * Initialize this job with the supplied information.
   * @param jobInfo information on the job.
   * @param node information on the node.
   */
  JobDispatch(final JobInformation jobInfo, final TopologyNode node) {
    super(node.getUuid());
    this.jobInfo = jobInfo;
    this.node = node;
  }

  /**
   * Get the information on the node for ths job dispatch.
   * @return a {@link TopologyNode} instance.
   */
  public TopologyNode getNode() {
    return node;
  }

  /**
   * Get the job to which this dispatch belongs.
   * @return a {@link Job} instance.
   */
  public Job getJob() {
    return (Job) getParent();
  }

  /**
   * Get the information on the job.
   * @return an instance of {@link JobInformation}.
   */
  public synchronized JobInformation getJobInformation() {
    return jobInfo;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("uuid=").append(uuid);
    sb.append(", jobInfo=").append(jobInfo);
    sb.append(", node=").append(node);
    sb.append(']');
    return sb.toString();
  }


  @Override
  public String getDisplayName() {
    return node != null ? node.getDisplayName() : "" + uuid;
  }
}
