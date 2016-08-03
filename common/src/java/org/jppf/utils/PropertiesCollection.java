/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.utils;

import java.io.Serializable;

/**
 * 
 * @param <E> the type of keys to use.
 * @author Laurent Cohen
 */
public interface PropertiesCollection<E> extends Serializable
{
  /**
   * Add the specified properties with the specified keys.
   * @param key the key to use.
   * @param properties the properties to add.
   */
  void addProperties(E key, TypedProperties properties);

  /**
   * Add the specified properties with the specified key.
   * @param key the key to use to retrieve the properties.
   * @return the properties to correponding to the key, or null if the key could not be found.
   */
  TypedProperties getProperties(E key);

  /**
   * Get all the properties as an array.
   * @return an array of all the sets of properties.
   */
  TypedProperties[] getPropertiesArray();
}
