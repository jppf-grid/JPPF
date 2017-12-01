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

import org.jppf.client.Operator;

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
  public synchronized int get() {
    return value;
  }

  /**
   * Set to a new value.
   * @param newValue the value to set.
   */
  public synchronized void set(final int newValue) {
    value = newValue;
  }

  /**
   * Increment the value.
   * @return the new value.
   */
  public synchronized int incrementAndGet() {
    return ++value;
  }

  /**
   * Increment the value.
   * @return the new value.
   */
  public synchronized int decrementAndGet() {
    return --value;
  }

  /**
   * Add the specified update and return the new value.
   * @param update the value to add.
   * @return the new value.
   */
  public synchronized int addAndGet(final int update) {
    value += update;
    return value;
  }

  /**
   * Set a new value and return the old value.
   * @param newValue the value to set.
   * @return the value before the set.
   */
  public synchronized int getAndSet(final int newValue) {
    int oldValue = value;
    value = newValue;
    return oldValue;
  }

  /**
   * Compare the value witht he expected value, and set it to the update value if they are equal.
   * @param expected the expected value.
   * @param update the new value to set to if the comparison succeeds.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public synchronized boolean compareAndSet(final int expected, final int update) {
    if (value == expected) {
      value = update;
      return true;
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
  public synchronized boolean compareAndSet(final Operator operator, final int expected, final int update) {
    if (operator.evaluate(value, expected)) {
      value = update;
      return true;
    }
    return false;
  }

  /**
   * Compare the value with the expected update value using the specified operator, and set it to this update value if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expectedUpdate the expected value.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public synchronized boolean compareAndSet(final Operator operator, final int expectedUpdate) {
    if (operator.evaluate(value, expectedUpdate)) {
      value = expectedUpdate;
      return true;
    }
    return false;
  }

  /**
   * Compare the value with the expected value using the specified operator, and increment it if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expected the expected value.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public synchronized boolean compareAndIncrement(final Operator operator, final int expected) {
    if (operator.evaluate(value, expected)) {
      value++;
      return true;
    }
    return false;
  }

  /**
   * Compare the value with the expected value using the specified operator, and increment it if the comparison succeeds.
   * @param operator the comparison operator.
   * @param expected the expected value.
   * @return {@code true} if the update was performed, {@code false} otherwise.
   */
  public synchronized boolean compareAndDecrement(final Operator operator, final int expected) {
    if (operator.evaluate(value, expected)) {
      value--;
      return true;
    }
    return false;
  }
}
