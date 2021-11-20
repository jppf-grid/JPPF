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

import org.jppf.load.balancer.*;
import org.jppf.utils.TypedProperties;

/**
 * <p>Interface for all load-balancing algorithm providers.
 * An implementation of this interface shall be provided for each load-balancing algorithm, to enable its dynamic discovery by JPPF components.
 * <p>To integrate a load-balancing algorithm provider, the following steps should be performed:
 * <ul>
 * <li>At one of the classpath roots, ensure that there is a folder named META-INF/services</li>
 * <li>In this folder, create or edit a file named {@code org.jppf.load.balancer.spi.JPPFBundlerProvider}</li>
 * <li>In this file, add a line containing the fully qualified name of the class implementing the {@link JPPFBundlerProvider} interface</li>
 * </ul>
 * @param <T> the type of parameters profile used by the bundler.
 * @author Laurent Cohen
 */
public interface JPPFBundlerProvider<T extends LoadBalancingProfile> {
  /**
   * Get the name of the algorithm defined by this provider. Each algorithm must have a name distinct from that of all other algorithms.
   * @return the algorithm's name as a string.
   */
  String getAlgorithmName();

  /**
   * Create a bundler instance using the specified parameters profile.
   * The parameters profile is created by JPPF internally via a call to {@link #createProfile(TypedProperties)}.
   * @param profile an {@link Bundler} instance.
   * @return an instance of the bundler implementation defined by this provider.
   */
  Bundler<T> createBundler(T profile);

  /**
   * <p>Create a bundler profile containing the parameters of the algorithm.
   * <p>The configuration parameter contains a set of properties that define the parameters names and values.<br>
   * The parameter names are provided <i>without any JPPF configuration-specific prefix</i>.
   * <p>For example: if the JPPF configuration file specifies a profile named "myProfile" (through the property "jppf.load.balancing.profile = myProfile"),
   * and the algorithm has a parameter named "{@code myParameter}", then in the configuration file it will be specified as "{@code jppf.load.balancing.profile.myProfile.myParameter = some_value}".<br>
   * When this method is called, only the parameter name is kept, and its definition becomes "{@code myParameter = some_value}".
   * @param configuration a set of properties defining the algorithm's parameters.
   * @return an {@link LoadBalancingProfile} instance.
   */
  T createProfile(TypedProperties configuration);
}
