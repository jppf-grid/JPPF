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

import org.jppf.management.JPPFManagementInfo;


/**
 * Instances of this class represent peer drivers as nodes for the driver they are connected to.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyPeer extends TopologyNode {
  /**
   * The peer driver.
   */
  protected String peerUuid;

  /**
   * Initialize this topology data as holding information about a node.
   * @param managementInfo information on this peer driver.
   * @param peerUuid the peer driver uuid.
   */
  public TopologyPeer(final JPPFManagementInfo managementInfo, final String peerUuid) {
    super(managementInfo);
    this.peerUuid = peerUuid;
  }

  @Override
  public boolean isPeer() {
    return true;
  }

  @Override
  public boolean isNode() {
    return false;
  }

  /**
   * Get the uuid of the referenced peer driver.
   * @return the peer uuid.
   */
  public String getPeerUuid() {
    return peerUuid;
  }

  /**
   * Set the uuid of the referenced peer driver.
   * @param peerUuid the peer uuid.
   */
  public void setPeerUuid(final String peerUuid) {
    this.peerUuid = peerUuid;
  }
}
