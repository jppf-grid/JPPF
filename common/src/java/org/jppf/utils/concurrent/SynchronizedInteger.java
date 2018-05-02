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

import org.jppf.utils.ComparisonOperator;

/**
 * An integer value on which a set of operations can be performed atomically.
 * @author Laurent Cohen
 */
public class SynchronizedInteger {
  /**
   * The int value.
   */
  private int value;

  /**
   * Initialize with a value of 0.
   */
  public SynchronizedInteger() {
  }

  /**
   * Initialize with the specified value.
   * @param value the initial value.
   */
  public SynchronizedInteger(final int value) {
    this.value = value;
  }

  /**
   * Get the value.
   * @return the current value.
   */
  public int get() {
    synchronized(this) {
      return value;
    }
  }

  /**
   * Set to a new value.
   * @param newValue the value to set.
   */
  public void set(final int newValue) {
    synchronized(this) {
      value = newValue;
    }
  }

  /**
   * Increment the value.
   * @return the new value.
   */
  public int incrementAndGet() {
    synchronized(this) {
      return ++value;
    }
  }

  /**
   * Increment the value.
   * @return the new value.
   */
  public int incrementGetAndNotify() {
    synchronized(this) {
      ++value;
      notify();
      return value;
    }
  }

  /**
   * Wait until the value equals the specified target.
   * @param target the target value to wait for.
   */
  public void awaitValue(final int target) {
    synchronized(this) {
      try {
        while (value != target) wait();
      } catch (@SuppressWarnings("unused") final InterruptedException e) {
      }
    }
  }

  /**
   * Increment the value.
   * @return the new value.
   */
  public int decrementAndGet() {
    synchronized(this) {
      return --value;
    }
  }

  /**
   * Add the specified update and return the new value.
   * @param update the value to add.
   * @return the new value.
   */
  public int addAndGet(final int update) {
    synchronized(this) {
      value += update;
      return value;
    }
  }

  /**
   * Set a new value and return the old value.
   * @param newValue the value to set.
   * @return the value before the set.
   */
  public int getAndSet(final int newValue) {
    final int oldValue;
    synchronized(this) {
      oldValue = value;
      value = newValue;
    }
    return oldValue;
  }

  /**
   * Compare the value with the expected value, and set it to the update value if they are equal.
   * @param expected the expected value.
   * @param update the new value to set to if the comparison succeeds.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndSet(final int expected, final int update) {
    synchronized(this) {
      if (value == expected) {
        value = update;
        return true;
      }
    }
    return false;
  }

  /**
   * Compare the value with the expected value using the specified operator, and set it to the update value if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expected the expected value.
   * @param update the new value to set to if the comparison succeeds.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndSet(final ComparisonOperator operator, final int expected, final int update) {
    synchronized(this) {
      if (operator.evaluate(value, expected)) {
        value = update;
        return true;
      }
    }
    return false;
  }

  /**
   * Compare the value with the expected update value using the specified operator, and set it to this update value if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expectedUpdate the expected value.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndSet(final ComparisonOperator operator, final int expectedUpdate) {
    synchronized(this) {
      if (operator.evaluate(value, expectedUpdate)) {
        value = expectedUpdate;
        return true;
      }
    }
    return false;
  }

  /**
   * Compare the value with the expected value using the specified operator, and increment it if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expected the expected value.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndIncrement(final ComparisonOperator operator, final int expected) {
    synchronized(this) {
      if (operator.evaluate(value, expected)) {
        value++;
        return true;
      }
    }
    return false;
  }

  /**
   * Compare the value with the expected value using the specified operator, and increment it if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expected the expected value.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public boolean compareAndDecrement(final ComparisonOperator operator, final int expected) {
    synchronized(this) {
      if (operator.evaluate(value, expected)) {
        value--;
        return true;
      }
    }
    return false;
  }
}
