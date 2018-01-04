/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.client.monitoring.AbstractComponent;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.diagnostics.HealthSnapshot;

/**
 * Base superclass for components of a JPPF grid topology.
 * This class proivdes an API to navigate the topology tree and attributes common to all ellements in te tree.
 * @author Laurent Cohen
 * @since 5.0
 */
public abstract class AbstractTopologyComponent extends AbstractComponent<AbstractTopologyComponent> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Object describing the current health snapshot of a node or driver.
   */
  protected transient HealthSnapshot healthSnapshot = new HealthSnapshot();
  /**
   * The management informtation on this topology component.
   */
  protected transient JPPFManagementInfo managementInfo;

  /**
   * Initialize this component witht he specified uuid.
   * @param uuid the uuid assigned to this component.
   */
  AbstractTopologyComponent(final String uuid) {
    super(uuid);
  }

  /**
   * Determine whether this object represents a driver.
   * @return {@code true} if this object represents a driver, {@code false} otherwise.
   */
  public boolean isDriver() {
    return false;
  }

  /**
   * Determine whether this object represents a node.
   * @return {@code true} if this object represents a node, {@code false} otherwise.
   */
  public boolean isNode() {
    return false;
  }

  /**
   * Determine whether this object represents a peer driver.
   * @return {@code true} if this object represnets a peer driver, {@code false} otherwise.
   */
  public boolean isPeer() {
    return false;
  }

  /**
   * Get the object describing the health of a node or driver.
   * @return a {@link HealthSnapshot} instance.
   */
  public synchronized HealthSnapshot getHealthSnapshot() {
    return healthSnapshot;
  }

  /**
   * Refresh the health snapshot state of the driver or node represented by this topology data.
   * @param newSnapshot the new health snapshot fetched from the grid.
   */
  synchronized void refreshHealthSnapshot(final HealthSnapshot newSnapshot) {
    if (isPeer()) return;
    this.healthSnapshot = newSnapshot;
  }

  /**
   * Get the management informtation on this topology component.
   * @return a {@link JPPFManagementInfo} instance.
   */
  public synchronized JPPFManagementInfo getManagementInfo() {
    return managementInfo;
  }

  @Override
  public String toString() {
    return (getManagementInfo() == null) ? "unknown" : getManagementInfo().getHost() + ':' + getManagementInfo().getPort();
  }
}
