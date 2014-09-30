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

package org.jppf.ui.monitoring.topology;

import java.util.EventObject;

/**
 * 
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
   * Data for the peer, if any.
   */
  private final TopologyPeer peerData;

  /**
   * The possible types of events.
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
   * Initialize this event.
   * @param source the source of this event.
   * @param driverData the driver data.
   * @param nodeData the node data.
   * @param peerData the peer data.
   */
  public TopologyEvent(final TopologyManager source, final TopologyDriver driverData, final TopologyNode nodeData, final TopologyPeer peerData) {
    super(source);
    this.driverData = driverData;
    this.nodeData = nodeData;
    this.peerData = peerData;
  }

  /**
   * Get the driver data.
   * @return a {@link TopologyDriver} instance.
   */
  public TopologyDriver getDriverData() {
    return driverData;
  }

  /**
   * Get the node data.
   * @return a {@link TopologyNode} instance.
   */
  public TopologyNode getNodeData() {
    return nodeData;
  }

  /**
   * Get the peer data.
   * @return a {@link TopologyPeer} instance.
   */
  public TopologyPeer getPeerData() {
    return peerData;
  }

  /**
   * Get the topology manager which emitted this event.
   * @return a {@link TopologyManager} instance.
   */
  public TopologyManager getTopologyManager() {
    return (TopologyManager) getSource();
  }
}
