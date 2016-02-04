/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import org.jppf.server.node.JPPFNode;

/**
 * Implementation of the {@link JPPFNodeMaintenanceMBean} MBean interface.
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFNodeMaintenance implements JPPFNodeMaintenanceMBean
{
  /**
   * The node whose this MBean is registered with.
   */
  private transient JPPFNode node = null;

  /**
   * Initialize this node management bean with the specified node.
   * @param node the node whose state is monitored.
   */
  public JPPFNodeMaintenance(final JPPFNode node)
  {
    this.node = node;
  }

  @Override
  public void requestResourceCacheReset() throws Exception
  {
    node.requestResourceCacheReset();
  }
}
