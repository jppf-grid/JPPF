/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node.protocol;

import java.util.*;

import org.jppf.location.Location;

/**
 * A simple implementation of the {@link ClassPath} interface
 * @author Laurent Cohen
 */
public class ClassPathImpl implements ClassPath {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Mapping of classpath elements to their names.
   */
  private final List<ClassPathElement> elementList = new ArrayList<>();
  /**
   * Determines whether the node should force a reset of the class loader before executing the tasks.
   */
  private boolean forceClassLoaderReset = false;

  @Override
  public Iterator<ClassPathElement> iterator() {
    return elementList.iterator();
  }

  @Override
  public ClassPath add(final ClassPathElement element) {
    elementList.add(element);
    return this;
  }

  @Override
  public ClassPath add(final Location<?> location) {
    elementList.add(new ClassPathElementImpl(location, location));
    return this;
  }

  @Override
  public ClassPath add(final Location<?> sourceLocation, final Location<?> targetLocation) {
    elementList.add(new ClassPathElementImpl(sourceLocation, targetLocation));
    return this;
  }

  @Override
  public ClassPath add(final Location<?> sourceLocation, final Location<?> targetLocation, final boolean copyToExistingFileLocation) {
    elementList.add(new ClassPathElementImpl(sourceLocation, targetLocation, copyToExistingFileLocation));
    return this;
  }

  /**
   * {@inheritDoc}
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  @Override
  public ClassPath add(final String name, final Location<?> location) {
    elementList.add(new ClassPathElementImpl(location));
    return this;
  }

  /**
   * {@inheritDoc}
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  @Override
  public ClassPath add(final String name, final Location<?> localLocation, final Location<?> remoteLocation) {
    elementList.add(new ClassPathElementImpl(localLocation, remoteLocation));
    return this;
  }

  @Override
  public ClassPath remove(final ClassPathElement element) {
    elementList.remove(element);
    return this;
  }

  /**
   * {@inheritDoc}
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  @Override
  public ClassPath remove(final String name) {
    ClassPathElement toRemove = null;
    if (name == null) return null;
    for (final ClassPathElement elt: elementList) {
      if (name.equals(elt.getName())) {
        toRemove = elt;
        break;
      }
    }
    if (toRemove != null) elementList.remove(toRemove);
    return this;
  }

  @Override
  public ClassPath clear() {
    elementList.clear();
    return this;
  }

  /**
   * {@inheritDoc}
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  @Override
  public ClassPathElement element(final String name) {
    if (name == null) return null;
    for (final ClassPathElement elt: elementList) {
      if (name.equals(elt.getName())) return elt;
    }
    return null;
  }

  @Override
  public Collection<ClassPathElement> allElements() {
    return new ArrayList<>(elementList);
  }

  @Override
  public boolean isEmpty() {
    return elementList.isEmpty();
  }

  @Override
  public boolean isForceClassLoaderReset() {
    return forceClassLoaderReset;
  }

  @Override
  public ClassPath setForceClassLoaderReset(final boolean forceClassLoaderReset) {
    this.forceClassLoaderReset = forceClassLoaderReset;
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    int count = 0;
    for (final ClassPathElement elt: this) {
      if (count > 0) sb.append(", ClassPathElement[");
      sb.append("source=").append(elt.getSourceLocation().getPath());
      sb.append(", target=").append(elt.getTargetLocation().getPath());
      sb.append(']');
      count++;
    }
    sb.append(']');
    return sb.toString();
  }
}
