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

package org.jppf.node.throttling;

import org.jppf.node.Node;

/**
 * Provides a callback to determine when a node accepts new jobs
 * @author Laurent Cohen
 * @since 6.1
 */
public interface JPPFNodeThrottling {
  /**
   * Determine whether the node accepts new jobs.
   * @param node the node to which the throttling applies.
   * @return {@code true} if the node accepts new jobs, {@code false} otherwise.
   */
  boolean acceptsNewJobs(Node node);
}
