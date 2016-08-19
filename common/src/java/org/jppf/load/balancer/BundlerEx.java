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

package org.jppf.load.balancer;

/**
 * A bundler which provides a more advanced feedback method with additional performance data.
 * @author Laurent Cohen
 */
public interface BundlerEx extends Bundler
{
  /**
   * Feedback the bundler with the result of using the bundle with the specified size.
   * The feedback data consists in providing a number of tasks that were executed, and their total execution time in milliseconds.
   * The execution time includes the network round trip between node and server.
   * @param nbTasks number of tasks that were executed.
   * @param totalTime the total execution and transport time in nanoseconds.
   * @param accumulatedElapsed the total accumalated elapsed time (in the node) in nanoseconds for the execution of the tasks.
   * @param overheadTime the transport time in nanoseconds.
   */
  void feedback(final int nbTasks, final double totalTime, final double accumulatedElapsed, final double overheadTime);
}
