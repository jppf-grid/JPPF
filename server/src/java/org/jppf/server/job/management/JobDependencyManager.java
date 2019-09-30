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

package org.jppf.server.job.management;

import java.util.Set;

import org.jppf.node.protocol.graph.JobDependencyGraph;
import org.jppf.server.JPPFDriver;

/**
 * Concrete implementation of the job dependency graph management interface.
 * @author Laurent Cohen
 */
public class JobDependencyManager implements JobDependencyManagerMBean {
  /**
   * The JPPF driver.
   */
  final JPPFDriver driver;
  /**
   * 
   */
  private final JobDependencyGraph graph;

  /**
   * 
   * @param driver the JPPF driver.
   */
  public JobDependencyManager(final JPPFDriver driver) {
    this.driver = driver;
    this.graph = driver.getQueue().getDependenciesHandler().getGraph();
  }

  @Override
  public void removeNodes(final String... ids) {
    for (String id: ids) {
      if (id != null) graph.removeNode(id);
    }
  }

  @Override
  public Set<String> getNodeIds() {
    return graph.getNodeIds();
  }
}
