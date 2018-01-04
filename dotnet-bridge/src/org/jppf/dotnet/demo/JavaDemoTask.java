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

package org.jppf.dotnet.demo;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.ExceptionUtils;

/**
 * A simple JPPF task which waits for a specified duration to simulate an execution.
 * @author Laurent Cohen
 */
public class JavaDemoTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  public static final long serialVersionUID = 1L;
  /**
   * The time this task will wait to simulate an execution.
   */
  protected final long duration;

  /**
   * Initialize this task with a duration of zero.
   */
  public JavaDemoTask() {
    this.duration = 0L;
  }

  /**
   * Initialize this task with the specified duration in millis.
   * @param duration the time this task will wait to simulate an execution. 
   */
  public JavaDemoTask(final long duration) {
    this.duration = duration;
  }

  @Override
  public void run() {
    try {
      System.out.printf("starting %s, duration = %d ms%n", getClass().getSimpleName(), duration);
      if (duration > 0L) Thread.sleep(duration);
      final String message = "execution successful";
      setResult(message);
      System.out.println(message);
    } catch (final Throwable e) {
      setThrowable(e);
      System.out.printf("exception during execution: %s%n", ExceptionUtils.getStackTrace(e));
    }
  }
}
