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

package org.jppf.example.adaptivegrid;

import org.jppf.node.protocol.AbstractTask;

/**
 * A simple JPPF task which waits for a specified duration to simulate an execution.
 * @author Laurent Cohen
 */
public class SimpleTask extends AbstractTask<String> {
  /**
   * The time this task will wait to simulate an execution.
   */
  private final long duration;

  /**
   * Initialize this task with the specified duration in millis.
   * @param duration the time this task will wait to simulate an execution. 
   */
  public SimpleTask(final long duration) {
    this.duration = duration;
  }

  @Override
  public void run() {
    try {
      if (duration > 0L) Thread.sleep(duration);
      setResult("execution successful");
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}
