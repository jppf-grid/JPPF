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
package org.jppf.load.balancer;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the interface of all strategies for defining bundle task size.
 * A Bundler defines the current bundle size using different algorithms, depending on the implementation.
 * @author Domingos Creado
 * @author Laurent Cohen
 */
public interface Bundler {
  /**
   * Count of the bundlers used to generate a readable unique id.
   * @exclude
   */
  AtomicInteger BUNDLER_COUNT = new AtomicInteger(0);

  /**
   * Get the current bundle size.
   * @return the bundle size as an int value.
   */
  int getBundleSize();

  /**
   * Provide feedback from a channel after execution of a set of tasks.
   * The feedback data consists in providing a number of tasks that were executed, and their total execution time in milliseconds.
   * The execution time includes the network round trip between node and server.
   * @param nbTasks number of tasks that were executed.
   * @param totalTime the total execution and transport time in nanoseconds.
   */
  void feedback(final int nbTasks, final double totalTime);

  /**
   * Make a copy of this bundler.
   * Which parts are actually copied depends on the implementation.
   * @return a new <code>Bundler</code> instance.
   * @deprecated this method is not needed anymore, all Bundler instantiations are done via the declared {@link org.jppf.load.balancer.spi.JPPFBundlerProvider JPPFBundlerProvider}s.
   */
  Bundler copy();

  /**
   * Get the timestamp at which this bundler was created.
   * This is used to enable node channels to know when the bundler settings have changed.
   * @return the timestamp as a long value.
   */
  long getTimestamp();

  /**
   * Release the resources used by this bundler.
   */
  void dispose();

  /**
   * Perform context-independent initializations.
   */
  void setup();

  /**
   * Get the parameters profile used by this load-balancer.
   * @return an instance of <code>LoadBalancingProfile</code>.
   */
  LoadBalancingProfile getProfile();
}
