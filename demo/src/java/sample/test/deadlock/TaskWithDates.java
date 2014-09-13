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

import java.util.Date;

import org.jppf.node.protocol.AbstractTask;

/**
 * A simple task used in the demo.
 */
public class TaskWithDates extends AbstractTask<String> {
  /**
   * A string message to transform and set as result of this task.
   */
  private final Date[] dates;

  /**
   * Initialize this task.
   */
  public TaskWithDates() {
    dates = new Date[100];
    for (int i=0; i<dates.length; i++) dates[i] = new Date();
  }

  @Override
  public void run() {
    try {
      for (int i=0; i<dates.length; i++) {
        assert dates[i] != null : "dates[" + i + "] is null";
      }
      Thread.sleep(100L);
      setResult("execution success");
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}