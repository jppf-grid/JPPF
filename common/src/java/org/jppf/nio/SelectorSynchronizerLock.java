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
import java.util.concurrent.locks.*;

import org.slf4j.*;

/**
 * Synchronization aid for a {@link Selector}, to prevent {@link java.nio.channels.SelectionKey#interestOps(int) SelectionKey.interestOps(int)} invocations from blocking for too long.
 * <p><b>Usage pattern</b>:
 * <p>- in the selection loop:
 * <pre>
 * SelectorSynchronizer sync = new SelectorSynchronizerLock(selector);
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
public class SelectorSynchronizerLock implements SelectorSynchronizer {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SelectorSynchronizerLock.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The selector to wakeup.
   */
  private final Selector selector;
  /**
   * The value of this synchronizer.
   */
  private int count;
  /**
   * 
   */
  private final Lock lock = new ReentrantLock();
  /**
   * 
   */
  private final Condition condition = lock.newCondition();

  /**
   * Initialize with the specified selector.
   * @param selector the selector to wakeup.
   */
  public SelectorSynchronizerLock(final Selector selector) {
    this.selector = selector;
  }

  @Override
  public void waitForZeroAndSetToMinusOne() {
    lock.lock();
    try {
      while (count != 0) condition.await();
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    } finally {
      count = -1;
      condition.signal();
      lock.unlock();
    }
  }

  @Override
  public void setToZeroIfNegative() {
    lock.lock();
    try {
      if (count < 0) {
        count = 0;
        condition.signal();
      }
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void decrement() {
    lock.lock();
    try {
      count--;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  @Override
  public void wakeUpAndSetOrIncrement() {
    lock.lock();
    try {
      if (count < 0) {
        selector.wakeup();
        count = 1;
      } else count++;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }
}
