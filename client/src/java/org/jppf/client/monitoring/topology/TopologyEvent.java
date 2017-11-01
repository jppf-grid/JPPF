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

import java.util.EventObject;

/**
 * Instances of this class represent topology events, including addition/removal of a driver,
 * addition/removal/update of a node and addition/removal of a peer driver connected as a node.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyEvent extends EventObject {
  /**
   * Data for the driver.
   */
  private final TopologyDriver driverData;
  /**
   * Data for the node, if any.
   */
  private final TopologyNode nodeData;
  /**
   * The type of update.
   */
  private final UpdateType updateType;

  /**
   * The possible types of events.
   * @exclude
   */
  enum Type {
    /**
     * A driver was added.
     */
    DRIVER_ADDED,
    /**
     * A driver was removed.
     */
    DRIVER_REMOVED,
    /**
     * A driver was updated.
     */
    DRIVER_UPDATED,
    /**
     * A node was added.
     */
    NODE_ADDED,
    /**
     * A node was removed.
     */
    NODE_REMOVED,
    /**
     * A node was updated.
     */
    NODE_UPDATED
  };

  /**
   * The types of updates for an event.
   * @exclude
   */
  public enum UpdateType {
    /**
     * Topology update: driver or node has been added or removed.
     */
    TOPOLOGY,
    /**
     * JVM health snapshot was updated.
     */
    JVM_HEALTH,
    /**
     * Node state was updated.
     */
    NODE_STATE,
  }

  /**
   * Initialize this event.
   * @param source the source of this event.
   * @param driverData the driver data.
   * @param nodeData the node data.
   * @param updateType the type of update.
   * @exclude
   */
  public TopologyEvent(final TopologyManager source, final TopologyDriver driverData, final TopologyNode nodeData, final UpdateType updateType) {
    super(source);
    this.driverData = driverData;
    this.nodeData = nodeData;
    this.updateType = updateType;
  }

  /**
   * Get the driver data.
   * @return a {@link TopologyDriver} instance.
   */
  public TopologyDriver getDriver() {
    return driverData;
  }

  /**
   * Get the node data.
   * @return a {@link TopologyNode} instance.
   */
  public TopologyNode getNodeOrPeer() {
    return nodeData;
  }

  /**
   * Get the topology manager which emitted this event.
   * @return a {@link TopologyManager} instance.
   */
  public TopologyManager getTopologyManager() {
    return (TopologyManager) getSource();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("source=").append(getTopologyManager());
    sb.append(", driver=").append(getDriver());
    sb.append(", node/peer=").append(getNodeOrPeer());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Get the type of update.
   * @return an {@link UpdateType} enum value.
   * @exclude
   */
  public UpdateType getUpdateType() {
    return updateType;
  }
}
