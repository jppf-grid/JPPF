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

package org.jppf.management;

import java.io.Serializable;

/**
 * Marker interface for selecting nodes when using the {@link org.jppf.management.forwarding.NodeForwardingMBean NodeForwardingMBean} methods.
 * @author Laurent Cohen
 */
public interface NodeSelector extends Serializable {
  /**
   * Constant for a selector which accepts all nodes.
   */
  NodeSelector ALL_NODES = new org.jppf.management.AllNodesSelector();

  /**
   * Determine whether a node is accepted by this selector.
   * @param nodeInfo information on the node.
   * @return {@code true} if the node is accepted, {@code false} otherwise.
   */
  boolean accepts(JPPFManagementInfo nodeInfo);
}
