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
package sample.test.jppfcallable;

import java.util.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.StringUtils;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class JPPFCallableRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(JPPFCallableRunner.class);

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   * @throws Exception if an error is raised during the execution.
   */
  public static void main(final String...args) throws Exception {
    int nbTasks = 1;
    int nbJobs = 2;
    int maxChannels = 1;
    int size = 10;
    long time = 5000L;
    int nbRuns = 2;
    int nbClients = 2;
    JPPFClient[] jppfClient = new JPPFClient[nbClients];
    for (int n=0; n<nbClients; n++) {
      jppfClient[n] = new JPPFClient("Client 1");
      while (!jppfClient[n].hasAvailableConnection()) Thread.sleep(20L);
    }
    try {
      for (int run=0; run<=1; run++) {
        List<JPPFJob> jobList = new ArrayList<>();
        print("submitting " + nbJobs + " jobs with " + nbTasks + " tasks");
        for (int i=1; i<=nbJobs; i++) {
          for (int n=0; n<nbClients; n++) {
            String name = jppfClient[n].getUuid() + ":job-" + StringUtils.padLeft(String.valueOf(i), '0', 4);
            JPPFJob job = new JPPFJob();
            job.setName(name);
            job.getClientSLA().setMaxChannels(maxChannels);
            job.setBlocking(false);
            for (int j=1; j<=nbTasks; j++) job.add(new MyTask(time, jppfClient[n].getUuid())).setId(name + ":task-" + StringUtils.padLeft(String.valueOf(j), '0', 5));
            jobList.add(job);
            jppfClient[n].submitJob(job);
          }
        }
        for (JPPFJob job: jobList) {
          JPPFResultCollector coll = (JPPFResultCollector) job.getResultListener();
          List<Task<?>> results = coll.awaitResults();
          print("got results for job '" + job.getName() + "'");
        }
      }
    } finally {
      for (JPPFClient client: jppfClient) client.close();
    }
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
