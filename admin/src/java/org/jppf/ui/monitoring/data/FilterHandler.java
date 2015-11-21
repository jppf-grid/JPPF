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

package org.jppf.ui.monitoring.data;

import org.jppf.client.monitoring.topology.TopologyManager;

/**
 * Handles the node filtering.
 * @author Laurent Cohen
 * @since 5.2
 */
public class FilterHandler {
  /**
   * The singleton instance of this class.
   */
  private static final FilterHandler INSTANCE = new FilterHandler();
  /**
   * The topology manager used by the admin console.
   */
  private final TopologyManager manager = StatsHandler.getInstance().getTopologyManager();

  /**
   * Activate or deactivate the node filtering with the specified execution policy.
   * @param executionPolicyString the XML execution policy to use.
   */
  public void activateFilter(final String executionPolicyString) {
  }
}
