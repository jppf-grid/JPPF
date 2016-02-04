/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package org.jppf.execute;

import java.util.concurrent.Future;

/**
 * Instances of this class are scheduled by a timer to execute one time, check
 * whether the corresponding JPPF task timeout has been reached, and abort the
 * task if necessary.
 * @exclude
 */
public class TimeoutTimerTask implements Runnable {
  /**
   * The task to cancel.
   */
  private final NodeTaskWrapper taskWrapper;

  /**
   * Initialize this timer task with the specified future.
   * @param taskWrapper the task to cancel.
   */
  public TimeoutTimerTask(final NodeTaskWrapper taskWrapper) {
    if (taskWrapper == null) throw new IllegalArgumentException("taskWrapper is null");
    this.taskWrapper = taskWrapper;
  }

  @Override
  public void run() {
    Future<?> future = taskWrapper.getFuture();
    if (!future.isDone()) {
      taskWrapper.timeout();
      future.cancel(taskWrapper.getTask().isInterruptible());
    }
  }
}
