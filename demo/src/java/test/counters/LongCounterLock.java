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

package test.counters;

import java.util.concurrent.locks.*;

/**
 * A long counter based on a primitive long value, relying on locks for thread safety.
 * @author Laurent Cohen
 */
public class LongCounterLock implements LongCounter {
  /**
   * The value of this counter
   */
  private long value;
  /**
   * The lock for synchronizing access to the value.
   */
  private Lock lock = new ReentrantLock();

  /**
   * Default constructor, initializes the value to 0L.
   */
  public LongCounterLock() {
    value = 0L;
  }

  /**
   * Initializes this counter to the specified value.
   * @param value the initial value.
   */
  public LongCounterLock(final long value) {
    this.value = value;
  }

  @Override
  public long incrementAndGet() {
    lock.lock();
    try {
      return ++value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long decrementAndGet() {
    lock.lock();
    try {
      return --value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long addAndGet(final long value) {
    lock.lock();
    try {
      return this.value += value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public long get()
  {
    lock.lock();
    try {
      return value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void set(final long value) {
    lock.lock();
    try {
      this.value = value;
    } finally {
      lock.unlock();
    }
  }
}
