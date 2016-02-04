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

package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.LoadBalancingProfile;
import org.jppf.utils.TypedProperties;

/**
 * Profile for the "nodethreads" load-balancing algorithm.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeThreadsProfile implements LoadBalancingProfile
{
  /**
   * The multiplicator for the number of threads in the node.
   * The max number of tasks sent to the node will be <code>multiplicator * number_of_threads</code>.
   */
  private int multiplicator = 1;

  /**
   * Default constructor.
   */
  public NodeThreadsProfile()
  {
  }

  /**
   * Initialize this profile with values read from the specified configuration.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public NodeThreadsProfile(final TypedProperties config)
  {
    multiplicator = config.getInt("multiplicator", 1);
    if (multiplicator < 1) multiplicator = 1;
  }

  /**
   * Make a copy of this profile.
   * @return a newly created <code>FixedSizeProfile</code> instance.
   * @see org.jppf.load.balancer.LoadBalancingProfile#copy()
   */
  @Override
  public LoadBalancingProfile copy()
  {
    NodeThreadsProfile other = new NodeThreadsProfile();
    other.setMultiplicator(multiplicator);
    return other;
  }

  /**
   * Get the multiplicator for the number of threads in the node.
   * @return the multiplicator size as an int.
   */
  public int getMultiplicator()
  {
    return multiplicator;
  }

  /**
   * Set the multiplicator for the number of threads in the node.
   * @param multiplicator the bundle size as an int.
   */
  public void setMultiplicator(final int multiplicator)
  {
    if (multiplicator >= 1) this.multiplicator = multiplicator;
  }
}
