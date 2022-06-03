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

package org.jppf.server.event;

import java.util.EventObject;

import org.jppf.management.JPPFManagementInfo;

/**
 * Event emitted when a node connects to or disconnects from the server.
 * @author Laurent Cohen
 */
public class NodeConnectionEvent extends EventObject {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Initialize this event with the specified source.
   * @param nodeInfo a {@link JPPFManagementInfo} instance.
   */
  public NodeConnectionEvent(final JPPFManagementInfo nodeInfo) {
    super(nodeInfo);
  }

  /**
   * Get the node information for this event.
   * @return a {@link JPPFManagementInfo} instance.
   */
  public JPPFManagementInfo getNodeInformation() {
    return (JPPFManagementInfo) getSource();
  }
}
