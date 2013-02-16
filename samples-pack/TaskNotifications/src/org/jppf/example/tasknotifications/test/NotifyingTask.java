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
package org.jppf.example.tasknotifications.test;

import org.jppf.example.tasknotifications.startup.TaskNotifier;
import org.jppf.server.protocol.JPPFTask;

/**
 * Example of a JPPF task that sends status notifications during its execution.
 * @author Laurent Cohen
 */
public class NotifyingTask extends JPPFTask
{
  /**
   * Initialize this task with the specified id.
   * @param id the task id.
   */
  public NotifyingTask(final String id)
  {
    setId(id);
  }

  /**
   * This method contains the code that will be executed by a node.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      for (int i=1; i<=10; i++)
      {
        Thread.sleep(1000L);
        // send a status notification
        TaskNotifier.addNotification("Task '" + getId() + "' : reached stage " + i);
      }
      setResult("the execution was performed successfully");
    }
    catch(InterruptedException e)
    {
      setException(e);
    }
  }
}
