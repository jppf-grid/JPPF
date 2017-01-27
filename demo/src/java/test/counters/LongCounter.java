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

package test.counters;

/**
 * 
 * @author Laurent Cohen
 */
public interface LongCounter {
  /**
   * Increment this counter and get the new value.
   * @return the old value + 1.
   */
  long incrementAndGet();

  /**
   * Decrement this counter and get the new value.
   * @return the old value - 1.
   */
  long decrementAndGet();

  /**
   * Add the specified value to this counter and get the new value.
   * @param value the value to add.
   * @return the old value + the add value.
   */
  long addAndGet(long value);

  /**
   * Get the value of this counter.
   * @return the old value + 1.
   */
  long get();

  /**
   * Set the value of this counter.
   * @param value the value to set.
   */
  void set(long value);
}
