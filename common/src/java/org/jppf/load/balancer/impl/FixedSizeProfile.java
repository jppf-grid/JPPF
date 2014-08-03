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

package org.jppf.load.balancer.impl;

import org.jppf.load.balancer.LoadBalancingProfile;
import org.jppf.utils.TypedProperties;

/**
 * Profile for the fixed bundle size load-balancing algorithm.
 * @author Laurent Cohen
 */
public class FixedSizeProfile implements LoadBalancingProfile
{
  /**
   * The bundle size.
   */
  private int size = 1;

  /**
   * Default constructor.
   */
  public FixedSizeProfile()
  {
  }

  /**
   * Initialize this profile with values read from the specified configuration.
   * @param config contains a mapping of the profile parameters to their value.
   */
  public FixedSizeProfile(final TypedProperties config)
  {
    size = config.getInt("size", 1);
  }

  /**
   * Make a copy of this profile.
   * @return a newly created <code>FixedSizeProfile</code> instance.
   * @see org.jppf.load.balancer.LoadBalancingProfile#copy()
   */
  @Override
  public LoadBalancingProfile copy()
  {
    FixedSizeProfile other = new FixedSizeProfile();
    other.setSize(size);
    return other;
  }

  /**
   * Get the bundle size.
   * @return the bundle size as an int.
   */
  public int getSize()
  {
    return size;
  }

  /**
   * Set the bundle size.
   * @param size the bundle size as an int.
   */
  public void setSize(final int size)
  {
    this.size = size;
  }
}
