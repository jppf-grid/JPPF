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
   * How long in millis this task will sleep to simulate code execution.
   */
  private final long duration;
  /**
   * Whether to simulate CPU usage.
   */
  private final boolean useCPU;
  /**
   * Some dummy data to simulate a specified memory footprint.
   */
  @SuppressWarnings("unused")
  private final byte[] dummyData;

  /**
   * Initialize this task.
   * @param message a string message to transform and set as result of this task.
   * @param options holds the configuration properties used for the tasks.
   */
  public MyTask(final String message, final TaskOptions options) {
    this.message = message;
    this.duration = options.taskDuration;
    this.useCPU = options.useCPU;
    dummyData = options.dataSize < 0 ? null : new byte[options.dataSize];
  }

  @Override
  public void run() {
    try {
      if (duration > 0L) {
        if (!useCPU) {
          synchronized(this) {
            wait(duration);
          }
        } else {
          for (long elapsed=0L, taskStart=System.nanoTime(); elapsed<duration; elapsed=(System.nanoTime()-taskStart) / 1_000_000L) {
            String s = "";
            for (int i=0; i<10; i++) s += "A10";
            s.toString();
          }
        }
      }
      setResult("execution success for " + message);
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}
