/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package sample;

import java.util.Date;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.Pair;

/**
 * This task implementation encapsulates base functionality for demonstration tasks, such as start and end event notifications.
 * @author Laurent Cohen
 */
public abstract class BaseDemoTask extends AbstractTask<Object>
{
  /**
   * Run the task with start and end event notifications.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    Pair<String, Date> p = new Pair<>("hellow", new Date());
    //fireNotification("start exec");
    try
    {
      doWork();
    }
    finally
    {
      //fireNotification("end exec");
    }
  }

  /**
   * Perform the actual work of this task.
   */
  public abstract void doWork();
}
