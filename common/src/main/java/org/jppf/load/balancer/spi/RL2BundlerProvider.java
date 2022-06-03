/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.load.balancer.spi;

import org.jppf.load.balancer.Bundler;
import org.jppf.load.balancer.impl.*;
import org.jppf.utils.TypedProperties;

/**
 * Provider implementation for the reinforcement learning load-balancing algorithm.
 * @author Laurent Cohen
 */
public class RL2BundlerProvider implements JPPFBundlerProvider<RL2Profile> {
  /**
   * Create a bundler instance using the specified parameters profile.
   * @param profile encapsulates the parameters of this algorithm.
   * @return an instance of the bundler implementation defined by this provider.
   */
  @Override
  public Bundler<RL2Profile> createBundler(final RL2Profile profile) {
    return new RL2Bundler(profile);
  }

  /**
   * Create a bundler profile containing the parameters of the algorithm.
   * @param configuration a set of properties defining the algorithm's parameters.
   * @return an {@link RL2Profile} instance.
   */
  @Override
  public RL2Profile createProfile(final TypedProperties configuration) {
    return new RL2Profile(configuration);
  }

  /**
   * Get the name of the algorithm defined by this provider.
   * @return the algorithm's name as a string.
   */
  @Override
  public String getAlgorithmName() {
    return "rl2";
  }
}
