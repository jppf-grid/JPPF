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

import org.jppf.node.protocol.*;

/**
 * Bundler implementations should implement this interface if they wish to have access to a job's information.
 * @author Laurent Cohen
 */
public interface JobAwarenessEx {
  /**
   * Get the current job for which load-balancing is being performed.
   * @return a {@link JPPFDistributedJob} instance.
   */
  JPPFDistributedJob getJob();

  /**
   * Set the current job for which load-balancing is being performed.
   * This method should never be called directly, as it is set automatically be the JPPF load balancing framework.
   * @param job a {@link JPPFDistributedJob} instance.
   */
  void setJob(JPPFDistributedJob job);
}
