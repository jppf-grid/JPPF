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

package org.jppf.node.protocol;

import java.io.Serializable;
import java.util.Collection;

import org.jppf.location.*;

/**
 * A container for class path elements.
 * @author Laurent Cohen
 */
public interface ClassPath extends Serializable, Iterable<ClassPathElement> {
  /**
   * Add the specified element to this classpath.
   * @param element the classpath element to add.
   * @return this <code>ClassPath</code>.
   */
  ClassPath add(ClassPathElement element);

  /**
   * Add the specified element to this classpath.
   * When using this method, the local and remote locations are assumed to be the same.
   * @param location the location of the element to add.
   * @return this {@code ClassPath}, for method call chaining.
   */
  ClassPath add(Location<?> location);

  /**
   * Add the specified element to this classpath.
   * Upon processing by the node, the local location will be copied into the remote location.
   * @param sourceLocation the location of the element to add, in the client environment.
   * @param targetLocation the location of the element to add, in the node environment.
   * @return this {@code ClassPath}, for method call chaining.
   */
  ClassPath add(Location<?> sourceLocation, Location<?> targetLocation);

  /**
   * Add the specified element to this classpath.
   * Upon processing by the node, the local location will be copied into the remote location, unless all of the following are true:
   * <ul>
   * <li>copyToExistingFile is {@code false}</li>
   * <li>targetLocation is a {@link FileLocation}</li>
   * <li>the file pointed to by targetLocation already exists on the file system</li>
   * </ul>
   * @param sourceLocation the location of the element to add, in the client environment.
   * @param targetLocation the location of the element to add, in the node environment.
   * @param copyToExistingFile whether to copy the source to the target if the target is either a {@link FileLocation}
   * or an {@link URLLocation} with a {@code file} URL protocol (e.g. {@code file:/home/user/mylib.jar}), and if it already exists on the file system.
   * @return this {@code ClassPath}, for method call chaining.
   */
  ClassPath add(Location<?> sourceLocation, Location<?> targetLocation, final boolean copyToExistingFile);

  /**
   * Add the specified element to this classpath.
   * When using this method, the local and remote locations are assumed to be the same.
   * @param name the the name of the element to add.
   * @param location the location of the element to add.
   * @return this {@code ClassPath}, for method call chaining.
   * @deprecated the {@code name} attribute has no clearly definied, consistent semantics. It is no longer used.
   * Use {@link #add(Location) add(location)} instead.
   */
  ClassPath add(String name, Location<?> location);

  /**
   * Add the specified element to this classpath.
   * Upon processing by the node, the source location will be copied into the target location.
   * @param name the the name of the element to add.
   * @param sourceLocation the location of the element to add, in the client environment.
   * @param targetLocation the location of the element to add, in the node environment.
   * @return this {@code ClassPath}, for method call chaining.
   * @deprecated the {@code name} attribute has no clearly definied, consistent semantics. It is no longer used.
   * Use {@link #add(Location, Location) add(sourceLocation, remoteLocation)} instead.
   */
  ClassPath add(String name, Location<?> sourceLocation, Location<?> targetLocation);

  /**
   * Remove the specified element from this classpath.
   * @param element the classpath element to remove.
   * @return this {@code ClassPath}, for method call chaining.
   */
  ClassPath remove(ClassPathElement element);

  /**
   * Remove the specified element from this classpath.
   * @param name the name of the classpath element to remove.
   * @return this {@code ClassPath}, for method call chaining.
   * @deprecated the {@code name} attribute has no clearly definied, consistent semantics. It is no longer used.
   * Use {@link #remove(ClassPathElement)} instead.
   */
  ClassPath remove(String name);

  /**
   * Empty this classpath (remove all classpath elements).
   * @return this {@code ClassPath}, for method call chaining.
   */
  ClassPath clear();

  /**
   * Get the element with the specified name.
   * @param name the name of the classpath element to find.
   * @return a {@link ClassPathElement} instance or <code>null</code> if the element could no be found.
   * @deprecated the {@code name} attribute has no clearly definied, consistent semantics. It is no longer used.
   */
  ClassPathElement element(String name);

  /**
   * Get a collection of all the classpath elements in this classpath.
   * @return a {@link Collection} of {@link ClassPathElement}s.
   */
  Collection<ClassPathElement> allElements();

  /**
   * Determine whether this classpath is empty.
   * @return <code>true</code> if this classpath has no element, <code>false</code> otherwise.
   */
  boolean isEmpty();

  /**
   * Determine whether the node should force a reset of the class loader before executing the tasks.
   * <p>This only applies when this classpath is empty. If it is not empty, then the reset will occur regardless the value of this flag.
   * @return <code>true</code> if the class loader reset should be forced, <code>false</code> otherwise.
   */
  boolean isForceClassLoaderReset();

  /**
   * Specify whether the node should force a reset of the class loader before executing the tasks.
   * <p>This only applies when this classpath is empty. If it is not empty, then the reset will occur regardless the value of the specified flag.
   * @param forceReset <code>true</code> if the class loader reset should be forced, <code>false</code> otherwise.
   * @return this {@code ClassPath}, for method call chaining.
   */
  ClassPath setForceClassLoaderReset(boolean forceReset);
}
