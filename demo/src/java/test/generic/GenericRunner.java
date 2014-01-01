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

package test.generic;

import java.io.*;
import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

/**
 * 
 * @author lcohen
 */
public class GenericRunner
{
  /**
   * Run the test.
   * @param args not used.
   * @throws Exception if any error occurs.
   */
  public static void main(final String[] args) throws Exception
  {
    JPPFClient client = null;
    try
    {
      JPPFJob job = new JPPFJob();
      addConfiguredTasks(job);
      //job.addTask(new CallableTask());
      job.add(new LotsOfOutputTask(50000, 200));
      client = new JPPFClient();
      List<Task<?>> results = null;
      //results = client.submit(job);
      JPPFResultCollector collector = new JPPFResultCollector(job)
      {
        @Override
        public synchronized void resultsReceived(final TaskResultEvent event)
        {
          System.out.println("received " + event.getTasks().size() + " tasks");
          super.resultsReceived(event);
        }
      };
      job.setBlocking(false);
      job.setResultListener(collector);
      client.submitJob(job);
      results = collector.awaitResults();
      for (Task task: results)
      {
        System.out.println("*****************************************");
        System.out.println("Result: " + task.getResult());
        if (task.getThrowable() != null)
        {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          task.getThrowable().printStackTrace(pw);
          System.out.println("Exception: " + sw.toString());
          pw.close();
        }
      }
    }
    catch(Throwable t)
    {
      t.printStackTrace();
    }
    finally
    {
      if (client != null) client.close();
    }
    System.exit(0);
  }

  /**
   * Add tasks whose class names are read form a configuration file.
   * @param job the job to add the tasks to.
   * @throws Exception if any IO error occurs.
   */
  private static void addConfiguredTasks(final JPPFJob job) throws Exception
  {
    String path = JPPFConfiguration.getProperties().getString("task.list.file", null);
    if (path == null) return;
    String text = FileUtils.readTextFile(path);
    String[] classnames = text.split("\n");
    for (String s: classnames)
    {
      Class clazz = Class.forName(s);
      Object o = clazz.newInstance();
      job.add(o);
    }
  }
}
