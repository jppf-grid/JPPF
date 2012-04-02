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

package org.jppf.utils.pooling;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A ByteBuffer pool backed by a {@link ConcurrentLinkedQueue}.
 * @param <T>
 * @author Laurent Cohen
 */
public abstract class AbstractObjectPoolQueue<T> implements ObjectPool<T>
{
  /**
   * The pool of {@link ByteBuffer}.
   */
  protected final Queue<T> queue = new ConcurrentLinkedQueue<T>();

  /**
   * {@inheritDoc}
   */
  @Override
  public T get()
  {
    T t = queue.poll();
    return (t == null) ? create() : t;
  }

  /**
   * Create a new object for the pool.
   * @return an object that can be returned to the pool.
   */
  protected abstract T create();
 
  /**
   * {@inheritDoc}
   */
  @Override
  public void put(final T t)
  {
    queue.offer(t);
  }
  

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEmpty()
  {
    return queue.isEmpty();
  }

  /**
   * Use this method with precaution, as its performance is in O(n).<br/>
   * {@inheritDoc}
   */
  @Override
  public int size()
  {
    return queue.size();
  }
}
