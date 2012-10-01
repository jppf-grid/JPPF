/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.caching;

/**
 * Interface for caches implementations that rely on simple collections (as opposed to maps).
 * @param <E> the type of elements.
 * @author Laurent Cohen
 */
public interface JPPFCollectionCache<E>
{
  /**
   * Put an element in the cache.
   * @param element the element's key for retrival.
   */
  void add(E element);

  /**
   * Determine whether an element is in the cache.
   * @param element the element's to lookup.
   * @return <code>true</code> if the element is in the cache, <code>false</code> otherwise.
   */
  boolean has(E element);

  /**
   * Remove an element from the cache.
   * @param element the element to remove.
   * @return the the removed element if it was in the cache, or <code>null</code> if it wasn't.
   */
  E remove(E element);

  /**
   * Clear the cache. This removes all elements from the cache.
   */
  void clear();
}
