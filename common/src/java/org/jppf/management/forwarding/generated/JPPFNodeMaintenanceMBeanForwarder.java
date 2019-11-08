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

package org.jppf.management.forwarding.generated;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFNodeMaintenanceMBean;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.AbstractNodeForwardingProxy;
import org.jppf.utils.ResultsMap;

/**
 * Forwarding proxy for the {@link JPPFNodeMaintenanceMBean} MBean.
 * MBean description: maintenance operations on the nodes.
 * @since 6.2
 */
public class JPPFNodeMaintenanceMBeanForwarder extends AbstractNodeForwardingProxy {
  /**
   * Initialize this proxy.
   * @param jmx a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs..
   */
  public JPPFNodeMaintenanceMBeanForwarder(final JMXDriverConnectionWrapper jmx) throws Exception {
    super(jmx, "org.jppf:name=node.maintenance,type=node");
  }

  /**
   * Invoke the {@code requestResourceCacheReset} operation for all selected nodes (request a reset of the resource caches of all the JPPF class loaders maintained by the node).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> requestResourceCacheReset(final NodeSelector selector) throws Exception {
    return invoke(selector, "requestResourceCacheReset");
  }
}
