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

import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.*;

/**
 * This class is for logging and debugging purposes. It allows distinguishing the lock on the JPPF queue (driver and client) from any other lock in the same JVM.
 * @author Laurent Cohen
 */
public class JPPFQueueLock extends ReentrantLock {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JPPFQueueLock.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Name given to this lock.
   */
  private final String name;

  /**
   * Creates an instance of {@code JPPFReentrantLock}.
   * @param name the name.
   */
  public JPPFQueueLock(final String name) {
    final String s = getClass().getSimpleName();
    this.name = (name == null) ? s : s + " [" + name + "]";
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void lock() {
    if (debugEnabled) log.debug(">> locking {}", this);
    super.lock();
    if (debugEnabled) log.debug(">> locked {}", this);
  }

  @Override
  public void unlock() {
    if (debugEnabled) log.debug("<< unlocking {}", this);
    super.unlock();
    if (debugEnabled) log.debug("<< unlocked {}", this);
  }
}
