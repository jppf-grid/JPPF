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

package test.priority;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task simply prints a message.
 * @author Laurent Cohen
 */
public class PrioritizedTask extends JPPFTask
{
  /**
   * The time to wait.
   */
  private int priority = 0;

  /**
   * Initialize this task with the specified priority.
   * @param priority the task priority.
   */
  public PrioritizedTask(final int priority)
  {
    this.priority = priority;
  }

  /**
   * Execute this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    System.out.println("executing task with priority " + priority);
  }
}
