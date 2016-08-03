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

package sample.test;

import org.jppf.scheduling.JPPFSchedule;

/**
 * Test task to check that the timeout period triggers the task's abortion.
 * @author Laurent Cohen
 */
public class TimeoutTask extends JPPFTestTask {
  /**
   * Initialize this task.
   */
  public TimeoutTask() {
    setTimeoutSchedule(new JPPFSchedule(2000L));
  }

  /**
   * Execute the task
   * @see java.lang.Runnable#run()
   */
  public void test() {
    try {
      Thread.sleep(5000L);
    } catch (@SuppressWarnings("unused") InterruptedException e) {
    }
  }

  /**
   * Called when this task times out.
   */
  @Override
  public void onTimeout() {
    setResult("this task timed out");
  }
}
