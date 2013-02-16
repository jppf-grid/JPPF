/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.server.node;

import java.util.concurrent.Future;

/**
 * Instances of this class are scheduled by a timer to execute one time, check
 * whether the corresponding JPPF task timeout has been reached, and abort the
 * task if necessary.
 * @exclude
 */
public class TimeoutTimerTask implements Runnable
{
  /**
   * The future on which to call the cancel() method.
   */
  private final Future<?> future;
  /**
   * The task to cancel.
   */
  private final NodeTaskWrapper taskWrapper;

  /**
   * Initialize this timer task with the specified future.
   * @param future the future on which to call the cancel() method.
   * @param taskWrapper the task to cancel.
   */
  public TimeoutTimerTask(final Future<?> future, final NodeTaskWrapper taskWrapper)
  {
    if (future == null) throw new IllegalArgumentException("future is null");
    if (taskWrapper == null) throw new IllegalArgumentException("taskWrapper is null");

    this.future = future;
    this.taskWrapper = taskWrapper;
  }

  /**
   * Execute this task.
   * @see java.util.TimerTask#run()
   */
  @Override
  public void run()
  {
    if (!future.isDone())
    {
        taskWrapper.timeout();
        future.cancel(true);
    }
  }
}
