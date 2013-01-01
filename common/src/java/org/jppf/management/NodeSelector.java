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

package org.jppf.management;

import java.io.Serializable;
import java.util.*;

import org.jppf.node.policy.ExecutionPolicy;

/**
 * Marker interface for selecting nodes when using the {@link JPPFNodeForwardingMBean} methods.
 * @author Laurent Cohen
 */
public interface NodeSelector extends Serializable
{
  /**
   * Selects all nodes.
   */
  public final static class AllNodesSelector implements NodeSelector
  {
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
     * The execution policy to use to select the nodes.
     */
    private final ExecutionPolicy policy;

    /**
     * Default constructor.
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
     * The list of uuids of the nodes to select. This list is immutable.
     */
    private final List<String> uuidList;

    /**
     * Default constructor.
     * @param uuidList the uuids of the nodes to select.
     */
    public UuidSelector(final List<String> uuidList)
    {
      this.uuidList = (uuidList == null) ? Collections.EMPTY_LIST : Collections.unmodifiableList(uuidList);
    }

    /**
     * Default constructor.
     * @param uuids the uuids of the nodes to select.
     */
    public UuidSelector(final String...uuids)
    {
      this.uuidList = (uuids == null) ? Collections.EMPTY_LIST : Collections.unmodifiableList(Arrays.asList(uuids));
    }

    /**
     * Get the list of uuids of the nodes to select. This list is immutable.
     * @return a list of uuids as strings.
     */
    public List<String> getUuidList()
    {
      return uuidList;
    }
  }
}
