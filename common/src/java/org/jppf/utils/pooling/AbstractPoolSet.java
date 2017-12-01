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

package org.jppf.utils.pooling;

import java.util.*;

/**
 * A thread-safe, dynamic object pool backed by a {@link HashMap}.
 * This pool is designed for maximum throughput, thus its size will grow dynamically,
 * up to maximum concurrent usage.
 * <p>Subclasses that wish to avoid a size too large for the pool may for instance
 * introduce a core pool size, and override the <code>put(T)</code> method such that it
 * doesn't put released object back into the queue if its size > core size.
 * <p>In this case the {@link #size()} method might need to be overriden as well, as its
 * performance in O(n) may be the main bottleneck of the implementation.
 * @param <T> the type of objects in the pool.
 * @author Laurent Cohen
 */
public abstract class AbstractPoolSet<T> implements ObjectPool<T> {
  /**
   * The pool of objects.
   */
  protected final Map<T, Void> queue = new HashMap<>();

  @Override
  public synchronized T get() {
    Iterator<Map.Entry<T, Void>> it = queue.entrySet().iterator();
    T t = null;
    if (it.hasNext()) {
      t = it.next().getKey();
      it.remove();
      return t;
    }
    return create();
  }

  /**
   * Create a new object for the pool.
   * @return an object that can be returned to the pool.
   */
  protected abstract T create();

  @Override
  public synchronized void put(final T t) {
    queue.put(t, null);
  }

  @Override
  public synchronized boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Use this method with precaution, as its performance is in O(n).<br/>
   * {@inheritDoc}
   */
  @Override
  public synchronized int size() {
    return queue.size();
  }
}
