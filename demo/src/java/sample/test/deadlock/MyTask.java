/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package sample.test.deadlock;

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
   * Whether to simulate CPU usage.
   */
  private final boolean useCPU;

  /**
   * Initialize this task.
   * @param duration how long this task will sleep to simulate code execution.
   * @param useCPU whether to simulate CPU usage.
   */
  public MyTask(final long duration, final boolean useCPU) {
    this(null, duration, useCPU);
  }

  /**
   * Initialize this task.
   * @param message a string message to transform and set as result of this task.
   * @param duration how long this task will sleep to simulate code execution.
   * @param useCPU whether to simulate CPU usage.
   */
  public MyTask(final String message, final long duration, final boolean useCPU) {
    this.message = message;
    this.duration = duration;
    this.useCPU = useCPU;
  }

  @Override
  public void run() {
    //System.out.println("starting execution for "  + getId());
    try {
      if (!useCPU) {
        if (duration > 0L) Thread.sleep(duration);
      } else {
        long taskStart = System.currentTimeMillis();
        for (long elapsed = 0L; elapsed < duration; elapsed = System.currentTimeMillis() - taskStart) {
          String s = "";
          for (int i=0; i<10; i++) s += "A10";
        }
      }
      //setResult("execution success for " + message);
      //System.out.println("execution success for "  + getId());
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}