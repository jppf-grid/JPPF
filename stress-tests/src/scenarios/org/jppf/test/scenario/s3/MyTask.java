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

package org.jppf.test.scenario.s3;

import java.util.Random;

import org.jppf.node.protocol.AbstractTask;

/**
 * These tasks count from 0 up to a large integer.
 * @author Laurent Cohen
 */
public class MyTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The time it takes to perform the count.
   */
  private long elapsed = 0L;
  /**
   * The count of integer to count.
   */
  private final long busyTime;
  /**
   * 
   */
  private transient Random rand;

  /**
   * Initialize this task with the specified busy time.
   * @param busyTime the busy time in milliseconds.
   */
  public MyTask(final long busyTime) {
    this.busyTime = busyTime;
  }

  @Override
  public void run() {
    final long start = System.nanoTime();
    rand = new Random(start);
    getBusy(busyTime);
    elapsed = System.nanoTime() - start;
  }

  /**
   * Perform CPU-consuming operations for the specified duration.
   * @param millis the duration in milliseconds.
   */
  private void getBusy(final long millis) {
    final long nanos = millis * 1000000L;
    final long start = System.nanoTime();
    while (System.nanoTime() - start < nanos) {
      Math.log(rand.nextLong());
    }
  }

  /**
   * Get the time it took to perform the count.
   * @return the time in nanoseconds.
   */
  public long getElapsed() {
    return elapsed;
  }
}
