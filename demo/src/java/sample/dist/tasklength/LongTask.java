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
package sample.dist.tasklength;

import org.jppf.node.protocol.AbstractTask;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation.
 * @author Laurent Cohen
 */
public class LongTask extends AbstractTask<String> {
  /**
   * Determines how long this task will run.
   */
  private final long taskLength;
  /**
   * Determines this task's behavior: false if it should just sleep during its allocated time, or true if it should
   * do some make-do work that uses the cpu.
   */
  private final boolean useCPU;

  /**
   * Default constructor.
   */
  public LongTask() {
    this(0L, false);
  }

  /**
   * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
   * @param taskLength determines how long this task will run.
   */
  public LongTask(final long taskLength) {
    this(taskLength, false);
  }

  /**
   * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
   * @param taskLength determines how long this task will run.
   * @param useCPU determines whether this task should just sleep during its allocated time or do some cpu-intensive work.
   */
  public LongTask(final long taskLength, final boolean useCPU) {
    this.taskLength = taskLength;
    this.useCPU = useCPU;
  }

  @Override
  public void run() {
    long taskStart = System.nanoTime();
    long elapsed = 0L;
    try {
      if (useCPU) {
        for (; elapsed < taskLength; elapsed = (System.nanoTime() - taskStart) / 1_000_000L) {
          String s = "";
          for (int i=0; i<10; i++) s += "A10";
        }
      } else {
        if (taskLength > 0) Thread.sleep(taskLength);
        elapsed = (System.nanoTime() - taskStart) / 1_000_000L;
      }
      String result = "task '" + getId() + "' has run for " + elapsed + " ms";
      setResult(result);
      System.out.println(result);
    } catch(InterruptedException e) {
      setResult(e.getClass().getName() + " : " + e.getMessage());
    }
  }

  @Override
  public void onCancel() {
    String s = "task " + getId() + " has been cancelled";
    setResult(s);
    System.out.println(s);
  }
}
