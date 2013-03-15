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

package org.jppf.ui.monitoring.node;

import java.util.EventObject;

/**
 * 
 * @author Laurent Cohen
 */
public class TopologyChangeEvent extends EventObject
{
  /**
   * Data for the driver.
   */
  private final TopologyData driverData;
  /**
   * Data for the node, if any.
   */
  private final TopologyData nodeData;
  /**
   * Data for the peer, if any.
   */
  private final TopologyData peerData;

  /**
   * Initialize this event.
   * @param source the source of this event.
   * @param driverData the driver data.
   * @param nodeData the node data.
   * @param peerData the peer data.
   */
  public TopologyChangeEvent(final NodeDataPanel source, final TopologyData driverData, final TopologyData nodeData, final TopologyData peerData)
  {
    super(source);
    this.driverData = driverData;
    this.nodeData = nodeData;
    this.peerData = peerData;
  }

  /**
   * Get the driver data.
   * @return a {@link TopologyData} instance.
   */
  public TopologyData getDriverData()
  {
    return driverData;
  }

  /**
   * Get the node data.
   * @return a {@link TopologyData} instance.
   */
  public TopologyData getNodeData()
  {
    return nodeData;
  }

  /**
   * Get the peer data.
   * @return a {@link TopologyData} instance.
   */
  public TopologyData getPeerData()
  {
    return peerData;
  }

  /**
   * Get the node panel which emitted this event.
   * @return a {@link NodeDataPanel} instance.
   */
  public NodeDataPanel getNodePanel()
  {
    return (NodeDataPanel) getSource();
  }
}
