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

package org.jppf.management;

import java.io.Serializable;
import java.util.Collection;

import org.jppf.node.policy.ExecutionPolicy;

/**
 * Marker interface for selecting nodes when using the {@link org.jppf.management.forwarding.JPPFNodeForwardingMBean JPPFNodeForwardingMBean} methods.
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

  /**
   * Selects all nodes.
   * @deprecated use {@link org.jppf.management.AllNodesSelector} instead.
   */
  public static class AllNodesSelector extends org.jppf.management.AllNodesSelector {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;
  }

  /**
   * Selects nodes based on an {@link ExecutionPolicy}.
   * @deprecated use {@link org.jppf.management.ExecutionPolicySelector} instead.
   */
  public static class ExecutionPolicySelector extends org.jppf.management.ExecutionPolicySelector {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this selector with the specified execution policy.
     * @param policy the execution policy to use to select the nodes.
     * @deprecated use {@link org.jppf.management.ExecutionPolicySelector} instead.
     */
    public ExecutionPolicySelector(final ExecutionPolicy policy) {
      super(policy);
    }
  }

  /**
   * Selects nodes based on their uuids.
   * @deprecated use {@link org.jppf.management.UuidSelector} instead.
   */
  public static class UuidSelector extends org.jppf.management.UuidSelector {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this selector with the specified list of node UUIDs.
     * @param uuids the uuids of the nodes to select.
     * @deprecated use {@link org.jppf.management.UuidSelector} instead.
     */
    public UuidSelector(final Collection<String> uuids) {
      super(uuids);
    }

    /**
     * Initialize this selector with the specified array of node UUIDs.
     * @param uuids the uuids of the nodes to select.
     * @deprecated use {@link org.jppf.management.UuidSelector} instead.
     */
    public UuidSelector(final String...uuids) {
      super(uuids);
    }
  }
}
