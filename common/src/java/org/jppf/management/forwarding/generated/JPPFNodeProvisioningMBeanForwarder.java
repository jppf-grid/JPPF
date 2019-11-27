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

import javax.management.Notification;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.NodeSelector;
import org.jppf.management.forwarding.AbstractMBeanForwarder;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.node.provisioning.JPPFProvisioningInfo;
import org.jppf.utils.ResultsMap;
import org.jppf.utils.TypedProperties;

/**
 * Forwarding proxy for the {@link JPPFNodeProvisioningMBean} MBean.
 * MBean description: interface for provisioning, managing and monitoring slave nodes.
 * <p>This Mbean emits notification of type {@link Notification}:
 * <br>- notification that a slave node has started or stopped.
 * <br>- user data type: {@link JPPFProvisioningInfo}.
 * @since 6.2
 */
public class JPPFNodeProvisioningMBeanForwarder extends AbstractMBeanForwarder {
  /**
   * Initialize this proxy.
   * @param jmx a {@link JMXDriverConnectionWrapper} instance.
   * @throws Exception if any error occurs..
   */
  public JPPFNodeProvisioningMBeanForwarder(final JMXDriverConnectionWrapper jmx) throws Exception {
    super(jmx, "org.jppf:name=provisioning,type=node");
  }

  /**
   * Get the value of the {@code NbSlaves} attribute for all selected nodes (the number of slave nodes started by this MBean).
   * @param selector a {@link NodeSelector} instance.
   * @return a mapping of node uuids to {@link Integer} instances.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Integer> getNbSlaves(final NodeSelector selector) throws Exception {
    return getAttribute(selector, "NbSlaves");
  }

  /**
   * Invoke the {@code provisionSlaveNodes} operation for all selected nodes (provision the specified number of slave nodes, starting new ones or stopping existing ones as needed).
   * @param selector a {@link NodeSelector} instance.
   * @param slaves a {@code Integer}.
   * @param configOverrides a {@link TypedProperties} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> provisionSlaveNodes(final NodeSelector selector, final int slaves, final TypedProperties configOverrides) throws Exception {
    return invoke(selector, "provisionSlaveNodes", new Object[] { slaves, configOverrides }, new String[] { int.class.getName(), TypedProperties.class.getName() });
  }

  /**
   * Invoke the {@code provisionSlaveNodes} operation for all selected nodes (provision the specified number of slave nodes, starting new ones or stopping existing ones as needed).
   * @param selector a {@link NodeSelector} instance.
   * @param slaves a {@code Integer}.
   * @param interrupt a {@code Boolean}.
   * @param configgOverrides a {@link TypedProperties} instance.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> provisionSlaveNodes(final NodeSelector selector, final int slaves, final boolean interrupt, final TypedProperties configgOverrides) throws Exception {
    return invoke(selector, "provisionSlaveNodes", new Object[] { slaves, interrupt, configgOverrides }, new String[] { int.class.getName(), boolean.class.getName(), TypedProperties.class.getName() });
  }

  /**
   * Invoke the {@code provisionSlaveNodes} operation for all selected nodes (provision the specified number of slave nodes, starting new ones or stopping existing ones as needed).
   * @param selector a {@link NodeSelector} instance.
   * @param slaves a {@code Integer}.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> provisionSlaveNodes(final NodeSelector selector, final int slaves) throws Exception {
    return invoke(selector, "provisionSlaveNodes", new Object[] { slaves }, new String[] { int.class.getName() });
  }

  /**
   * Invoke the {@code provisionSlaveNodes} operation for all selected nodes (provision the specified number of slave nodes, starting new ones or stopping existing ones as needed).
   * @param selector a {@link NodeSelector} instance.
   * @param slaves a {@code Integer}.
   * @param interruptIfRunning a {@code Boolean}.
   * @return a mapping of node uuids to objects that wrap either {@code null} or an exeption.
   * @throws Exception if any error occurs.
   */
  public ResultsMap<String, Void> provisionSlaveNodes(final NodeSelector selector, final int slaves, final boolean interruptIfRunning) throws Exception {
    return invoke(selector, "provisionSlaveNodes", new Object[] { slaves, interruptIfRunning }, new String[] { int.class.getName(), boolean.class.getName() });
  }
}
