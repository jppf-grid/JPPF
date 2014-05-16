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

package org.jppf.node.protocol;

import java.io.Serializable;

/**
 * Instances of this class represent classpath elements that can be added dynamically to a JPPF class loader.
 * The actual classpath element is a jar or zip file wrapped into, or pointed to by, a {@link Location} object.
 * They are intended to be transported along with a job SLA.
 * @author Laurent Cohen
 */
public interface ClassPathElement extends Serializable
{
  /**
   * Get the name of this classpath element.
   * @return the name as a string.
   */
  String getName();

  /**
   * Get the location of this element, pointing to or embedding the underlying jar or zip file in the client environment.
   * @return a {@link Location} object.
   */
  Location<?> getLocalLocation();

  /**
   * Get the location of this element, pointing to or embedding the underlying jar or zip file in the node environment.
   * @return a {@link Location} object.
   */
  Location<?> getRemoteLocation();

  /**
   * Perform a validation of this classpath element.
   * If validation fails, if will not be added to the node's classpath and its classes will not be loaded nor executed.
   * <p>An example use is to check that a jar file is signed and that it is secure to use it.
   * @return <code>true</code> if the validation issuccessful, <code>false</code> if it fails.
   */
  boolean validate();
}
