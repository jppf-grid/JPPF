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
package test.annotated;

import org.jppf.node.protocol.AbstractTask;

/**
 * This task is for testing the network transfer of task with various data sizes.
 * @author Laurent Cohen
 */
public class AnnotatedTask extends AbstractTask<String> {
  /**
   * The time this task will sleep.
   */
  private long time = 0;

  /**
   * The task id.
   */
  private int id = 0;

  /**
   * Initialize this task with a byte array of the specified size.
   * The array is created at construction time and passed on to the node.
   * @param time .
   * @param id .
   */
  public AnnotatedTask(final long time, final int id) {
    this.time = time;
    this.id = id;
  }

  /**
   * Perform the multiplication of a matrix row by another matrix.
   * @see sample.BaseDemoTask#doWork()
   */
  @Override
  public void run() {
    try {
      if (time > 0L) Thread.sleep(time);
      for (int i = 0; i < 3; i++) {
        getClass().getClassLoader().getResources("META-INF/services/xxx");
      }
      setResult("task #" + id + " execution successful");
    } catch (Exception e) {
      setThrowable(e);
      setResult("task #" + id + ' ' + e.getMessage());
    }
    //System.out.print(StringUtils.padRight(result, ' ', 250));
  }
}
