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

package sample.dist.taskcommunication;

import java.io.*;
import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;

/**
 * Runner for the task communication sample..
 * @author Laurent Cohen
 */
public class MyTaskRunner
{
  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    JPPFClient client = null;
    try
    {
      client = new JPPFClient();
      JPPFJob job = new JPPFJob();
      job.add(new MyTask1());
      job.add(new MyTask2());
      List<Task<?>> results = client.submitJob(job);
      System.out.println("********** Results: **********");
      for (Task task: results)
      {
        System.out.println("result for task [" + task.getId() + "]: " + task.getResult());
        if (task.getThrowable() != null)
        {
          StringWriter sw = new StringWriter();
          task.getThrowable().printStackTrace(new PrintWriter(sw));
          System.out.println(sw.toString());
        }
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      client.close();
    }
  }
}
