/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package sample.dist.xstream;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;

/**
 * Runner class for the XStream demo.
 * @author Laurent Cohen
 */
public class XstreamRunner
{
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class, submits the tasks to the JPPF grid.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      jppfClient = new JPPFClient();
      long start = System.nanoTime();
      JPPFJob job = new JPPFJob();
      Person person = new Person("John", "Smith", new PhoneNumber(123, "456-7890"));
      job.add(new XstreamTask(person));
      // submit the tasks for execution
      List<Task<?>> results = jppfClient.submitJob(job);
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      System.out.println("Task executed in " + elapsed + " ms");
      Task<?> result = results.get(0);
      if (result.getThrowable() != null) throw result.getThrowable();
      System.out.println("Task execution result: " + result.getResult());
    }
    catch(Throwable e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }
}
