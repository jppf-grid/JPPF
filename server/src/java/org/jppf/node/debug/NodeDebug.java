/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.node.debug;

import java.util.List;

import org.jppf.server.node.*;

/**
 *
 * @author Laurent Cohen
 */
public class NodeDebug implements NodeDebugMBean {
  /**
   * Reference to the node where this mbean is registered.
   */
  private final JPPFNode node;

  /**
   * Initialize this MBean.
   * @param node a reference to the node where this mbean is registered.
   */
  public NodeDebug(final JPPFNode node) {
    this.node = node;
  }

  @Override
  public String[] getClassloaderCache() {
    if (node == null) return new String[] {"node reference is null"};
    AbstractClassLoaderManager mgr = node.getClassLoaderManager();
    List<JPPFContainer> list = mgr.getContainerList();
    String[] result = new String[list.size() + 1];
    int count = 0;
    result[count++] = node.getClassLoader().toString();
    for (JPPFContainer cont: list) result[count++] = cont.getClassLoader().toString();
    return result;
  }
}
