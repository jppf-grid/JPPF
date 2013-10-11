/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

/**
 * A container for class path elements.
 * @author Laurent Cohen
 */
public interface ClassPath extends Serializable, Iterable<ClassPathElement>
{
  /**
   * Add the specified element to this classpath.
   * @param element the classpath element to add.
   * @return this <code>ClassPath</code>.
   */
  ClassPath add(ClassPathElement element);

  /**
   * Add the specified element to this classpath.
   * When using this method, the local and remote locations are assumed to be the same.
   * @param name the the name of the element to add.
   * @param location the location of the element to add.
   * @return this <code>ClassPath</code>.
   */
  ClassPath add(String name, Location<?> location);

  /**
   * Add the specified element to this classpath.
   * Upon processing by the node, the local location will be copied into the remote location.
   * @param name the the name of the element to add.
   * @param localLocation the location of the element to add, in the client environment.
   * @param remoteLocation the location of the element to add, in the node environment.
   * @return this <code>ClassPath</code>.
   */
  ClassPath add(String name, Location<?> localLocation, Location<?> remoteLocation);

  /**
   * Remove the specified element from this classpath.
   * @param element the classpath element to remove.
   * @return this <code>ClassPath</code>.
   */
  ClassPath remove(ClassPathElement element);

  /**
   * Remove the specified element from this classpath.
   * @param name the name of the classpath element to remove.
   * @return this <code>ClassPath</code>.
   */
  ClassPath remove(String name);

  /**
   * Empty this classpath (remove all classpath elements).
   * @return this classpath.
   */
  ClassPath clear();

  /**
   * Get the element with the specified name.
   * @param name the name of the classpath element to find.
   * @return a {@link ClassPathElement} instance or <code>null</code> if the element could no be found.
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
   */
  void setForceClassLoaderReset(boolean forceReset);
}
