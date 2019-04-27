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

package org.jppf.node.protocol.graph;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
@FunctionalInterface
public interface TaskNodeVisitor {
  /**
   * Visit the specified node.
   * @param node the node to visit.
   * @return the result of the node's visit as a {@link TaskNodeVisitResult} enum element.
   */
  TaskNodeVisitResult visitTaskNode(JobTaskNode node);

  /**
   * Called before visiting the specified node.
   * @param node the node to visit.
   */
  default void preVisitNode(JobTaskNode node) {
  }

  /**
   * Called after visiting the specified node.
   * @param node the node to visit.
   */
  default void postVisitNode(JobTaskNode node) {
  }
}
