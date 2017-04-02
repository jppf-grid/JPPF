/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.client.monitoring;

import java.util.*;

/**
 * Base superclass for components of a JPPF grid topology.
 * This class proivdes an API to navigate the topology tree and attributes common to all ellements in te tree.
 * @param <E> the type of component.
 * @author Laurent Cohen
 * @since 5.1
 */
public abstract class AbstractComponent<E extends AbstractComponent> {
  /**
   * The children of this component.
   */
  protected final Map<String, E> children = new HashMap<>();
  /**
   * The parent of this component.
   */
  protected E parent;
  /**
   * The uuid of this component.
   */
  protected final String uuid;

  /**
   * Initialize this component witht he specified uuid.
   * @param uuid the uuid assigned to this component.
   */
  protected AbstractComponent(final String uuid) {
    this.uuid = uuid;
  }

  /**
   * Get the parent of this compponent.
   * @return the parent as a {@link AbstractComponent} instance.
   */
  public synchronized E getParent() {
    return parent;
  }

  /**
   * Set the parent of this compponent.
   * @param parent the parent as a {@link AbstractComponent} instance.
   */
  synchronized void setParent(final E parent) {
    this.parent = parent;
  }

  /**
   * Get the child with the specified uuid.
   * @param uuid the uuid of the child to look for.
   * @return a {@link AbstractComponent} or {@code null} if there is no child with this uuid.
   */
  public synchronized E getChild(final String uuid) {
    return children.get(uuid);
  }

  /**
   * Get the number of children of this topology component.
   * @return the number of children.
   */
  public synchronized int getChildCount() {
    return children.size();
  }

  /**
   * Get the children of this component in a thread-safe way.
   * The returned list is a copy of the list of children and can be mainupalted without affect the internal state of this object.
   * @return a list of {@link AbstractComponent} instances.
   */
  public synchronized List<E> getChildren() {
    return new ArrayList<>(children.values());
  }

  /**
   * Add a child to this component.
   * @param child the child component to add.
   * @exclude
   */
  public synchronized void add(final E child) {
    children.put(child.getUuid(), child);
    child.setParent(this);
  }

  /**
   * Remove a child from this component.
   * @param child the child component to remove.
   * @exclude
   */
  public synchronized void remove(final E child) {
    children.remove(child.getUuid());
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
   * Get a user-friendly representation of this topology component.
   * @return a displayable string representing this object.
   */
  public String getDisplayName() {
    return toString();
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
    AbstractComponent other = (AbstractComponent) obj;
    if (uuid == null) return other.getUuid() == null;
    return uuid.equals(other.getUuid());
  }
}
