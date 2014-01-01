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

package org.jppf.server.nio.nodeserver;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jppf.server.protocol.ServerTaskBundleNode;

/**
 * 
 * @author Laurent Cohen
 */
public class OfflineNodeHandler
{
  /**
   * Mapping of job uuid + node bundle id to <code>ServerTaskBundleNode</code> instances.
   */
  private final Map<String, ServerTaskBundleNode> bundleMap = new ConcurrentHashMap<>();

  /**
   * Add the specified node bundle to the map.
   * @param nodeBundle the bundle to add.
   */
  public void addNodeBundle(final ServerTaskBundleNode nodeBundle)
  {
    bundleMap.put(ServerTaskBundleNode.makeKey(nodeBundle), nodeBundle);
  }

  /**
   * Remove the specified node bundle from the map.
   * @param jobUuid the uuid of th job for which to get the node bundle.
   * @param bundleId the id of the bundle to remove.
   * @return the removed node bundle.
   */
  public ServerTaskBundleNode removeNodeBundle(final String jobUuid, final long bundleId)
  {
    return bundleMap.remove(ServerTaskBundleNode.makeKey(jobUuid, bundleId));
  }
}
