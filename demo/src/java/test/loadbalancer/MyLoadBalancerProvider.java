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

import org.jppf.load.balancer.*;
import org.jppf.load.balancer.spi.JPPFBundlerProvider;
import org.jppf.utils.TypedProperties;

/**
 * Provider implementation for the "MyLoadBalancer" load-balancer.
 */
public class MyLoadBalancerProvider implements JPPFBundlerProvider {
  @Override
  public String getAlgorithmName() {
    // the algorithm name used in the load balancing configuration
    return "MyLoadBalancer";
  }

  @Override
  public Bundler createBundler(final LoadBalancingProfile profile) {
    return new MyLoadBalancer(profile);
  }

  @Override
  public LoadBalancingProfile createProfile(final TypedProperties configuration) {
    return new MyLoadBalancingProfile(configuration);
  }
}
