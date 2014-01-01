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

package sample.helloworld;

import java.io.*;
import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;

/**
 * Runner for the hello world application.
 * @author Laurent Cohen
 */
public class HelloWorldRunner
{
  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      JPPFClient client = new JPPFClient();
      JPPFJob job = new JPPFJob();
      job.add(new HelloWorld());
      job.add(new HelloWorldAnnotated(), "hello message", 1);
      job.add(HelloWorldAnnotatedStatic.class, "hello message", 2);
      job.add(HelloWorldAnnotatedConstructor.class, "hello message", 3);
      job.add("helloPojoMethod", new HelloWorldPojo(), "hello message", 4);
      job.add("helloPojoStaticMethod", HelloWorldPojoStatic.class, "hello message", 5);
      job.add("HelloWorldPojoConstructor", HelloWorldPojoConstructor.class, "hello message", 6);
      job.add(new HelloWorldRunnable());
      job.add(new HelloWorldCallable());
      List<Task<?>> results = client.submitJob(job);
      System.out.println("********** Results: **********");
      for (Task task: results)
      {
        if (task.getThrowable() != null)
        {
          StringWriter sw = new StringWriter();
          task.getThrowable().printStackTrace(new PrintWriter(sw));
          System.out.println(sw.toString());
        }
        else System.out.println("" + task.getResult());
      }
      client.close();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    System.exit(0);
  }
}
