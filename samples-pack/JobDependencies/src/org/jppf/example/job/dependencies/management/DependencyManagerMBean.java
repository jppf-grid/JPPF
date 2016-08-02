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

package org.jppf.example.job.dependencies.management;

/**
 * A simple management interface for the server-side job dependency graph.
 * @author Laurent Cohen
 */
public interface DependencyManagerMBean {
  /**
   * Name of this server-side MBean.
   */
  String MBEAN_NAME = "org.jppf:name=dependency.manager,type=driver";

  /**
   * Remove the jobs with the specified ids from the dependency graph.
   * @param ids the ids of the jobs to remove.
   */
  void removeNodes(final String...ids);

  /**
   * Get the ids of all the nodes currently in the graph
   * @return an array of node ids.
   */
  String[] getNodeIds();
}
