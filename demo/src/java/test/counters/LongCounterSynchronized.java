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

/**
 * A long counter based on a primitive long value, whose public methods are all synchronized.
 * @author Laurent Cohen
 */
public class LongCounterSynchronized implements LongCounter {
  /**
   * The value of this counter
   */
  private long value;

  /**
   * Default constructor, initializes the value to 0L.
   */
  public LongCounterSynchronized() {
    value = 0L;
  }

  /**
   * Initializes this counter to the specified value.
   * @param value the initial value.
   */
  public LongCounterSynchronized(final long value) {
    this.value = value;
  }

  @Override
  public synchronized long incrementAndGet() {
    return ++value;
  }

  @Override
  public synchronized long decrementAndGet() {
    return --value;
  }

  @Override
  public synchronized long addAndGet(final long value) {
    this.value += value;
    return value;
  }

  @Override
  public synchronized long get() {
    return value;
  }

  @Override
  public synchronized void set(final long value) {
    this.value = value;
  }
}
