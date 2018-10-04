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

import org.jppf.location.Location;

/**
 * A simple implementation of the {@link ClassPathElement} interface.
 * @author Laurent Cohen
 */
public class ClassPathElementImpl implements ClassPathElement {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The location of this classpath element in the client environment.
   */
  private final Location<?> sourceLocation;
  /**
   * The location of this classpath element in the node environment.
   */
  private final Location<?> targetLocation;
  /**
   * The name of this classpath element.
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  private final String name;
  /**
   * Whether to copy the source to the target if the target is a file and if it already exists on the file system.
   */
  private final boolean copyToExistingFile;

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param name the name of this classpath element.
   * @param location the location of this classpath element.
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  ClassPathElementImpl(final String name, final Location<?> location) {
    this(name, location, location);
  }

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param name the name of this classpath element.
   * @param sourceLocation the location of this classpath element in the client environment.
   * @param targetLocation the location of this classpath element in the node environment.
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  ClassPathElementImpl(final String name, final Location<?> sourceLocation, final Location<?> targetLocation) {
    this.name = name;
    this.sourceLocation = sourceLocation;
    this.targetLocation = targetLocation;
    this.copyToExistingFile = true;
  }

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param location the location of this classpath element.
   */
  ClassPathElementImpl(final Location<?> location) {
    this(location, location, true);
  }

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param sourceLocation the location of this classpath element in the client environment.
   * @param targetLocation the location of this classpath element in the node environment.
   */
  ClassPathElementImpl(final Location<?> sourceLocation, final Location<?> targetLocation) {
    this(sourceLocation, targetLocation, true);
  }

  /**
   * Initialize this classpath element with the specified name and local location.
   * The remote location is set to the same location.
   * @param sourceLocation the location of this classpath element in the client environment.
   * @param targetLocation the location of this classpath element in the node environment.
   * @param copyToExistingFileLocation whether to copy the source to the target if the target is a file and if it already exists on the file system.
   */
  ClassPathElementImpl(final Location<?> sourceLocation, final Location<?> targetLocation, final boolean copyToExistingFileLocation) {
    this.name = null;
    this.sourceLocation = sourceLocation;
    this.targetLocation = targetLocation;
    this.copyToExistingFile = copyToExistingFileLocation;
  }

  /**
   * {@inheritDoc}
   * @deprecated the {@code name} attribute has no clearly defined, consistent semantics. It is no longer used.
   */
  @Override
  public String getName() {
    return name;
  }

  @Override
  public Location<?> getSourceLocation() {
    return sourceLocation;
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #getSourceLocation()} instead.
   * @exclude
   */
  @Override
  public Location<?> getLocalLocation() {
    return sourceLocation;
  }

  @Override
  public Location<?> getTargetLocation() {
    return targetLocation;
  }

  /**
   * {@inheritDoc}
   * @deprecated use {@link #getTargetLocation()} instead.
   * @exclude
   */
  @Override
  public Location<?> getRemoteLocation() {
    return targetLocation;
  }

  /**
   * This default implementation always return true.
   * @return <code>true</code>.
   */
  @Override
  public boolean validate() {
    return true;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("source=").append(sourceLocation)
      .append(", target=").append(targetLocation)
      .append(']').toString();
  }

  @Override
  public boolean isCopyToExistingFile() {
    return copyToExistingFile;
  }
}
