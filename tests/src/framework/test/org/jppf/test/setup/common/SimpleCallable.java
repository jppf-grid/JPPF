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

package test.org.jppf.test.setup.common;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * A simple callable task implementation.
 */
public class SimpleCallable implements Callable<TaskResult>, Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The position of this task.
   */
  private int position = 0;
  /**
   * The duration of this task.
   */
  private long duration = 0;

  /**
   * Default constructor.
   */
  public SimpleCallable() {
  }

  /**
   * Initialize this task with the specified position.
   * @param position the position of this task.
   */
  public SimpleCallable(final int position) {
    this.position = position;
  }

  /**
   * Initialize this task with the specified position.
   * @param position the position of this task.
   * @param duration the duration of this task.
   */
  public SimpleCallable(final int position, final long duration) {
    this.position = position;
    this.duration = duration;
  }

  /**
   * Execute this task.
   * @return a {@link TaskResult} object.
   * @throws Exception if any error occurs.
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public TaskResult call() throws Exception {
    final TaskResult executionResult = new TaskResult();
    executionResult.message = BaseTestHelper.EXECUTION_SUCCESSFUL_MESSAGE;
    executionResult.position = position;
    if (duration > 0L) {
      try {
        Thread.sleep(duration);
      } catch (final InterruptedException e) {
        executionResult.message = e.getMessage();
      }
    }
    return executionResult;
  }
}
