/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.node.policy;

import org.jppf.node.protocol.*;
import org.jppf.utils.stats.JPPFStatistics;

/**
 * Instances of this class provide contextual information to execution policies.
 * It contains information on the job, if any, against which the policy is evaluated, and on the server for server-side policies.
 * @since 5.0
 * @author Laurent Cohen
 */
public class PolicyContext {
  /**
   * The job server side SLA, set at runtime by the server.
   */
  private final JobSLA sla;
  /**
   * The job client side SLA, set at runtime by the server.
   */
  private final JobClientSLA clientSLA;
  /**
   * The job metadata, set at runtime by the server.
   */
  private final JobMetadata metadata;
  /**
   * Number of nodes the job is already dispatched to.
   */
  private final int jobDispatches;
  /**
   * The server statistics.
   */
  private final JPPFStatistics stats;

  /**
   * Initialize this policy context.
   * @param sla the job server side SLA, set at runtime by the server.
   * @param clientSLA the job client side SLA, set at runtime by the server.
   * @param metadata the job metadata, set at runtime by the server.
   * @param jobDispatches the number of nodes the job is already dispatched to.
   * @param stats the server statistics.
   * @exclude
   */
  public PolicyContext(final JobSLA sla, final JobClientSLA clientSLA, final JobMetadata metadata, final int jobDispatches, final JPPFStatistics stats) {
    this.sla = sla;
    this.clientSLA = clientSLA;
    this.metadata = metadata;
    this.jobDispatches = jobDispatches;
    this.stats = stats;
  }

  /**
   * Get the job server side SLA, set at runtime by the server.
   * @return a {@link JobSLA} object.
   */
  public JobSLA getSLA() {
    return sla;
  }

  /**
   * Get the job client side SLA, set at runtime by the server.
   * @return a {@link JobClientSLA} object.
   */
  public JobClientSLA getClientSLA() {
    return clientSLA;
  }

  /**
   * Get the job metadata, set at runtime by the server.
   * @return a {@link JobMetadata} object.
   */
  public JobMetadata getMetadata() {
    return metadata;
  }

  /**
   * Get the number of nodes the job is already dispatched to.
   * @return the number of nodes as an int.
   */
  public int getJobDispatches() {
    return jobDispatches;
  }

  /**
   * Get the server statistics.
   * @return a {@link JPPFStatistics} object.
   */
  public JPPFStatistics getStats() {
    return stats;
  }
}
