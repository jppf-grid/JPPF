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

package org.jppf.ui.monitoring.topology;

import java.util.*;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.diagnostics.HealthSnapshot;

/**
 * Base superclass for components of a JPPF grid topology.
 * @author Laurent Cohen
 * @since 5.0
 */
public abstract class AbstractTopologyComponent {
  /**
   * The children of this component.
   */
  protected final List<AbstractTopologyComponent> children = new ArrayList<>();
  /**
   * The parent of this component.
   */
  protected AbstractTopologyComponent parent;
  /**
   * The uuid of this component.
   */
  protected String uuid;
  /**
   * The displayable id of this component.
   */
  protected String id;
  /**
   * Object describing the current health of a node or driver.
   */
  protected HealthSnapshot healthSnapshot = new HealthSnapshot();
  /**
   * Information on the JPPF node.
   */
  protected JPPFManagementInfo managementInfo;

  /**
   * Get the parent of this compponent.
   * @return the parent as a {@link AbstractTopologyComponent} instance.
   */
  public synchronized AbstractTopologyComponent getParent() {
    return parent;
  }

  /**
   * Set the parent of this compponent.
   * @param parent the parent as a {@link AbstractTopologyComponent} instance.
   */
  public synchronized void setParent(final AbstractTopologyComponent parent) {
    this.parent = parent;
  }

  /**
   * Get the number of children of this topology component.
   * @return the number of children.
   */
  public synchronized int getChildCount() {
    return children.size();
  }

  /**
   * Get the children of this component.
   * @return a list of {@link AbstractTopologyComponent} instance.
   */
  public List<AbstractTopologyComponent> getChildren() {
    return children;
  }

  /**
   * Get the children of this component in a thread-safe way.
   * @return a list of {@link AbstractTopologyComponent} instance.
   */
  public synchronized List<AbstractTopologyComponent> getChildrenSynchronized() {
    return new ArrayList<>(children);
  }

  /**
   * Add a child to this component.
   * @param child the child component to add.
   */
  public synchronized void add(final AbstractTopologyComponent child) {
    children.add(child);
    child.setParent(this);
  }

  /**
   * Add a child to this component.
   * @param child the child component to add.
   * @param index the index at which to insert the child.
   */
  public synchronized void add(final AbstractTopologyComponent child, final int index) {
    children.add(index, child);
    child.setParent(this);
  }

  /**
   * Remove a child from this component.
   * @param child the child component to remove.
   */
  public synchronized void remove(final AbstractTopologyComponent child) {
    children.remove(child);
    child.setParent(null);
  }

  /**
   * Get the uuid of this ocmponent.
   * @return the uuid as a strring.
   */
  public synchronized String getUuid() {
    return uuid;
  }

  /**
   * Set the connection uuid.
   * @param uuid the uuid as a string.
   */
  public synchronized void setUuid(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * Get the displayable id of this component.
   * @return the id as a strring.
   */
  public synchronized String getId() {
    return id;
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
  public synchronized void refreshHealthSnapshot(final HealthSnapshot newSnapshot) {
    if (isPeer()) return;
    this.healthSnapshot = newSnapshot;
  }

  /**
   * Get the information on a JPPF node.
   * @return a {@link JPPFManagementInfo} instance.
   */
  public synchronized JPPFManagementInfo getManagementInfo() {
    return managementInfo;
  }

  /**
   * Set the information on a JPPF node.
   * @param info a {@link JPPFManagementInfo} instance.
   */
  protected synchronized void setManagementInfo(final JPPFManagementInfo info) {
    this.managementInfo = info;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AbstractTopologyComponent other = (AbstractTopologyComponent) obj;
    if (uuid == null) return other.getUuid() == null;
    return uuid.equals(other.getUuid());
  }

  @Override
  public String toString() {
    return (getManagementInfo() == null) ? "unknown" : getManagementInfo().getHost() + ':' + getManagementInfo().getPort();
  }
}
