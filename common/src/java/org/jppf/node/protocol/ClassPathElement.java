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

import java.io.Serializable;

import org.jppf.location.Location;

/**
 * Instances of this class represent classpath elements that can be added dynamically to a JPPF class loader.
 * The actual classpath element is a jar or zip file wrapped into, or pointed to by, a {@link Location} object.
 * They are intended to be transported along with a job SLA.
 * @author Laurent Cohen
 */
public interface ClassPathElement extends Serializable {
  /**
   * Get the name of this classpath element.
   * @return the name as a string.
   * @deprecated the {@code name} attribute has no clearly definied, consistent semantics. It can be bypassed entirely.
   */
  String getName();

  /**
   * Get the location of this element, pointing to or embedding the underlying jar or zip file in the client environment.
   * @return a {@link Location} object.
   */
  Location<?> getSourceLocation();

  /**
   * Get the location of this element, pointing to or embedding the underlying jar or zip file in the client environment.
   * @return a {@link Location} object.
   * @deprecated use {@link #getSourceLocation()} instead.
   * @exclude
   */
  Location<?> getLocalLocation();

  /**
   * Get the location of this element, pointing to or embedding the underlying jar or zip file in the node environment.
   * @return a {@link Location} object.
   */
  Location<?> getTargetLocation();

  /**
   * Get the location of this element, pointing to or embedding the underlying jar or zip file in the client environment.
   * @return a {@link Location} object.
   * @deprecated use {@link #getTargetLocation()} instead.
   * @exclude
   */
  Location<?> getRemoteLocation();

  /**
   * Perform a validation of this classpath element.
   * If validation fails, if will not be added to the node's classpath and its classes will not be loaded nor executed.
   * <p>An example use is to check that a jar file is signed and that it is secure to use it.
   * @return <code>true</code> if the validation issuccessful, <code>false</code> if it fails.
   */
  boolean validate();

  /**
   * Determine whether to copy the source to the target if the target is a {@link FileLocation} and if it already exists on the file system.
   * This is an optimization that can avoid unnecessary netword and/or disk I/O.
   * @return {@code false} if the target is a {@link FileLocation} and should not be copied into, {@code true} otherwise.
   */
  boolean isCopyToExistingFile();
}
