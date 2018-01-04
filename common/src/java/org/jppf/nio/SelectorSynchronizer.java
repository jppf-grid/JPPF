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
 * <p><b>Usage pattern</b>:
 * <p>- in the selection loop:
 * <pre>
 * SelectorSynchronizer sync = ...;
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
public interface SelectorSynchronizer {

  /**
   * Wait for the value to go down to zero, then set it to -1.
   */
  void waitForZeroAndSetToMinusOne();

  /**
   * Set the value to zero if it is less than 0.
   */
  void setToZeroIfNegative();

  /**
   * Decrement the value.
   */
  void decrement();

  /**
   * Compare the value with the expected value, and run run an action and incrment the value if the comparison succeeds,
   * or set the value to the update value if it does not succeed..
   */
  void wakeUpAndSetOrIncrement();

}