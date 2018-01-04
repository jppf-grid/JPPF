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

package org.jppf.client.monitoring.topology;

import org.jppf.management.*;

/**
 * Implementation of {@link TopologyDriver} for JPPF nodes.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyNode extends AbstractTopologyComponent {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * EMpty state used when no state can be determined.
   */
  private static final JPPFNodeState EMPTY_STATE = new JPPFNodeState();
  /**
   * Object describing the current state of a node.
   */
  private transient JPPFNodeState nodeState = null;
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
   * @exclude
   */
  public TopologyNode(final JPPFManagementInfo nodeInformation) {
    this(nodeInformation, EMPTY_STATE);
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
   * Initialize this topology data as holding information about a node.
   * @param uuid the node uuid.
   */
  TopologyNode(final String uuid) {
    super(uuid);
    this.managementInfo = null;
    this.nodeState = EMPTY_STATE;
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

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("managementInfo=").append(managementInfo);
    sb.append(", uuid=").append(uuid);
    sb.append(", nodeState=").append(nodeState);
    sb.append(']');
    //return (jmx == null) ? (managementInfo == null ? "?" : managementInfo.toDisplayString()) : jmx.getDisplayName();
    return sb.toString();
  }

  /**
   * Get the currently pending action forthis node, if any.
   * @return the pending action as a {@link NodePendingAction} enum element.
   */
  public NodePendingAction getPendingAction() {
    return (nodeState == null) ? null : nodeState.getPendingAction();
  }

  /**
   * Convenience method to get the driver this node is attached to as a {@link TopologyDriver} instance.
   * @return a {@link TopologyDriver} if this node is a real node, or {@code null} if this is a peer server.
   * @since 5.1
   */
  public TopologyDriver getDriver() {
    return isNode() ? (TopologyDriver) getParent() : null;
  }

  @Override
  public String getDisplayName() {
    return managementInfo != null ? managementInfo.toDisplayString() : toString();
  }
}
