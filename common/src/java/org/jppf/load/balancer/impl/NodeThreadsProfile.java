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

import org.jppf.load.balancer.*;
import org.jppf.utils.TypedProperties;

/**
 * Profile for the "nodethreads" load-balancing algorithm.
 * @author Laurent Cohen
 */
public class NodeThreadsProfile extends AbstractLoadBalancingProfile {
  /**
   * The multiplicator for the number of threads in the node.
   * The max number of tasks sent to the node will be <code>multiplicator * number_of_threads</code>.
   */
  private int multiplicator = 1;

  /**
   * Default constructor.
   */
  public NodeThreadsProfile() {
  }

  /**
   * Initialize this profile with values read from the specified configuration.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public NodeThreadsProfile(final TypedProperties config) {
    multiplicator = config.getInt("multiplicator", 1);
    if (multiplicator < 1) multiplicator = 1;
  }

  /**
   * Get the multiplicator for the number of threads in the node.
   * @return the multiplicator size as an int.
   */
  public int getMultiplicator() {
    return multiplicator;
  }

  /**
   * Set the multiplicator for the number of threads in the node.
   * @param multiplicator the bundle size as an int.
   */
  public void setMultiplicator(final int multiplicator) {
    if (multiplicator >= 1) this.multiplicator = multiplicator;
  }
}
