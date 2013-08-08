/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
import java.util.*;

import org.jppf.node.policy.ExecutionPolicy;

/**
 * Marker interface for selecting nodes when using the {@link org.jppf.management.forwarding.JPPFNodeForwardingMBean JPPFNodeForwardingMBean} methods.
 * @author Laurent Cohen
 */
public interface NodeSelector extends Serializable
{
  /**
   * Constant for a selector which accepts all nodes.
   */
  NodeSelector ALL_NODES = new AllNodesSelector();

  /**
   * Selects all nodes.
   */
  public final static class AllNodesSelector implements NodeSelector
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public AllNodesSelector()
    {
    }
  }

  /**
   * Selects nodes based on an {@link ExecutionPolicy}.
   */
  public final static class ExecutionPolicySelector implements NodeSelector
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The execution policy to use to select the nodes.
     */
    private final ExecutionPolicy policy;

    /**
     * Initialize this selector with the specified execution policy.
     * @param policy the execution policy to use to select the nodes.
     */
    public ExecutionPolicySelector(final ExecutionPolicy policy)
    {
      this.policy = policy;
    }

    /**
     * Get the execution policy to use to select the nodes.
     * @return an {@link ExecutionPolicy}.
     */
    public ExecutionPolicy getPolicy()
    {
      return policy;
    }
  }

  /**
   * Selects nodes based on their uuids.
   */
  public final static class UuidSelector implements NodeSelector
  {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The list of uuids of the nodes to select. This list is immutable.
     */
    private final Collection<String> uuids;

    /**
     * Initialize this selector with the specified list of node UUIDs.
     * @param uuids the uuids of the nodes to select.
     */
    public UuidSelector(final Collection<String> uuids)
    {
      this.uuids = (uuids == null) ? Collections.<String>emptyList() : new ArrayList<>(uuids);
    }

    /**
     * Initialize this selector with the specified array of node UUIDs.
     * @param uuids the uuids of the nodes to select.
     */
    public UuidSelector(final String...uuids)
    {
      if (uuids == null) this.uuids = Collections.<String>emptyList();
      else
      {
        this.uuids = new ArrayList<>(uuids.length);
        for (String uuid: uuids) this.uuids.add(uuid);
      }
    }

    /**
     * Get the list of uuids of the nodes to select. This list is immutable.
     * @return a collection of uuids as strings.
     */
    public Collection<String> getUuids()
    {
      return uuids;
    }
  }
}
