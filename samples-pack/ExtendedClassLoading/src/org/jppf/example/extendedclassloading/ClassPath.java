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

package org.jppf.example.extendedclassloading;

import java.io.Serializable;
import java.util.*;

/**
 * This interface represents a set of classpath elements dynamically added to or removed from the classpath of a node.
 * @author Laurent Cohen
 */
public interface ClassPath extends Serializable {
  /**
   * Add an element with the specified name and signature to this repository.
   * @param key the name of the element to add.
   * @param signature the signature of the element to add.
   * @return <code>true</code> if the element was sucessfully added, 
   * <code>false</code> otherwise, typically if an element with the same name and signature already exists.
   */
  boolean addElement(String key, String signature);

  /**
   * Remove an element with the specified name from this repository.
   * @param key the name of the element to remove.
   * @return <code>true</code> if the element was sucessfully removed, 
   * <code>false</code> otherwise, typically if no element with the same name exists.
   */
  boolean removeElement(String key);

  /**
   * Get the signature of the specified element.
   * @param key the key associated with the element to lookup. 
   * @return the element's signature, or <code>null</code> if th key could not be found.
   */
  String getElementSignature(String key);

  /**
   * Get a map of the elements in this repository.
   * The returned map is immutable and decoupled from this repository instance.
   * @return mapping of element names to their signature.
   */
  Map<String, String> elements();

  /**
   * Get a collection of the names of the elements in this repository.
   * The returned collection is immutable and decoupled from this repository instance.
   * @return a collection of elements names.
   */
  Collection<String> elementNames();

  /**
   * Get the number of elements in this classpath.
   * @return the classpath size.
   */
  int size();
}
