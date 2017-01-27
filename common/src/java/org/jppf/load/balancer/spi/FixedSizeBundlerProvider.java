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

package org.jppf.load.balancer.spi;

import org.jppf.load.balancer.Bundler;
import org.jppf.load.balancer.impl.*;
import org.jppf.utils.TypedProperties;

/**
 * Provider implementation for the fixed bundle size load-balancing algorithm.
 * @author Laurent Cohen
 */
public class FixedSizeBundlerProvider implements JPPFBundlerProvider<FixedSizeProfile> {
  /**
   * Create a bundler instance using the specified parameters profile.
   * @param profile an <code>FixedSizeProfile</code> instance.
   * @return an instance of the bundler implementation defined by this provider.
   */
  @Override
  public Bundler<FixedSizeProfile> createBundler(final FixedSizeProfile profile) {
    return new FixedSizeBundler(profile);
  }

  /**
   * Create a bundler profile containing the parameters of the algorithm.
   * @param configuration a set of properties defining the algorithm's parameters.
   * @return an {@link org.jppf.load.balancer.impl.FixedSizeProfile FixedSizeProfile} instance.
   */
  @Override
  public FixedSizeProfile createProfile(final TypedProperties configuration) {
    return new FixedSizeProfile(configuration);
  }

  /**
   * Get the name of the algorithm defined by this provider.
   * @return the algorithm's name as a string.
   */
  @Override
  public String getAlgorithmName() {
    return "manual";
  }
}
