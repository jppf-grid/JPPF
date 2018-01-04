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

package org.jppf.example.job.dependencies.management;

import java.util.Set;

import org.jppf.example.job.dependencies.DependencyGraph;

/**
 * Concrete implementation of the dependency graph management interface.
 * @author Laurent Cohen
 */
public class DependencyManager implements DependencyManagerMBean {
  @Override
  public void removeNodes(final String... ids) {
    for (String id: ids) {
      if (id != null) DependencyGraph.getInstance().removeNode(id);
    }
  }

  @Override
  public String[] getNodeIds() {
    final Set<String> set = DependencyGraph.getInstance().getNodeIds();
    return set.toArray(new String[set.size()]);
  }
}
