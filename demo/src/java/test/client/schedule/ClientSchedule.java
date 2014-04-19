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
package test.client.schedule;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

import sample.dist.tasklength.LongTask;


/**
 * Runner class for the non-SLA job timeout.
 * @author Laurent Cohen
 */
public class ClientSchedule {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ClientSchedule.class);

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String...args) {
    JPPFClient client = null;
    try {
      client = new MyJPPFClient();
      while (!client.hasAvailableConnection()) Thread.sleep(10L);
      TypedProperties config = JPPFConfiguration.getProperties();
      // this job lasts longer than its timeout, so ws can see it being canelled
      int length = 6_000;
      int nbTasks = 1;
      print("submitting job with " + StringUtils.singularPlural(nbTasks, "task") + " of length = " + length + " ms");
      JPPFJob job = new JPPFJob();
      job.setName("test of job schedule");
      for (int i=1; i<=nbTasks; i++) job.add(new LongTask(length)).setId("#" + i);
      job.setBlocking(false);
      job.getMetadata().setParameter(MyJobListener.EXPIRATION_KEY, 4000L);
      client.submitJob(job);
      print("job submitted, waiting for result ...");
      Thread.sleep(2000L);
      client.reset();
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      List<Task<?>> results = collector.awaitResults();
      print("got results: " + results.get(0).getResult());
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Print a message to the log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }
}
