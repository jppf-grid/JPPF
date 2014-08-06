/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.jppf.node.policy.ExecutionPolicy;

/**
 * This class represents the Service Level Agreement Between a JPPF job and a server.
 * It determines the state, conditions and order in which a job will be executed.
 * @author Laurent Cohen
 */
public class JPPFJobClientSLA extends AbstractCommonSLA implements JobClientSLA
{
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
   */
  public JPPFJobClientSLA()
  {
  }

  /**
   * Initialize this job SLA with the specified execution policy.
   * @param policy the tasks execution policy.
   */
  public JPPFJobClientSLA(final ExecutionPolicy policy)
  {
    super(policy);
  }

  @Override
  public int getMaxChannels()
  {
    return maxChannels;
  }

  @Override
  public void setMaxChannels(final int maxChannels)
  {
    this.maxChannels = maxChannels > 0 ? maxChannels : Integer.MAX_VALUE;
  }

  /**
   * Create a copy of this job SLA.
   * @return a {@link JPPFJobClientSLA} instance.
   */
  public JPPFJobClientSLA copy()
  {
    JPPFJobClientSLA sla = new JPPFJobClientSLA();
    copyTo(sla);
    sla.setMaxChannels(maxChannels);
    return sla;
  }
}
