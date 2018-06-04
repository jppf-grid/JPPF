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

package org.jppf.utils.concurrent;

/**
 * An {@code Object} value on which a set of operations can be performed atomically.
 * @param <T> thr type of the refered object.
 * @author Laurent Cohen
 */
public class SynchronizedReference<T> {
  /**
   * The actual value.
   */
  private T value;

  /**
   * Initialize with the value ste to {@code false}.
   */
  public SynchronizedReference() {
    value = null;
  }

  /**
   * Iinitialize witht he specified value.
   * @param value the initial value.
   */
  public SynchronizedReference(final T value) {
    this.value = value;
  }

  /**
   * @return the value of this boolan.
   */
  public synchronized T get() {
    return value;
  }

  /**
   * Set a new value.
   * @param newValue the value to set.
   */
  public synchronized void set(final T newValue) {
    value = newValue;
  }

  /**
   * Set a new value if it is different from the current value.
   * @param newValue the value to set.
   * @return {@code true} if the value was changed, {@code false} otherwise.
   */
  public synchronized boolean setIfDifferent(final T newValue) {
    if (newValue == value) return false;
    value = newValue;
    return true;
  }

  /**
   * Compare the value with the expected value, and set it to the update value if they are equal.
   * @param expected the expected value.
   * @param update the new value to set to if the comparison succeeds.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public synchronized boolean compareAndSet(final T expected, final T update) {
    if (value == expected) {
      value = update;
      return true;
    }
    return false;
  }

  /**
   * Set a new value and return the old value.
   * @param newValue the value to set.
   * @return the value before the set.
   */
  public synchronized Object getAndSet(final T newValue) {
    final Object oldValue = value;
    value = newValue;
    return oldValue;
  }

  /**
   * Run an action if the value is the expected one.
   * @param expected the expected value.
   * @param action the aciton to run.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public synchronized boolean compareAndRun(final T expected, final Runnable action) {
    if (value == expected) {
      if (action != null) action.run();
      return true;
    }
    return false;
  }
}
