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

package org.jppf.client.monitoring.topology;

import org.jppf.management.*;

/**
 * Implementation of {@link TopologyDriver} for JPPF nodes.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyNode extends AbstractTopologyComponent {
  /**
   * Object describing the current state of a node.
   */
  private JPPFNodeState nodeState = null;
  /**
   * The number of slaves for a master node.
   */
  private int nbSlaveNodes = -1;
  /**
   * The status of the node.
   */
  private TopologyNodeStatus status = TopologyNodeStatus.UP;

  /**
   * Initialize this topology data as holding information about a node.
   * @param nodeInformation information on the JPPF node.
   */
  TopologyNode(final JPPFManagementInfo nodeInformation) {
    this(nodeInformation, new JPPFNodeState());
  }

  /**
   * Initialize this topology data as holding information about a node.
   * @param nodeInformation information on the JPPF node.
   * @param nodeState the current state of this node.
   */
  TopologyNode(final JPPFManagementInfo nodeInformation, final JPPFNodeState nodeState) {
    super(nodeInformation.getUuid());
    this.managementInfo = nodeInformation;
    this.nodeState = nodeState;
  }

  /**
   * This method always returns {@code true}.
   * @return {@code true}.
   */
  @Override
  public boolean isNode() {
    return true;
  }
  /**
   * Get the object describing the current state of a node.
   * @return a <code>JPPFNodeState</code> instance.
   */
  public JPPFNodeState getNodeState() {
    return nodeState;
  }

  /**
   * Refresh the state of the node represented by this topology data.
   * @param newState the new node state fetched from the grid.
   * @exclude
   */
  public void refreshNodeState(final JPPFNodeState newState) {
    this.nodeState = newState;
    setStatus(this.nodeState == null ? TopologyNodeStatus.DOWN : TopologyNodeStatus.UP);
  }

  /**
   * Get the status of the node.
   * @return the node status.
   * @exclude
   */
  public TopologyNodeStatus getStatus() {
    return status;
  }

  /**
   * Set the status of the node.
   * @param status the node status.
   * @exclude
   */
  public void setStatus(final TopologyNodeStatus status) {
    if (status == TopologyNodeStatus.DOWN)
      this.status = status;
  }

  /**
   * Get the number of slaves for a master node.
   * @return the number of slaves as an int.
   */
  public int getNbSlaveNodes() {
    return nbSlaveNodes;
  }

  /**
   * Set the number of slaves for a master node.
   * @param nbSlaveNodes the number of slaves as an int.
   * @exclude
   */
  public void setNbSlaveNodes(final int nbSlaveNodes) {
    this.nbSlaveNodes = nbSlaveNodes;
  }
}
