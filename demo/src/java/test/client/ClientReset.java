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
package test.client;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;


/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ClientReset
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ClientReset.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient client = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      client = new JPPFClient();
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      TypedProperties config = JPPFConfiguration.getProperties();
      int length = 10000;
      int nbTasks = 1;
      print("Running Long Task demo with " + nbTasks + " tasks of length = " + length + " ms");
      JPPFJob job = new JPPFJob();
      job.setName("test of client reset");
      for (int i=1; i<=nbTasks; i++) job.add(new LongTask(length)).setId("#" + i);
      JPPFResultCollector collector = new JPPFResultCollector(job);
      job.setResultListener(collector);
      job.setBlocking(false);
      client.submitJob(job);
      print("job submitted");
      for (int i=1; i<=5; i++) {
        Thread.sleep(4000L);
        print("client reset #" + i);
        client.reset();
      }
      print("waiting for result ...");
      List<Task<?>> results = collector.awaitResults();
      print("got results: " + results.get(0).getResult());
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (client != null) client.close();
    }
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg)
  {
    log.info(msg);
    System.out.println(msg);
  }
}
