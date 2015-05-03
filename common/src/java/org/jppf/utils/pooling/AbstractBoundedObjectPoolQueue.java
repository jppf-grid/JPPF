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

package org.jppf.utils.pooling;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * An extension of {@link AbstractObjectPoolQueue} which specifies a maximum size for the pool.
 * <p>In this implementation, objects are still created dynamically if the pool is empty, however
 * the <code>put(T)</code> method does not return an element to the backing queue if the pool size >= max size.
 * <p> the {@link #size()} method is also overriden to provide results in constant time insteat of O(n) in
 * the super class, since it is used frequently when comparing current pool size with the max size.
 * @param <T> the type of the elements in the pool.
 * @author Laurent Cohen
 */
public abstract class AbstractBoundedObjectPoolQueue<T> extends AbstractObjectPoolQueue<T>
{
  /**
   * The current size of the pool.
   */
  protected AtomicInteger size = new AtomicInteger(0);
  /**
   * The pool max size.
   */
  protected final int maxSize;

  /**
   * Initialize this pool with the specified maximum size.
   * @param maxSize the pool max size.
   */
  public AbstractBoundedObjectPoolQueue(final int maxSize)
  {
    this.maxSize = maxSize;
  }

  @Override
  public T get()
  {
    T t = queue.poll();
    if (t != null) size.decrementAndGet();
    else t = create();
    return t;
  }

  @Override
  public void put(final T t)
  {
    if (size.get() < maxSize)
    {
      size.incrementAndGet();
      queue.offer(t);
    }
  }

  @Override
  public int size()
  {
    return size.get();
  }
}
