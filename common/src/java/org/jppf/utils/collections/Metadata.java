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

package org.jppf.utils.collections;

import java.io.Serializable;
import java.util.Map;

/**
 * A generic dictionary of metadata.
 * @author Laurent Cohen
 */
public interface Metadata extends Serializable
{
  /**
   * Retrieve a parameter in the metadata.
   * @param key the parameter's key.
   * @return the parameter's value or null if no parameter with the specified key exists.
   * @param <T> the type of the value to get.
   */
  <T> T getParameter(Object key);

  /**
   * Retrieve a parameter in the metadata.
   * @param key the parameter's key.
   * @param def a default value to return if no parameter with the specified key can be found.
   * @return the parameter's value or null if no parameter with the specified key exists.
   * @param <T> the type of the value to get.
   */
  <T> T getParameter(Object key, T def);

  /**
   * Set a parameter in the metadata.
   * If a parameter with the same key already exists, its value is replaced with the new one.
   * @param key the parameter's key.
   * @param value the parameter's value.
   */
  void setParameter(Object key, Object value);

  /**
   * Remove a parameter from the metadata.
   * @param key the parameter's key.
   * @return the removed parameter's value or null if no parameter with the specified key exists.
   * @param <T> the type of the value to remove.
   */
  <T> T removeParameter(Object key);

  /**
   * Get a copy of the metadata map.
   * @return a map of the metadata contained in this object.
   */
  Map<Object, Object> getAll();

  /**
   * Clear the underlying map.
   */
  void clear();
}
