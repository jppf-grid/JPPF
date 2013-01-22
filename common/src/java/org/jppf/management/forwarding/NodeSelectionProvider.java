/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.management.forwarding;

import org.jppf.management.NodeSelector;

/**
 * Interface that provides a semantic of matching a node against a selector.
 */
public interface NodeSelectionProvider
{
  /**
   * Determine whether the specified selector accepts the specified node.
   * @param nodeUuid the uuid of the node to check.
   * @param selector the node selector used as a filter.
   * @return a set of {@link AbstractNodeContext} instances.
   */
  boolean isNodeAccepted(String nodeUuid, NodeSelector selector);
}