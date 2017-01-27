/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.*;

/**
 * Wraps an Iterator as an Enumeration
 * @param <T> the type of the enumerated objects.
 */
public class IteratorEnumeration<T> implements Enumeration<T>
{
  /**
   * The iterator to wrap as an enumeration.
   */
  private Iterator<T> iterator = null;

  /**
   * Initialize this enumeration with the specified iterator.
   * @param iterator the iterator to wrap as an enumeration.
   */
  public IteratorEnumeration(final Iterator<T> iterator)
  {
    this.iterator = iterator;
  }

  /**
   * Tests if this enumeration contains more elements.
   * @return true if and only if this enumeration object contains at least one more element to provide, false otherwise.
   * @see java.util.Enumeration#hasMoreElements()
   */
  @Override
  public boolean hasMoreElements()
  {
    return iterator.hasNext();
  }

  /**
   * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
   * @return the next element of this enumeration.
   * @exception NoSuchElementException if no more elements exist.
   * @see java.util.Enumeration#nextElement()
   */
  @Override
  public T nextElement()
  {
    if (!iterator.hasNext()) throw new NoSuchElementException("this enumeration has no more element to provide");
    return iterator.next();
  }
}
