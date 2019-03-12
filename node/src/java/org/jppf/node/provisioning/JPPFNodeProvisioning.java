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

package org.jppf.node.provisioning;

import javax.management.*;

import org.jppf.node.Node;
import org.jppf.server.node.JPPFNode;
import org.jppf.utils.TypedProperties;

/**
 * Implementation of the {@link JPPFNodeProvisioningMBean} interface.
 * @author Laurent Cohen
 * @since 4.1
 * @exclude
 */
public class JPPFNodeProvisioning extends NotificationBroadcasterSupport implements JPPFNodeProvisioningMBean {
  /**
   * The slave node manager, to which all operations are delegated.
   */
  private final SlaveNodeManager slaveManager;
  /**
   * Whether the node is actually a slave.
   */
  private final boolean master;

  /**
   * Initialize this MBean.
   * @param node the JPPF node that holds this manager.
   */
  public JPPFNodeProvisioning(final Node node) {
    slaveManager = ((JPPFNode) node).getSlaveManager();
    slaveManager.mbean = this;
    master = node.isMasterNode();
  }

  @Override
  public int getNbSlaves() {
    return slaveManager.nbSlaves();
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes) {
    if (master) slaveManager.submitProvisioningRequest(nbNodes, true, null);
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes, final boolean interruptIfRunning) {
    if (master) slaveManager.submitProvisioningRequest(nbNodes, interruptIfRunning, null);
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes, final TypedProperties configOverrides) {
    if (master) slaveManager.submitProvisioningRequest(nbNodes, true, configOverrides);
  }

  @Override
  public void provisionSlaveNodes(final int nbNodes, final boolean interruptIfRunning, final TypedProperties configOverrides) {
    if (master) slaveManager.submitProvisioningRequest(nbNodes, interruptIfRunning, configOverrides);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener) throws ListenerNotFoundException {
    if (master) super.removeNotificationListener(listener);
  }

  @Override
  public void removeNotificationListener(final NotificationListener listener, final NotificationFilter filter, final Object handback) throws ListenerNotFoundException {
    if (master) super.removeNotificationListener(listener, filter, handback);
  }

  @Override
  public void sendNotification(final Notification notification) {
    if (master) super.sendNotification(notification);
  }
}
