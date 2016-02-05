/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.loadbalancer;

import org.jppf.load.balancer.LoadBalancingProfile;
import org.jppf.utils.TypedProperties;

/**
 * Parameters porfile for the "MyLoadBalancer" algorithm.
 * This profile has a single parameter named "sizePerExecutor".
 */
public class MyLoadBalancingProfile implements LoadBalancingProfile {
  /**
   * The size per executor, that is, client-local executor or actual node.
   */
  private int sizePerExecutor;

  /**
   * Default constructor.
   */
  public MyLoadBalancingProfile() {
    sizePerExecutor = MyLoadBalancer.DEFAULT_SIZE;
  }

  /**
   * Intiialize from the configuration.
   * @param config the configuration to use.
   */
  public MyLoadBalancingProfile(final TypedProperties config) {
    sizePerExecutor = config.getInt("sizePerExecutor", MyLoadBalancer.DEFAULT_SIZE);
    if (sizePerExecutor <= 0) sizePerExecutor = MyLoadBalancer.DEFAULT_SIZE;
  }

  @Override
  public LoadBalancingProfile copy() {
    MyLoadBalancingProfile profile = new MyLoadBalancingProfile();
    profile.sizePerExecutor = sizePerExecutor;
    return profile;
  }

  /**
   * Get the size per executor, that is, client-local executor or actual node.
   * @return an int value > 0.
   */
  public int getSizePerExecutor() {
    return sizePerExecutor;
  }
}
