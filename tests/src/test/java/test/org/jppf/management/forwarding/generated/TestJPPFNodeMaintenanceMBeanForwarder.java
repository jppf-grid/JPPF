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

package test.org.jppf.management.forwarding.generated;

import org.jppf.management.JPPFNodeMaintenanceMBean;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.generated.JPPFNodeMaintenanceMBeanForwarder;
import org.jppf.utils.ResultsMap;
import org.junit.Before;
import org.junit.Test;
import test.org.jppf.management.forwarding.AbstractTestForwarderProxy;

/**
 * Test of the forwarding proxy for the {@link JPPFNodeMaintenanceMBean} MBean.
 * MBean description: maintenance operations on the nodes.
 * @since 6.2
 */
public class TestJPPFNodeMaintenanceMBeanForwarder extends AbstractTestForwarderProxy {
  /**
   * Reference to the forwarding proxy.
   */
  private JPPFNodeMaintenanceMBeanForwarder proxy;

  /**
   * Initial setup.
   * @throws Exception if any error occurs.
   */
  @Before
  public void setupInstance() throws Exception {
    if (proxy == null) proxy = (JPPFNodeMaintenanceMBeanForwarder) getForwardingProxy(JPPFNodeMaintenanceMBean.class);
  }

  /**
   * Test invoking the {@code requestResourceCacheReset} operation for all selected nodes.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testRequestResourceCacheReset() throws Exception {
    final ResultsMap<String, Void> results = proxy.requestResourceCacheReset(NodeSelector.ALL_NODES);
    checkResults(results, void.class);
  }
}
