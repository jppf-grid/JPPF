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

package test.counters;

import java.util.concurrent.atomic.AtomicLong;

/**
 * A long counter based on an AtomicLong value.
 * @author Laurent Cohen
 */
public class LongCounterAtomic implements LongCounter {
  /**
   * The value of this counter
   */
  private final AtomicLong value;

  /**
   * Default constructor, initializes the value to 0L.
   */
  public LongCounterAtomic() {
    value = new AtomicLong(0L);
  }

  /**
   * Initializes this counter to the specified value.
   * @param value the initial value.
   */
  public LongCounterAtomic(final long value) {
    this.value = new AtomicLong(value);
  }

  @Override
  public long incrementAndGet() {
    return value.incrementAndGet();
  }

  @Override
  public long decrementAndGet() {
    return value.decrementAndGet();
  }

  @Override
  public long addAndGet(final long value) {
    return this.value.addAndGet(value);
  }

  @Override
  public long get() {
    return value.get();
  }

  @Override
  public void set(final long value) {
    this.value.set(value);
  }
}
