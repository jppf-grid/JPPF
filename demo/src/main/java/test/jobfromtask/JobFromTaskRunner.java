/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package test.jobfromtask;

import java.util.List;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class JobFromTaskRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JobFromTaskRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      final TypedProperties config = JPPFConfiguration.getProperties();
      final int poolSize = config.get(JPPFProperties.POOL_SIZE);
      // ensure we have at least 2 connections to the server
      if (poolSize < 2) config.set(JPPFProperties.POOL_SIZE, 2);
      jppfClient = new JPPFClient();
      print("Running Long Task demo with");
      final long start = System.nanoTime();
      final JPPFJob job = new JPPFJob();
      job.setName("source job");
      job.add(new SourceTask()).setId("source");
      job.getSLA().setMaxNodes(1);
      final List<Task<?>> results = jppfClient.submit(job);
      for (final Task<?> t: results) {
        final Throwable e = t.getThrowable();
        if (e != null) throw e;
        else print("task '" + t.getId() + "' result: " + t.getResult());
      }
      final long elapsed = DateTimeUtils.elapsedFrom(start);
      print("processing  performed in " + StringUtils.toStringDuration(elapsed));
    } catch (final Throwable e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Submit a job with a single {@link DestinationTask}.
   * @param input an input string.
   * @return a string showing successful execution.
   * @throws Exception if any error occurs.
   */
  public static String submitDestinationJob(final String input) throws Exception {
    final JPPFJob job = new JPPFJob();
    job.setName("destination job");
    job.add(new DestinationTask(input)).setId("destination task");
    job.getSLA().setMaxNodes(1);
    final List<Task<?>> result = jppfClient.submit(job);
    return (String) result.get(0).getResult();
  }

  /**
   * Print a message tot he log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }
}
