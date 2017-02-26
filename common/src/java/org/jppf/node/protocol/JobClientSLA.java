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

package org.jppf.node.protocol;

/**
 * This interface represents the Service Level Agreement between a JPPF job and a JPPF client.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public class JobClientSLA extends JobCommonSLA<JobClientSLA> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The maximum number of nodes this job can run on.
   * The default value is set to <code>1</code> to preserve backward compatibility, by emulating the behavior of previous versions.
   */
  private int maxChannels = 1;

  /**
   * Default constructor.
   * @exclude
   */
  public JobClientSLA() {
  }

  /**
   * Get the maximum number of channels, including the local execution channel if it is enabled, this job can be sent on.
   * @return the number of channels as an int value.
   */
  public int getMaxChannels() {
    return maxChannels;
  }

  /**
   * Set the maximum number of channels, including the local execution channel if it is enabled, this job can be sent on.
   * @param maxChannels the number of channels as an int value. A value <= 0 means no limit on the number of channels.
   * @return this SLA, for mathod chaining.
   */
  public JobClientSLA setMaxChannels(final int maxChannels) {
    this.maxChannels = maxChannels > 0 ? maxChannels : Integer.MAX_VALUE;
    return this;
  }

  /**
   * Create a copy of this job SLA.
   * @return a {@link JPPFJobClientSLA} instance.
   */
  public JobClientSLA copy() {
    JobClientSLA sla = new JobClientSLA();
    copyTo(sla);
    sla.setMaxChannels(maxChannels);
    return sla;
  }
}
