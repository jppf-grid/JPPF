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

package org.jppf.example.jobrecovery;

import org.jppf.server.protocol.JPPFTask;

/**
 * A sample task implementation, which waits for a specified time
 * then prints a message on the node console.
 * @author Laurent Cohen
 */
public class MyTask extends JPPFTask
{
  /**
   * How long this task waits before completing.
   */
  private final long duration;

  /**
   * Initialize this task with the specified duration and id.
   * @param duration the duration in milliseconds.
   * @param id an id assigned to this task.
   */
  public MyTask(final long duration, final int id)
  {
    this.duration = duration;
    setId("" + id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      Thread.sleep(duration);
      System.out.println("task " + getId() + " completed successfully");
      setResult("successful completion");
    }
    catch (Exception e)
    {
      System.out.println("task " + getId() + " completed with error [" + e.getClass().getName() + ": " + e.getMessage() + ']');
      setThrowable(e);
    }
  }
}
