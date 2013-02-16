/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.gigaspaces.test;

import org.jppf.server.protocol.JPPFTask;

/**
 * Simple Hello World task that displays "Hello World" on a node's console.
 * @author Laurent Cohen
 */
public class HelloTask extends JPPFTask
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = -3172628037383355176L;

  /**
   * Duration of this task in milliseconds.
   */
  private long duration = 0L;

  /**
   * Default constructor.
   */
  public HelloTask()
  {
  }

  /**
   * Initialize this task with the specified duration.
   * @param duration duration of this task in milliseconds.
   */
  public HelloTask(final long duration)
  {
    this.duration = duration;
  }

  /**
   * Execute the task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    String s;
    try
    {
      if (duration <= 0L)
      {
        s = "Hello World";
      }
      else
      {
        Thread.sleep(duration);
        s = "successfully executed for " + duration + " milliseconds";
      }
    }
    catch(Exception e)
    {
      setException(e);
      s = "execution failed with exception message: " + e.getMessage();
    }
    System.out.println(s);
    setResult(s);
  }
}
