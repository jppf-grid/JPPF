/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.example.concurrentjobs;

import org.jppf.node.protocol.AbstractTask;

/**
 * A simple task used in the demo.
 */
public class MyTask extends AbstractTask<String> {
  /**
   * A string message to transform and set as result of this task.
   */
  private final String message;
  /**
   * How long this task will sleep to simulate code execution.
   */
  private final long duration;

  /**
   * Initialize this task.
   * @param message a string message to transform and set as result of this task.
   * @param duration how long this task will sleep to simulate code execution.
   */
  public MyTask(final String message, final long duration) {
    this.message = message;
    this.duration = duration;
  }

  @Override
  public void run() {
    try {
      // wait for the specified time, to simulate actual execution
      if (duration > 0) Thread.sleep(duration);
      setResult("execution success for " + message);
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}
