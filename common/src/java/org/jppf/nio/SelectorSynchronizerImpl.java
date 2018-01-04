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

package org.jppf.nio;

import java.nio.channels.Selector;

/**
 * Synchronization aid for a {@link Selector}, to prevent {@link java.nio.channels.SelectionKey#interestOps(int) SelectionKey.interestOps(int)} invocations from blocking for too long.
 * This implementation synchronizes on itself.
 * <p><b>Usage pattern</b>:
 * <p>- in the selection loop:
 * <pre>
 * SelectorSynchronizer sync = new SelectorSynchronizerImpl(selector);
 * 
 * try {
 *   int n = 0;
 *   while (true) {
 *     sync.waitForZeroAndSetToMinusOne();
 *     try {
 *       n = selector.select(selectTimeout);
 *     } finally {
 *       sync.setToZeroIfNegative();
 *     }
 *     if (n > 0) {
 *       // process selected keys
 *     }
 *   }
 * } catch (Exception e) {
 *   // process exception
 * }
 * </pre>
 * <p>- in separate threads:
 * <pre>
 * public void setInterestOps(SelectionKey key, int newOps) throws Exception {
 *   sync.wakeUpAndSetOrIncrement();
 *   try {
 *     key.interestOps(newOps);
 *   } finally {
 *     sync.decrement();
 *   }
 * }
 * </pre>
 * @author Laurent Cohen
 */
public class SelectorSynchronizerImpl implements SelectorSynchronizer {
  /**
   * The selector to wakeup.
   */
  private final Selector selector;
  /**
   * The value of this synchronizer.
   */
  private int count;

  /**
   * Initialize with the specified selector.
   * @param selector the selector to wakeup.
   */
  public SelectorSynchronizerImpl(final Selector selector) {
    this.selector = selector;
  }

  /**
   * Wait for the value to go down to zero, then set it to -1.
   */
  @Override
  public void waitForZeroAndSetToMinusOne() {
    synchronized(this) {
      try {
        while (count != 0) wait();
      } catch (@SuppressWarnings("unused") final Exception e) {
      }
      count = -1;
      notify();
    }
  }

  /**
   * Set the value to zero if it is less than 0.
   */
  @Override
  public void setToZeroIfNegative() {
    synchronized(this) {
      if (count < 0) {
        count = 0;
        notify();
      }
    }
  }

  /**
   * Decrement the value.
   */
  @Override
  public void decrement() {
    synchronized(this) {
      count--;
      notify();
    }
  }

  /**
   * Compare the value with the expected value, and run run an action and incrment the value if the comparison succeeds,
   * or set the value to the update value if it does not succeed..
   */
  @Override
  public void wakeUpAndSetOrIncrement() {
    synchronized(this) {
      if (count < 0) {
        selector.wakeup();
        count = 1;
      } else count++;
      notify();
    }
  }
}
